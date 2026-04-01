package com.yao.crm.controller;

import com.yao.crm.entity.Tenant;
import com.yao.crm.repository.ApprovalTaskRepository;
import com.yao.crm.repository.LeadImportJobRepository;
import com.yao.crm.repository.NotificationJobRepository;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.service.ApiRequestMetricsService;
import com.yao.crm.service.AuditExportJobService;
import com.yao.crm.service.I18nService;
import com.yao.crm.service.NotificationJobScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.yao.crm.support.TestTenant.TENANT_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class V1OpsControllerTest {

    private DataSource dataSource;
    private NotificationJobScheduler notificationJobScheduler;
    private ApprovalTaskRepository approvalTaskRepository;
    private NotificationJobRepository notificationJobRepository;
    private LeadImportJobRepository leadImportJobRepository;
    private TenantRepository tenantRepository;
    private V1OpsController controller;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);

        notificationJobScheduler = mock(NotificationJobScheduler.class);
        approvalTaskRepository = mock(ApprovalTaskRepository.class);
        notificationJobRepository = mock(NotificationJobRepository.class);
        leadImportJobRepository = mock(LeadImportJobRepository.class);
        tenantRepository = mock(TenantRepository.class);
        HealthController healthController = mock(HealthController.class);
        AuditExportJobService auditExportJobService = mock(AuditExportJobService.class);
        ApiRequestMetricsService apiRequestMetricsService = mock(ApiRequestMetricsService.class);
        I18nService i18nService = mock(I18nService.class);
        when(i18nService.msg(any(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));

        controller = new V1OpsController(
                dataSource,
                notificationJobScheduler,
                approvalTaskRepository,
                notificationJobRepository,
                leadImportJobRepository,
                tenantRepository,
                healthController,
                auditExportJobService,
                apiRequestMetricsService,
                " wecom, dingtalk;wecom \n feishu ",
                0.02d,
                0.20d,
                1500L,
                0.10d,
                0.30d,
                0.02d,
                0.05d,
                0.05d,
                0.03d,
                0.02d,
                0.50d,
                0.35d,
                0.20d,
                "SRE Primary",
                "SRE Lead -> Eng Manager",
                i18nService
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void healthShouldNormalizeWebhookProviders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authRole", "ADMIN");
        request.setAttribute("authTenantId", "  " + TENANT_TEST + "  ");
        when(tenantRepository.findById(TENANT_TEST)).thenReturn(Optional.of(mock(Tenant.class)));

        ResponseEntity<?> response = controller.health(request, "  Asia/Shanghai ");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("ops_health_loaded", body.get("code"));
        assertEquals(TENANT_TEST, body.get("tenantId"));
        assertEquals("Asia/Shanghai", body.get("timezone"));
        assertEquals("WECOM,DINGTALK,FEISHU", body.get("webhookProviders"));
        assertEquals(Arrays.asList("WECOM", "DINGTALK", "FEISHU"), body.get("webhookProviderList"));
        assertTrue(Boolean.TRUE.equals(body.get("webhookConfigured")));
        verify(tenantRepository).findById(TENANT_TEST);
    }

    @Test
    @SuppressWarnings("unchecked")
    void healthShouldReturnInvalidTimezoneCodeWhenTimezoneInvalid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authRole", "ADMIN");
        request.setAttribute("authTenantId", TENANT_TEST);
        when(tenantRepository.findById(TENANT_TEST)).thenReturn(Optional.of(mock(Tenant.class)));

        ResponseEntity<?> response = controller.health(request, "  Mars/Phobos ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("invalid_timezone", body.get("code"));
        Map<String, Object> details = (Map<String, Object>) body.get("details");
        assertEquals("invalid_timezone", details.get("reason"));
        assertEquals("Mars/Phobos", details.get("timezone"));
        verifyNoInteractions(dataSource, notificationJobScheduler);
    }

    @Test
    @SuppressWarnings("unchecked")
    void metricsSummaryShouldUseGroupedNotificationQueries() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authRole", "ADMIN");
        request.setAttribute("authTenantId", TENANT_TEST);
        when(tenantRepository.findById(TENANT_TEST)).thenReturn(Optional.of(mock(Tenant.class)));

        when(approvalTaskRepository.countByTenantIdAndStatusIn(anyString(), any())).thenReturn(3L);
        when(approvalTaskRepository.countByTenantIdAndStatusInAndDeadlineAtBefore(anyString(), any(), any(LocalDateTime.class))).thenReturn(1L);

        when(notificationJobRepository.countGroupedByStatus(TENANT_TEST))
                .thenReturn(Arrays.<Object[]>asList(
                        new Object[]{"SUCCESS", 8L},
                        new Object[]{"FAILED", 2L},
                        new Object[]{"RETRY", 1L},
                        new Object[]{"PENDING", 3L},
                        new Object[]{"RUNNING", 1L}
                ));
        when(notificationJobRepository.countRetryBuckets(TENANT_TEST))
                .thenReturn(Arrays.<Object[]>asList(
                        new Object[]{"RETRY0", 7L},
                        new Object[]{"RETRY1TO2", 5L},
                        new Object[]{"RETRY3PLUS", 3L}
                ));
        when(leadImportJobRepository.findByTenantIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(anyString(), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        ResponseEntity<?> response = controller.metricsSummary(request, "Asia/Shanghai");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("ops_metrics_loaded", body.get("code"));
        assertEquals(15L, body.get("notificationTotal"));
        assertEquals(8L, body.get("notificationSuccess"));
        assertEquals(2L, body.get("notificationFailed"));
        assertEquals(1L, body.get("notificationRetry"));
        Map<String, Object> retry = (Map<String, Object>) body.get("notificationRetryDistribution");
        assertEquals(7L, retry.get("retry0"));
        assertEquals(5L, retry.get("retry1to2"));
        assertEquals(3L, retry.get("retry3plus"));
        verify(notificationJobRepository).countGroupedByStatus(TENANT_TEST);
        verify(notificationJobRepository).countRetryBuckets(TENANT_TEST);
        verify(notificationJobRepository, never()).countByTenantIdAndStatusIn(anyString(), any());
    }
}
