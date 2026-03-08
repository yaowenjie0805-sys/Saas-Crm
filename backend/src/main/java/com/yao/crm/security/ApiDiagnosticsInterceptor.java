package com.yao.crm.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class ApiDiagnosticsInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiDiagnosticsInterceptor.class);
    private static final String START = "api_start_ms";
    private final long slowThresholdMs;

    public ApiDiagnosticsInterceptor(@Value("${ops.slow-log-threshold-ms:500}") long slowThresholdMs) {
        this.slowThresholdMs = Math.max(50L, slowThresholdMs);
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
        if (ex != null || status >= 500) {
            log.error("api_error traceId={} method={} uri={} status={} costMs={} err={}", traceId, request.getMethod(), request.getRequestURI(), status, cost, ex == null ? "" : ex.getMessage());
            return;
        }
        if (cost >= slowThresholdMs) {
            log.warn("api_slow traceId={} method={} uri={} status={} costMs={}", traceId, request.getMethod(), request.getRequestURI(), status, cost);
        }
    }
}

