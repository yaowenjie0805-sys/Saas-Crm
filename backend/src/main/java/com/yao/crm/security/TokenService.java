package com.yao.crm.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class TokenService {

    private final String secret;
    private final long ttlMillis;

    public TokenService(
            @Value("${auth.token.secret:crm-secret-change-me}") String secret,
            @Value("${auth.token.ttl-ms:86400000}") long ttlMillis
    ) {
        this.secret = secret;
        this.ttlMillis = ttlMillis;
    }

    public String createToken(String username, String role, String ownerScope) {
        return createToken(username, role, ownerScope, "tenant_default", true);
    }

    public String createToken(String username, String role, String ownerScope, String tenantId, boolean mfaVerified) {
        long exp = System.currentTimeMillis() + ttlMillis;
        String payload = username + "|" + role + "|" + exp + "|" + safe(ownerScope) + "|" + safe(tenantId) + "|" + (mfaVerified ? "1" : "0");
        String encodedPayload = base64UrlEncode(payload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(encodedPayload);
        return encodedPayload + "." + signature;
    }

    public AuthPrincipal verify(String token) {
        if (token == null || !token.contains(".")) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            return null;
        }
        String payload = parts[0];
        String signature = parts[1];
        if (!sign(payload).equals(signature)) {
            return null;
        }
        String decoded = new String(base64UrlDecode(payload), StandardCharsets.UTF_8);
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
        String ownerScope = values.length >= 4 ? values[3] : values[0];
        String tenantId = values.length >= 6 ? safe(values[4]) : "tenant_default";
        boolean mfaVerified = values.length >= 6 ? "1".equals(values[5]) : true;
        return new AuthPrincipal(values[0], values[1], ownerScope, tenantId, mfaVerified);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(digest);
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
}
