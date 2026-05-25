package com.codingshuttle.project.uber.uberApp.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.MigrationVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for tracking migration versions and providing advanced migration management.
 * Extends Flyway's built-in tracking with custom business logic and monitoring.
 */
@Service
public class MigrationVersionTracker {

    private static final Logger logger = LoggerFactory.getLogger(MigrationVersionTracker.class);
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final Flyway flyway;
    private final JdbcTemplate jdbcTemplate;
    
    // Custom tracking table name
    private static final String TRACKING_TABLE = "migration_audit_log";
    
    public MigrationVersionTracker(Flyway flyway, JdbcTemplate jdbcTemplate) {
        this.flyway = flyway;
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * Initialize custom migration tracking table.
     */
    @PostConstruct
    @Transactional
    public void initializeTracking() {
        logger.info("Initializing migration version tracking...");
        
        // Create custom tracking table if it doesn't exist
        String createTableSql = String.format("""
            CREATE TABLE IF NOT EXISTS %s (
                id BIGSERIAL PRIMARY KEY,
                migration_version VARCHAR(50) NOT NULL,
                migration_description VARCHAR(500) NOT NULL,
                migration_type VARCHAR(50) NOT NULL,
                migration_script VARCHAR(500) NOT NULL,
                applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                applied_by VARCHAR(100),
                application_version VARCHAR(50),
                environment VARCHAR(50),
                execution_time_ms BIGINT,
                success BOOLEAN NOT NULL DEFAULT TRUE,
                error_message TEXT,
                checksum VARCHAR(64),
                rollback_script VARCHAR(500),
                rollback_applied_at TIMESTAMP,
                rollback_success BOOLEAN,
                metadata JSONB,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """, TRACKING_TABLE);
        
        jdbcTemplate.execute(createTableSql);
        
        // Create indexes for efficient querying
        jdbcTemplate.execute(String.format(
            "CREATE INDEX IF NOT EXISTS idx_%s_version ON %s (migration_version)",
            TRACKING_TABLE, TRACKING_TABLE));
        jdbcTemplate.execute(String.format(
            "CREATE INDEX IF NOT EXISTS idx_%s_applied_at ON %s (applied_at)",
            TRACKING_TABLE, TRACKING_TABLE));
        jdbcTemplate.execute(String.format(
            "CREATE INDEX IF NOT EXISTS idx_%s_success ON %s (success)",
            TRACKING_TABLE, TRACKING_TABLE));
        
        logger.info("Migration tracking table initialized: {}", TRACKING_TABLE);
        
        // Sync existing migrations to tracking table
        syncExistingMigrations();
    }
    
    /**
     * Sync existing Flyway migrations to custom tracking table.
     */
    @Transactional
    public void syncExistingMigrations() {
        logger.info("Syncing existing migrations to tracking table...");
        
        MigrationInfoService infoService = flyway.info();
        MigrationInfo[] allMigrations = infoService.all();
        
        int syncedCount = 0;
        for (MigrationInfo migration : allMigrations) {
            if (migration.getState().isApplied() && !isMigrationTracked(migration)) {
                trackMigration(migration, null, true, null);
                syncedCount++;
            }
        }
        
        logger.info("Synced {} existing migrations to tracking table", syncedCount);
    }
    
    /**
     * Track a migration in the custom tracking table.
     */
    @Transactional
    public void trackMigration(MigrationInfo migration, String appliedBy, boolean success, String errorMessage) {
        String sql = String.format("""
            INSERT INTO %s (
                migration_version, migration_description, migration_type,
                migration_script, applied_at, applied_by, success,
                error_message, checksum, execution_time_ms, metadata
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb))
            """, TRACKING_TABLE);
        
        try {
            Object installedOnObj = migration.getInstalledOn();
            java.util.Date appliedAt;
            if (installedOnObj == null) {
                appliedAt = new java.util.Date();
            } else if (installedOnObj instanceof java.time.Instant) {
                appliedAt = java.util.Date.from((java.time.Instant) installedOnObj);
            } else if (installedOnObj instanceof java.util.Date) {
                appliedAt = (java.util.Date) installedOnObj;
            } else {
                appliedAt = new java.util.Date();
            }

            jdbcTemplate.update(sql,
                migration.getVersion() != null ? migration.getVersion().getVersion() : "REPEATABLE",
                migration.getDescription(),
                migration.getType().name(),
                migration.getScript(),
                appliedAt,
                appliedBy != null ? appliedBy : "system",
                success,
                errorMessage,
                migration.getChecksum() != null ? String.valueOf(migration.getChecksum()) : null,
                migration.getExecutionTime(),
                createMetadata(migration)
            );
            
            logger.debug("Tracked migration: {} - {}", 
                migration.getVersion(), migration.getDescription());
                
        } catch (Exception e) {
            logger.error("Failed to track migration {}: {}", 
                migration.getVersion(), e.getMessage(), e);
        }
    }
    
    /**
     * Check if a migration is already tracked.
     */
    private boolean isMigrationTracked(MigrationInfo migration) {
        String sql = String.format(
            "SELECT COUNT(*) FROM %s WHERE migration_version = ? AND migration_script = ?",
            TRACKING_TABLE);
        
        String version = migration.getVersion() != null ? 
            migration.getVersion().getVersion() : "REPEATABLE";
        
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, version, migration.getScript());
        return count != null && count > 0;
    }
    
    /**
     * Create metadata JSON for a migration.
     */
    private String createMetadata(MigrationInfo migration) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("state", migration.getState().name());
        metadata.put("type", migration.getType().name());
        metadata.put("script", migration.getScript());
        
        if (migration.getVersion() != null) {
            metadata.put("version", migration.getVersion().getVersion());
        }
        
        if (migration.getInstalledOn() != null) {
            Object installedOnObj = migration.getInstalledOn();
            Instant installedInstant = null;
            if (installedOnObj instanceof Instant) {
                installedInstant = (Instant) installedOnObj;
            } else if (installedOnObj instanceof java.util.Date) {
                installedInstant = ((java.util.Date) installedOnObj).toInstant();
            }
            if (installedInstant != null) {
                metadata.put("installedOn", DATE_FORMATTER.format(LocalDateTime.ofInstant(installedInstant, ZoneId.systemDefault())));
            } else {
                metadata.put("installedOn", installedOnObj.toString());
            }
        }
        
        metadata.put("executionTime", migration.getExecutionTime());
        
        // Convert to JSON string (simplified - in production use Jackson or similar)
        return "{" + metadata.entrySet().stream()
            .map(e -> String.format("\"%s\":\"%s\"", e.getKey(), e.getValue()))
            .collect(java.util.stream.Collectors.joining(",")) + "}";
    }
    
    /**
     * Get migration tracking report.
     */
    public Map<String, Object> getTrackingReport() {
        Map<String, Object> report = new HashMap<>();
        
        // Get statistics from tracking table
        String statsSql = String.format("""
            SELECT 
                COUNT(*) as total,
                COUNT(CASE WHEN success = true THEN 1 END) as successful,
                COUNT(CASE WHEN success = false THEN 1 END) as failed,
                MIN(applied_at) as first_migration,
                MAX(applied_at) as last_migration
            FROM %s
            """, TRACKING_TABLE);
        
        try {
            Map<String, Object> stats = jdbcTemplate.queryForMap(statsSql);
            report.put("trackingStatistics", stats);
        } catch (Exception e) {
            report.put("trackingStatistics", Map.of("error", e.getMessage()));
        }
        
        // Get recent migrations
        String recentSql = String.format("""
            SELECT 
                migration_version,
                migration_description,
                applied_at,
                success,
                execution_time_ms
            FROM %s
            ORDER BY applied_at DESC
            LIMIT 10
            """, TRACKING_TABLE);
        
        try {
            List<Map<String, Object>> recentMigrations = jdbcTemplate.queryForList(recentSql);
            report.put("recentMigrations", recentMigrations);
        } catch (Exception e) {
            report.put("recentMigrations", List.of(Map.of("error", e.getMessage())));
        }
        
        // Compare with Flyway's history
        MigrationInfoService infoService = flyway.info();
        MigrationInfo[] flywayMigrations = infoService.all();
        
        long appliedCount = Arrays.stream(flywayMigrations)
            .filter(m -> m.getState().isApplied())
            .count();
        
        report.put("flywayStatistics", Map.of(
            "totalMigrations", flywayMigrations.length,
            "appliedMigrations", appliedCount,
            "pendingMigrations", Arrays.stream(flywayMigrations)
                .filter(m -> m.getState() == MigrationState.PENDING)
                .count(),
            "failedMigrations", Arrays.stream(flywayMigrations)
                .filter(m -> m.getState() == MigrationState.FAILED)
                .count()
        ));
        
        return report;
    }
    
    /**
     * Get migration history with detailed information.
     */
    public List<Map<String, Object>> getMigrationHistory(int limit) {
        String sql = String.format("""
            SELECT 
                id,
                migration_version as version,
                migration_description as description,
                migration_type as type,
                migration_script as script,
                applied_at as appliedAt,
                applied_by as appliedBy,
                success,
                execution_time_ms as executionTime,
                error_message as errorMessage,
                metadata
            FROM %s
            ORDER BY applied_at DESC
            LIMIT ?
            """, TRACKING_TABLE);
        
        return jdbcTemplate.queryForList(sql, limit);
    }
    
    /**
     * Get migration statistics by time period.
     */
    public Map<String, Object> getMigrationStatistics(String period) {
        String sql = String.format("""
            SELECT 
                DATE_TRUNC(?, applied_at) as period,
                COUNT(*) as total_migrations,
                COUNT(CASE WHEN success = true THEN 1 END) as successful,
                COUNT(CASE WHEN success = false THEN 1 END) as failed,
                AVG(execution_time_ms) as avg_execution_time,
                MAX(execution_time_ms) as max_execution_time
            FROM %s
            WHERE applied_at >= CURRENT_DATE - INTERVAL '30 days'
            GROUP BY DATE_TRUNC(?, applied_at)
            ORDER BY period DESC
            """, TRACKING_TABLE);
        
        String timeUnit;
        switch (period.toLowerCase()) {
            case "day":
                timeUnit = "day";
                break;
            case "week":
                timeUnit = "week";
                break;
            case "month":
                timeUnit = "month";
                break;
            default:
                timeUnit = "day";
        }
        
        List<Map<String, Object>> statistics = jdbcTemplate.queryForList(sql, timeUnit, timeUnit);
        
        return Map.of(
            "period", period,
            "statistics", statistics,
            "summary", calculateStatisticsSummary(statistics)
        );
    }
    
    /**
     * Calculate summary statistics from detailed statistics.
     */
    private Map<String, Object> calculateStatisticsSummary(List<Map<String, Object>> statistics) {
        if (statistics.isEmpty()) {
            return Map.of("message", "No statistics available");
        }
        
        long totalMigrations = 0;
        long successfulMigrations = 0;
        long failedMigrations = 0;
        double totalExecutionTime = 0;
        double maxExecutionTime = 0;
        
        for (Map<String, Object> stat : statistics) {
            totalMigrations += ((Number) stat.get("total_migrations")).longValue();
            successfulMigrations += ((Number) stat.get("successful")).longValue();
            failedMigrations += ((Number) stat.get("failed")).longValue();
            
            Number avgTime = (Number) stat.get("avg_execution_time");
            Number maxTime = (Number) stat.get("max_execution_time");
            
            if (avgTime != null) {
                totalExecutionTime += avgTime.doubleValue();
            }
            if (maxTime != null && maxTime.doubleValue() > maxExecutionTime) {
                maxExecutionTime = maxTime.doubleValue();
            }
        }
        
        double successRate = totalMigrations > 0 ? 
            (successfulMigrations * 100.0) / totalMigrations : 0;
        double avgExecutionTime = statistics.size() > 0 ? 
            totalExecutionTime / statistics.size() : 0;
        
        return Map.of(
            "totalMigrations", totalMigrations,
            "successfulMigrations", successfulMigrations,
            "failedMigrations", failedMigrations,
            "successRate", String.format("%.2f%%", successRate),
            "averageExecutionTime", String.format("%.2f ms", avgExecutionTime),
            "maxExecutionTime", String.format("%.2f ms", maxExecutionTime)
        );
    }
    
    /**
     * Record a rollback operation.
     */
    @Transactional
    public void recordRollback(String migrationVersion, String rollbackScript, 
                              boolean success, String errorMessage) {
        String sql = String.format("""
            UPDATE %s 
            SET rollback_script = ?,
                rollback_applied_at = CURRENT_TIMESTAMP,
                rollback_success = ?,
                error_message = CASE WHEN ? IS NOT NULL THEN ? ELSE error_message END,
                updated_at = CURRENT_TIMESTAMP
            WHERE migration_version = ?
            ORDER BY applied_at DESC
            LIMIT 1
            """, TRACKING_TABLE);
        
        jdbcTemplate.update(sql, rollbackScript, success, 
            errorMessage, errorMessage, migrationVersion);
        
        logger.info("Recorded rollback for migration: {}", migrationVersion);
    }
    
    /**
     * Scheduled task to clean up old tracking records.
     * Runs daily at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldRecords() {
        logger.info("Cleaning up old migration tracking records...");
        
        String cleanupSql = String.format("""
            DELETE FROM %s 
            WHERE applied_at < CURRENT_DATE - INTERVAL '90 days'
            AND success = true
            AND (rollback_success IS NULL OR rollback_success = true)
            """, TRACKING_TABLE);
        
        int deletedCount = jdbcTemplate.update(cleanupSql);
        
        if (deletedCount > 0) {
            logger.info("Cleaned up {} old migration tracking records", deletedCount);
        }
    }
    
    /**
     * Get migration health status.
     */
    public Map<String, Object> getMigrationHealth() {
        Map<String, Object> health = new HashMap<>();
        
        // Check for recent failures
        String failureSql = String.format("""
            SELECT COUNT(*) 
            FROM %s 
            WHERE success = false 
            AND applied_at >= CURRENT_DATE - INTERVAL '7 days'
            """, TRACKING_TABLE);
        
        Integer recentFailures = jdbcTemplate.queryForObject(failureSql, Integer.class);
        
        // Check for long-running migrations
        String longRunningSql = String.format("""
            SELECT COUNT(*) 
            FROM %s 
            WHERE execution_time_ms > 30000 
            AND applied_at >= CURRENT_DATE - INTERVAL '7 days'
            """, TRACKING_TABLE);
        
        Integer longRunningMigrations = jdbcTemplate.queryForObject(longRunningSql, Integer.class);
        
        health.put("status", recentFailures > 0 ? "DEGRADED" : "HEALTHY");
        health.put("recentFailures", recentFailures);
        health.put("longRunningMigrations", longRunningMigrations);
        health.put("lastCheck", LocalDateTime.now().format(DATE_FORMATTER));
        
        return health;
    }
}