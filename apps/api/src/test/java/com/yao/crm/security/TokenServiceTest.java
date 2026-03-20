package com.yao.crm.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TokenServiceTest {

    @Test
    void createAndVerifyTokenShouldWork() {
        TokenService service = new TokenService("unit-test-secret", 3600000);
        String token = service.createToken("alice", "ADMIN", "alice_scope");

        AuthPrincipal principal = service.verify(token);

        assertNotNull(principal);
        assertEquals("alice", principal.getUsername());
        assertEquals("ADMIN", principal.getRole());
        assertEquals("alice_scope", principal.getOwnerScope());
    }

    @Test
    void verifyShouldRejectTamperedToken() {
        TokenService service = new TokenService("unit-test-secret", 3600000);
        String token = service.createToken("alice", "ADMIN", "alice_scope") + "x";

        AuthPrincipal principal = service.verify(token);

        assertNull(principal);
    }

    @Test
    void verifyShouldRejectExpiredToken() throws InterruptedException {
        TokenService service = new TokenService("unit-test-secret", 1);
        String token = service.createToken("alice", "ADMIN", "alice_scope");
        Thread.sleep(5);

        AuthPrincipal principal = service.verify(token);

        assertNull(principal);
    }
}
