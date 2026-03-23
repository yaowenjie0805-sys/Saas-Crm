package com.yao.crm.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "approval_tasks")
public class ApprovalTask {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 64)
    private String instanceId;

    @Column(length = 64)
    private String templateId;

    @Column(nullable = false, length = 40)
    private String approverRole;

    @Column(length = 80)
    private String approverUser;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 500)
    private String comment;

    @Column(nullable = false)
    private Integer seq;

    @Column(length = 80)
    private String nodeKey;

    @Column
    private Integer slaMinutes;

    @Column
    private LocalDateTime deadlineAt;

    @Column
    private LocalDateTime slaDeadline;

    @Column
    private Integer priority;

    @Column(length = 64)
    private String parentTaskId;

    @Column(length = 20)
    private String addSignType;

    @Column(length = 200)
    private String escalateToRoles;

    @Column
    private Integer escalationLevel;

    @Column(length = 64)
    private String escalationSourceTaskId;

    @Column(length = 64)
    private String assigneeId;

    @Column(length = 64)
    private String delegatedFrom;

    @Column
    private LocalDateTime transferredAt;

    @Column
    private LocalDateTime delegatedAt;

    @Column(length = 500)
    private String transferHistory;

    @Column
    private LocalDateTime notifiedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (tenantId == null || tenantId.trim().isEmpty()) tenantId = "tenant_default";
        if (status == null || status.trim().isEmpty()) status = "PENDING";
        if (escalationLevel == null || escalationLevel < 0) escalationLevel = 0;
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
    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getApproverRole() { return approverRole; }
    public void setApproverRole(String approverRole) { this.approverRole = approverRole; }
    public String getApproverUser() { return approverUser; }
    public void setApproverUser(String approverUser) { this.approverUser = approverUser; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Integer getSeq() { return seq; }
    public void setSeq(Integer seq) { this.seq = seq; }
    public String getNodeKey() { return nodeKey; }
    public void setNodeKey(String nodeKey) { this.nodeKey = nodeKey; }
    public Integer getSlaMinutes() { return slaMinutes; }
    public void setSlaMinutes(Integer slaMinutes) { this.slaMinutes = slaMinutes; }
    public LocalDateTime getDeadlineAt() { return deadlineAt; }
    public void setDeadlineAt(LocalDateTime deadlineAt) { this.deadlineAt = deadlineAt; }
    public LocalDateTime getSlaDeadline() { return slaDeadline; }
    public void setSlaDeadline(LocalDateTime slaDeadline) { this.slaDeadline = slaDeadline; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getParentTaskId() { return parentTaskId; }
    public void setParentTaskId(String parentTaskId) { this.parentTaskId = parentTaskId; }
    public String getAddSignType() { return addSignType; }
    public void setAddSignType(String addSignType) { this.addSignType = addSignType; }
    public String getEscalateToRoles() { return escalateToRoles; }
    public void setEscalateToRoles(String escalateToRoles) { this.escalateToRoles = escalateToRoles; }
    public Integer getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(Integer escalationLevel) { this.escalationLevel = escalationLevel; }
    public String getEscalationSourceTaskId() { return escalationSourceTaskId; }
    public void setEscalationSourceTaskId(String escalationSourceTaskId) { this.escalationSourceTaskId = escalationSourceTaskId; }
    public LocalDateTime getNotifiedAt() { return notifiedAt; }
    public void setNotifiedAt(LocalDateTime notifiedAt) { this.notifiedAt = notifiedAt; }
    public String getAssigneeId() { return assigneeId; }
    public void setAssigneeId(String assigneeId) { this.assigneeId = assigneeId; }
    public String getDelegatedFrom() { return delegatedFrom; }
    public void setDelegatedFrom(String delegatedFrom) { this.delegatedFrom = delegatedFrom; }
    public LocalDateTime getTransferredAt() { return transferredAt; }
    public void setTransferredAt(LocalDateTime transferredAt) { this.transferredAt = transferredAt; }
    public LocalDateTime getDelegatedAt() { return delegatedAt; }
    public void setDelegatedAt(LocalDateTime delegatedAt) { this.delegatedAt = delegatedAt; }
    public String getTransferHistory() { return transferHistory; }
    public void setTransferHistory(String transferHistory) { this.transferHistory = transferHistory; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
