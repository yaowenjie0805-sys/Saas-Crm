package com.yao.crm.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SessionCookieService {

    private final String cookieName;
    private final String cookiePath;
    private final String sameSite;
    private final boolean secure;
    private final long maxAgeSeconds;

    public SessionCookieService(
            @Value("${auth.cookie.name:CRM_SESSION}") String cookieName,
            @Value("${auth.cookie.path:/}") String cookiePath,
            @Value("${auth.cookie.same-site:Lax}") String sameSite,
            @Value("${auth.cookie.secure:false}") boolean secure,
            @Value("${auth.cookie.max-age-seconds:86400}") long maxAgeSeconds
    ) {
        this.cookieName = sanitizeCookieName(cookieName);
        this.cookiePath = sanitizeCookiePath(cookiePath);
        this.sameSite = normalizeSameSite(sameSite);
        this.secure = secure;
        this.maxAgeSeconds = maxAgeSeconds;
    }

    public String cookieName() {
        return cookieName;
    }

    public String buildSessionCookie(String token) {
        return buildCookie(token == null ? "" : token, maxAgeSeconds);
    }

    public String buildClearCookie() {
        return buildCookie("", 0);
    }

    private String buildCookie(String value, long maxAge) {
        String sanitizedValue = sanitizeCookieValue(value);
        StringBuilder builder = new StringBuilder();
        builder.append(cookieName).append("=").append(sanitizedValue);
        builder.append("; Path=").append(cookiePath);
        builder.append("; Max-Age=").append(Math.max(0, maxAge));
        builder.append("; HttpOnly");
        // SameSite=None requires Secure in modern browsers.
        boolean shouldUseSecure = secure || "None".equalsIgnoreCase(sameSite);
        if (shouldUseSecure) {
            builder.append("; Secure");
        }
        if (sameSite != null && !sameSite.trim().isEmpty()) {
            builder.append("; SameSite=").append(sameSite.trim());
        }
        return builder.toString();
    }

    private String sanitizeCookieValue(String value) {
        if (value == null || value.isEmpty()) return "";
        StringBuilder safe = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch <= 0x1F || ch == 0x7F) continue;
            if (ch == ';' || ch == ',' || ch == '\r' || ch == '\n') continue;
            safe.append(ch);
        }
        return safe.toString();
    }

    private String sanitizeCookieName(String value) {
        String raw = value == null ? "" : value.trim();
        if (raw.isEmpty()) return "CRM_SESSION";
        StringBuilder safe = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '-') {
                safe.append(ch);
            }
        }
        return safe.length() == 0 ? "CRM_SESSION" : safe.toString();
    }

    private String sanitizeCookiePath(String value) {
        String raw = value == null ? "" : value.trim();
        if (raw.isEmpty() || raw.contains("\r") || raw.contains("\n") || raw.contains(";")) {
            return "/";
        }
        return raw.startsWith("/") ? raw : "/" + raw;
    }

    private String normalizeSameSite(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return "Lax";
        }
        String normalized = rawValue.trim();
        if ("none".equalsIgnoreCase(normalized)) {
            return "None";
        }
        if ("strict".equalsIgnoreCase(normalized)) {
            return "Strict";
        }
        return "Lax";
    }
}
