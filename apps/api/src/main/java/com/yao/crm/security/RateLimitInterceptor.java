package com.yao.crm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.dto.ApiErrorResponse;
import com.yao.crm.service.I18nService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.regex.Pattern;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String AUTH_PRINCIPAL_ATTR = "authPrincipal";
    private static final String AUTH_TENANT_ID_ATTR = "authTenantId";
    private static final String AUTH_USERNAME_ATTR = "authUsername";
    private static final String AUTH_ROLE_ATTR = "authRole";
    private static final String AUTH_OWNER_SCOPE_ATTR = "authOwnerScope";
    private static final String AUTH_MFA_VERIFIED_ATTR = "authMfaVerified";
    private static final Pattern NUMERIC_PATH_SEGMENT = Pattern.compile("^\\d+$");
    private static final Pattern UUID_PATH_SEGMENT = Pattern.compile(
            "^(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
    );

    private final RateLimitService rateLimitService;
    private final I18nService i18nService;
    private final ObjectMapper objectMapper;
    private final TokenService tokenService;
    private final SessionCookieService sessionCookieService;
    private final int loginMaxRequests;
    private final int approvalMaxRequests;
    private final int batchRetryMaxRequests;
    private final int exportMaxRequests;

    public RateLimitInterceptor(
            RateLimitService rateLimitService,
            I18nService i18nService,
            ObjectMapper objectMapper,
            TokenService tokenService,
            SessionCookieService sessionCookieService,
            @Value("${security.rate-limit.login.max-requests:30}") int loginMaxRequests,
            @Value("${security.rate-limit.approval.max-requests:120}") int approvalMaxRequests,
            @Value("${security.rate-limit.batch-retry.max-requests:20}") int batchRetryMaxRequests,
            @Value("${security.rate-limit.export.max-requests:40}") int exportMaxRequests
    ) {
        this.rateLimitService = rateLimitService;
        this.i18nService = i18nService;
        this.objectMapper = objectMapper;
        this.tokenService = tokenService;
        this.sessionCookieService = sessionCookieService;
        this.loginMaxRequests = loginMaxRequests;
        this.approvalMaxRequests = approvalMaxRequests;
        this.batchRetryMaxRequests = batchRetryMaxRequests;
        this.exportMaxRequests = exportMaxRequests;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String requestUri = request.getRequestURI();
        String key = buildRateLimitKey(request, requestUri);
        int maxRequests = resolveLimit(requestUri);
        if (rateLimitService.allow(key, maxRequests)) {
            return true;
        }

        Object trace = request.getAttribute(TraceIdInterceptor.TRACE_ID_ATTR);
        ApiErrorResponse body = ApiErrorResponse.of(
                429,
                "Too Many Requests",
                requestUri != null && requestUri.startsWith("/api/v1/") ? "rate_limited" : "RATE_LIMITED",
                i18nService.msg(request, "too_many_requests"),
                requestUri,
                trace == null ? "" : String.valueOf(trace)
        );

        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
        return false;
    }

    private String buildRateLimitKey(HttpServletRequest request, String requestUri) {
        String route = normalizeRouteKeyPart(requestUri, "unknown-route");
        String ip = normalizeKeyPart(request.getRemoteAddr(), "unknown");
        AuthPrincipal cachedPrincipal = getCachedAuthenticatedPrincipal(request);
        if (cachedPrincipal != null) {
            return buildRateLimitKey(cachedPrincipal, ip, route);
        }
        String tenantId = normalizeRequestAttributePart(request.getAttribute(AUTH_TENANT_ID_ATTR));
        String username = normalizeRequestAttributePart(request.getAttribute(AUTH_USERNAME_ATTR));
        if (tenantId != null && username != null) {
            return tenantId + "|" + username + "|" + route;
        }
        if (username != null) {
            return buildUsernameIpRouteKey(username, ip, route);
        }

        AuthPrincipal principal = resolveAuthenticatedPrincipal(request);
        if (principal != null) {
            cacheAuthenticatedPrincipal(request, principal);
            return buildRateLimitKey(principal, ip, route);
        }
        return ip + "|" + route;
    }

    private String buildRateLimitKey(AuthPrincipal principal, String ip, String route) {
        String principalTenantId = normalizeRequestAttributePart(principal.getTenantId());
        String principalUsername = normalizeRequestAttributePart(principal.getUsername());
        if (principalTenantId != null && principalUsername != null) {
            return principalTenantId + "|" + principalUsername + "|" + route;
        }
        if (principalUsername != null) {
            return buildUsernameIpRouteKey(principalUsername, ip, route);
        }
        return ip + "|" + route;
    }

    private String buildUsernameIpRouteKey(String username, String ip, String route) {
        return "user:" + username + "|" + ip + "|" + route;
    }

    private void cacheAuthenticatedPrincipal(HttpServletRequest request, AuthPrincipal principal) {
        request.setAttribute(AUTH_PRINCIPAL_ATTR, principal);
        request.setAttribute(AUTH_USERNAME_ATTR, principal.getUsername());
        request.setAttribute(AUTH_ROLE_ATTR, principal.getRole());
        request.setAttribute(AUTH_OWNER_SCOPE_ATTR, principal.getOwnerScope());
        request.setAttribute(AUTH_TENANT_ID_ATTR, principal.getTenantId());
        request.setAttribute(AUTH_MFA_VERIFIED_ATTR, principal.isMfaVerified());
    }

    private AuthPrincipal getCachedAuthenticatedPrincipal(HttpServletRequest request) {
        Object principal = request.getAttribute(AUTH_PRINCIPAL_ATTR);
        if (principal instanceof AuthPrincipal) {
            AuthPrincipal cachedPrincipal = (AuthPrincipal) principal;
            if (hasText(cachedPrincipal.getUsername())
                    && hasText(cachedPrincipal.getRole())
                    && hasText(cachedPrincipal.getOwnerScope())
                    && hasText(cachedPrincipal.getTenantId())) {
                return cachedPrincipal;
            }
        }
        return null;
    }

    private AuthPrincipal resolveAuthenticatedPrincipal(HttpServletRequest request) {
        AuthPrincipal cachedPrincipal = getCachedAuthenticatedPrincipal(request);
        if (cachedPrincipal != null) {
            return cachedPrincipal;
        }
        String token = resolveToken(request);
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        return verifySafely(token.trim());
    }

    private String resolveToken(HttpServletRequest request) {
        String token = resolveBearerToken(request.getHeader("Authorization"));
        if (token != null) {
            return token;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie != null && sessionCookieService.cookieName().equals(cookie.getName())) {
                String value = cookie.getValue();
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            }
        }
        return null;
    }

    private String resolveBearerToken(String authHeader) {
        if (authHeader == null) {
            return null;
        }
        String candidate = authHeader.trim();
        if (candidate.length() < 7 || !candidate.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = candidate.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private AuthPrincipal verifySafely(String token) {
        try {
            return tokenService.verify(token);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalizeRequestAttributePart(Object value) {
        if (!(value instanceof String)) {
            return null;
        }
        String candidate = ((String) value).trim();
        if (candidate.isEmpty()) {
            return null;
        }
        return candidate.replace("|", "%7C");
    }

    private String normalizeRouteKeyPart(String requestUri, String fallback) {
        String candidate = normalizeKeyPart(requestUri, fallback);
        if (fallback.equals(candidate)) {
            return candidate;
        }
        String[] segments = candidate.split("/");
        StringBuilder normalized = new StringBuilder(candidate.length());
        for (String segment : segments) {
            if (segment.isEmpty()) {
                continue;
            }
            normalized.append('/');
            normalized.append(normalizeRouteSegment(segment));
        }
        return normalized.length() == 0 ? fallback : normalized.toString();
    }

    private String normalizeRouteSegment(String segment) {
        if (NUMERIC_PATH_SEGMENT.matcher(segment).matches()) {
            return "{id}";
        }
        if (UUID_PATH_SEGMENT.matcher(segment).matches()) {
            return "{uuid}";
        }
        return segment.replace("|", "%7C");
    }

    private String normalizeKeyPart(String value, String fallback) {
        String candidate = value == null ? "" : value.trim();
        if (candidate.isEmpty()) {
            candidate = fallback;
        }
        return candidate.replace("|", "%7C");
    }

    private int resolveLimit(String uri) {
        if (uri == null) return 0;
        if (uri.contains("/auth/login")) return loginMaxRequests;
        if (uri.contains("/approval/")) return approvalMaxRequests;
        if (uri.contains("/batch-retry") || uri.contains("/retry-by-filter")) return batchRetryMaxRequests;
        if (uri.contains("/export-jobs")) return exportMaxRequests;
        return 0;
    }
}
