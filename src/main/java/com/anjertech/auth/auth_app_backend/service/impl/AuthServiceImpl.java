package com.anjertech.auth.auth_app_backend.service.impl;

import com.anjertech.auth.auth_app_backend.dto.UserDto;
import com.anjertech.auth.auth_app_backend.service.AuthService;
import com.anjertech.auth.auth_app_backend.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    @Override
    public UserDto registerUser(UserDto userDto) {
        //add logic like verify email, verify password, default roles etc
        userDto.setPassword(passwordEncoder.encode(userDto.getPassword())); //this line was added during security config
        return userService.creatUser(userDto);
    }
}
