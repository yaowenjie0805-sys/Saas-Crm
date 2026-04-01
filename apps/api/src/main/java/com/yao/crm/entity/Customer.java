package com.yao.crm.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 120)
    private String owner;

    @Column(nullable = false, length = 120)
    private String tag;

    @Column(nullable = false)
    private Long value;

    @Column(nullable = false, length = 120)
    private String status;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(length = 120)
    private String industry;

    @Column(length = 40)
    private String scale;

    @Column(length = 40)
    private String phone;

    @Column(length = 255)
    private String website;

    @Column(length = 500)
    private String address;

    @Column(columnDefinition = "TEXT")
    private String description;

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
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public Long getValue() { return value; }
    public void setValue(Long value) { this.value = value; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
    public String getScale() { return scale; }
    public void setScale(String scale) { this.scale = scale; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
