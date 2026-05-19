package com.codingshuttle.project.uber.uberApp.services.impl;

import com.codingshuttle.project.uber.uberApp.configs.AppSecurityProperties;
import com.codingshuttle.project.uber.uberApp.services.CaptchaVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class HeaderCaptchaVerificationService implements CaptchaVerificationService {

    private static final String CAPTCHA_HEADER = "X-Captcha-Token";

    private final AppSecurityProperties appSecurityProperties;

    @Override
    public void assertValidCaptcha(HttpServletRequest request) {
        if (!appSecurityProperties.isCaptchaEnabled()) {
            return;
        }
        String captchaToken = request.getHeader(CAPTCHA_HEADER);
        if (!StringUtils.hasText(captchaToken)) {
            throw new IllegalArgumentException("Captcha token is required.");
        }
    }
}
