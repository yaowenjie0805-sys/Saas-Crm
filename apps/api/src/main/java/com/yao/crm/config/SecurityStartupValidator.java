package com.yao.crm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

@Component
public class SecurityStartupValidator {
    private static final Logger log = LoggerFactory.getLogger(SecurityStartupValidator.class);
    private static final String DEFAULT_AUTH_TOKEN_SECRET = "crm-secret-change-me";
    
    @Value("${auth.token.secret:crm-secret-change-me}")
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
        String normalizedSecret = tokenSecret == null ? "" : tokenSecret.trim();
        boolean isBlankSecret = normalizedSecret.isEmpty();
        boolean isDefaultSecret = DEFAULT_AUTH_TOKEN_SECRET.equals(normalizedSecret);

        if (isBlankSecret) {
            log.error("SECURITY CRITICAL: AUTH_TOKEN_SECRET is blank (null/empty/whitespace). Set AUTH_TOKEN_SECRET to a non-blank value.");
            throw new IllegalStateException("AUTH_TOKEN_SECRET must be configured and non-blank in all environments");
        }

        if (!isDefaultSecret) {
            return;
        }

        if (isProductionProfileActive()) {
            log.error("SECURITY CRITICAL: AUTH_TOKEN_SECRET is using default value '{}' in production! Set AUTH_TOKEN_SECRET to a non-default value.", DEFAULT_AUTH_TOKEN_SECRET);
            throw new IllegalStateException("AUTH_TOKEN_SECRET must not use default value in production environment");
        } else {
            log.warn("SECURITY WARNING: AUTH_TOKEN_SECRET is using default value '{}'. This is risky even in non-production and may cause login 500/signature failures.", DEFAULT_AUTH_TOKEN_SECRET);
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
            if (isProductionProfileActive()) {
                log.warn("SECURITY WARNING: RabbitMQ is using default 'guest' password in production environment.");
            } else {
                log.warn("SECURITY WARNING: RabbitMQ is using default 'guest' password. This is acceptable for development only.");
            }
        }
    }

    private boolean isProductionProfileActive() {
        if (activeProfile == null) {
            return false;
        }
        String[] profiles = activeProfile.split(",");
        for (String profile : profiles) {
            String normalized = profile == null ? "" : profile.trim().toLowerCase(Locale.ROOT);
            if ("prod".equals(normalized) || "production".equals(normalized)) {
                return true;
            }
        }
        return false;
    }
}
