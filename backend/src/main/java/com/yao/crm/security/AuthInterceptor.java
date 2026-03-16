package com.yao.crm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.service.I18nService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;
    private final I18nService i18nService;
    private final ObjectMapper objectMapper;
    private final TenantRepository tenantRepository;

    public AuthInterceptor(TokenService tokenService, I18nService i18nService, ObjectMapper objectMapper, TenantRepository tenantRepository) {
        this.tokenService = tokenService;
        this.i18nService = i18nService;
        this.objectMapper = objectMapper;
        this.tenantRepository = tenantRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        if (path.equals("/api/health")
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

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorized(request, response, i18nService.msg(request, "missing_bearer"));
            return false;
        }

        String token = authHeader.substring(7);
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
        String tenantDateFormat = tenantRepository.findById(principal.getTenantId())
                .map(t -> t.getDateFormat())
                .orElse("yyyy-MM-dd");
        if ("YYYY-MM-DD".equalsIgnoreCase(tenantDateFormat)) {
            tenantDateFormat = "yyyy-MM-dd";
        }
        request.setAttribute("authTenantDateFormat", tenantDateFormat);

        String headerTenant = request.getHeader("X-Tenant-Id");
        if (path.startsWith("/api/v1/") || path.startsWith("/api/v2/")) {
            if (headerTenant == null || headerTenant.trim().isEmpty()) {
                writeForbidden(request, response, i18nService.msg(request, "tenant_header_required"));
                return false;
            }
            if (!headerTenant.trim().equals(principal.getTenantId())) {
                writeForbidden(request, response, i18nService.msg(request, "tenant_mismatch"));
                return false;
            }
        } else if (headerTenant != null && !headerTenant.trim().isEmpty() && !headerTenant.trim().equals(principal.getTenantId())) {
            writeForbidden(request, response, i18nService.msg(request, "tenant_mismatch"));
            return false;
        }
        return true;
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response, String message) throws Exception {
        String path = request.getRequestURI() == null ? "" : request.getRequestURI();
        String code = path.startsWith("/api/v1/") ? "unauthorized" : "UNAUTHORIZED";
        writeError(response, request, 401, code, message);
    }

    private void writeForbidden(HttpServletRequest request, HttpServletResponse response, String message) throws Exception {
        String path = request.getRequestURI() == null ? "" : request.getRequestURI();
        String code = path.startsWith("/api/v1/") ? "forbidden" : "FORBIDDEN";
        writeError(response, request, 403, code, message);
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
