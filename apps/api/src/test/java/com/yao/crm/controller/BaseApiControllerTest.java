package com.yao.crm.controller;

import com.yao.crm.security.TenantRequirementMode;
import com.yao.crm.service.I18nService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class BaseApiControllerTest {

    private TestController controller;

    @BeforeEach
    void setUp() {
        controller = new TestController(mock(I18nService.class));
    }

    @AfterEach
    void tearDown() {
        new TenantRequirementMode(false);
    }

    @Test
    void currentTenantShouldRejectMissingWhenTenantContextAbsent() {
        new TenantRequirementMode(false);
        MockHttpServletRequest request = new MockHttpServletRequest();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> controller.currentTenantForTest(request)
        );

        assertEquals("tenant_header_required", exception.getMessage());
    }

    @Test
    void currentTenantShouldRejectMissingWhenRejectMissingEnabled() {
        new TenantRequirementMode(true);
        MockHttpServletRequest request = new MockHttpServletRequest();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> controller.currentTenantForTest(request)
        );

        assertEquals("tenant_header_required", exception.getMessage());
    }

    @Test
    void currentTenantShouldRejectMissingOnTenantScopedPathWhenRejectMissingDisabled() {
        new TenantRequirementMode(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/customers");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> controller.currentTenantForTest(request)
        );

        assertEquals("tenant_header_required", exception.getMessage());
    }

    @Test
    void currentTenantShouldRejectMissingForAuthenticatedRequestWhenRejectMissingDisabled() {
        new TenantRequirementMode(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/dashboard");
        request.setAttribute("authUsername", "alice");
        request.setAttribute("authRole", "ADMIN");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> controller.currentTenantForTest(request)
        );

        assertEquals("tenant_header_required", exception.getMessage());
    }

    @Test
    void currentTenantShouldRejectMissingOnV2PathWhenHeaderBlank() {
        new TenantRequirementMode(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v2/customers");
        request.addHeader("X-Tenant-Id", "   ");
        request.setAttribute("authTenantId", "  ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> controller.currentTenantForTest(request)
        );

        assertEquals("tenant_header_required", exception.getMessage());
    }

    @Test
    void errorBodyShouldIncludeOperationAndDefaultRetriable() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/orders");
        request.setAttribute("traceId", "trace-001");

        Map<String, Object> body = controller.errorBodyForTest(request, "conflict", "Conflict", null);

        assertEquals("POST /api/v1/orders", body.get("operation"));
        assertEquals("trace-001", body.get("requestId"));
        assertFalse((Boolean) body.get("retriable"));
    }

    @Test
    void errorBodyShouldReadRetriableFromDetails() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/reports/export-jobs");
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("retriable", true);

        Map<String, Object> body = controller.errorBodyForTest(request, "bad_gateway", "upstream failed", details);

        assertTrue((Boolean) body.get("retriable"));
    }

    private static final class TestController extends BaseApiController {

        private TestController(I18nService i18nService) {
            super(i18nService);
        }

        private String currentTenantForTest(MockHttpServletRequest request) {
            return currentTenant(request);
        }

        private Map<String, Object> errorBodyForTest(MockHttpServletRequest request,
                                                     String code,
                                                     String message,
                                                     Map<String, Object> details) {
            return errorBody(request, code, message, details);
        }
    }
}
