package com.codingshuttle.project.uber.uberApp.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple controller that returns a 200 OK for the root URL.
 * Render's default health‑check expects '/' to respond with HTTP 200.
 * This delegates to the existing {@link FlywayHealthIndicator} so the
 * detailed migration health information is still available at
 * {@code /actuator/health}.
 */
@RestController
public class RootHealthController {

    private final FlywayHealthIndicator flywayHealthIndicator;

    public RootHealthController(FlywayHealthIndicator flywayHealthIndicator) {
        this.flywayHealthIndicator = flywayHealthIndicator;
    }

    @GetMapping("/")
    public Health health() {
        // Delegate to the comprehensive Flyway health indicator.
        return flywayHealthIndicator.health();
    }
}
