package com.yao.crm.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.yao.crm.service.ApiRequestMetricsService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class ApiDiagnosticsInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiDiagnosticsInterceptor.class);
    private static final String START = "api_start_ms";
    private final long slowThresholdMs;
    private final ApiRequestMetricsService apiRequestMetricsService;

    public ApiDiagnosticsInterceptor(@Value("${ops.slow-log-threshold-ms:500}") long slowThresholdMs,
                                     ApiRequestMetricsService apiRequestMetricsService) {
        this.slowThresholdMs = Math.max(50L, slowThresholdMs);
        this.apiRequestMetricsService = apiRequestMetricsService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object startObj = request.getAttribute(START);
        if (!(startObj instanceof Long)) return;
        long cost = System.currentTimeMillis() - (Long) startObj;
        int status = response == null ? 0 : response.getStatus();
        String traceId = String.valueOf(request.getAttribute(TraceIdInterceptor.TRACE_ID_ATTR) == null ? "" : request.getAttribute(TraceIdInterceptor.TRACE_ID_ATTR));
        String tenantId = String.valueOf(request.getAttribute("authTenantId") == null ? "" : request.getAttribute("authTenantId"));
        String user = String.valueOf(request.getAttribute("authUsername") == null ? "" : request.getAttribute("authUsername"));
        String explicitCode = String.valueOf(request.getAttribute("apiErrorCode") == null ? "" : request.getAttribute("apiErrorCode"));
        String errorCode = explicitCode == null || explicitCode.trim().isEmpty()
                ? (status >= 400 ? "HTTP_" + status : "")
                : explicitCode.trim();
        apiRequestMetricsService.observe(request.getRequestURI(), status, cost, errorCode);

        log.info("api_request requestId={} tenantId={} user={} route={} method={} status={} latencyMs={} errorCode={}",
                traceId,
                tenantId,
                user,
                request.getRequestURI(),
                request.getMethod(),
                status,
                cost,
                errorCode);

        if (ex != null || status >= 500) {
            log.error("api_error requestId={} tenantId={} user={} route={} method={} status={} latencyMs={} errorCode={} err={}",
                    traceId,
                    tenantId,
                    user,
                    request.getRequestURI(),
                    request.getMethod(),
                    status,
                    cost,
                    errorCode,
                    ex == null ? "" : ex.getMessage());
            return;
        }
        if (cost >= slowThresholdMs) {
            log.warn("api_slow requestId={} tenantId={} user={} route={} method={} status={} latencyMs={}",
                    traceId,
                    tenantId,
                    user,
                    request.getRequestURI(),
                    request.getMethod(),
                    status,
                    cost);
        }
    }
}
