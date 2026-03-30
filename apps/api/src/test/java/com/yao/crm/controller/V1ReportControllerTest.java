package com.yao.crm.controller;

import com.yao.crm.service.DashboardMetricsCacheService;
import com.yao.crm.service.I18nService;
import com.yao.crm.service.ReportExportJobService;
import com.yao.crm.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class V1ReportControllerTest {

    private ReportService reportService;
    private ReportExportJobService reportExportJobService;
    private V1ReportController controller;

    @BeforeEach
    void setUp() {
        reportService = mock(ReportService.class);
        reportExportJobService = mock(ReportExportJobService.class);
        controller = new V1ReportController(reportService, reportExportJobService, new I18nService());
    }

    @Test
    @SuppressWarnings("unchecked")
    void submitExportShouldRejectInvalidDateRange() {
        MockHttpServletRequest request = authedRequest("ANALYST");

        ResponseEntity<?> response = controller.submitExport(
                request,
                "",
                "",
                "",
                "2026-03-10",
                "2026-03-01",
                "",
                ""
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("date_range_invalid", body.get("code"));
        verifyNoInteractions(reportExportJobService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void overviewShouldTrimFiltersBeforeDelegating() {
        MockHttpServletRequest request = authedRequest("ANALYST");
        Map<String, Object> cachedBody = new HashMap<String, Object>();
        DashboardMetricsCacheService.CachedValue<Map<String, Object>> cached =
                new DashboardMetricsCacheService.CachedValue<Map<String, Object>>(cachedBody, false, "LOCAL", false);
        when(reportService.overviewByTenantCached(anyString(), anyString(), anyString(), any(LocalDate.class), any(LocalDate.class), anyString(), anyString(), anyString()))
                .thenReturn(cached);

        ResponseEntity<?> response = controller.overview(
                request,
                "2026-03-01",
                "2026-03-31",
                "  manager  ",
                "  north  ",
                "  alice  ",
                "  Asia/Shanghai  ",
                "  usd  "
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(reportService).overviewByTenantCached(
                "tenant_default",
                "alice",
                "ANALYST",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                "manager",
                "alice",
                "north"
        );
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        Map<String, Object> filters = (Map<String, Object>) body.get("filters");
        assertEquals("Asia/Shanghai", filters.get("timezone"));
        assertEquals("USD", filters.get("currency"));
        assertEquals("manager", filters.get("role"));
        assertEquals("north", filters.get("department"));
        assertEquals("alice", filters.get("owner"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void retryExportShouldReturnForbiddenWhenServiceThrowsForbidden() {
        MockHttpServletRequest request = authedRequest("ADMIN");
        when(reportExportJobService.retryByTenant(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenThrow(new IllegalArgumentException("forbidden"));

        ResponseEntity<?> response = controller.retryExport(request, " job_1 ");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("forbidden", body.get("code"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportStatusShouldReturnNotFoundWhenJobMissing() {
        MockHttpServletRequest request = authedRequest("ADMIN");
        when(reportExportJobService.statusByTenant(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenThrow(new IllegalArgumentException("export_job_not_found"));

        ResponseEntity<?> response = controller.exportStatus(request, "job_404");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("export_job_not_found", body.get("code"));
    }

    private MockHttpServletRequest authedRequest(String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authRole", role);
        request.setAttribute("authUsername", "alice");
        request.setAttribute("authTenantId", "tenant_default");
        return request;
    }
}
