package com.codingshuttle.project.uber.uberApp.services.sms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConsoleSmsProvider implements SmsProvider {

    private final Environment environment;

    @Override
    public void sendVerificationCode(String phoneNumber, String otp) {
        if (isDevelopmentMode()) {
            log.info("[DEV OTP] phoneNumber={} otp={}", phoneNumber, otp);
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
