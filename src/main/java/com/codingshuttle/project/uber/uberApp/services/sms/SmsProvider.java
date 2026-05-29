package com.codingshuttle.project.uber.uberApp.services.sms;

public interface SmsProvider {
    void sendVerificationCode(String phoneNumber, String otp);
}
