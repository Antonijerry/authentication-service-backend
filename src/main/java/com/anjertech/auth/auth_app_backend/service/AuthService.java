package com.anjertech.auth.auth_app_backend.service;

import com.anjertech.auth.auth_app_backend.dto.UserDto;

public interface AuthService {
    UserDto registerUser(UserDto userDto);

    //login user
}
