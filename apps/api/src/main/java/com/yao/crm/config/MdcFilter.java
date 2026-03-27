package com.yao.crm.config;

import org.slf4j.MDC;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            
            // Generate or extract trace ID
            String traceId = httpRequest.getHeader("X-Trace-Id");
            if (traceId == null || traceId.isEmpty()) {
                traceId = UUID.randomUUID().toString().substring(0, 8);
            }
            
            MDC.put("traceId", traceId);
            MDC.put("tenantId", httpRequest.getHeader("X-Tenant-Id") != null ? 
                httpRequest.getHeader("X-Tenant-Id") : "unknown");
            
            // userId will be set by AuthInterceptor after authentication
            // We set a default here
            MDC.put("userId", "anonymous");
            
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
