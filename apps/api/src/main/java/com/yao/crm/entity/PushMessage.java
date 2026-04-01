package com.yao.crm.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 消息推送实体 - 国内特色
 * 支持企业微信、钉钉、短信、邮件、站内通知
 */
@Entity
@Table(name = "push_messages")
public class PushMessage {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 80)
    private String userId;

    @Column(nullable = false, length = 20)
    private String channel;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 20)
    private String contentType;

    @Column(length = 40)
    private String relatedType;

    @Column(length = 64)
    private String relatedId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column
    private LocalDateTime sentAt;

    @Column
    private LocalDateTime readAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private Integer retryCount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (id == null) id = java.util.UUID.randomUUID().toString();
        if (createdAt == null) createdAt = now;
        if (retryCount == null) retryCount = 0;
        if (status == null || status.trim().isEmpty()) status = "PENDING";
        if (contentType == null || contentType.trim().isEmpty()) contentType = "TEXT";
        if (tenantId == null || tenantId.trim().isEmpty()) throw new IllegalStateException("tenant_id_required");
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getRelatedType() { return relatedType; }
    public void setRelatedType(String relatedType) { this.relatedType = relatedType; }

    public String getRelatedId() { return relatedId; }
    public void setRelatedId(String relatedId) { this.relatedId = relatedId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
