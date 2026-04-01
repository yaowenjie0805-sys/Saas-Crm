package com.yao.crm.event;

import com.yao.crm.service.CacheService;
import com.yao.crm.service.DashboardMetricsCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class CacheInvalidationListener {
    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationListener.class);
    private final CacheService cacheService;
    private final DashboardMetricsCacheService dashboardMetricsCacheService;

    public CacheInvalidationListener(CacheService cacheService,
                                     DashboardMetricsCacheService dashboardMetricsCacheService) {
        this.cacheService = cacheService;
        this.dashboardMetricsCacheService = dashboardMetricsCacheService;
    }

    @EventListener
    public void onCustomerCreated(CustomerCreatedEvent event) {
        log.debug("Invalidating cache for tenant {} after customer created", event.getTenantId());
        cacheService.invalidateDashboard(event.getTenantId());
        cacheService.invalidateReport(event.getTenantId());
        evictDashboardDomain(event.getTenantId());
        evictReportDomain(event.getTenantId());
    }

    @EventListener
    public void onCustomerUpdated(CustomerUpdatedEvent event) {
        log.debug("Invalidating cache for tenant {} after customer updated", event.getTenantId());
        cacheService.invalidateDashboard(event.getTenantId());
        evictDashboardDomain(event.getTenantId());
    }

    @EventListener
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        log.debug("Invalidating cache for tenant {} after order status changed", event.getTenantId());
        cacheService.invalidateDashboard(event.getTenantId());
        cacheService.invalidateReport(event.getTenantId());
        evictDashboardDomain(event.getTenantId());
        evictReportDomain(event.getTenantId());
    }

    @EventListener
    public void onApprovalCompleted(ApprovalCompletedEvent event) {
        log.debug("Invalidating cache for tenant {} after approval completed", event.getTenantId());
        cacheService.invalidateDashboard(event.getTenantId());
        evictDashboardDomain(event.getTenantId());
    }

    private void evictDashboardDomain(String tenantId) {
        dashboardMetricsCacheService.evictDomain(tenantId, "dashboard");
    }

    private void evictReportDomain(String tenantId) {
        dashboardMetricsCacheService.evictDomain(tenantId, "reports");
    }
}
