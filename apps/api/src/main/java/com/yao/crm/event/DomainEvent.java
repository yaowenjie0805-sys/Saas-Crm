package com.yao.crm.event;

import java.time.Instant;
import java.util.UUID;

public abstract class DomainEvent {
    private final String eventId;
    private final String eventType;
    private final String tenantId;
    private final String entityId;
    private final String entityType;
    private final Instant timestamp;
    private final String triggeredBy; // userId

    protected DomainEvent(String eventType, String tenantId, String entityId,
                          String entityType, String triggeredBy) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.tenantId = tenantId;
        this.entityId = entityId;
        this.entityType = entityType;
        this.triggeredBy = triggeredBy;
        this.timestamp = Instant.now();
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    @Override
    public String toString() {
        return "DomainEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", entityId='" + entityId + '\'' +
                ", entityType='" + entityType + '\'' +
                ", timestamp=" + timestamp +
                ", triggeredBy='" + triggeredBy + '\'' +
                '}';
    }
}
