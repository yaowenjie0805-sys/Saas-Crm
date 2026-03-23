package com.yao.crm.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 审批节点配置实体 - 国内特色
 * 支持加签、转交、驳回等审批功能
 */
@Entity
@Table(name = "approval_nodes")
public class ApprovalNode {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String workflowNodeId;

    @Column(nullable = false, length = 20)
    private String approvalType;

    @Column(nullable = false, length = 20)
    private String approverType;

    @Column(length = 512)
    private String approverIds;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String approverConfig;

    @Column
    private Integer slaHours;

    @Column(nullable = false)
    private Boolean allowAddSign;

    @Column(nullable = false)
    private Boolean allowTransfer;

    @Column(nullable = false)
    private Boolean allowReject;

    @Column(length = 20)
    private String rejectTo;

    @Column(length = 64)
    private String rejectNodeId;

    @Column(nullable = false)
    private Boolean notifyOnCreate;

    @Column(nullable = false)
    private Boolean notifyOnComplete;

    @Column(nullable = false)
    private Boolean notifyOnTimeout;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (id == null) id = java.util.UUID.randomUUID().toString();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (allowAddSign == null) allowAddSign = true;
        if (allowTransfer == null) allowTransfer = true;
        if (allowReject == null) allowReject = true;
        if (notifyOnCreate == null) notifyOnCreate = true;
        if (notifyOnComplete == null) notifyOnComplete = true;
        if (notifyOnTimeout == null) notifyOnTimeout = true;
        if (approvalType == null || approvalType.trim().isEmpty()) approvalType = "SINGLE";
        if (approverType == null || approverType.trim().isEmpty()) approverType = "USER";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkflowNodeId() { return workflowNodeId; }
    public void setWorkflowNodeId(String workflowNodeId) { this.workflowNodeId = workflowNodeId; }

    public String getApprovalType() { return approvalType; }
    public void setApprovalType(String approvalType) { this.approvalType = approvalType; }

    public String getApproverType() { return approverType; }
    public void setApproverType(String approverType) { this.approverType = approverType; }

    public String getApproverIds() { return approverIds; }
    public void setApproverIds(String approverIds) { this.approverIds = approverIds; }

    public String getApproverConfig() { return approverConfig; }
    public void setApproverConfig(String approverConfig) { this.approverConfig = approverConfig; }

    public Integer getSlaHours() { return slaHours; }
    public void setSlaHours(Integer slaHours) { this.slaHours = slaHours; }

    public Boolean getAllowAddSign() { return allowAddSign; }
    public void setAllowAddSign(Boolean allowAddSign) { this.allowAddSign = allowAddSign; }

    public Boolean getAllowTransfer() { return allowTransfer; }
    public void setAllowTransfer(Boolean allowTransfer) { this.allowTransfer = allowTransfer; }

    public Boolean getAllowReject() { return allowReject; }
    public void setAllowReject(Boolean allowReject) { this.allowReject = allowReject; }

    public String getRejectTo() { return rejectTo; }
    public void setRejectTo(String rejectTo) { this.rejectTo = rejectTo; }

    public String getRejectNodeId() { return rejectNodeId; }
    public void setRejectNodeId(String rejectNodeId) { this.rejectNodeId = rejectNodeId; }

    public Boolean getNotifyOnCreate() { return notifyOnCreate; }
    public void setNotifyOnCreate(Boolean notifyOnCreate) { this.notifyOnCreate = notifyOnCreate; }

    public Boolean getNotifyOnComplete() { return notifyOnComplete; }
    public void setNotifyOnComplete(Boolean notifyOnComplete) { this.notifyOnComplete = notifyOnComplete; }

    public Boolean getNotifyOnTimeout() { return notifyOnTimeout; }
    public void setNotifyOnTimeout(Boolean notifyOnTimeout) { this.notifyOnTimeout = notifyOnTimeout; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
