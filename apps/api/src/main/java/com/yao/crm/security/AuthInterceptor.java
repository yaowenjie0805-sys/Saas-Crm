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

        String path = request.getRequestURI();
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

        String token = resolveToken(request);
        if (token == null || token.trim().isEmpty()) {
            writeUnauthorized(request, response, i18nService.msg(request, "missing_bearer"));
            return false;
        }
        AuthPrincipal principal = tokenService.verify(token);
        if (principal == null) {
            writeUnauthorized(request, response, i18nService.msg(request, "invalid_or_expired"));
            return false;
        }

        request.setAttribute("authUsername", principal.getUsername());
        request.setAttribute("authRole", principal.getRole());
        request.setAttribute("authOwnerScope", principal.getOwnerScope());
        request.setAttribute("authTenantId", principal.getTenantId());
        request.setAttribute("authMfaVerified", principal.isMfaVerified());
        request.setAttribute("authTenantDateFormat", getTenantDateFormat(principal.getTenantId()));

        String headerTenant = request.getHeader("X-Tenant-Id");
        if (path.startsWith("/api/v1/") || path.startsWith("/api/v2/")) {
            if (headerTenant == null || headerTenant.trim().isEmpty()) {
                markCrossTenantForbidden(request, principal, "tenant_header_missing");
                writeForbidden(request, response, i18nService.msg(request, "tenant_header_required"));
                return false;
            }
            if (!headerTenant.trim().equals(principal.getTenantId())) {
                markCrossTenantForbidden(request, principal, "tenant_mismatch");
                writeForbidden(request, response, i18nService.msg(request, "tenant_mismatch"));
                return false;
            }
        } else if (headerTenant != null && !headerTenant.trim().isEmpty() && !headerTenant.trim().equals(principal.getTenantId())) {
            markCrossTenantForbidden(request, principal, "tenant_mismatch");
            writeForbidden(request, response, i18nService.msg(request, "tenant_mismatch"));
            return false;
        }
        return true;
    }

    private String getTenantDateFormat(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            return DEFAULT_TENANT_DATE_FORMAT;
        }
        return tenantDateFormatCache.get(tenantId, this::loadTenantDateFormat);
    }

    private String loadTenantDateFormat(String tenantId) {
        String tenantDateFormat = tenantRepository.findById(tenantId)
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
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isEmpty()) {
                return token;
            }
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
