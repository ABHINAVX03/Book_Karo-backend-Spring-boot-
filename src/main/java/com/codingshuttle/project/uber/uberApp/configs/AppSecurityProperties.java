package com.codingshuttle.project.uber.uberApp.configs;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    @NotBlank
    private String jwtSecret;

    @NotBlank
    private String jwtIssuer = "bookcar-api";

    @NotBlank
    private String jwtAudience = "bookcar-clients";

    @Min(15)
    private long accessTokenMinutes = 60;

    @Min(1)
    private long refreshTokenDays = 30;

    @Min(1)
    private int loginMaxAttempts = 5;

    @Min(1)
    private int otpMaxAttempts = 5;

    @Min(1)
    private int otpMaxSendsPerWindow = 3;

    @Min(1)
    private int refreshMaxAttemptsPerMinute = 20;

    @Min(1)
    private int loginMaxAttemptsPerMinute = 10;

    @Min(1)
    private int otpMaxRequestsPerMinute = 6;

    @Min(1)
    private int otpVerifyMaxRequestsPerMinute = 10;

    private Duration lockoutDuration = Duration.ofMinutes(15);

    private Duration otpExpiry = Duration.ofMinutes(5);

    private Duration verifiedPhoneWindow = Duration.ofMinutes(10);

    private Duration authSessionRetention = Duration.ofDays(30);

    private boolean refreshCookieSecure = true;

    @NotBlank
    private String refreshCookieSameSite = "None";

    private boolean accessCookieSecure = true;

    @NotBlank
    private String accessCookieSameSite = "None";

    private boolean captchaEnabled = false;

    private List<String> allowedOrigins = List.of();

    @AssertTrue(message = "JWT secret must be at least 32 characters")
    public boolean isJwtSecretLongEnough() {
        return jwtSecret != null && jwtSecret.trim().length() >= 32;
    }
}
