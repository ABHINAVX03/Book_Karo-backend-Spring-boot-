package com.codingshuttle.project.uber.uberApp.controllers;

import com.codingshuttle.project.uber.uberApp.dto.OtpRequestDto;
import com.codingshuttle.project.uber.uberApp.dto.OtpVerifyDto;
import com.codingshuttle.project.uber.uberApp.services.CaptchaVerificationService;
import com.codingshuttle.project.uber.uberApp.services.OtpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;
    private final CaptchaVerificationService captchaVerificationService;

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(@Valid @RequestBody OtpRequestDto otpRequestDto, HttpServletRequest request) {
        captchaVerificationService.assertValidCaptcha(request);
        String otp = otpService.sendOtp(otpRequestDto.getPhoneNumber());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Verification code generated",
                "otp", otp
        ));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Boolean>> verifyOtp(@Valid @RequestBody OtpVerifyDto otpVerifyDto, HttpServletRequest request) {
        captchaVerificationService.assertValidCaptcha(request);
        boolean isValid = otpService.verifyOtp(otpVerifyDto.getPhoneNumber(), otpVerifyDto.getOtp());
        return ResponseEntity.ok(Map.of("valid", isValid));
    }
}
