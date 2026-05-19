package com.codingshuttle.project.uber.uberApp.services;

import jakarta.servlet.http.HttpServletRequest;

public interface CaptchaVerificationService {
    void assertValidCaptcha(HttpServletRequest request);
}
