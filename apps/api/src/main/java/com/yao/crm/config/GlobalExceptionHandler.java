package com.yao.crm.config;

import com.yao.crm.dto.ApiErrorResponse;
import com.yao.crm.exception.AccessDeniedException;
import com.yao.crm.exception.BusinessException;
import com.yao.crm.exception.BusinessRuleViolationException;
import com.yao.crm.exception.DuplicateEntityException;
import com.yao.crm.exception.EntityNotFoundException;
import com.yao.crm.exception.ValidationException;
import com.yao.crm.security.TraceIdInterceptor;
import com.yao.crm.service.I18nService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final I18nService i18nService;

    public GlobalExceptionHandler(I18nService i18nService) {
        this.i18nService = i18nService;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> validationErrors = new LinkedHashMap<String, String>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            if (fieldError == null || fieldError.getField() == null) {
                continue;
            }
            String key = fieldError.getDefaultMessage() == null ? "bad_request" : fieldError.getDefaultMessage();
            if (!validationErrors.containsKey(fieldError.getField())) {
                validationErrors.put(fieldError.getField(), i18nService.msg(request, key));
            }
        }

        String message = validationErrors.isEmpty()
                ? i18nService.msg(request, "bad_request")
                : validationErrors.values().iterator().next();

        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                codeFor(request, "VALIDATION_ERROR", "validation_error"),
                message,
                request.getRequestURI(),
                traceId(request)
        );

        if (!validationErrors.isEmpty()) {
            body.setValidationErrors(validationErrors);
        }
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("type", "validation");
        details.put("count", validationErrors.size());
        body.setDetails(details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception ex, HttpServletRequest request) {
        String code = codeFor(request, "BAD_REQUEST", "bad_request");
        String message = i18nService.msg(request, "bad_request");
        if (ex instanceof IllegalArgumentException && ex.getMessage() != null && !ex.getMessage().trim().isEmpty()) {
            String key = ex.getMessage().trim();
            if (request.getRequestURI() != null && request.getRequestURI().startsWith("/api/v1/")) {
                code = normalizeCode(key);
                message = i18nService.msg(request, key);
            } else {
                code = normalizeCode(key).toUpperCase(Locale.ROOT);
                message = i18nService.msg(request, key);
            }
        }
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                code,
                message,
                request.getRequestURI(),
                traceId(request)
        );
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("type", ex.getClass().getSimpleName());
        body.setDetails(details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ============ 业务异常处理 ============

    /**
     * 处理 EntityNotFoundException - HTTP 404
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        log.warn("EntityNotFoundException at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI(),
                traceId(request)
        );
        
        if (ex.hasField()) {
            Map<String, Object> details = new LinkedHashMap<String, Object>();
            details.put("field", ex.getField());
            body.setDetails(details);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * 处理 ValidationException - HTTP 400
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(ValidationException ex, HttpServletRequest request) {
        log.warn("ValidationException at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI(),
                traceId(request)
        );
        
        if (ex.hasField()) {
            Map<String, String> validationErrors = new LinkedHashMap<String, String>();
            validationErrors.put(ex.getField(), ex.getMessage());
            body.setValidationErrors(validationErrors);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * 处理 AccessDeniedException - HTTP 403
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("AccessDeniedException at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI(),
                traceId(request)
        );
        
        if (ex.hasField()) {
            Map<String, Object> details = new LinkedHashMap<String, Object>();
            details.put("field", ex.getField());
            body.setDetails(details);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    /**
     * 处理 DuplicateEntityException - HTTP 409
     */
    @ExceptionHandler(DuplicateEntityException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateEntity(DuplicateEntityException ex, HttpServletRequest request) {
        log.warn("DuplicateEntityException at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI(),
                traceId(request)
        );
        
        if (ex.hasField()) {
            Map<String, Object> details = new LinkedHashMap<String, Object>();
            details.put("field", ex.getField());
            body.setDetails(details);
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * 处理 BusinessRuleViolationException - HTTP 422
     */
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRuleViolation(BusinessRuleViolationException ex, HttpServletRequest request) {
        log.warn("BusinessRuleViolationException at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Unprocessable Entity",
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI(),
                traceId(request)
        );
        
        if (ex.hasField()) {
            Map<String, Object> details = new LinkedHashMap<String, Object>();
            details.put("field", ex.getField());
            body.setDetails(details);
        }
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    /**
     * 处理通用 BusinessException - 作为兜底处理
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("BusinessException at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Business Error",
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI(),
                traceId(request)
        );
        
        if (ex.hasField()) {
            Map<String, Object> details = new LinkedHashMap<String, Object>();
            details.put("field", ex.getField());
            body.setDetails(details);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ============ 系统异常处理 ============

    /**
     * 通用异常兜底处理
     * 区分可恢复异常（如 IO/网络异常）和不可恢复异常（如 Error）
     * 返回相应的 HTTP 状态码和错误分类
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleAnyException(Exception ex, HttpServletRequest request) {
        // 构建请求上下文日志
        String requestContext = buildRequestContext(request);
        
        // 判断异常类型并返回相应响应
        if (isRecoverableIOException(ex)) {
            log.warn("Recoverable IO error {}: {}", requestContext, ex.getMessage());
            return buildErrorResponse(ex, request, HttpStatus.SERVICE_UNAVAILABLE, "ServiceUnavailable", "service_unavailable", "RecoverableError");
        }
        
        if (isDatabaseTimeoutOrConnectionError(ex)) {
            log.warn("Database connection error {}: {}", requestContext, ex.getMessage());
            return buildErrorResponse(ex, request, HttpStatus.SERVICE_UNAVAILABLE, "DatabaseUnavailable", "database_unavailable", "DatabaseError");
        }
        
        if (isIllegalArgument(ex)) {
            log.warn("Bad request {}: {}", requestContext, ex.getMessage());
            return buildErrorResponse(ex, request, HttpStatus.BAD_REQUEST, "BadRequest", "bad_request", "ValidationError");
        }
        
        if (isSecurityError(ex)) {
            log.warn("Security error {}: {}", requestContext, ex.getMessage());
            return buildErrorResponse(ex, request, HttpStatus.INTERNAL_SERVER_ERROR, "SecurityError", "internal_error", "SecurityError");
        }
        
        // 其他未预期异常：记录完整堆栈
        log.error("Unexpected error {}: {}", requestContext, ex.getMessage(), ex);
        return buildErrorResponse(ex, request, HttpStatus.INTERNAL_SERVER_ERROR, "InternalError", "internal_error", sanitizeExceptionType(ex));
    }
    
    /**
     * 构建请求上下文日志字符串
     */
    private String buildRequestContext(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(request.getMethod()).append(" ").append(request.getRequestURI()).append("]");
        
        String userId = request.getHeader("X-User-Id");
        String tenantId = request.getHeader("X-Tenant-Id");
        
        if (userId != null || tenantId != null) {
            sb.append(" user=").append(userId != null ? userId : "anonymous");
            sb.append(" tenant=").append(tenantId != null ? tenantId : "none");
        }
        
        // 请求参数概要（防止敏感信息泄露）
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            sb.append(" qs=").append(queryString.length() > 100 ? queryString.substring(0, 100) + "..." : queryString);
        }
        
        return sb.toString();
    }
    
    /**
     * 判断是否为可恢复的 IO 异常
     */
    private boolean isRecoverableIOException(Exception ex) {
        return ex instanceof java.net.SocketTimeoutException
                || ex instanceof java.net.ConnectException
                || ex instanceof java.net.UnknownHostException
                || (ex instanceof java.io.IOException && !(ex instanceof java.io.EOFException));
    }
    
    /**
     * 判断是否为数据库超时或连接错误
     */
    private boolean isDatabaseTimeoutOrConnectionError(Exception ex) {
        String className = ex.getClass().getName();
        if (className.startsWith("java.sql.") || className.startsWith("javax.persistence.")) {
            return true;
        }
        if (className.startsWith("org.springframework.dao.") || className.startsWith("org.hibernate.")) {
            return true;
        }
        // 检查常见的数据库连接异常消息
        String message = ex.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            return lowerMessage.contains("connection") 
                    || lowerMessage.contains("timeout")
                    || lowerMessage.contains("unavailable")
                    || lowerMessage.contains("refused");
        }
        return false;
    }
    
    /**
     * 判断是否为 IllegalArgumentException（注意：IllegalArgumentException 已在 handleBadRequest 中处理，
     * 但如果逃逸到这里，仍需要正确分类）
     */
    private boolean isIllegalArgument(Exception ex) {
        return ex instanceof IllegalArgumentException;
    }
    
    /**
     * 判断是否为安全相关错误
     */
    private boolean isSecurityError(Exception ex) {
        String className = ex.getClass().getName();
        return className.startsWith("org.springframework.security.")
                || className.contains("AccessDenied")
                || className.contains("Authentication");
    }
    
    /**
     * 构建错误响应
     */
    private ResponseEntity<ApiErrorResponse> buildErrorResponse(Exception ex, HttpServletRequest request, 
            HttpStatus status, String errorType, String messageKey, String sanitizedType) {
        ApiErrorResponse body = ApiErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                codeFor(request, errorType, messageKey),
                i18nService.msg(request, messageKey),
                request.getRequestURI(),
                traceId(request)
        );
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("type", sanitizedType);
        body.setDetails(details);
        return ResponseEntity.status(status).body(body);
    }

    /**
     * 对异常类型进行脱敏处理，防止信息泄露
     * 已知安全的异常类型返回分类名，未知异常统一返回 "InternalError"
     */
    private String sanitizeExceptionType(Exception ex) {
        if (ex == null) {
            return "InternalError";
        }
        Class<?> clazz = ex.getClass();
        
        // 已知安全的异常类型映射
        if (clazz.getName().startsWith("com.yao.crm.exception.")) {
            return "BusinessError";
        }
        if (clazz.getName().startsWith("org.springframework.security.")) {
            return "SecurityError";
        }
        if (clazz.getName().startsWith("org.springframework.validation.")) {
            return "ValidationError";
        }
        if (clazz.getName().startsWith("org.springframework.web.bind.")) {
            return "BindingError";
        }
        if (clazz.getName().startsWith("java.sql.")) {
            return "DatabaseError";
        }
        if (clazz.getName().startsWith("org.springframework.dao.")) {
            return "DatabaseError";
        }
        if (clazz.getName().startsWith("java.io.")) {
            return "IOError";
        }
        if (clazz.getName().startsWith("java.net.")) {
            return "NetworkError";
        }
        if (clazz.getName().startsWith("javax.persistence.")) {
            return "PersistenceError";
        }
        if (clazz.getName().startsWith("org.hibernate.")) {
            return "PersistenceError";
        }
        
        // 未知异常类型统一返回 InternalError，不暴露具体类名
        return "InternalError";
    }

    private String traceId(HttpServletRequest request) {
        Object traceId = request.getAttribute(TraceIdInterceptor.TRACE_ID_ATTR);
        return traceId == null ? "" : String.valueOf(traceId);
    }

    private String codeFor(HttpServletRequest request, String legacyCode, String v1Code) {
        String path = request.getRequestURI() == null ? "" : request.getRequestURI();
        return path.startsWith("/api/v1/") ? v1Code : legacyCode;
    }

    private String normalizeCode(String value) {
        String v = value == null ? "" : value.trim().toLowerCase();
        if (v.isEmpty()) return "bad_request";
        v = v.replaceAll("[^a-z0-9]+", "_");
        v = v.replaceAll("^_+|_+$", "");
        return v.isEmpty() ? "bad_request" : v;
    }
}
