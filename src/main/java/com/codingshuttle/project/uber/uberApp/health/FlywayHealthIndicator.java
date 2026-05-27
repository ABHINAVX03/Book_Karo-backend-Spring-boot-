package com.codingshuttle.project.uber.uberApp.health;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Custom health indicator for Flyway migrations.
 * Provides detailed information about migration status, versions, and any issues.
 */
@Component
public class FlywayHealthIndicator implements HealthIndicator {

    private final Flyway flyway;
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public FlywayHealthIndicator(Flyway flyway) {
        this.flyway = flyway;
    }

    @Override
    public Health health() {
        try {
            MigrationInfoService infoService = flyway.info();
            MigrationInfo current = infoService.current();
            MigrationInfo[] allMigrations = infoService.all();
            
            // Check for any failed migrations
            List<MigrationInfo> failedMigrations = Arrays.stream(allMigrations)
                .filter(m -> m.getState() == MigrationState.FAILED)
                .collect(Collectors.toList());
            
            // Check for pending migrations
            List<MigrationInfo> pendingMigrations = Arrays.stream(allMigrations)
                .filter(m -> m.getState() == MigrationState.PENDING)
                .collect(Collectors.toList());
            
            // Check for out of order migrations
            List<MigrationInfo> outOfOrderMigrations = Arrays.stream(allMigrations)
                .filter(m -> m.getState() == MigrationState.OUT_OF_ORDER)
                .collect(Collectors.toList());
            
            // Build health details
            Health.Builder healthBuilder = Health.up();
            
            // Add basic migration info
            healthBuilder.withDetail("migration.enabled", true);
            healthBuilder.withDetail("migration.baselineVersion", 
                flyway.getConfiguration().getBaselineVersion().getVersion());
            
            // Add current migration info
            if (current != null) {
                healthBuilder.withDetail("migration.current.version", current.getVersion().getVersion());
                healthBuilder.withDetail("migration.current.description", current.getDescription());
                healthBuilder.withDetail("migration.current.state", current.getState().name());
                healthBuilder.withDetail("migration.current.installedOn", 
                    formatDate(current.getInstalledOn()));
                healthBuilder.withDetail("migration.current.executionTime", 
                    current.getExecutionTime() + "ms");
            } else {
                healthBuilder.withDetail("migration.current", "No migrations applied yet");
            }
            
            // Add migration statistics
            healthBuilder.withDetail("migration.statistics.total", allMigrations.length);
            healthBuilder.withDetail("migration.statistics.applied", 
                Arrays.stream(allMigrations).filter(m -> m.getState().isApplied()).count());
            healthBuilder.withDetail("migration.statistics.pending", pendingMigrations.size());
            healthBuilder.withDetail("migration.statistics.failed", failedMigrations.size());
            healthBuilder.withDetail("migration.statistics.outOfOrder", outOfOrderMigrations.size());
            
            // Add migration history summary
            List<Map<String, Object>> migrationHistory = Arrays.stream(allMigrations)
                .map(this::toMigrationMap)
                .collect(Collectors.toList());
            healthBuilder.withDetail("migration.history", migrationHistory);
            
            // Check for issues and downgrade health if needed
            if (!failedMigrations.isEmpty()) {
                healthBuilder.down()
                    .withDetail("migration.issues.failed", 
                        failedMigrations.stream()
                            .map(m -> m.getVersion() + ": " + m.getDescription())
                            .collect(Collectors.toList()))
                    .withDetail("migration.issues.failedCount", failedMigrations.size());
            } else if (!pendingMigrations.isEmpty()) {
                healthBuilder.status("PENDING_MIGRATIONS")
                    .withDetail("migration.issues.pending", 
                        pendingMigrations.stream()
                            .map(m -> m.getVersion() + ": " + m.getDescription())
                            .collect(Collectors.toList()))
                    .withDetail("migration.issues.pendingCount", pendingMigrations.size());
            } else if (!outOfOrderMigrations.isEmpty()) {
                healthBuilder.status("OUT_OF_ORDER_MIGRATIONS")
                    .withDetail("migration.issues.outOfOrder", 
                        outOfOrderMigrations.stream()
                            .map(m -> m.getVersion() + ": " + m.getDescription())
                            .collect(Collectors.toList()))
                    .withDetail("migration.issues.outOfOrderCount", outOfOrderMigrations.size());
            }
            
            // Add validation info
            try {
                flyway.validateWithResult();
                healthBuilder.withDetail("migration.validation.passed", true);
            } catch (Exception e) {
                healthBuilder.withDetail("migration.validation.passed", false)
                    .withDetail("migration.validation.error", e.getMessage());
            }
            
            return healthBuilder.build();
            
        } catch (Exception e) {
            return Health.down()
                .withDetail("migration.enabled", true)
                .withDetail("migration.healthCheckError", e.getMessage())
                .withException(e)
                .build();
        }
    }
    
    private Map<String, Object> toMigrationMap(MigrationInfo migration) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("version", migration.getVersion() != null ? migration.getVersion().getVersion() : "Repeatable");
        map.put("description", migration.getDescription());
        map.put("type", migration.getType() != null ? migration.getType().name() : null);
        map.put("state", migration.getState() != null ? migration.getState().name() : null);
        map.put("installedOn", formatDate(migration.getInstalledOn()));
        map.put("executionTime", migration.getExecutionTime() != null ? migration.getExecutionTime() + "ms" : null);
        map.put("script", migration.getScript());
        return map;
    }
    
    private String formatDate(Object installedOn) {
        if (installedOn == null) return null;
        Instant instant = null;
        if (installedOn instanceof Instant) {
            instant = (Instant) installedOn;
        } else if (installedOn instanceof java.util.Date) {
            instant = ((java.util.Date) installedOn).toInstant();
        } else {
            return installedOn.toString();
        }
        return DATE_FORMATTER.format(instant);
    }
    
    /**
     * Get detailed migration report for monitoring and debugging.
     */
    public Map<String, Object> getMigrationReport() {
        MigrationInfoService infoService = flyway.info();
        MigrationInfo current = infoService.current();
        MigrationInfo[] allMigrations = infoService.all();
        
        Map<String, Object> report = new java.util.LinkedHashMap<>();
        
        if (current != null) {
            Map<String, Object> currentMap = new java.util.LinkedHashMap<>();
            currentMap.put("version", current.getVersion() != null ? current.getVersion().getVersion() : null);
            currentMap.put("description", current.getDescription());
            currentMap.put("state", current.getState() != null ? current.getState().name() : null);
            currentMap.put("installedOn", formatDate(current.getInstalledOn()));
            report.put("currentMigration", currentMap);
        } else {
            report.put("currentMigration", null);
        }
        
        Map<String, Object> statistics = new java.util.LinkedHashMap<>();
        statistics.put("total", allMigrations.length);
        statistics.put("applied", Arrays.stream(allMigrations).filter(m -> m.getState().isApplied()).count());
        statistics.put("pending", Arrays.stream(allMigrations).filter(m -> m.getState() == MigrationState.PENDING).count());
        statistics.put("failed", Arrays.stream(allMigrations).filter(m -> m.getState() == MigrationState.FAILED).count());
        report.put("statistics", statistics);
        
        report.put("migrations", Arrays.stream(allMigrations)
            .map(this::toMigrationMap)
            .collect(Collectors.toList()));
            
        Map<String, Object> validation = new java.util.LinkedHashMap<>();
        validation.put("passed", isValidationPassing());
        validation.put("message", getValidationMessage());
        report.put("validation", validation);
        
        return report;
    }
    
    private boolean isValidationPassing() {
        try {
            flyway.validateWithResult();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private String getValidationMessage() {
        try {
            flyway.validateWithResult();
            return "All migrations validated successfully";
        } catch (Exception e) {
            return "Validation failed: " + e.getMessage();
        }
    }
}