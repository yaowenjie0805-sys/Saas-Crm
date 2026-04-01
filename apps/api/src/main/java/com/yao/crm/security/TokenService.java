package com.yao.crm.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private static final int TOKEN_VERSION = 2;
    private static final int MAX_TOKEN_LENGTH = 4096;
    private static final int MAX_JSON_LENGTH = 2048;
    private static final long MAX_EXPIRY_FUTURE_MILLIS = 30L * 24 * 60 * 60 * 1000; // 30 days
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Internal DTO for token payload deserialization.
     */
    private static class TokenPayload {
        @JsonProperty("v")
        private int version;

        @JsonProperty("u")
        private String username;

        @JsonProperty("r")
        private String role;

        @JsonProperty("e")
        private long expiry;

        @JsonProperty("o")
        private String ownerId;

        @JsonProperty("t")
        private String tenantId;

        @JsonProperty("m")
        private int mfaVerified;

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public long getExpiry() {
            return expiry;
        }

        public void setExpiry(long expiry) {
            this.expiry = expiry;
        }

        public String getOwnerId() {
            return ownerId;
        }

        public void setOwnerId(String ownerId) {
            this.ownerId = ownerId;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public int getMfaVerified() {
            return mfaVerified;
        }

        public void setMfaVerified(int mfaVerified) {
            this.mfaVerified = mfaVerified;
        }
    }

    private final String secret;
    private final long ttlMillis;

    public TokenService(
            @Value("${auth.token.secret:crm-secret-change-me}") String configuredSecret,
            @Value("${auth.token.ttl-ms:86400000}") long ttlMillis
    ) {
        this.secret = resolveSecret(configuredSecret);
        this.ttlMillis = ttlMillis;
    }

    @PostConstruct
    public void validateSecretKey() {
        // Check if secret key is too short
        if (secret == null || secret.length() < 32) {
            log.warn("JWT secret key is too short (< 32 chars). Please configure a stronger key for production.");
        }
        // Check for common default/weak values
        Set<String> weakSecrets = new HashSet<String>(Arrays.asList(
            "secret", "changeme", "your_jwt_secret_here", "default", "test",
            "crm-secret-change-me", "crm-secret", "jwt-secret", "jwt-secret-key"
        ));
        if (secret != null && weakSecrets.contains(secret.toLowerCase())) {
            log.warn("JWT secret key appears to be a default/weak value. Change it immediately for production use!");
        }
    }

    public String createToken(String username, String role, String ownerScope) {
        return createToken(username, role, ownerScope, "", true);
    }

    public String createToken(String username, String role, String ownerScope, String tenantId, boolean mfaVerified) {
        long exp = System.currentTimeMillis() + ttlMillis;
        // V2: JSON-based payload for safe serialization (no delimiter collision)
        String payload = "{\"v\":" + TOKEN_VERSION
                + ",\"u\":\"" + jsonEscape(safe(username))
                + "\",\"r\":\"" + jsonEscape(safe(role))
                + "\",\"e\":" + exp
                + ",\"o\":\"" + jsonEscape(safe(ownerScope))
                + "\",\"t\":\"" + jsonEscape(safe(tenantId))
                + "\",\"m\":" + (mfaVerified ? 1 : 0)
                + "}";
        String encodedPayload = base64UrlEncode(payload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(encodedPayload);
        return encodedPayload + "." + signature;
    }

    public AuthPrincipal verify(String token) {
        if (token == null) {
            return null;
        }
        String normalizedToken = token.trim();
        if (normalizedToken.isEmpty() || normalizedToken.length() > MAX_TOKEN_LENGTH) {
            return null;
        }
        int firstDot = normalizedToken.indexOf('.');
        if (firstDot <= 0 || firstDot != normalizedToken.lastIndexOf('.') || firstDot == normalizedToken.length() - 1) {
            return null;
        }
        String payload = normalizedToken.substring(0, firstDot);
        String signature = normalizedToken.substring(firstDot + 1);
        try {
            if (!sign(payload).equals(signature)) {
                return null;
            }
            String decoded = new String(base64UrlDecode(payload), StandardCharsets.UTF_8);

            // V2 JSON format
            if (decoded.startsWith("{")) {
                return verifyJsonPayload(decoded);
            }
            // V1 legacy pipe-delimited format (backward compatible)
            return verifyLegacyPayload(decoded);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private AuthPrincipal verifyJsonPayload(String json) {
        // Security: validate JSON length
        if (json == null || json.length() > MAX_JSON_LENGTH) {
            return null;
        }

        try {
            TokenPayload payload = MAPPER.readValue(json, TokenPayload.class);
            String username = normalizeWhitespace(payload.getUsername());
            String role = normalizeWhitespace(payload.getRole());
            String ownerScope = normalizeWhitespace(payload.getOwnerId());
            String tenantId = normalizeWhitespace(payload.getTenantId());

            // Validate required fields
            if (username == null || username.isEmpty()) {
                return null;
            }
            if (payload.getExpiry() <= 0) {
                return null;
            }

            // Security: validate expiry is not too far in the future
            long now = System.currentTimeMillis();
            if (payload.getExpiry() > now + MAX_EXPIRY_FUTURE_MILLIS) {
                return null;
            }

            // Check token expiration
            if (now > payload.getExpiry()) {
                return null;
            }

            return new AuthPrincipal(
                    username,
                    safe(role),
                    ownerScope.isEmpty() ? username : ownerScope,
                    safe(tenantId),
                    payload.getMfaVerified() == 1
            );
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private AuthPrincipal verifyLegacyPayload(String decoded) {
        String[] values = decoded.split("\\|", -1);
        if (values.length != 3 && values.length != 4 && values.length != 6) {
            return null;
        }

        long exp;
        try {
            exp = Long.parseLong(values[2]);
        } catch (NumberFormatException ex) {
            return null;
        }

        if (System.currentTimeMillis() > exp) {
            return null;
        }
        String username = normalizeWhitespace(values[0]);
        if (username == null || username.isEmpty()) {
            return null;
        }
        String role = safe(normalizeWhitespace(values[1]));
        String ownerScope = values.length >= 4 ? safe(normalizeWhitespace(values[3])) : username;
        String tenantId = values.length >= 6 ? safe(normalizeWhitespace(values[4])) : "";
        boolean mfaVerified = values.length >= 6 ? "1".equals(values[5]) : true;
        return new AuthPrincipal(
                username,
                role,
                ownerScope.isEmpty() ? username : ownerScope,
                tenantId,
                mfaVerified
        );
    }

    private String jsonEscape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(digest);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Failed to sign token: invalid auth.token.secret configuration. "
                            + "Set AUTH_TOKEN_SECRET to a non-empty secret value.",
                    ex
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign token", ex);
        }
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalizeWhitespace(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    private static String resolveSecret(String configuredSecret) {
        if (configuredSecret == null || configuredSecret.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Invalid auth.token.secret configuration: value is empty. "
                            + "Set AUTH_TOKEN_SECRET to a non-empty secret value."
            );
        }
        return configuredSecret.trim();
    }
}
