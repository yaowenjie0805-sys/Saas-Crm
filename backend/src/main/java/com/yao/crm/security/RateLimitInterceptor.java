package com.yao.crm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.dto.ApiErrorResponse;
import com.yao.crm.service.I18nService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;
    private final I18nService i18nService;
    private final ObjectMapper objectMapper;
    private final int loginMaxRequests;
    private final int approvalMaxRequests;
    private final int batchRetryMaxRequests;
    private final int exportMaxRequests;

    public RateLimitInterceptor(
            RateLimitService rateLimitService,
            I18nService i18nService,
            ObjectMapper objectMapper,
            @Value("${security.rate-limit.login.max-requests:30}") int loginMaxRequests,
            @Value("${security.rate-limit.approval.max-requests:120}") int approvalMaxRequests,
            @Value("${security.rate-limit.batch-retry.max-requests:20}") int batchRetryMaxRequests,
            @Value("${security.rate-limit.export.max-requests:40}") int exportMaxRequests
    ) {
        this.rateLimitService = rateLimitService;
        this.i18nService = i18nService;
        this.objectMapper = objectMapper;
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

        String ip = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
        String key = ip + "|" + request.getRequestURI();
        int maxRequests = resolveLimit(request.getRequestURI());
        if (rateLimitService.allow(key, maxRequests)) {
            return true;
        }

        Object trace = request.getAttribute(TraceIdInterceptor.TRACE_ID_ATTR);
        ApiErrorResponse body = ApiErrorResponse.of(
                429,
                "Too Many Requests",
                request.getRequestURI() != null && request.getRequestURI().startsWith("/api/v1/") ? "rate_limited" : "RATE_LIMITED",
                i18nService.msg(request, "too_many_requests"),
                request.getRequestURI(),
                trace == null ? "" : String.valueOf(trace)
        );

        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
        return false;
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
