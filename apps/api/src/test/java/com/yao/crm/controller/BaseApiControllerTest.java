package com.yao.crm.controller;

import com.yao.crm.security.TenantRequirementMode;
import com.yao.crm.service.I18nService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private static final class TestController extends BaseApiController {

        private TestController(I18nService i18nService) {
            super(i18nService);
        }

        private String currentTenantForTest(MockHttpServletRequest request) {
            return currentTenant(request);
        }
    }
}
