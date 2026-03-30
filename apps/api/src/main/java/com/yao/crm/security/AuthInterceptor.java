package com.yao.crm.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String AUTH_PRINCIPAL_ATTR = "authPrincipal";
    private static final String AUTH_USERNAME_ATTR = "authUsername";
    private static final String AUTH_ROLE_ATTR = "authRole";
    private static final String AUTH_OWNER_SCOPE_ATTR = "authOwnerScope";
    private static final String AUTH_TENANT_ID_ATTR = "authTenantId";
    private static final String AUTH_MFA_VERIFIED_ATTR = "authMfaVerified";
    private static final String AUTH_TENANT_DATE_FORMAT_ATTR = "authTenantDateFormat";
    private static final String DEFAULT_TENANT_DATE_FORMAT = "yyyy-MM-dd";
    private static final long TENANT_DATE_FORMAT_TTL_MINUTES = 5L;

    private final TokenService tokenService;
    private final I18nService i18nService;
    private final ObjectMapper objectMapper;
    private final TenantRepository tenantRepository;
    private final AuditLogService auditLogService;
    private final String sessionCookieName;
    private final Cache<String, String> tenantDateFormatCache;

    public AuthInterceptor(TokenService tokenService,
                           I18nService i18nService,
                           ObjectMapper objectMapper,
                           TenantRepository tenantRepository,
                           AuditLogService auditLogService,
                           SessionCookieService sessionCookieService) {
        this.tokenService = tokenService;
        this.i18nService = i18nService;
        this.objectMapper = objectMapper;
        this.tenantRepository = tenantRepository;
        this.auditLogService = auditLogService;
        this.sessionCookieName = sessionCookieService.cookieName();
        this.tenantDateFormatCache = Caffeine.newBuilder()
                .expireAfterWrite(TENANT_DATE_FORMAT_TTL_MINUTES, TimeUnit.MINUTES)
                .maximumSize(1024)
                .build();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI() == null ? "" : request.getRequestURI();
        if (path.startsWith("/api/health")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/register")
                || path.equals("/api/auth/sso/config")
                || path.equals("/api/auth/sso/login")
                || path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/mfa/verify")
                || path.equals("/api/v1/auth/invitations/accept")
            || path.equals("/api/v1/auth/oidc/callback")) {
            return true;
        }

        AuthPrincipal principal = getCachedAuthenticatedPrincipal(request);
        if (principal == null) {
            principal = buildPrincipalFromRequestAttributes(request);
        }
        if (principal == null) {
            String token = resolveToken(request);
            if (token == null || token.trim().isEmpty()) {
                writeUnauthorized(request, response, i18nService.msg(request, "missing_bearer"));
                return false;
            }
            principal = verifySafely(token);
            if (principal == null) {
                writeUnauthorized(request, response, i18nService.msg(request, "invalid_or_expired"));
                return false;
            }
            request.setAttribute(AUTH_PRINCIPAL_ATTR, principal);
        }
        String principalTenantId = normalizeTenantId(principal.getTenantId());
        cacheAuthenticatedPrincipal(request, principal);
        request.setAttribute(AUTH_TENANT_DATE_FORMAT_ATTR, getTenantDateFormat(principalTenantId));

        String headerTenant = request.getHeader("X-Tenant-Id");
        if (isTenantScopedPath(path) && !hasText(principalTenantId)) {
            writeUnauthorized(request, response, i18nService.msg(request, "invalid_or_expired"));
            return false;
        }
        if (isTenantScopedPath(path)) {
            if (headerTenant == null || headerTenant.trim().isEmpty()) {
                markCrossTenantForbidden(request, principal, "tenant_header_missing");
                writeForbidden(request, response, i18nService.msg(request, "tenant_header_required"));
                return false;
            }
            if (!headerTenant.trim().equals(principalTenantId)) {
                markCrossTenantForbidden(request, principal, "tenant_mismatch");
                writeForbidden(request, response, i18nService.msg(request, "tenant_mismatch"));
                return false;
            }
        } else if (headerTenant != null && !headerTenant.trim().isEmpty()) {
            if (!hasText(principalTenantId) || !headerTenant.trim().equals(principalTenantId)) {
                markCrossTenantForbidden(request, principal, "tenant_mismatch");
                writeForbidden(request, response, i18nService.msg(request, "tenant_mismatch"));
                return false;
            }
        }
        return true;
    }

    private AuthPrincipal buildPrincipalFromRequestAttributes(HttpServletRequest request) {
        String cachedUsername = readRequestAttribute(request, AUTH_USERNAME_ATTR);
        String cachedRole = readRequestAttribute(request, AUTH_ROLE_ATTR);
        String cachedOwnerScope = readRequestAttribute(request, AUTH_OWNER_SCOPE_ATTR);
        String cachedTenantId = readRequestAttribute(request, AUTH_TENANT_ID_ATTR);
        Boolean cachedMfaVerified = readBooleanRequestAttribute(request, AUTH_MFA_VERIFIED_ATTR);
        if (cachedUsername != null
                && cachedRole != null
                && cachedOwnerScope != null
                && cachedTenantId != null
                && cachedMfaVerified != null) {
            AuthPrincipal principal = new AuthPrincipal(
                    cachedUsername,
                    cachedRole,
                    cachedOwnerScope,
                    cachedTenantId,
                    cachedMfaVerified
            );
            request.setAttribute(AUTH_PRINCIPAL_ATTR, principal);
            return principal;
        }
        return null;
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

    private void cacheAuthenticatedPrincipal(HttpServletRequest request, AuthPrincipal principal) {
        request.setAttribute(AUTH_PRINCIPAL_ATTR, principal);
        request.setAttribute(AUTH_USERNAME_ATTR, normalizePrincipalAttribute(principal.getUsername(), ""));
        request.setAttribute(AUTH_ROLE_ATTR, normalizePrincipalAttribute(principal.getRole(), ""));
        request.setAttribute(AUTH_OWNER_SCOPE_ATTR, normalizePrincipalAttribute(principal.getOwnerScope(), ""));
        request.setAttribute(AUTH_TENANT_ID_ATTR, normalizeTenantId(principal.getTenantId()));
        request.setAttribute(AUTH_MFA_VERIFIED_ATTR, principal.isMfaVerified());
    }

    private String readRequestAttribute(HttpServletRequest request, String attributeName) {
        Object value = request.getAttribute(attributeName);
        if (!(value instanceof String)) {
            return null;
        }
        String candidate = ((String) value).trim();
        return candidate.isEmpty() ? null : candidate;
    }

    private Boolean readBooleanRequestAttribute(HttpServletRequest request, String attributeName) {
        Object value = request.getAttribute(attributeName);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            String candidate = ((String) value).trim();
            if (candidate.isEmpty()) {
                return null;
            }
            return Boolean.parseBoolean(candidate);
        }
        return null;
    }

    private String getTenantDateFormat(String tenantId) {
        String normalizedTenantId = normalizeTenantId(tenantId);
        if (normalizedTenantId == null) {
            return DEFAULT_TENANT_DATE_FORMAT;
        }
        return tenantDateFormatCache.get(normalizedTenantId, this::loadTenantDateFormat);
    }

    private String loadTenantDateFormat(String tenantId) {
        String normalizedTenantId = normalizeTenantId(tenantId);
        if (normalizedTenantId == null) {
            return DEFAULT_TENANT_DATE_FORMAT;
        }
        String tenantDateFormat = tenantRepository.findById(normalizedTenantId)
                .map(t -> t.getDateFormat())
                .orElse(DEFAULT_TENANT_DATE_FORMAT);
        if ("YYYY-MM-DD".equalsIgnoreCase(tenantDateFormat)) {
            return DEFAULT_TENANT_DATE_FORMAT;
        }
        if (tenantDateFormat == null || tenantDateFormat.trim().isEmpty()) {
            return DEFAULT_TENANT_DATE_FORMAT;
        }
        return tenantDateFormat;
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
            if (cookie != null && sessionCookieName.equals(cookie.getName())) {
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

    private String normalizeTenantId(String tenantId) {
        return normalizePrincipalAttribute(tenantId, null);
    }

    private String normalizePrincipalAttribute(String value, String fallback) {
        String candidate = value == null ? "" : value.trim();
        if (candidate.isEmpty()) {
            return fallback;
        }
        return candidate;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean isTenantScopedPath(String path) {
        return path != null && (path.startsWith("/api/v1/") || path.startsWith("/api/v2/"));
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response, String message) throws Exception {
        String path = request.getRequestURI() == null ? "" : request.getRequestURI();
        String code = path.startsWith("/api/v1/") ? "unauthorized" : "UNAUTHORIZED";
        request.setAttribute("apiErrorCode", code);
        writeError(response, request, 401, code, message);
    }

    private void writeForbidden(HttpServletRequest request, HttpServletResponse response, String message) throws Exception {
        String path = request.getRequestURI() == null ? "" : request.getRequestURI();
        String code = path.startsWith("/api/v1/") ? "forbidden" : "FORBIDDEN";
        request.setAttribute("apiErrorCode", code);
        writeError(response, request, 403, code, message);
    }

    private void markCrossTenantForbidden(HttpServletRequest request, AuthPrincipal principal, String reason) {
        String traceId = String.valueOf(request.getAttribute(TraceIdInterceptor.TRACE_ID_ATTR) == null
                ? ""
                : request.getAttribute(TraceIdInterceptor.TRACE_ID_ATTR));
        String details = "reason=" + reason
                + ";route=" + request.getRequestURI()
                + ";requestId=" + traceId;
        auditLogService.record(
                principal == null ? "unknown" : principal.getUsername(),
                principal == null ? "UNKNOWN" : principal.getRole(),
                "TENANT_FORBIDDEN",
                "AUTHZ",
                request.getRequestURI(),
                details,
                principal == null ? "tenant_default" : principal.getTenantId()
        );
    }

    private void writeError(HttpServletResponse response, HttpServletRequest request, int status, String code, String message) throws Exception {
        Object traceId = request.getAttribute(TraceIdInterceptor.TRACE_ID_ATTR);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("code", code);
        body.put("message", message);
        body.put("requestId", traceId == null ? "" : String.valueOf(traceId));
        body.put("details", new LinkedHashMap<String, Object>());
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
