package com.yao.crm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Configuration
public class ProductionSecurityGuard {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 5)
    public ApplicationRunner productionSecurityFailFast(Environment environment,
                                                        @Value("${auth.token.secret:crm-secret-change-me}") String tokenSecret,
                                                        @Value("${security.mfa.static-code:000000}") String mfaStaticCode,
                                                        @Value("${security.sso.mode:mock}") String ssoMode,
                                                        @Value("${auth.bootstrap.default-password:admin123}") String bootstrapDefaultPassword,
                                                        @Value("${spring.rabbitmq.username:guest}") String rabbitmqUser,
                                                        @Value("${spring.rabbitmq.password:guest}") String rabbitmqPassword) {
        return args -> {
            if (!isProd(environment)) {
                return;
            }
            if ("crm-secret-change-me".equals(tokenSecret)) {
                throw new IllegalStateException("SECURITY_GUARD: AUTH_TOKEN_SECRET uses insecure default in prod.");
            }
            if ("000000".equals(mfaStaticCode)) {
                throw new IllegalStateException("SECURITY_GUARD: SECURITY_MFA_STATIC_CODE uses insecure default in prod.");
            }
            if ("mock".equalsIgnoreCase(String.valueOf(ssoMode).trim())) {
                throw new IllegalStateException("SECURITY_GUARD: SECURITY_SSO_MODE=mock is forbidden in prod.");
            }
            if ("admin123".equals(bootstrapDefaultPassword)) {
                throw new IllegalStateException("SECURITY_GUARD: AUTH_BOOTSTRAP_DEFAULT_PASSWORD uses insecure default in prod.");
            }
            if ("guest".equals(rabbitmqUser) || "guest".equals(rabbitmqPassword)) {
                throw new IllegalStateException("SECURITY_GUARD: RabbitMQ uses default guest credentials in prod. Set RABBITMQ_USER and RABBITMQ_PASSWORD.");
            }
        };
    }

    private boolean isProd(Environment environment) {
        String[] profiles = environment.getActiveProfiles();
        if (profiles == null || profiles.length == 0) {
            return false;
        }
        return Arrays.stream(profiles).anyMatch(p -> "prod".equalsIgnoreCase(String.valueOf(p)));
    }
}
