package com.yao.crm.event;

import com.yao.crm.service.CacheService;
import com.yao.crm.service.DashboardMetricsCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CacheInvalidationListenerTest {

    @Mock
    private CacheService cacheService;

    @Mock
    private DashboardMetricsCacheService dashboardMetricsCacheService;

    private CacheInvalidationListener listener;

    @BeforeEach
    void setUp() {
        listener = new CacheInvalidationListener(cacheService, dashboardMetricsCacheService);
    }

    @Test
    void shouldInvalidateDashboardAndReportsAfterCustomerCreated() {
        var event = new CustomerCreatedEvent("tenant-id", "entity", "trigger", "name");

        listener.onCustomerCreated(event);

        verify(cacheService).invalidateDashboard("tenant-id");
        verify(cacheService).invalidateReport("tenant-id");
        verify(dashboardMetricsCacheService).evictDomain("tenant-id", "dashboard");
        verify(dashboardMetricsCacheService).evictDomain("tenant-id", "reports");
        verifyNoMoreInteractions(cacheService, dashboardMetricsCacheService);
    }

    @Test
    void shouldInvalidateDashboardDomainAfterCustomerUpdated() {
        var event = new CustomerUpdatedEvent("tenant-id", "entity", "trigger", "name");

        listener.onCustomerUpdated(event);

        verify(cacheService).invalidateDashboard("tenant-id");
        verify(dashboardMetricsCacheService).evictDomain("tenant-id", "dashboard");
        verifyNoMoreInteractions(cacheService, dashboardMetricsCacheService);
    }

    @Test
    void shouldInvalidateDashboardAndReportsAfterOrderStatusChanged() {
        var event = new OrderStatusChangedEvent("tenant-id", "order", "trigger", "OPEN", "CLOSED");

        listener.onOrderStatusChanged(event);

        verify(cacheService).invalidateDashboard("tenant-id");
        verify(cacheService).invalidateReport("tenant-id");
        verify(dashboardMetricsCacheService).evictDomain("tenant-id", "dashboard");
        verify(dashboardMetricsCacheService).evictDomain("tenant-id", "reports");
        verifyNoMoreInteractions(cacheService, dashboardMetricsCacheService);
    }

    @Test
    void shouldInvalidateDashboardDomainAfterApprovalCompleted() {
        var event = new ApprovalCompletedEvent("tenant-id", "approval", "trigger", "APPROVED", "template");

        listener.onApprovalCompleted(event);

        verify(cacheService).invalidateDashboard("tenant-id");
        verify(dashboardMetricsCacheService).evictDomain("tenant-id", "dashboard");
        verifyNoMoreInteractions(cacheService, dashboardMetricsCacheService);
    }
}
