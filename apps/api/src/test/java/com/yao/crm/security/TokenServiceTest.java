package com.yao.crm.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
        String token = tokenService.createToken("testuser", "USER", "owner-1", "tenant-1", true);

        assertNotNull(token);
        assertTrue(token.contains("."));
    }

    @Test
    @DisplayName("shouldVerifyValidToken_whenTokenIsValid")
    void shouldVerifyValidToken_whenTokenIsValid() {
        String token = tokenService.createToken("testuser", "USER", "owner-1", "tenant-1", true);

        AuthPrincipal principal = tokenService.verify(token);

        assertNotNull(principal);
        assertEquals("testuser", principal.getUsername());
        assertEquals("USER", principal.getRole());
        assertEquals("owner-1", principal.getOwnerScope());
        assertEquals("tenant-1", principal.getTenantId());
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
        String token = tokenService.createToken("testuser", "USER", "", "tenant-1", false);

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
        assertEquals("tenant_default", principal.getTenantId());
    }

    @Test
    @DisplayName("shouldSetMfaVerifiedCorrectly")
    void shouldSetMfaVerifiedCorrectly() {
        String tokenWithMfa = tokenService.createToken("testuser", "USER", "owner-1", "tenant-1", true);
        String tokenWithoutMfa = tokenService.createToken("testuser", "USER", "owner-1", "tenant-1", false);

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

        assertNotNull(principal);
        assertEquals("", principal.getUsername());
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
    @DisplayName("shouldCreateDifferentTokensAtDifferentTimes")
    void shouldCreateDifferentTokensAtDifferentTimes() throws InterruptedException {
        String token1 = tokenService.createToken("testuser", "USER", "owner-1");
        Thread.sleep(10);
        String token2 = tokenService.createToken("testuser", "USER", "owner-1");

        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("shouldVerifyTokenWithDefaultTenant")
    void shouldVerifyTokenWithDefaultTenant() {
        String token = tokenService.createToken("testuser", "USER", "owner-1");

        AuthPrincipal principal = tokenService.verify(token);

        assertNotNull(principal);
        assertEquals("tenant_default", principal.getTenantId());
    }
}
