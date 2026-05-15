package com.codingshuttle.project.uber.uberApp.services;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OtpService {

    private final SmsService smsService;
    private final Map<String, String> otpCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> verifiedPhoneNumbers = new ConcurrentHashMap<>();

    public void sendOtp(String phoneNumber) {
        String otp = String.format("%06d", new Random().nextInt(1000000));
        otpCache.put(phoneNumber, otp);
        
        String message = "Your BookCar verification code is: " + otp + ". It expires in 5 minutes.";
        smsService.sendSms(phoneNumber, message);
        
        log.info("OTP generated for {}: {}", phoneNumber, otp);
        
        // Remove OTP after 5 minutes
        new Thread(() -> {
            try {
                Thread.sleep(5 * 60 * 1000);
                otpCache.remove(phoneNumber);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public boolean verifyOtp(String phoneNumber, String otp) {
        String cachedOtp = otpCache.get(phoneNumber);
        if (cachedOtp != null && cachedOtp.equals(otp)) {
            otpCache.remove(phoneNumber);
            verifiedPhoneNumbers.put(phoneNumber, true);
            
            // Remove from verified list after 10 minutes
            new Thread(() -> {
                try {
                    Thread.sleep(10 * 60 * 1000);
                    verifiedPhoneNumbers.remove(phoneNumber);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
            return true;
        }
        return false;
    }

    public boolean isPhoneNumberVerified(String phoneNumber) {
        return verifiedPhoneNumbers.getOrDefault(phoneNumber, false);
    }

    public void clearVerification(String phoneNumber) {
        verifiedPhoneNumbers.remove(phoneNumber);
    }

    public String generateRideOtp() {
        return String.format("%04d", new Random().nextInt(10000));
    }
}
