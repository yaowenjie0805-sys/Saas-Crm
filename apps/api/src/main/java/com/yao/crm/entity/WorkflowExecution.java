package com.yao.crm.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 工作流执行记录实体
 */
@Entity
@Table(name = "workflow_executions")
public class WorkflowExecution extends BaseEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 64)
    private String workflowId;

    @Column(nullable = false)
    private Integer workflowVersion;

    @Column(nullable = false, length = 40)
    private String triggerType;

    @Column(length = 80)
    private String triggerSource;

    @Column(columnDefinition = "TEXT")
    private String triggerPayload;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 64)
    private String currentNodeId;

    @Column(columnDefinition = "TEXT")
    private String executionContext;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String errorDetails;

    @Column
    private Integer executionDurationMs;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (id == null) id = java.util.UUID.randomUUID().toString();
        if (startedAt == null) startedAt = now;
        if (workflowVersion == null) workflowVersion = 1;
        if (status == null || status.trim().isEmpty()) status = "RUNNING";
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

    public Integer getWorkflowVersion() { return workflowVersion; }
    public void setWorkflowVersion(Integer workflowVersion) { this.workflowVersion = workflowVersion; }

    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }

    public String getTriggerSource() { return triggerSource; }
    public void setTriggerSource(String triggerSource) { this.triggerSource = triggerSource; }

    public String getTriggerPayload() { return triggerPayload; }
    public void setTriggerPayload(String triggerPayload) { this.triggerPayload = triggerPayload; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(String currentNodeId) { this.currentNodeId = currentNodeId; }

    public String getExecutionContext() { return executionContext; }
    public void setExecutionContext(String executionContext) { this.executionContext = executionContext; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getErrorDetails() { return errorDetails; }
    public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }

    public Integer getExecutionDurationMs() { return executionDurationMs; }
    public void setExecutionDurationMs(Integer executionDurationMs) { this.executionDurationMs = executionDurationMs; }
}
