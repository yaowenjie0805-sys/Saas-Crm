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
    
    // Feishu credentials
    @Value("${integration.feishu.app-id:}")
    private String feishuAppId;
    
    @Value("${integration.feishu.app-secret:}")
    private String feishuAppSecret;
    
    // Database password
    @Value("${spring.datasource.password:}")
    private String dbPassword;
    
    // RabbitMQ password
    @Value("${spring.rabbitmq.password:}")
    private String rabbitmqPassword;
    
    @EventListener(ApplicationReadyEvent.class)
    public void validateSecurityConfig() {
        validateJwtSecret();
        validateFeishuCredentials();
        validateDatabasePassword();
        validateRabbitmqPassword();
    }
    
    private void validateJwtSecret() {
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
    
    private void validateFeishuCredentials() {
        // Check if Feishu integration is configured
        boolean hasAppId = feishuAppId != null && !feishuAppId.isEmpty();
        boolean hasAppSecret = feishuAppSecret != null && !feishuAppSecret.isEmpty();
        
        if (hasAppId || hasAppSecret) {
            // If Feishu is configured, both should be from environment variables
            if (!hasAppId) {
                log.warn("SECURITY WARNING: FEISHU_APP_ID is not configured but FEISHU_APP_SECRET is set.");
            }
            if (!hasAppSecret) {
                log.warn("SECURITY WARNING: FEISHU_APP_SECRET is not configured but FEISHU_APP_ID is set.");
            }
        }
        
        // Check for placeholder values
        if (hasAppId && ("your-app-id".equals(feishuAppId) || "placeholder".equals(feishuAppId))) {
            log.warn("SECURITY WARNING: FEISHU_APP_ID appears to be using a placeholder value.");
        }
        if (hasAppSecret && ("your-app-secret".equals(feishuAppSecret) || "placeholder".equals(feishuAppSecret))) {
            log.warn("SECURITY WARNING: FEISHU_APP_SECRET appears to be using a placeholder value.");
        }
    }
    
    private void validateDatabasePassword() {
        if (dbPassword == null || dbPassword.isEmpty()) {
            log.warn("SECURITY WARNING: Database password is not configured.");
            return;
        }
        
        // Check for common weak passwords
        String[] weakPasswords = {"password", "123456", "admin", "root", "mysql", "crm", "test"};
        String lowerPassword = dbPassword.toLowerCase();
        for (String weak : weakPasswords) {
            if (lowerPassword.contains(weak)) {
                log.warn("SECURITY WARNING: Database password appears to be weak or using a common pattern.");
                break;
            }
        }
    }
    
    private void validateRabbitmqPassword() {
        if (rabbitmqPassword == null || rabbitmqPassword.isEmpty()) {
            log.warn("SECURITY WARNING: RabbitMQ password is not configured.");
            return;
        }
        
        // Check for default guest password
        if ("guest".equals(rabbitmqPassword)) {
            if ("prod".equals(activeProfile) || "production".equals(activeProfile)) {
                log.warn("SECURITY WARNING: RabbitMQ is using default 'guest' password in production environment.");
            } else {
                log.warn("SECURITY WARNING: RabbitMQ is using default 'guest' password. This is acceptable for development only.");
            }
        }
    }
}
