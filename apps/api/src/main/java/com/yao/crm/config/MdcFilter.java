package com.yao.crm.config;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcFilter implements Filter {
    private static final Pattern SAFE_TRACE_ID = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");
    private static final Pattern SAFE_TENANT_ID = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            MDC.put("traceId", sanitizeTraceId(httpRequest.getHeader("X-Trace-Id")));
            MDC.put("tenantId", sanitizeTenantId(httpRequest.getHeader("X-Tenant-Id")));
            // userId will be set by AuthInterceptor after authentication
            MDC.put("userId", "anonymous");
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String sanitizeTraceId(String candidate) {
        if (candidate != null) {
            String normalized = candidate.trim();
            if (SAFE_TRACE_ID.matcher(normalized).matches()) {
                return normalized;
            }
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String sanitizeTenantId(String candidate) {
        if (candidate != null) {
            String normalized = candidate.trim();
            if (SAFE_TENANT_ID.matcher(normalized).matches()) {
                return normalized;
            }
        }
        return "unknown";
    }
}
