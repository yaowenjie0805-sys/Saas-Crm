package com.yao.crm.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

@Service
public class OidcAuthService {
    private static final Logger log = LoggerFactory.getLogger(OidcAuthService.class);

    private final String mode;
    private final String providerName;
    private final String clientId;
    private final String clientSecret;
    private final String authorizeEndpoint;
    private final String tokenEndpoint;
    private final String jwksUri;
    private final String redirectUri;
    private final String scope;
    private final String usernameClaim;
    private final String displayNameClaim;
    private final String expectedIssuer;
    private final String expectedAudience;
    private final OidcTokenVerifier tokenVerifier;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public OidcAuthService(
            @Value("${security.sso.mode:mock}") String mode,
            @Value("${security.sso.provider-name:Enterprise SSO}") String providerName,
            @Value("${security.sso.oidc.client-id:}") String clientId,
            @Value("${security.sso.oidc.client-secret:}") String clientSecret,
            @Value("${security.sso.oidc.authorize-endpoint:}") String authorizeEndpoint,
            @Value("${security.sso.oidc.token-endpoint:}") String tokenEndpoint,
            @Value("${security.sso.oidc.jwks-uri:}") String jwksUri,
            @Value("${security.sso.oidc.redirect-uri:http://localhost:5173}") String redirectUri,
            @Value("${security.sso.oidc.scope:openid profile email}") String scope,
            @Value("${security.sso.oidc.username-claim:preferred_username}") String usernameClaim,
            @Value("${security.sso.oidc.display-name-claim:name}") String displayNameClaim,
            @Value("${security.sso.oidc.expected-issuer:}") String expectedIssuer,
            @Value("${security.sso.oidc.expected-audience:}") String expectedAudience,
            @Value("${security.sso.oidc.http-connect-timeout-ms:5000}") long connectTimeoutMs,
            @Value("${security.sso.oidc.http-read-timeout-ms:8000}") long readTimeoutMs,
            OidcTokenVerifier tokenVerifier,
            ObjectMapper objectMapper,
            RestTemplateBuilder restTemplateBuilder
    ) {
        this.mode = mode == null ? "mock" : mode.trim().toLowerCase(Locale.ROOT);
        this.providerName = providerName;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authorizeEndpoint = authorizeEndpoint;
        this.tokenEndpoint = tokenEndpoint;
        this.jwksUri = jwksUri;
        this.redirectUri = redirectUri;
        this.scope = scope;
        this.usernameClaim = usernameClaim;
        this.displayNameClaim = displayNameClaim;
        this.expectedIssuer = normalize(expectedIssuer);
        this.expectedAudience = normalize(expectedAudience);
        this.tokenVerifier = tokenVerifier;
        this.objectMapper = objectMapper;
        long safeConnectTimeout = Math.max(100L, connectTimeoutMs);
        long safeReadTimeout = Math.max(100L, readTimeoutMs);
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(safeConnectTimeout))
            .setReadTimeout(Duration.ofMillis(safeReadTimeout))
            .build();
    }

    public boolean isOidcMode() {
        return "oidc".equals(mode);
    }

    public String mode() {
        return mode;
    }

    public String providerName() {
        return providerName;
    }

    public String redirectUri() {
        return redirectUri;
    }

    public String authorizeEndpoint() {
        return authorizeEndpoint;
    }

    public String clientId() {
        return clientId;
    }

    public String scope() {
        return scope;
    }

    public SsoIdentity resolveByAuthorizationCode(String code) {
        return resolveByAuthorizationCode(code, null);
    }

    public SsoIdentity resolveByAuthorizationCode(String code, String expectedNonce) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        try {
            String idToken = exchangeCode(code.trim());
            if (idToken == null) {
                return null;
            }
            Map<String, Object> claims = tokenVerifier.verify(idToken, jwksUri);
            if (claims == null || claims.isEmpty()) {
                return null;
            }
            if (!isTokenActive(claims) || !isClaimsTrusted(claims, normalize(expectedNonce))) {
                return null;
            }

            String username = string(claims.get(usernameClaim));
            if (isBlank(username)) {
                username = string(claims.get("email"));
            }
            if (isBlank(username)) {
                username = string(claims.get("sub"));
            }
            if (isBlank(username)) {
                return null;
            }

            String displayName = string(claims.get(displayNameClaim));
            if (isBlank(displayName)) {
                displayName = username;
            }
            return new SsoIdentity(username, displayName);
        } catch (ResourceAccessException ex) {
            log.warn("OIDC code exchange timed out or unreachable: {}", ex.getMessage());
            return null;
        } catch (Exception ex) {
            log.debug("OIDC authorization-code resolve failed", ex);
            return null;
        }
    }

    private String exchangeCode(String code) {
        if (isBlank(tokenEndpoint) || isBlank(clientId) || isBlank(clientSecret)) {
            return null;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(form, headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(tokenEndpoint, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("OIDC token endpoint returned non-2xx: {}", response.getStatusCodeValue());
                return null;
            }
            String body = response.getBody();
            if (body == null || body.trim().isEmpty()) {
                return null;
            }
            Map<String, Object> map = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
            return string(map.get("id_token"));
        } catch (ResourceAccessException ex) {
            log.warn("OIDC token endpoint timeout: {}", ex.getMessage());
            return null;
        } catch (Exception ex) {
            log.debug("OIDC token endpoint exchange failed", ex);
            return null;
        }
    }

    private boolean isTokenActive(Map<String, Object> claims) {
        Object expObj = claims.get("exp");
        if (expObj == null) return true;
        try {
            long exp = Long.parseLong(String.valueOf(expObj));
            return exp > Instant.now().getEpochSecond();
        } catch (Exception ex) {
            return false;
        }
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    boolean isClaimsTrusted(Map<String, Object> claims) {
        return isClaimsTrusted(claims, "");
    }

    boolean isClaimsTrusted(Map<String, Object> claims, String expectedNonce) {
        String normalizedExpectedAudience = !isBlank(expectedAudience)
                ? expectedAudience
                : normalize(clientId);

        if (!isBlank(expectedIssuer)) {
            String issuer = normalize(string(claims.get("iss")));
            if (!expectedIssuer.equals(issuer)) {
                return false;
            }
        }

        boolean multiAud = false;
        if (!isBlank(normalizedExpectedAudience)) {
            Object aud = claims.get("aud");
            if (aud instanceof Iterable) {
                multiAud = true;
                for (Object item : (Iterable<?>) aud) {
                    if (normalizedExpectedAudience.equals(normalize(string(item)))) {
                        multiAud = false;
                        break;
                    }
                }
                if (multiAud) {
                    return false;
                }
            } else if (!normalizedExpectedAudience.equals(normalize(string(aud)))) {
                return false;
            }
        }

        Object audClaim = claims.get("aud");
        if (audClaim instanceof Iterable) {
            int audCount = 0;
            for (Object ignored : (Iterable<?>) audClaim) {
                audCount++;
            }
            if (audCount > 1) {
                String azp = normalize(string(claims.get("azp")));
                if (isBlank(azp)) {
                    return false;
                }
                if (!isBlank(normalizedExpectedAudience) && !normalizedExpectedAudience.equals(azp)) {
                    return false;
                }
            }
        }

        if (!isBlank(expectedNonce)) {
            String nonce = normalize(string(claims.get("nonce")));
            if (!expectedNonce.equals(nonce)) {
                return false;
            }
        }

        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
