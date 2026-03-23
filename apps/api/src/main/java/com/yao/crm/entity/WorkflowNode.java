package com.yao.crm.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 工作流节点实体
 */
@Entity
@Table(name = "workflow_nodes")
public class WorkflowNode {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String workflowId;

    @Column(nullable = false, length = 40)
    private String nodeType;

    @Column(length = 80)
    private String nodeSubtype;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer positionX;

    @Column(nullable = false)
    private Integer positionY;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String configJson;

    @Column(columnDefinition = "TEXT")
    private String inputMapping;

    @Column(columnDefinition = "TEXT")
    private String outputMapping;

    @Column(nullable = false, length = 20)
    private String configValidation;

    @Column(columnDefinition = "TEXT")
    private String validationMessage;

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
        if (configValidation == null || configValidation.trim().isEmpty()) configValidation = "VALID";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }

    public String getNodeSubtype() { return nodeSubtype; }
    public void setNodeSubtype(String nodeSubtype) { this.nodeSubtype = nodeSubtype; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getPositionX() { return positionX; }
    public void setPositionX(Integer positionX) { this.positionX = positionX; }

    public Integer getPositionY() { return positionY; }
    public void setPositionY(Integer positionY) { this.positionY = positionY; }

    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }

    public String getInputMapping() { return inputMapping; }
    public void setInputMapping(String inputMapping) { this.inputMapping = inputMapping; }

    public String getOutputMapping() { return outputMapping; }
    public void setOutputMapping(String outputMapping) { this.outputMapping = outputMapping; }

    public String getConfigValidation() { return configValidation; }
    public void setConfigValidation(String configValidation) { this.configValidation = configValidation; }

    public String getValidationMessage() { return validationMessage; }
    public void setValidationMessage(String validationMessage) { this.validationMessage = validationMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
