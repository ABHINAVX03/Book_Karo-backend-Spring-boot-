package com.codingshuttle.project.uber.uberApp.advices;

import org.springframework.http.HttpStatus;

import java.util.List;

public class ApiError {

    private HttpStatus status;
    private String message;
    private List<String> subErrors;

    public ApiError() {
    }

    public ApiError(HttpStatus status, String message, List<String> subErrors) {
        this.status = status;
        this.message = message;
        this.subErrors = subErrors;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getSubErrors() {
        return subErrors;
    }

    public void setSubErrors(List<String> subErrors) {
        this.subErrors = subErrors;
    }

}
