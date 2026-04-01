package com.yao.crm.security;

import com.yao.crm.service.DashboardMetricsCacheService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class DashboardCacheInvalidationInterceptor implements HandlerInterceptor {

    private final DashboardMetricsCacheService dashboardMetricsCacheService;

    public DashboardCacheInvalidationInterceptor(DashboardMetricsCacheService dashboardMetricsCacheService) {
        this.dashboardMetricsCacheService = dashboardMetricsCacheService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (ex != null) {
            return;
        }
        if (response == null || response.getStatus() >= 400) {
            return;
        }
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return;
        }
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith("/api")) {
            return;
        }
        Object tenantAttr = request.getAttribute("authTenantId");
        String tenantId = tenantAttr == null ? request.getHeader("X-Tenant-Id") : String.valueOf(tenantAttr);
        if (tenantId == null || tenantId.trim().isEmpty()) {
            return;
        }
        String domain = resolveDomain(uri);
        dashboardMetricsCacheService.evictDomain(tenantId, domain);
    }

    private String resolveDomain(String uri) {
        String raw = uri == null ? "" : uri.toLowerCase();
        if (raw.contains("/reports") || raw.contains("/audit")) return "reports";
        if (raw.contains("/quote") || raw.contains("/order") || raw.contains("/product") || raw.contains("/price-book")) return "commerce";
        if (raw.contains("/timeline")) return "timeline";
        if (raw.contains("/workbench") || raw.contains("/task") || raw.contains("/follow-up")) return "workbench";
        if (raw.contains("/customer") || raw.contains("/opportunit") || raw.contains("/lead")) return "dashboard";
        return "default";
    }
}
