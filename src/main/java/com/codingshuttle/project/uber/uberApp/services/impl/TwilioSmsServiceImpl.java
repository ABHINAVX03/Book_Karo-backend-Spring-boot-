package com.codingshuttle.project.uber.uberApp.services.impl;

import com.codingshuttle.project.uber.uberApp.services.SmsService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
@Slf4j
public class TwilioSmsServiceImpl implements SmsService {

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.phone-number:}")
    private String twilioPhoneNumber;

    @PostConstruct
    public void initTwilio() {
        if (!accountSid.isEmpty() && !authToken.isEmpty()) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio initialized successfully");
        } else {
            log.warn("Twilio credentials missing. SMS will be logged to console instead of sent.");
        }
    }

    @Override
    public void sendSms(String phoneNumber, String message) {
        if (!accountSid.isEmpty() && !authToken.isEmpty() && !twilioPhoneNumber.isEmpty()) {
            try {
                Message.creator(
                        new PhoneNumber(phoneNumber),
                        new PhoneNumber(twilioPhoneNumber),
                        message
                ).create();
                log.info("SMS sent to {}: {}", phoneNumber, message);
            } catch (Exception e) {
                log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
            }
        } else {
            log.info("[MOCK SMS] To: {}, Message: {}", phoneNumber, message);
        }
    }
}
