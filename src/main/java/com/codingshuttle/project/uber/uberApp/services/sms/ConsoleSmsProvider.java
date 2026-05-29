package com.codingshuttle.project.uber.uberApp.services.sms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConsoleSmsProvider implements SmsProvider {

    private final Environment environment;

    @Value("${app.otp.log-enabled:false}")
    private boolean otpLogEnabled;

    @Override
    public void sendVerificationCode(String phoneNumber, String otp) {
        if (otpLogEnabled || isDevelopmentMode()) {
            log.info("[OTP DEBUG] phoneNumber={} otp={}", phoneNumber, otp);
            return;
        }
        log.warn("No SMS provider is configured. OTP generated for phoneNumber={} but was not sent.", phoneNumber);
    }

    private boolean isDevelopmentMode() {
        String[] profiles = environment.getActiveProfiles();
        return profiles.length == 0 || Arrays.stream(profiles)
                .anyMatch(profile -> profile.equalsIgnoreCase("dev") || profile.equalsIgnoreCase("test"));
    }
}
