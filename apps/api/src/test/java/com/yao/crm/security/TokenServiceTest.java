package com.yao.crm.security;

import static com.yao.crm.support.TestTenant.TENANT_TEST;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TokenService
 */
class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService("test-secret-key-for-unit-tests", 86400000L);
    }

    @Test
    @DisplayName("shouldCreateToken_whenValidInputs")
    void shouldCreateToken_whenValidInputs() {
        String token = tokenService.createToken("testuser", "USER", "owner-1");

        assertNotNull(token);
        assertTrue(token.contains("."));
    }

    @Test
    @DisplayName("shouldCreateTokenWithAllParams_whenValidInputs")
    void shouldCreateTokenWithAllParams_whenValidInputs() {
        String token = tokenService.createToken("testuser", "USER", "owner-1", TENANT_TEST, true);

        assertNotNull(token);
        assertTrue(token.contains("."));
    }

    @Test
    @DisplayName("shouldVerifyValidToken_whenTokenIsValid")
    void shouldVerifyValidToken_whenTokenIsValid() {
        String token = tokenService.createToken("testuser", "USER", "owner-1", TENANT_TEST, true);

        AuthPrincipal principal = tokenService.verify(token);

        assertNotNull(principal);
        assertEquals("testuser", principal.getUsername());
        assertEquals("USER", principal.getRole());
        assertEquals("owner-1", principal.getOwnerScope());
        assertEquals(TENANT_TEST, principal.getTenantId());
        assertTrue(principal.isMfaVerified());
    }

    @Test
    @DisplayName("shouldReturnNull_whenTokenIsNull")
    void shouldReturnNull_whenTokenIsNull() {
        AuthPrincipal principal = tokenService.verify(null);

        assertNull(principal);
    }

    @Test
    @DisplayName("shouldReturnNull_whenTokenHasNoDot")
    void shouldReturnNull_whenTokenHasNoDot() {
        AuthPrincipal principal = tokenService.verify("invalid-token-without-dot");

        assertNull(principal);
    }

    @Test
    @DisplayName("shouldReturnNull_whenTokenHasInvalidSignature")
    void shouldReturnNull_whenTokenHasInvalidSignature() {
        String token = tokenService.createToken("testuser", "USER", "owner-1");
        String tamperedToken = token.substring(0, token.indexOf(".") + 1) + "invalidSignature";

        AuthPrincipal principal = tokenService.verify(tamperedToken);

        assertNull(principal);
    }

    @Test
    @DisplayName("shouldReturnNull_whenTokenIsExpired")
    void shouldReturnNull_whenTokenIsExpired() {
        TokenService shortLivedService = new TokenService("test-secret-key", -1000L);
        String token = shortLivedService.createToken("testuser", "USER", "owner-1");

        AuthPrincipal principal = tokenService.verify(token);

        assertNull(principal);
    }

    @Test
    @DisplayName("shouldHandleSpecialCharactersInUsername")
    void shouldHandleSpecialCharactersInUsername() {
        String specialUsername = "user@test.com";
        String token = tokenService.createToken(specialUsername, "USER", "owner-1");

        AuthPrincipal principal = tokenService.verify(token);

        assertNotNull(principal);
        assertEquals(specialUsername, principal.getUsername());
    }

    @Test
    @DisplayName("shouldHandleEmptyOwnerScope")
    void shouldHandleEmptyOwnerScope() {
        String token = tokenService.createToken("testuser", "USER", "", TENANT_TEST, false);

        AuthPrincipal principal = tokenService.verify(token);

        assertNotNull(principal);
        assertEquals("testuser", principal.getOwnerScope());
    }

    @Test
    @DisplayName("shouldHandleEmptyTenantId")
    void shouldHandleEmptyTenantId() {
        String token = tokenService.createToken("testuser", "USER", "owner-1", "", false);

        AuthPrincipal principal = tokenService.verify(token);

        assertNotNull(principal);
        assertEquals("", principal.getTenantId());
    }

    @Test
    @DisplayName("shouldSetMfaVerifiedCorrectly")
    void shouldSetMfaVerifiedCorrectly() {
        String tokenWithMfa = tokenService.createToken("testuser", "USER", "owner-1", TENANT_TEST, true);
        String tokenWithoutMfa = tokenService.createToken("testuser", "USER", "owner-1", TENANT_TEST, false);

        AuthPrincipal withMfa = tokenService.verify(tokenWithMfa);
        AuthPrincipal withoutMfa = tokenService.verify(tokenWithoutMfa);

        assertTrue(withMfa.isMfaVerified());
        assertFalse(withoutMfa.isMfaVerified());
    }

    @Test
    @DisplayName("shouldGenerateDifferentTokensForDifferentUsers")
    void shouldGenerateDifferentTokensForDifferentUsers() {
        String token1 = tokenService.createToken("user1", "USER", "owner-1");
        String token2 = tokenService.createToken("user2", "USER", "owner-1");

        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("shouldGenerateDifferentTokensForDifferentRoles")
    void shouldGenerateDifferentTokensForDifferentRoles() {
        String token1 = tokenService.createToken("testuser", "USER", "owner-1");
        String token2 = tokenService.createToken("testuser", "ADMIN", "owner-1");

        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("shouldHandleNullUsername")
    void shouldHandleNullUsername() {
        String token = tokenService.createToken(null, "USER", "owner-1");

        AuthPrincipal principal = tokenService.verify(token);

        assertNull(principal);
    }

    @Test
    @DisplayName("shouldHandleNullRole")
    void shouldHandleNullRole() {
        String token = tokenService.createToken("testuser", null, "owner-1");

        AuthPrincipal principal = tokenService.verify(token);

        assertNotNull(principal);
        assertEquals("", principal.getRole());
    }

    @Test
    @DisplayName("shouldReturnNull_whenTokenPartsCountInvalid")
    void shouldReturnNull_whenTokenPartsCountInvalid() {
        String invalidToken = "part1.part2.part3";

        AuthPrincipal principal = tokenService.verify(invalidToken);

        assertNull(principal);
    }

    @Test
    @DisplayName("shouldReturnNull_whenTokenIsTooLarge")
    void shouldReturnNull_whenTokenIsTooLarge() {
        String oversizedToken = repeat('a', 4090) + "." + repeat('b', 32);

        AuthPrincipal principal = tokenService.verify(oversizedToken);

        assertNull(principal);
    }

    @Test
    @DisplayName("shouldReturnNull_whenPayloadBase64IsInvalidEvenWithValidSignature")
    void shouldReturnNull_whenPayloadBase64IsInvalidEvenWithValidSignature() throws Exception {
        String payload = "!!!!";
        String token = payload + "." + sign(payload);

        AuthPrincipal principal = tokenService.verify(token);

        assertNull(principal);
    }

    @Test
    @DisplayName("shouldCreateDifferentTokensAtDifferentTimes")
    void shouldCreateDifferentTokensAtDifferentTimes() throws InterruptedException {
        String token1 = tokenService.createToken("testuser", "USER", "owner-1");
        Thread.sleep(10);
        String token2 = tokenService.createToken("testuser", "USER", "owner-1");

        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("shouldCreateTokenWithoutDefaultTenantFallback")
    void shouldCreateTokenWithoutDefaultTenantFallback() {
        String token = tokenService.createToken("testuser", "USER", "owner-1");

        AuthPrincipal principal = tokenService.verify(token);

        assertNotNull(principal);
        assertEquals("", principal.getTenantId());
    }

    @Test
    @DisplayName("shouldThrowHelpfulException_whenConfiguredSecretIsBlank")
    void shouldThrowHelpfulException_whenConfiguredSecretIsBlank() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new TokenService("   ", 86400000L)
        );

        assertTrue(exception.getMessage().contains("auth.token.secret"));
        assertTrue(exception.getMessage().contains("AUTH_TOKEN_SECRET"));
    }

    @Test
    @DisplayName("shouldThrowHelpfulException_whenConfiguredSecretIsNull")
    void shouldThrowHelpfulException_whenConfiguredSecretIsNull() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new TokenService(null, 86400000L)
        );

        assertTrue(exception.getMessage().contains("auth.token.secret"));
        assertTrue(exception.getMessage().contains("AUTH_TOKEN_SECRET"));
    }

    @Test
    @DisplayName("shouldNotDefaultTenantForLegacyTokenWithoutTenantSegment")
    void shouldNotDefaultTenantForLegacyTokenWithoutTenantSegment() throws Exception {
        long exp = System.currentTimeMillis() + 60000;
        String legacyPayload = "legacy-user|USER|" + exp;
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(legacyPayload.getBytes(StandardCharsets.UTF_8));
        String token = encoded + "." + sign(encoded);

        AuthPrincipal principal = tokenService.verify(token);

        assertNotNull(principal);
        assertEquals("", principal.getTenantId());
    }

    private String sign(String payload) throws Exception {
        Method method = TokenService.class.getDeclaredMethod("sign", String.class);
        method.setAccessible(true);
        return (String) method.invoke(tokenService, payload);
    }

    private String repeat(char value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }
}

