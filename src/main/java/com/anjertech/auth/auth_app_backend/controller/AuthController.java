package com.anjertech.auth.auth_app_backend.controller;

import com.anjertech.auth.auth_app_backend.dto.LoginRequest;
import com.anjertech.auth.auth_app_backend.dto.RefreshTokenRequest;
import com.anjertech.auth.auth_app_backend.dto.TokenResponse;
import com.anjertech.auth.auth_app_backend.dto.UserDto;
import com.anjertech.auth.auth_app_backend.entity.RefreshToken;
import com.anjertech.auth.auth_app_backend.entity.User;
import com.anjertech.auth.auth_app_backend.repository.RefreshTokenRepository;
import com.anjertech.auth.auth_app_backend.repository.UserRepository;
import com.anjertech.auth.auth_app_backend.security.CookieService;
import com.anjertech.auth.auth_app_backend.security.JwtService;
import com.anjertech.auth.auth_app_backend.service.AuthService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {
    private final AuthService authService;

    private final AuthenticationManager authenticationManager; //this might flag error, just create a bean of authenticationManager at the securityConfig
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final ModelMapper mapper;



    //login method

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response){

        //authenticate
        Authentication authentication = authenticate(loginRequest);
        User user = userRepository.findByEmail(loginRequest.email()).orElseThrow(()-> new BadCredentialsException("Invalid Username or Password"));
        if (!user.isEnabled()){
            throw new DisabledException("User is disable");
        }

        //for refresh token
        String jti = UUID.randomUUID().toString();
        var refreshTokenOb = RefreshToken.builder()
                .jti(jti)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .revoked(false)
                .build();

        //refresh token save --information
        refreshTokenRepository.save(refreshTokenOb);


        //generate accessToken
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, refreshTokenOb.getJti()); //added during implementing for refreshToken which the value "refreshToken" is passed into the tokenResponse.of() replacing the empty "" for refreshToken

        //use cookie service to attach refresh token in cookie NB: add HttpServletResponse inside the login method signature i.e. ()
        cookieService.attachRefreshCookie(response,refreshToken, (int)jwtService.getRefreshTtlSeconds());
        cookieService.addNoStoreHeaders(response);



        TokenResponse tokenResponse = TokenResponse.of(accessToken, refreshToken, jwtService.getAccessTtlSeconds(), mapper.map(user, UserDto.class));
        return ResponseEntity.ok(tokenResponse);
    }

    private Authentication authenticate(LoginRequest loginRequest){
        try {
            return authenticationManager.authenticate (new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password()));
        } catch (Exception e){
            throw new BadCredentialsException("Invalid Username or password !!");
        }
    }


    //API for renewing access and refresh tokens.::::::::::::::::::::::::::::::::
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest body,
            HttpServletResponse response,
            HttpServletRequest request
    ){

        String refreshToken = readRefreshTokenFromRequest(body, request).orElseThrow(()-> new BadCredentialsException("Refresh Token is missing !!"));
        if (!jwtService.isRefreshToken(refreshToken)){
            throw new BadCredentialsException("Invalid Refresh Token type");
        }
        String jti = jwtService.getJti(refreshToken);
        UUID userId = jwtService.getUserId(refreshToken);
        RefreshToken storedRefreshToken = refreshTokenRepository.findByJti(jti).orElseThrow( () -> new BadCredentialsException("Invalid Refresh Token"));

        if (storedRefreshToken.isRevoked()){
            throw new BadCredentialsException("Refresh Token is expired or revoked");
        }

        if (storedRefreshToken.getExpiresAt().isBefore(Instant.now())){
            throw new BadCredentialsException("Refresh token expired");
        }

        if (!storedRefreshToken.getUser().getId().equals(userId)){
            throw  new BadCredentialsException("Refresh token does not belong to this user");
        }

        //refresh token ka rotate
        storedRefreshToken.setRevoked(true);
        String newJti = UUID.randomUUID().toString();
        storedRefreshToken.setReplacedByToken(newJti);
        refreshTokenRepository.save(storedRefreshToken);

        User user = storedRefreshToken.getUser();


        var newRefreshTokenOb = RefreshToken.builder()
                .jti(newJti)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(newRefreshTokenOb);
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user, newRefreshTokenOb.getJti());
        cookieService.attachRefreshCookie(response, newRefreshToken, (int) jwtService.getRefreshTtlSeconds());
        cookieService.addNoStoreHeaders(response);
        return ResponseEntity.ok(TokenResponse.of(newAccessToken, newRefreshToken, jwtService.getAccessTtlSeconds(), mapper.map(user, UserDto.class )));


    }


    //logout
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response){
        readRefreshTokenFromRequest(null,request).ifPresent(token -> {
            try{
                if (jwtService.isRefreshToken(token)){
                    String jti = jwtService.getJti(token);
                    refreshTokenRepository.findByJti(jti).ifPresent(rt -> {
                        rt.setRevoked(true);
                        refreshTokenRepository.save(rt);
                    });
                }
            }catch (JwtException ignored){

            }
        });

        //use cookieUtil (some behaviour)
        cookieService.clearRefreshCookie(response);
        cookieService.addNoStoreHeaders(response);
        SecurityContextHolder.clearContext();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    //this method will read refresh token from request header or body
    private Optional<String> readRefreshTokenFromRequest(RefreshTokenRequest body, HttpServletRequest request) {
//        1. prefer reading refresh token from cookie header
        if (request.getCookies()!=null){
            Optional<String> fromCookie = Arrays.stream( request.getCookies())
                    .filter( c -> cookieService.getRefreshTokenCookieName().equals(c.getName()))
                    .map(Cookie::getValue)
                    .filter(v -> !v.isBlank())
                    .findFirst();

            if (fromCookie.isPresent()) {
                return fromCookie;
            }

        }

//        2. if header not available, read from the body
        if (body!=null && body.refreshToken()!=null && !body.refreshToken().isBlank()){
            return Optional.of(body.refreshToken());
        }

//        3. custom header
        String refreshHeader = request.getHeader("X-Refresh=Token");
        if (refreshHeader != null && !refreshHeader.isBlank()){
            return Optional.of(refreshHeader.trim());
        }

//        // Authorization = Bearer <Token>
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer ", 0, 7  )){
            String candidate = authHeader.substring(7).trim();
            if (!candidate.isEmpty()){
                try{
                    if (jwtService.isRefreshToken(candidate)){
                        return Optional.of(candidate);
                    }
                } catch (Exception ignored){

                }
            }
        }

        return Optional.empty();

    }


    // register method

    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser(@RequestBody UserDto userDto){
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerUser(userDto));
    }
}
