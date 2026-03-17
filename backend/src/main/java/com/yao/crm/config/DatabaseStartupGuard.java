package com.yao.crm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Value;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;

@Configuration
public class DatabaseStartupGuard {
    private static final Logger log = LoggerFactory.getLogger(DatabaseStartupGuard.class);

    private static String versionOf(Class<?> clazz) {
        Package p = clazz.getPackage();
        String v = p == null ? null : p.getImplementationVersion();
        return (v == null || v.trim().isEmpty()) ? "unknown" : v;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ApplicationRunner flywayClasspathGuard() {
        return args -> {
            log.info("Startup dependency check: flyway-core={}, mysql-driver={}",
                    versionOf(org.flywaydb.core.Flyway.class),
                    versionOf(com.mysql.cj.jdbc.Driver.class));
            try {
                Class.forName("org.flywaydb.database.mysql.MySQLDatabaseType");
                log.info("Startup dependency check: flyway-mysql support detected.");
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException(
                        "Flyway MySQL support is missing from runtime classpath. " +
                        "Please ensure dependency org.flywaydb:flyway-mysql is available and IDE/Maven classpath is refreshed. " +
                        "Try: mvn -f backend/pom.xml clean compile -DskipTests and Maven Reload in IDE.",
                        ex
                );
            }
        };
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ApplicationRunner databaseHealthGuard(DataSource dataSource,
                                                 @Value("${spring.datasource.url:}") String datasourceUrl) {
        return args -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement("SELECT 1")) {
                stmt.execute();
                DatabaseMetaData meta = connection.getMetaData();
                log.info("Database precheck passed: url={}, product={} {}, driver={} {}",
                        datasourceUrl,
                        meta.getDatabaseProductName(),
                        meta.getDatabaseProductVersion(),
                        meta.getDriverName(),
                        meta.getDriverVersion());
            } catch (Exception ex) {
                throw new IllegalStateException(
                        "Database precheck failed: cannot connect/query datasource. " +
                        "Check DB_URL/DB_USER/DB_PASSWORD and MySQL availability. " +
                        "Current URL: " + datasourceUrl,
                        ex
                );
            }
        };
    }
}
