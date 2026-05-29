package com.codingshuttle.project.uber.uberApp.configs;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StartupConfigurationValidator {

    private static final Logger log = LoggerFactory.getLogger(StartupConfigurationValidator.class);

    private final AppSecurityProperties appSecurityProperties;
    private final Environment environment;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    // Optional service sentinels — the placeholder values signal "not configured"
    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${cloudinary.cloud-name:}")
    private String cloudinaryCloudName;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @PostConstruct
    public void validate() {
        // ── Required: database ──────────────────────────────────────────────
        requireConfigured("spring.datasource.url",      datasourceUrl);
        requireConfigured("spring.datasource.username", datasourceUsername);
        requireConfigured("spring.datasource.password", datasourcePassword);

        // ── Required: JWT secret (belt-and-suspenders — @NotBlank + @AssertTrue
        //    on AppSecurityProperties already validates this, but we want a clear
        //    startup error message rather than a cryptic constraint violation) ──
        String jwtSecret = appSecurityProperties.getJwtSecret();
        if (!StringUtils.hasText(jwtSecret)) {
            throw new IllegalStateException(
                    "JWT_SECRET_KEY must be set. " +
                    "Set the JWT_SECRET_KEY environment variable to a random string of at least 32 characters.");
        }
        if (jwtSecret.trim().length() < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET_KEY is too short (" + jwtSecret.trim().length() + " chars). " +
                    "It must be at least 32 characters.");
        }

        // ── Required: CORS origins ──────────────────────────────────────────
        if (appSecurityProperties.getAllowedOrigins() == null ||
                appSecurityProperties.getAllowedOrigins().isEmpty() ||
                appSecurityProperties.getAllowedOrigins().stream().allMatch(s -> !StringUtils.hasText(s))) {
            throw new IllegalStateException(
                    "APP_CORS_ALLOWED_ORIGINS must be set. " +
                    "Provide a comma-separated list of allowed frontend origins (e.g. https://app.example.com).");
        }

        // ── Production guard: no localhost in CORS ──────────────────────────
        boolean prodProfile = Arrays.stream(environment.getActiveProfiles())
                .anyMatch("prod"::equalsIgnoreCase);
        if (prodProfile && appSecurityProperties.getAllowedOrigins().stream()
                .map(String::trim)
                .anyMatch(origin -> origin.contains("localhost") || origin.contains("127.0.0.1"))) {
            throw new IllegalStateException("Production CORS origins must not include localhost entries.");
        }

        // ── Startup summary ─────────────────────────────────────────────────
        logStartupSummary();
    }

    private void logStartupSummary() {
        Map<String, Boolean> services = new LinkedHashMap<>();
        services.put("Database",   StringUtils.hasText(datasourceUrl));
        services.put("JWT",        StringUtils.hasText(appSecurityProperties.getJwtSecret()));
        services.put("CORS",       !appSecurityProperties.getAllowedOrigins().isEmpty());
        services.put("Razorpay",   isOptionalConfigured(razorpayKeyId, "MISSING_RAZORPAY_KEY_ID"));
        services.put("Cloudinary", StringUtils.hasText(cloudinaryCloudName));
        services.put("SMS Provider", true);
        services.put("Email",      StringUtils.hasText(mailUsername));

        log.info("=== Startup Configuration Summary ===");
        services.forEach((name, configured) -> {
            if (configured) {
                log.info("  [OK]      {}", name);
            } else {
                log.warn("  [MISSING] {} — feature will be unavailable", name);
            }
        });
        log.info("=====================================");
    }

    /**
     * Returns true if the value is non-blank and does not equal the sentinel
     * placeholder used in application-prod.properties for optional services.
     */
    private boolean isOptionalConfigured(String value, String sentinel) {
        return StringUtils.hasText(value) && !value.equals(sentinel);
    }

    private void requireConfigured(String propertyName, String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(propertyName + " must be configured before startup.");
        }
    }
}
