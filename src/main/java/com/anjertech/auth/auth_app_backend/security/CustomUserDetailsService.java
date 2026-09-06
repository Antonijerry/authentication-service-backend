package com.anjertech.auth.auth_app_backend.security;

import com.anjertech.auth.auth_app_backend.entity.User;
import com.anjertech.auth.auth_app_backend.exception.ResourceNotFoundException;
import com.anjertech.auth.auth_app_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username).orElseThrow( () -> new BadCredentialsException("Invalid Email or Password!!"));
    }
}
