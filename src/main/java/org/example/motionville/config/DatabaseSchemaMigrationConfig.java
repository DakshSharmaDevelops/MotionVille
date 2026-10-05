package org.example.motionville.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

@Configuration
public class DatabaseSchemaMigrationConfig implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaMigrationConfig.class);
    private boolean executed = false;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource dataSource && !executed) {
            executed = true;
            repairAppUserEmailVerified(dataSource);
        }
        return bean;
    }

    private void repairAppUserEmailVerified(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            DatabaseMetaData metaData = connection.getMetaData();
            boolean tableExists = checkTableExists(metaData, "app_user")
                    || checkTableExists(metaData, "APP_USER");

            if (tableExists) {
                log.info("Pre-migrating app_user table: ensuring email_verified column exists and contains no nulls...");

                // 1. Add column with default false if it doesn't exist
                try {
                    statement.execute("ALTER TABLE app_user ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT FALSE");
                } catch (Exception e) {
                    log.debug("Column creation skipped or already present: {}", e.getMessage());
                }

                // 2. Backfill any existing NULL values to FALSE
                try {
                    statement.execute("UPDATE app_user SET email_verified = FALSE WHERE email_verified IS NULL");
                } catch (Exception e) {
                    log.debug("Null backfill skipped: {}", e.getMessage());
                }

                // 3. Ensure DEFAULT is FALSE
                try {
                    statement.execute("ALTER TABLE app_user ALTER COLUMN email_verified SET DEFAULT FALSE");
                } catch (Exception e) {
                    log.debug("Default constraint update skipped: {}", e.getMessage());
                }

                log.info("Pre-migration for app_user email_verified completed.");
            }
        } catch (Exception e) {
            log.debug("Database pre-migration check skipped or failed: {}", e.getMessage());
        }
    }

    private boolean checkTableExists(DatabaseMetaData metaData, String tableName) {
        try (ResultSet rs = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        } catch (Exception e) {
            return false;
        }
    }
}
