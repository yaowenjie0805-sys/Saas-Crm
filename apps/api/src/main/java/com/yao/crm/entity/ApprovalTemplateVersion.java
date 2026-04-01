package com.yao.crm.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "approval_template_versions")
public class ApprovalTemplateVersion {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 64)
    private String templateId;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false, length = 40)
    private String bizType;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 40)
    private String role;

    @Column(length = 80)
    private String department;

    @Column(nullable = false, length = 400)
    private String approverRoles;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String flowDefinition;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false, length = 80)
    private String publishedBy;

    @Column(nullable = false)
    private LocalDateTime publishedAt;

    @PrePersist
    public void prePersist() {
        if (tenantId == null || tenantId.trim().isEmpty()) throw new IllegalStateException("tenant_id_required");
        if (status == null || status.trim().isEmpty()) status = "PUBLISHED";
        if (publishedBy == null || publishedBy.trim().isEmpty()) publishedBy = "system";
        if (publishedAt == null) publishedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getApproverRoles() { return approverRoles; }
    public void setApproverRoles(String approverRoles) { this.approverRoles = approverRoles; }
    public String getFlowDefinition() { return flowDefinition; }
    public void setFlowDefinition(String flowDefinition) { this.flowDefinition = flowDefinition; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPublishedBy() { return publishedBy; }
    public void setPublishedBy(String publishedBy) { this.publishedBy = publishedBy; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
}
