package com.yao.crm.security;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

@Component
public class TraceIdInterceptor implements HandlerInterceptor {

    public static final String TRACE_ID_ATTR = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String incoming = request.getHeader(TRACE_ID_HEADER);
        String traceId = (incoming == null || incoming.trim().isEmpty())
                ? UUID.randomUUID().toString().replace("-", "")
                : incoming.trim();
        request.setAttribute(TRACE_ID_ATTR, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        return true;
    }
}
