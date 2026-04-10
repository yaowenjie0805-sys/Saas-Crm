package com.yao.crm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class OidcAuthServiceTest {

    @Test
    void shouldValidateIssuerAndAudienceClaims() {
        OidcAuthService service = new OidcAuthService(
                "oidc",
                "Enterprise SSO",
                "client-id",
                "client-secret",
                "https://idp.example.com/authorize",
                "https://idp.example.com/token",
                "https://idp.example.com/jwks",
                "http://localhost:5173",
                "openid profile email",
                "preferred_username",
                "name",
                "https://idp.example.com",
                "crm-web",
                1000,
                1000,
                mock(OidcTokenVerifier.class),
                new ObjectMapper(),
                new RestTemplateBuilder()
        );

        assertTrue(service.isClaimsTrusted(Map.of(
                "iss", "https://idp.example.com",
                "aud", "crm-web"
        )));
        assertTrue(service.isClaimsTrusted(Map.of(
                "iss", "https://idp.example.com",
                "aud", List.of("crm-api", "crm-web")
        )));
        assertFalse(service.isClaimsTrusted(Map.of(
                "iss", "https://evil.example.com",
                "aud", "crm-web"
        )));
        assertFalse(service.isClaimsTrusted(Map.of(
                "iss", "https://idp.example.com",
                "aud", "another-app"
        )));

        assertTrue(service.isClaimsTrusted(Map.of(
                "iss", "https://idp.example.com",
                "aud", List.of("crm-web", "crm-api"),
                "azp", "crm-web",
                "nonce", "nonce-1"
        ), "nonce-1"));
        assertFalse(service.isClaimsTrusted(Map.of(
                "iss", "https://idp.example.com",
                "aud", List.of("crm-web", "crm-api"),
                "nonce", "nonce-1"
        ), "nonce-1"));
        assertFalse(service.isClaimsTrusted(Map.of(
                "iss", "https://idp.example.com",
                "aud", "crm-web",
                "nonce", "nonce-2"
        ), "nonce-1"));
    }
}
