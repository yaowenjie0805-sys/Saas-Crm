package com.yao.crm.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
public class TaskItem {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 120)
    private String time;

    @Column(nullable = false, length = 60)
    private String level;

    @Column(nullable = false)
    private Boolean done;

    @Column(length = 80)
    private String owner;

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
      if (tenantId == null || tenantId.trim().isEmpty()) throw new IllegalStateException("tenant_id_required");
      updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
      updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public Boolean getDone() { return done; }
    public void setDone(Boolean done) { this.done = done; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
