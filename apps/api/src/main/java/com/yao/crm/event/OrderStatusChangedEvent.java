package com.yao.crm.event;

public class OrderStatusChangedEvent extends DomainEvent {
    private final String previousStatus;
    private final String newStatus;

    public OrderStatusChangedEvent(String tenantId, String entityId, String triggeredBy,
                                   String previousStatus, String newStatus) {
        super("ORDER_STATUS_CHANGED", tenantId, entityId, "Order", triggeredBy);
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }
}
