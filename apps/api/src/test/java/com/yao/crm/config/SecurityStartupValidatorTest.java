package com.yao.crm.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(OutputCaptureExtension.class)
class SecurityStartupValidatorTest {

    @Test
    void shouldThrowWhenAuthTokenSecretIsBlankInNonProd() {
        SecurityStartupValidator validator = createValidatorWith("   ", "dev");

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            validator::validateSecurityConfig);

        assertTrue(
            error.getMessage().contains("AUTH_TOKEN_SECRET must be configured and non-blank"),
            "Expected fail-fast for blank AUTH_TOKEN_SECRET in non-production");
    }

    @Test
    void shouldThrowWhenAuthTokenSecretIsBlankInProd() {
        SecurityStartupValidator validator = createValidatorWith("   ", "prod");

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            validator::validateSecurityConfig);

        assertTrue(
            error.getMessage().contains("AUTH_TOKEN_SECRET must be configured and non-blank"),
            "Expected explicit production failure for blank AUTH_TOKEN_SECRET");
    }

    @Test
    void shouldWarnWhenAuthTokenSecretUsesDefaultInNonProd(CapturedOutput output) {
        SecurityStartupValidator validator = createValidatorWith("crm-secret-change-me", "dev");

        assertDoesNotThrow(validator::validateSecurityConfig);

        assertTrue(
            output.getOut().contains("SECURITY WARNING: AUTH_TOKEN_SECRET is using default value"),
            "Expected warning for default AUTH_TOKEN_SECRET in non-production");
    }

    @Test
    void shouldThrowWhenAuthTokenSecretUsesDefaultInProd() {
        SecurityStartupValidator validator = createValidatorWith("crm-secret-change-me", "prod");

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            validator::validateSecurityConfig);

        assertTrue(
            error.getMessage().contains("must not use default value"),
            "Expected production failure for default AUTH_TOKEN_SECRET");
    }

    @Test
    void shouldThrowWhenAuthTokenSecretIsBlankInCommaSeparatedProdProfile() {
        SecurityStartupValidator validator = createValidatorWith("   ", "prod,cn");

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            validator::validateSecurityConfig);

        assertTrue(
            error.getMessage().contains("AUTH_TOKEN_SECRET must be configured and non-blank"),
            "Expected production failure when active profile contains prod");
    }

    @Test
    void shouldThrowWhenAuthTokenSecretIsBlankInCaseInsensitiveProductionProfile() {
        SecurityStartupValidator validator = createValidatorWith("   ", "Production");

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            validator::validateSecurityConfig);

        assertTrue(
            error.getMessage().contains("AUTH_TOKEN_SECRET must be configured and non-blank"),
            "Expected production failure for case-insensitive production profile");
    }

    private SecurityStartupValidator createValidatorWith(String tokenSecret, String activeProfile) {
        SecurityStartupValidator validator = new SecurityStartupValidator();
        ReflectionTestUtils.setField(validator, "tokenSecret", tokenSecret);
        ReflectionTestUtils.setField(validator, "activeProfile", activeProfile);
        ReflectionTestUtils.setField(validator, "feishuAppId", "");
        ReflectionTestUtils.setField(validator, "feishuAppSecret", "");
        ReflectionTestUtils.setField(validator, "dbPassword", "safe-db-pass");
        ReflectionTestUtils.setField(validator, "rabbitmqPassword", "safe-rabbit-pass");
        return validator;
    }
}
