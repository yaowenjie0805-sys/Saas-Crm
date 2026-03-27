package com.yao.crm.event;

public class CustomerCreatedEvent extends DomainEvent {
    private final String customerName;

    public CustomerCreatedEvent(String tenantId, String entityId, String triggeredBy, String customerName) {
        super("CUSTOMER_CREATED", tenantId, entityId, "Customer", triggeredBy);
        this.customerName = customerName;
    }

    public String getCustomerName() {
        return customerName;
    }
}
