package com.codingshuttle.project.uber.uberApp.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InvalidRideStatusException extends RuntimeException {
    public InvalidRideStatusException(String message) {
        super(message);
    }
}