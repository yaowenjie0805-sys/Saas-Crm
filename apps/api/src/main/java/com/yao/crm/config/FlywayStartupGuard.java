package com.yao.crm.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayStartupGuard {

    @Bean
    public ApplicationRunner flywayHealthGuard(ObjectProvider<Flyway> flywayProvider) {
        return (args) -> {
            Flyway flyway = flywayProvider.getIfAvailable();
            if (flyway == null) return;
            MigrationInfoService info = flyway.info();
            for (MigrationInfo migration : info.all()) {
                if (migration == null) continue;
                MigrationState state = migration.getState();
                String stateName = state == null ? "" : state.name();
                if (state == MigrationState.FAILED
                        || state == MigrationState.MISSING_FAILED
                        || "MISSING_SUCCESS".equals(stateName)
                        || "FUTURE_FAILED".equals(stateName)
                        || "FAILED_FUTURE".equals(stateName)) {
                    throw new IllegalStateException("Flyway migration state invalid: " + migration.getVersion() + " " + state);
                }
            }
        };
    }
}
