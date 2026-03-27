package com.yao.crm.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Aspect
@Component
public class ServiceLoggingAspect {

    @Around("execution(public * com.yao.crm.service..*(..))")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger log = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
        String methodName = joinPoint.getSignature().getName();
        
        // Summarize args (truncate long values)
        String argsSummary = summarizeArgs(joinPoint.getArgs());
        
        long start = System.currentTimeMillis();
        
        if (log.isDebugEnabled()) {
            log.debug(">>> {}.{}() args={}", 
                joinPoint.getTarget().getClass().getSimpleName(), methodName, argsSummary);
        }
        
        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            
            if (elapsed > 1000) {
                log.warn("<<< {}.{}() SLOW: {}ms", 
                    joinPoint.getTarget().getClass().getSimpleName(), methodName, elapsed);
            } else if (log.isDebugEnabled()) {
                log.debug("<<< {}.{}() returned in {}ms", 
                    joinPoint.getTarget().getClass().getSimpleName(), methodName, elapsed);
            }
            
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("!!! {}.{}() FAILED after {}ms: {}", 
                joinPoint.getTarget().getClass().getSimpleName(), methodName, elapsed, e.getMessage());
            throw e;
        }
    }
    
    private String summarizeArgs(Object[] args) {
        if (args == null || args.length == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            if (args[i] == null) {
                sb.append("null");
            } else {
                String str = args[i].toString();
                if (str.length() > 100) {
                    sb.append(str.substring(0, 100)).append("...");
                } else {
                    sb.append(str);
                }
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
