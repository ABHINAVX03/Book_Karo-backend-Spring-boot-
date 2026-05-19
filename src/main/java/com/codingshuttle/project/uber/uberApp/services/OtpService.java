package com.codingshuttle.project.uber.uberApp.services;

import com.codingshuttle.project.uber.uberApp.configs.AppSecurityProperties;
import com.codingshuttle.project.uber.uberApp.entities.OtpChallenge;
import com.codingshuttle.project.uber.uberApp.repositories.OtpChallengeRepository;
import com.codingshuttle.project.uber.uberApp.security.TokenHashService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class OtpService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SmsService smsService;
    private final OtpChallengeRepository otpChallengeRepository;
    private final TokenHashService tokenHashService;
    private final AppSecurityProperties appSecurityProperties;

    @Transactional
    public void sendOtp(String phoneNumber) {
        LocalDateTime now = LocalDateTime.now();
        OtpChallenge challenge = otpChallengeRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> OtpChallenge.builder()
                        .phoneNumber(phoneNumber)
                        .sendCount(0)
                        .failedAttempts(0)
                        .build());

        if (challenge.getBlockedUntil() != null && challenge.getBlockedUntil().isAfter(now)) {
            throw new IllegalStateException("OTP requests are temporarily locked. Please try again later.");
        }

        if (challenge.getSendWindowStartedAt() == null
                || challenge.getSendWindowStartedAt().plusMinutes(10).isBefore(now)) {
            challenge.setSendWindowStartedAt(now);
            challenge.setSendCount(0);
        }

        if (challenge.getSendCount() >= appSecurityProperties.getOtpMaxSendsPerWindow()) {
            challenge.setBlockedUntil(now.plusMinutes(10));
            otpChallengeRepository.save(challenge);
            throw new IllegalStateException("OTP request limit reached. Please try again later.");
        }

        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        challenge.setOtpHash(tokenHashService.hash(otp));
        challenge.setExpiresAt(now.plus(appSecurityProperties.getOtpExpiry()));
        challenge.setVerifiedUntil(null);
        challenge.setFailedAttempts(0);
        challenge.setSendCount(challenge.getSendCount() + 1);

        otpChallengeRepository.save(challenge);
        smsService.sendSms(phoneNumber, "Your BookCar verification code is: " + otp + ". It expires in 5 minutes.");
        log.info("OTP sent to phoneNumber={} sendCount={}", phoneNumber, challenge.getSendCount());
    }

    @Transactional
    public boolean verifyOtp(String phoneNumber, String otp) {
        LocalDateTime now = LocalDateTime.now();
        OtpChallenge challenge = otpChallengeRepository.findByPhoneNumber(phoneNumber).orElse(null);
        if (challenge == null) {
            return false;
        }

        if (challenge.getBlockedUntil() != null && challenge.getBlockedUntil().isAfter(now)) {
            return false;
        }

        if (challenge.getExpiresAt() == null || challenge.getExpiresAt().isBefore(now)) {
            return false;
        }

        if (tokenHashService.hash(otp).equals(challenge.getOtpHash())) {
            challenge.setVerifiedUntil(now.plus(appSecurityProperties.getVerifiedPhoneWindow()));
            challenge.setOtpHash(tokenHashService.hash(generateRideOtp()));
            challenge.setFailedAttempts(0);
            challenge.setBlockedUntil(null);
            otpChallengeRepository.save(challenge);
            log.info("OTP verified for phoneNumber={}", phoneNumber);
            return true;
        }

        int failedAttempts = (challenge.getFailedAttempts() == null ? 0 : challenge.getFailedAttempts()) + 1;
        challenge.setFailedAttempts(failedAttempts);
        if (failedAttempts >= appSecurityProperties.getOtpMaxAttempts()) {
            challenge.setBlockedUntil(now.plusMinutes(15));
        }
        otpChallengeRepository.save(challenge);
        log.warn("OTP verification failed for phoneNumber={} failedAttempts={}", phoneNumber, failedAttempts);
        return false;
    }

    @Transactional(readOnly = true)
    public boolean isPhoneNumberVerified(String phoneNumber) {
        return otpChallengeRepository.findByPhoneNumber(phoneNumber)
                .map(challenge -> challenge.getVerifiedUntil() != null
                        && challenge.getVerifiedUntil().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    @Transactional
    public void clearVerification(String phoneNumber) {
        otpChallengeRepository.findByPhoneNumber(phoneNumber).ifPresent(challenge -> {
            challenge.setVerifiedUntil(null);
            otpChallengeRepository.save(challenge);
        });
    }

    public String generateRideOtp() {
        return String.format("%04d", SECURE_RANDOM.nextInt(10_000));
    }
}
