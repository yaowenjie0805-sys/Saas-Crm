package com.yao.crm.event;

public class CustomerUpdatedEvent extends DomainEvent {
    private final String customerName;

    public CustomerUpdatedEvent(String tenantId, String entityId, String triggeredBy, String customerName) {
        super("CUSTOMER_UPDATED", tenantId, entityId, "Customer", triggeredBy);
        this.customerName = customerName;
    }

    public String getCustomerName() {
        return customerName;
    }
}
