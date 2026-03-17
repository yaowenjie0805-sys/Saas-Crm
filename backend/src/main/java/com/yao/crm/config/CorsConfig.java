package com.yao.crm.config;

import com.yao.crm.security.AuthInterceptor;
import com.yao.crm.security.ApiDiagnosticsInterceptor;
import com.yao.crm.security.RateLimitInterceptor;
import com.yao.crm.security.TraceIdInterceptor;
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

    public CorsConfig(AuthInterceptor authInterceptor,
                      RateLimitInterceptor rateLimitInterceptor,
                      TraceIdInterceptor traceIdInterceptor,
                      ApiDiagnosticsInterceptor apiDiagnosticsInterceptor) {
        this.authInterceptor = authInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.traceIdInterceptor = traceIdInterceptor;
        this.apiDiagnosticsInterceptor = apiDiagnosticsInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:5173",
                        "http://127.0.0.1:5173",
                        "http://localhost:14173",
                        "http://127.0.0.1:14173"
                )
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "PUT", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders(TraceIdInterceptor.TRACE_ID_HEADER);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(traceIdInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(apiDiagnosticsInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
    }
}
