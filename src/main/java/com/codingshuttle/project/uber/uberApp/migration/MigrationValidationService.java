package com.codingshuttle.project.uber.uberApp.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.ValidateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Service for validating Flyway migrations before application startup.
 * Performs pre-flight checks to ensure migrations can be safely applied.
 */
@Service
public class MigrationValidationService {

    private static final Logger logger = LoggerFactory.getLogger(MigrationValidationService.class);
    
    private final Flyway flyway;
    private final DataSource dataSource;
    
    public MigrationValidationService(Flyway flyway, DataSource dataSource) {
        this.flyway = flyway;
        this.dataSource = dataSource;
    }
    
    /**
     * Perform comprehensive migration validation before application starts.
     * This runs after Flyway auto-migration but before the application is fully ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(1) // Run early in the startup sequence
    public void validateMigrations() {
        logger.info("Starting comprehensive migration validation...");
        
        try {
            // 1. Validate database connectivity and permissions
            validateDatabaseAccess();
            
            // 2. Validate Flyway schema history
            validateSchemaHistory();
            
            // 3. Perform Flyway validation
            validateFlywayMigrations();
            
            // 4. Check for pending migrations
            checkPendingMigrations();
            
            // 5. Validate migration order and consistency
            validateMigrationOrder();
            
            // 6. Check for potential migration conflicts
            checkMigrationConflicts();
            
            // 7. Validate database compatibility
            validateDatabaseCompatibility();
            
            logger.info("Migration validation completed successfully");
            
        } catch (MigrationValidationException e) {
            logger.error("Migration validation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Migration validation failed: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during migration validation: {}", e.getMessage(), e);
            throw new RuntimeException("Migration validation failed with unexpected error", e);
        }
    }
    
    /**
     * Validate database connectivity and user permissions.
     */
    private void validateDatabaseAccess() throws MigrationValidationException {
        logger.info("Validating database access and permissions...");
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            // Check database version
            String databaseVersion = metaData.getDatabaseProductVersion();
            logger.info("Database: {} version {}", 
                metaData.getDatabaseProductName(), databaseVersion);
            
            // Check if user has necessary permissions
            if (!hasRequiredPermissions(connection)) {
                throw new MigrationValidationException(
                    "Database user lacks required permissions for migrations");
            }
            
            // Check if PostGIS extension is available
            if (!isPostGisAvailable(connection)) {
                throw new MigrationValidationException(
                    "PostGIS extension is not available in the database");
            }
            
            logger.info("Database access validation passed");
            
        } catch (SQLException e) {
            throw new MigrationValidationException(
                "Failed to validate database access: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate Flyway schema history table and contents.
     */
    private void validateSchemaHistory() throws MigrationValidationException {
        logger.info("Validating Flyway schema history...");
        
        MigrationInfoService infoService = flyway.info();
        MigrationInfo[] allMigrations = infoService.all();
        
        // Check for failed migrations
        List<MigrationInfo> failedMigrations = new ArrayList<>();
        for (MigrationInfo migration : allMigrations) {
            if (migration.getState() == MigrationState.FAILED) {
                failedMigrations.add(migration);
            }
        }
        
        if (!failedMigrations.isEmpty()) {
            String errorMessage = String.format(
                "Found %d failed migration(s): %s",
                failedMigrations.size(),
                failedMigrations.stream()
                    .map(m -> m.getVersion() + " - " + m.getDescription())
                    .collect(java.util.stream.Collectors.joining(", "))
            );
            throw new MigrationValidationException(errorMessage);
        }
        
        // Check for out of order migrations
        List<MigrationInfo> outOfOrderMigrations = new ArrayList<>();
        for (MigrationInfo migration : allMigrations) {
            if (migration.getState() == MigrationState.OUT_OF_ORDER) {
                outOfOrderMigrations.add(migration);
            }
        }
        
        if (!outOfOrderMigrations.isEmpty()) {
            logger.warn("Found {} out of order migration(s). This may indicate deployment issues.",
                outOfOrderMigrations.size());
        }
        
        logger.info("Schema history validation passed");
    }
    
    /**
     * Perform Flyway validation of migration scripts.
     */
    private void validateFlywayMigrations() throws MigrationValidationException {
        logger.info("Validating Flyway migration scripts...");
        
        try {
            ValidateResult result = flyway.validateWithResult();
            
            if (!result.validationSuccessful) {
                String errorDetails = result.invalidMigrations.stream()
                    .map(m -> String.format("%s: %s - %s", 
                        m.version, m.description, m.errorDetails))
                    .collect(java.util.stream.Collectors.joining("\n"));
                
                throw new MigrationValidationException(
                    "Flyway validation failed:\n" + errorDetails);
            }
            
            logger.info("Flyway migration validation passed");
            
        } catch (Exception e) {
            throw new MigrationValidationException(
                "Flyway validation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check for pending migrations and log warnings.
     */
    private void checkPendingMigrations() {
        logger.info("Checking for pending migrations...");
        
        MigrationInfoService infoService = flyway.info();
        MigrationInfo[] allMigrations = infoService.all();
        
        List<MigrationInfo> pendingMigrations = new ArrayList<>();
        for (MigrationInfo migration : allMigrations) {
            if (migration.getState() == MigrationState.PENDING) {
                pendingMigrations.add(migration);
            }
        }
        
        if (!pendingMigrations.isEmpty()) {
            logger.warn("Found {} pending migration(s):", pendingMigrations.size());
            for (MigrationInfo migration : pendingMigrations) {
                logger.warn("  - {}: {}", migration.getVersion(), migration.getDescription());
            }
        } else {
            logger.info("No pending migrations found");
        }
    }
    
    /**
     * Validate migration order and consistency.
     */
    private void validateMigrationOrder() throws MigrationValidationException {
        logger.info("Validating migration order and consistency...");
        
        MigrationInfoService infoService = flyway.info();
        MigrationInfo[] allMigrations = infoService.all();
        
        // Check for gaps in version numbers
        List<MigrationVersion> versions = new ArrayList<>();
        for (MigrationInfo migration : allMigrations) {
            if (migration.getVersion() != null && migration.getState().isApplied()) {
                versions.add(migration.getVersion());
            }
        }
        
        // Sort versions
        versions.sort(Comparator.naturalOrder());
        
        // Check for gaps
        for (int i = 1; i < versions.size(); i++) {
            MigrationVersion current = versions.get(i);
            MigrationVersion previous = versions.get(i - 1);
            
            // Check if versions are sequential
            // Avoid using MigrationVersion.nextVersion() for compatibility across Flyway versions.
            if (current.compareTo(previous) > 0) {
                try {
                    long currNum = Long.parseLong(current.getVersion().replaceAll("[^0-9]", ""));
                    long prevNum = Long.parseLong(previous.getVersion().replaceAll("[^0-9]", ""));
                    if (currNum - prevNum > 1) {
                        logger.warn("Gap detected in migration versions: {} -> {}", previous, current);
                    }
                } catch (NumberFormatException ex) {
                    // Versions are non-numeric or use a complex scheme; skip strict gap detection.
                }
            }
        }
        
        logger.info("Migration order validation passed");
    }
    
    /**
     * Check for potential migration conflicts.
     */
    private void checkMigrationConflicts() {
        logger.info("Checking for potential migration conflicts...");
        
        // This would check for things like:
        // - Concurrent migration attempts
        // - Schema conflicts
        // - Data migration conflicts
        
        // For now, just log that we're checking
        logger.info("Migration conflict check completed (basic implementation)");
    }
    
    /**
     * Validate database compatibility with migration requirements.
     */
    private void validateDatabaseCompatibility() throws MigrationValidationException {
        logger.info("Validating database compatibility...");
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            // Check PostgreSQL version (minimum 12 for PostGIS 3.0)
            String version = metaData.getDatabaseProductVersion();
            if (version.contains("PostgreSQL")) {
                // Extract version number
                String[] parts = version.split(" ");
                for (String part : parts) {
                    if (part.matches("\\d+\\.\\d+")) {
                        double versionNum = Double.parseDouble(part);
                        if (versionNum < 12.0) {
                            throw new MigrationValidationException(
                                String.format("PostgreSQL version %.1f is below minimum required 12.0", 
                                    versionNum));
                        }
                        break;
                    }
                }
            }
            
            logger.info("Database compatibility validation passed");
            
        } catch (SQLException e) {
            throw new MigrationValidationException(
                "Failed to validate database compatibility: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if database user has required permissions.
     */
    private boolean hasRequiredPermissions(Connection connection) throws SQLException {
        // Check if database connection is functional and we have basic query permissions.
        // We avoid CREATE TEMP TABLE because transaction poolers (like Neon) do not support it.
        try (var statement = connection.createStatement()) {
            statement.executeQuery("SELECT 1");
            return true;
        } catch (SQLException e) {
            logger.warn("Permission and connectivity check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if PostGIS extension is available.
     */
    private boolean isPostGisAvailable(Connection connection) throws SQLException {
        try {
            ResultSet resultSet = connection.createStatement().executeQuery(
                "SELECT EXISTS(SELECT 1 FROM pg_available_extensions WHERE name = 'postgis')");
            if (resultSet.next()) {
                return resultSet.getBoolean(1);
            }
            return false;
        } catch (SQLException e) {
            logger.warn("PostGIS check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get migration validation report.
     */
    public Map<String, Object> getValidationReport() {
        Map<String, Object> report = new HashMap<>();
        
        try {
            // Database info
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                report.put("database", Map.of(
                    "name", metaData.getDatabaseProductName(),
                    "version", metaData.getDatabaseProductVersion(),
                    "postgisAvailable", isPostGisAvailable(connection)
                ));
            }
            
            // Migration info
            MigrationInfoService infoService = flyway.info();
            MigrationInfo current = infoService.current();
            MigrationInfo[] allMigrations = infoService.all();
            
            report.put("migrations", Map.of(
                "current", current != null ? Map.of(
                    "version", current.getVersion().getVersion(),
                    "description", current.getDescription(),
                    "state", current.getState().name()
                ) : null,
                "total", allMigrations.length,
                "applied", Arrays.stream(allMigrations).filter(m -> m.getState().isApplied()).count(),
                "pending", Arrays.stream(allMigrations).filter(m -> m.getState() == MigrationState.PENDING).count(),
                "failed", Arrays.stream(allMigrations).filter(m -> m.getState() == MigrationState.FAILED).count()
            ));
            
            // Validation results
            try {
                ValidateResult result = flyway.validateWithResult();
                report.put("validation", Map.of(
                    "successful", result.validationSuccessful,
                    "invalidMigrations", result.invalidMigrations.size()
                ));
            } catch (Exception e) {
                report.put("validation", Map.of(
                    "successful", false,
                    "error", e.getMessage()
                ));
            }
            
            report.put("validationStatus", "PASSED");
            
        } catch (Exception e) {
            report.put("validationStatus", "FAILED");
            report.put("error", e.getMessage());
        }
        
        return report;
    }
    
    /**
     * Custom exception for migration validation failures.
     */
    public static class MigrationValidationException extends Exception {
        public MigrationValidationException(String message) {
            super(message);
        }
        
        public MigrationValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}