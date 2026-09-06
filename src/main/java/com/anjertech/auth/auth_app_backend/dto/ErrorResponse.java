package com.anjertech.auth.auth_app_backend.dto;

import org.springframework.http.HttpStatus;

//this helps to define the json data response from not found exception like in the post man
//if data not found or there is error it will print out a json data with undefined values
public record ErrorResponse(
        String message,
        HttpStatus status,
        int statusCode

){

}
