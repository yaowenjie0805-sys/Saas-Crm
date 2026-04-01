package com.yao.crm.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "approval_templates")
public class ApprovalTemplate {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 40)
    private String bizType;

    @Column(nullable = false, length = 80)
    private String name;

    @Column
    private Long amountMin;

    @Column
    private Long amountMax;

    @Column(length = 40)
    private String role;

    @Column(length = 80)
    private String department;

    @Column(nullable = false, length = 400)
    private String approverRoles;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String flowDefinition;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (tenantId == null || tenantId.trim().isEmpty()) throw new IllegalStateException("tenant_id_required");
        if (enabled == null) enabled = true;
        if (version == null || version < 1) version = 1;
        if (status == null || status.trim().isEmpty()) status = enabled ? "PUBLISHED" : "DRAFT";
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
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getAmountMin() { return amountMin; }
    public void setAmountMin(Long amountMin) { this.amountMin = amountMin; }
    public Long getAmountMax() { return amountMax; }
    public void setAmountMax(Long amountMax) { this.amountMax = amountMax; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getApproverRoles() { return approverRoles; }
    public void setApproverRoles(String approverRoles) { this.approverRoles = approverRoles; }
    public String getFlowDefinition() { return flowDefinition; }
    public void setFlowDefinition(String flowDefinition) { this.flowDefinition = flowDefinition; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
