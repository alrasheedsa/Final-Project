package com.example.fproject.Api;

public class ApiException extends RuntimeException{
    public ApiException(String message){
        super(message);
    }
}