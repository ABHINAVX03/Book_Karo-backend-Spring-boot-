package com.codingshuttle.project.uber.uberApp.dto;

import lombok.Data;

@Data
public class OtpVerifyDto {
    private String phoneNumber;
    private String otp;
}
