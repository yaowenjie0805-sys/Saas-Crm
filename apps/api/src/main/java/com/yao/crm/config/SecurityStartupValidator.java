package com.yao.crm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class SecurityStartupValidator {
    private static final Logger log = LoggerFactory.getLogger(SecurityStartupValidator.class);
    
    @Value("${auth.token.secret:}")
    private String tokenSecret;
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;
    
    @EventListener(ApplicationReadyEvent.class)
    public void validateSecurityConfig() {
        if (tokenSecret == null || tokenSecret.isEmpty() || 
            "crm-secret-change-me".equals(tokenSecret)) {
            if ("prod".equals(activeProfile) || "production".equals(activeProfile)) {
                log.error("SECURITY CRITICAL: AUTH_TOKEN_SECRET is using default value in production! Application should be reconfigured.");
                throw new IllegalStateException("AUTH_TOKEN_SECRET must be changed from default value in production environment");
            } else {
                log.warn("SECURITY WARNING: AUTH_TOKEN_SECRET is using default value. This is acceptable for development only.");
            }
        }
    }
}
