package com.anjertech.auth.auth_app_backend.service;

import com.anjertech.auth.auth_app_backend.dto.UserDto;

import java.util.UUID;

public interface UserService {

    UserDto creatUser(UserDto userDto);
    UserDto getUserByEmail(String email);
    UserDto updateUser(UserDto userDto, String userId);
    void deleteUser(String userId);
    UserDto getUserById(String userId);
    Iterable<UserDto> getAllUsers();
}
