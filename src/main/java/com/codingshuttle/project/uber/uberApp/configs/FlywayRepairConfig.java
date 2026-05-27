package com.codingshuttle.project.uber.uberApp.configs;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway migration strategy that performs a repair before migrating.
 *
 * <p>This is needed when the production database's flyway_schema_history table contains
 * records for migration versions that no longer exist as local SQL files (e.g. V7, V8, V9
 * were deleted from the repo but are still recorded as "applied" in the DB).
 *
 * <p>Calling {@code repair()} marks those entries as "deleted" so that Flyway's
 * validate-on-migrate check passes, and any pending migrations (e.g. V10) can then run.
 */
@Configuration
public class FlywayRepairConfig {

    private static final Logger logger = LoggerFactory.getLogger(FlywayRepairConfig.class);

    @Bean
    public FlywayMigrationStrategy repairThenMigrate() {
        return flyway -> {
            logger.info("Running Flyway repair to align schema history with local migration files...");
            try {
                flyway.repair();
                logger.info("Flyway repair completed successfully.");
            } catch (Exception e) {
                // Repair failures are non-fatal: log and attempt migrate anyway.
                logger.warn("Flyway repair encountered an issue (non-fatal): {}", e.getMessage());
            }

            logger.info("Running Flyway migrate...");
            flyway.migrate();
            logger.info("Flyway migrate completed.");
        };
    }
}
