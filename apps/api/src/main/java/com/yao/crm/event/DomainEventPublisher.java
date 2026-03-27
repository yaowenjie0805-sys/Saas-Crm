package com.yao.crm.event;

public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
