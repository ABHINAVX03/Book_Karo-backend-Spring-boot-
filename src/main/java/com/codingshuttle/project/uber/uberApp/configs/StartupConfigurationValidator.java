package com.codingshuttle.project.uber.uberApp.configs;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class StartupConfigurationValidator {

    private final AppSecurityProperties appSecurityProperties;
    private final Environment environment;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    @PostConstruct
    public void validate() {
        requireConfigured("spring.datasource.url", datasourceUrl);
        requireConfigured("spring.datasource.username", datasourceUsername);
        requireConfigured("spring.datasource.password", datasourcePassword);

        boolean prodProfile = Arrays.stream(environment.getActiveProfiles())
                .anyMatch("prod"::equalsIgnoreCase);
        if (prodProfile && appSecurityProperties.getAllowedOrigins().stream()
                .map(String::trim)
                .anyMatch(origin -> origin.contains("localhost") || origin.contains("127.0.0.1"))) {
            throw new IllegalStateException("Production CORS origins must not include localhost entries.");
        }
    }

    private void requireConfigured(String propertyName, String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(propertyName + " must be configured before startup.");
        }
    }
}
