package com.yao.crm.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "opportunities")
public class Opportunity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 120)
    private String stage;

    @Column(nullable = false)
    private Integer count;

    @Column(nullable = false)
    private Long amount;

    @Column(length = 120)
    private String owner;

    @Column(nullable = false)
    private Integer progress;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
      LocalDateTime now = LocalDateTime.now();
      if (createdAt == null) createdAt = now;
      if (tenantId == null || tenantId.trim().isEmpty()) tenantId = "tenant_default";
      updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
      updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
