package com.yao.crm.event;

import com.yao.crm.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class CacheInvalidationListener {
    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationListener.class);
    private final CacheService cacheService;

    public CacheInvalidationListener(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @EventListener
    public void onCustomerCreated(CustomerCreatedEvent event) {
        log.debug("Invalidating cache for tenant {} after customer created", event.getTenantId());
        cacheService.invalidateDashboard(event.getTenantId());
        cacheService.invalidateReport(event.getTenantId());
    }

    @EventListener
    public void onCustomerUpdated(CustomerUpdatedEvent event) {
        log.debug("Invalidating cache for tenant {} after customer updated", event.getTenantId());
        cacheService.invalidateDashboard(event.getTenantId());
    }

    @EventListener
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        log.debug("Invalidating cache for tenant {} after order status changed", event.getTenantId());
        cacheService.invalidateDashboard(event.getTenantId());
        cacheService.invalidateReport(event.getTenantId());
    }

    @EventListener
    public void onApprovalCompleted(ApprovalCompletedEvent event) {
        log.debug("Invalidating cache for tenant {} after approval completed", event.getTenantId());
        cacheService.invalidateDashboard(event.getTenantId());
    }
}
