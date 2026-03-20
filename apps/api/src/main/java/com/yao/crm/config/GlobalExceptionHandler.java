package com.yao.crm.config;

import com.yao.crm.dto.ApiErrorResponse;
import com.yao.crm.security.TraceIdInterceptor;
import com.yao.crm.service.I18nService;
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleAnyException(Exception ex, HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                codeFor(request, "INTERNAL_ERROR", "internal_error"),
                i18nService.msg(request, "internal_error"),
                request.getRequestURI(),
                traceId(request)
        );
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("type", ex.getClass().getSimpleName());
        body.setDetails(details);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
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
