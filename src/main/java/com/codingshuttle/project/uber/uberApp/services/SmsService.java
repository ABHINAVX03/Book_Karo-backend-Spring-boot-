package com.codingshuttle.project.uber.uberApp.services;

public interface SmsService {
    void sendSms(String phoneNumber, String message);
}
