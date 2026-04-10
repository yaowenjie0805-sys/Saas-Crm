package com.yao.crm.config;

import com.yao.crm.dto.ApiErrorResponse;
import com.yao.crm.exception.*;
import com.yao.crm.security.TraceIdInterceptor;
import com.yao.crm.service.I18nService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GlobalExceptionHandler
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {
    private static class AuthenticationFailureException extends RuntimeException {
        AuthenticationFailureException(String message) {
            super(message);
        }
    }

    private static class AccessDeniedSecurityException extends RuntimeException {
        AccessDeniedSecurityException(String message) {
            super(message);
        }
    }

    @Mock
    private I18nService i18nService;

    @Mock
    private HttpServletRequest request;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(i18nService);
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        when(request.getAttribute(TraceIdInterceptor.TRACE_ID_ATTR)).thenReturn("test-trace-id");
    }

    @Test
    @DisplayName("shouldReturn404_whenEntityNotFoundException")
    void shouldReturn404_whenEntityNotFoundException() {
        EntityNotFoundException ex = new EntityNotFoundException("Customer not found");
        
        ResponseEntity<ApiErrorResponse> response = handler.handleEntityNotFound(ex, request);
        
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("ENTITY_NOT_FOUND", response.getBody().getCode());
        assertEquals("Customer not found", response.getBody().getMessage());
        assertEquals("/api/v1/test", response.getBody().getPath());
    }

    @Test
    @DisplayName("shouldReturn404WithField_whenEntityNotFoundExceptionWithField")
    void shouldReturn404WithField_whenEntityNotFoundExceptionWithField() {
        EntityNotFoundException ex = new EntityNotFoundException("Customer not found", "customerId");
        
        ResponseEntity<ApiErrorResponse> response = handler.handleEntityNotFound(ex, request);
        
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getDetails());
        assertEquals("customerId", response.getBody().getDetails().get("field"));
    }

    @Test
    @DisplayName("shouldReturn400_whenValidationException")
    void shouldReturn400_whenValidationException() {
        ValidationException ex = new ValidationException("Invalid email format");
        
        ResponseEntity<ApiErrorResponse> response = handler.handleValidationException(ex, request);
        
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().getCode());
        assertEquals("Invalid email format", response.getBody().getMessage());
    }

    @Test
    @DisplayName("shouldReturn400WithValidationErrors_whenValidationExceptionWithField")
    void shouldReturn400WithValidationErrors_whenValidationExceptionWithField() {
        ValidationException ex = new ValidationException("Invalid email format", "email");
        
        ResponseEntity<ApiErrorResponse> response = handler.handleValidationException(ex, request);
        
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getValidationErrors());
        assertEquals("Invalid email format", response.getBody().getValidationErrors().get("email"));
    }

    @Test
    @DisplayName("shouldReturn403_whenAccessDeniedException")
    void shouldReturn403_whenAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Access denied for this resource");
        
        ResponseEntity<ApiErrorResponse> response = handler.handleAccessDenied(ex, request);
        
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("ACCESS_DENIED", response.getBody().getCode());
        assertEquals("Access denied for this resource", response.getBody().getMessage());
    }

    @Test
    @DisplayName("shouldReturn403WithField_whenAccessDeniedExceptionWithField")
    void shouldReturn403WithField_whenAccessDeniedExceptionWithField() {
        AccessDeniedException ex = new AccessDeniedException("Access denied", "resourceId");
        
        ResponseEntity<ApiErrorResponse> response = handler.handleAccessDenied(ex, request);
        
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCodeValue());
        assertNotNull(response.getBody().getDetails());
        assertEquals("resourceId", response.getBody().getDetails().get("field"));
    }

    @Test
    @DisplayName("shouldReturn409_whenDuplicateEntityException")
    void shouldReturn409_whenDuplicateEntityException() {
        DuplicateEntityException ex = new DuplicateEntityException("Customer already exists");
        
        ResponseEntity<ApiErrorResponse> response = handler.handleDuplicateEntity(ex, request);
        
        assertEquals(HttpStatus.CONFLICT.value(), response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("DUPLICATE_ENTITY", response.getBody().getCode());
        assertEquals("Customer already exists", response.getBody().getMessage());
    }

    @Test
    @DisplayName("shouldReturn409WithField_whenDuplicateEntityExceptionWithField")
    void shouldReturn409WithField_whenDuplicateEntityExceptionWithField() {
        DuplicateEntityException ex = DuplicateEntityException.forField("Customer", "email", "test@example.com");
        
        ResponseEntity<ApiErrorResponse> response = handler.handleDuplicateEntity(ex, request);
        
        assertEquals(HttpStatus.CONFLICT.value(), response.getStatusCodeValue());
        assertNotNull(response.getBody().getDetails());
        assertEquals("email", response.getBody().getDetails().get("field"));
    }

    @Test
    @DisplayName("shouldReturn422_whenBusinessRuleViolationException")
    void shouldReturn422_whenBusinessRuleViolationException() {
        BusinessRuleViolationException ex = new BusinessRuleViolationException("Cannot delete customer with active orders");
        
        ResponseEntity<ApiErrorResponse> response = handler.handleBusinessRuleViolation(ex, request);
        
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("BUSINESS_RULE_VIOLATION", response.getBody().getCode());
        assertEquals("Cannot delete customer with active orders", response.getBody().getMessage());
    }

    @Test
    @DisplayName("shouldReturn422WithField_whenBusinessRuleViolationExceptionWithField")
    void shouldReturn422WithField_whenBusinessRuleViolationExceptionWithField() {
        BusinessRuleViolationException ex = new BusinessRuleViolationException("Rule violated", "orderId");
        
        ResponseEntity<ApiErrorResponse> response = handler.handleBusinessRuleViolation(ex, request);
        
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), response.getStatusCodeValue());
        assertNotNull(response.getBody().getDetails());
        assertEquals("orderId", response.getBody().getDetails().get("field"));
    }

    @Test
    @DisplayName("shouldReturn500_whenGenericException")
    void shouldReturn500_whenGenericException() {
        when(i18nService.msg(any(HttpServletRequest.class), anyString())).thenReturn("Internal server error");
        
        Exception ex = new RuntimeException("Unexpected error");
        
        ResponseEntity<ApiErrorResponse> response = handler.handleAnyException(ex, request);
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("shouldReturn401_whenSecurityAuthenticationException")
    void shouldReturn401_whenSecurityAuthenticationException() {
        when(i18nService.msg(any(HttpServletRequest.class), anyString())).thenReturn("Unauthorized");

        Exception ex = new AuthenticationFailureException("authentication failed");

        ResponseEntity<ApiErrorResponse> response = handler.handleAnyException(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCodeValue());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("shouldReturn403_whenSecurityAccessDeniedException")
    void shouldReturn403_whenSecurityAccessDeniedException() {
        when(i18nService.msg(any(HttpServletRequest.class), anyString())).thenReturn("Forbidden");

        Exception ex = new AccessDeniedSecurityException("access denied");

        ResponseEntity<ApiErrorResponse> response = handler.handleAnyException(ex, request);

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCodeValue());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("shouldIncludeTimestampInResponse_whenExceptionThrown")
    void shouldIncludeTimestampInResponse_whenExceptionThrown() {
        EntityNotFoundException ex = new EntityNotFoundException("Not found");
        
        ResponseEntity<ApiErrorResponse> response = handler.handleEntityNotFound(ex, request);
        
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("shouldReturn400_whenBusinessException")
    void shouldReturn400_whenBusinessException() {
        BusinessException ex = new BusinessException("CUSTOM_ERROR", "Custom business error");
        
        ResponseEntity<ApiErrorResponse> response = handler.handleBusinessException(ex, request);
        
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("CUSTOM_ERROR", response.getBody().getCode());
        assertEquals("Custom business error", response.getBody().getMessage());
    }
}
