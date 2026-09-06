package com.anjertech.auth.auth_app_backend.exception;

//this class can only catch the error and display the cause at the internal terminal and not globally
public class ResourceNotFoundException extends RuntimeException{

    public ResourceNotFoundException(String message){
        super(message);
    }

    public ResourceNotFoundException(){
        super("Resources not found !");
    }
}
