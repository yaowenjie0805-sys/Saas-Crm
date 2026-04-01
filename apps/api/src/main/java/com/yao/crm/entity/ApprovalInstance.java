package com.yao.crm.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "approval_instances")
public class ApprovalInstance {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 64)
    private String templateId;

    @Column
    private Integer templateVersion;

    @Column(nullable = false, length = 40)
    private String bizType;

    @Column(nullable = false, length = 64)
    private String bizId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false, length = 80)
    private String submitter;

    @Column(length = 500)
    private String comment;

    @Column
    private Integer currentSeq;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (tenantId == null || tenantId.trim().isEmpty()) throw new IllegalStateException("tenant_id_required");
        if (status == null || status.trim().isEmpty()) status = "PENDING";
        if (currentSeq == null || currentSeq < 1) currentSeq = 1;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public Integer getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(Integer templateVersion) { this.templateVersion = templateVersion; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public String getBizId() { return bizId; }
    public void setBizId(String bizId) { this.bizId = bizId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSubmitter() { return submitter; }
    public void setSubmitter(String submitter) { this.submitter = submitter; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Integer getCurrentSeq() { return currentSeq; }
    public void setCurrentSeq(Integer currentSeq) { this.currentSeq = currentSeq; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
