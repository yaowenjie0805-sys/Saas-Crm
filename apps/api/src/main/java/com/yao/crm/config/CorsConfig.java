package com.yao.crm.config;

import com.yao.crm.security.AuthInterceptor;
import com.yao.crm.security.ApiDiagnosticsInterceptor;
import com.yao.crm.security.DashboardCacheInvalidationInterceptor;
import com.yao.crm.security.RateLimitInterceptor;
import com.yao.crm.security.TraceIdInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final TraceIdInterceptor traceIdInterceptor;
    private final ApiDiagnosticsInterceptor apiDiagnosticsInterceptor;
    private final DashboardCacheInvalidationInterceptor dashboardCacheInvalidationInterceptor;
    private final String[] allowedOrigins;

    public CorsConfig(AuthInterceptor authInterceptor,
                      RateLimitInterceptor rateLimitInterceptor,
                      TraceIdInterceptor traceIdInterceptor,
                      ApiDiagnosticsInterceptor apiDiagnosticsInterceptor,
                      DashboardCacheInvalidationInterceptor dashboardCacheInvalidationInterceptor,
                      @Value("${security.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:5174,http://127.0.0.1:5174,http://localhost:5175,http://127.0.0.1:5175,http://localhost:5176,http://127.0.0.1:5176,http://localhost:5177,http://127.0.0.1:5177,http://localhost:14173,http://127.0.0.1:14173}") String allowedOrigins) {
        this.authInterceptor = authInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.traceIdInterceptor = traceIdInterceptor;
        this.apiDiagnosticsInterceptor = apiDiagnosticsInterceptor;
        this.dashboardCacheInvalidationInterceptor = dashboardCacheInvalidationInterceptor;
        this.allowedOrigins = splitCsv(allowedOrigins);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "PUT", "OPTIONS")
                .allowedHeaders("Content-Type", "Authorization", "X-Tenant-Id", "X-Requested-With", "Accept", "Origin", "Accept-Language")
                .allowCredentials(true)
                .exposedHeaders(TraceIdInterceptor.TRACE_ID_HEADER, "X-CRM-Cache", "X-CRM-Cache-Tier", "X-CRM-Cache-Fallback");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 追踪和诊断拦截器：所有 API 路径
        registry.addInterceptor(traceIdInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/health/**", "/api/actuator/**");
        
        registry.addInterceptor(apiDiagnosticsInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/health/**", "/api/actuator/**");
        
        // 限流和认证拦截器：仅需要认证的路径
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**");
        
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/v1/auth/login",
                        "/api/v1/auth/oidc/callback",
                        "/api/v1/auth/session",
                        "/api/v1/health/**",
                        "/api/health/**",
                        "/api/actuator/**"
                );
        
        registry.addInterceptor(dashboardCacheInvalidationInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/health/**", "/api/actuator/**");
    }

    private String[] splitCsv(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return new String[] {"http://localhost:5173"};
        }
        String[] parts = csv.split(",");
        int count = 0;
        for (String part : parts) {
            if (part != null && !part.trim().isEmpty()) {
                count++;
            }
        }
        String[] out = new String[Math.max(1, count)];
        int idx = 0;
        for (String part : parts) {
            if (part != null && !part.trim().isEmpty()) {
                out[idx++] = part.trim();
            }
        }
        if (idx == 0) {
            out[0] = "http://localhost:5173";
        }
        return out;
    }
}
