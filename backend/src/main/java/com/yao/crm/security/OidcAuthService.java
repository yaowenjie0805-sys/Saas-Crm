package com.yao.crm.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

@Service
public class OidcAuthService {

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
    private final OidcTokenVerifier tokenVerifier;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

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
            OidcTokenVerifier tokenVerifier,
            ObjectMapper objectMapper
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
        this.tokenVerifier = tokenVerifier;
        this.objectMapper = objectMapper;
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
            if (!isTokenActive(claims)) {
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
        } catch (Exception ex) {
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
        ResponseEntity<String> response = restTemplate.postForEntity(tokenEndpoint, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            return null;
        }
        String body = response.getBody();
        if (body == null || body.trim().isEmpty()) {
            return null;
        }
        try {
            Map<String, Object> map = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
            return string(map.get("id_token"));
        } catch (Exception ex) {
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
