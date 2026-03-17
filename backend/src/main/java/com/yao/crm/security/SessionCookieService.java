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
        this.cookieName = cookieName;
        this.cookiePath = cookiePath;
        this.sameSite = sameSite;
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
        StringBuilder builder = new StringBuilder();
        builder.append(cookieName).append("=").append(value);
        builder.append("; Path=").append(cookiePath);
        builder.append("; Max-Age=").append(Math.max(0, maxAge));
        builder.append("; HttpOnly");
        if (secure) {
            builder.append("; Secure");
        }
        if (sameSite != null && !sameSite.trim().isEmpty()) {
            builder.append("; SameSite=").append(sameSite.trim());
        }
        return builder.toString();
    }
}
