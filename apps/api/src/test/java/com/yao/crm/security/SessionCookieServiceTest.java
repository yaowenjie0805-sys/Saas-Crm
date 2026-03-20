package com.yao.crm.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SessionCookieServiceTest {

    @Test
    void shouldSanitizeCookieValueAndName() {
        SessionCookieService service = new SessionCookieService(
                "CRM\r\nSet-Cookie",
                "/",
                "Lax",
                false,
                120L
        );

        String cookie = service.buildSessionCookie("abc\r\n;malicious=1");
        Assertions.assertTrue(cookie.startsWith("CRMSet-Cookie=abcmalicious=1"));
        Assertions.assertFalse(cookie.contains("\r"));
        Assertions.assertFalse(cookie.contains("\n"));
        Assertions.assertFalse(cookie.contains(";malicious"));
    }

    @Test
    void shouldForceSecureWhenSameSiteNone() {
        SessionCookieService service = new SessionCookieService(
                "CRM_SESSION",
                "/",
                "None",
                false,
                120L
        );

        String cookie = service.buildSessionCookie("token");
        Assertions.assertTrue(cookie.contains("; Secure"));
        Assertions.assertTrue(cookie.contains("; SameSite=None"));
    }

    @Test
    void shouldNormalizeInvalidPathToRoot() {
        SessionCookieService service = new SessionCookieService(
                "CRM_SESSION",
                "bad;path",
                "Lax",
                false,
                120L
        );

        String cookie = service.buildSessionCookie("token");
        Assertions.assertTrue(cookie.contains("; Path=/"));
    }
}
