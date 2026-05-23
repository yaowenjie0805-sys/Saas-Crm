package com.yao.crm.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlan {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private Long priceCentsMonthly;

    @Column(nullable = false, length = 16)
    private String currency;

    @Column(nullable = false)
    private Integer maxUsers;

    @Column(nullable = false)
    private Integer maxCustomers;

    @Column(nullable = false)
    private Integer maxStorageMb;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String featuresJson;

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
        if (updatedAt == null) updatedAt = now;
        if (priceCentsMonthly == null) priceCentsMonthly = 0L;
        if (currency == null || currency.trim().isEmpty()) currency = "CNY";
        if (maxUsers == null || maxUsers < 1) maxUsers = 1;
        if (maxCustomers == null || maxCustomers < 1) maxCustomers = 100;
        if (maxStorageMb == null || maxStorageMb < 1) maxStorageMb = 512;
        if (featuresJson == null || featuresJson.trim().isEmpty()) featuresJson = "[]";
        if (enabled == null) enabled = true;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getPriceCentsMonthly() { return priceCentsMonthly; }
    public void setPriceCentsMonthly(Long priceCentsMonthly) { this.priceCentsMonthly = priceCentsMonthly; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Integer getMaxUsers() { return maxUsers; }
    public void setMaxUsers(Integer maxUsers) { this.maxUsers = maxUsers; }
    public Integer getMaxCustomers() { return maxCustomers; }
    public void setMaxCustomers(Integer maxCustomers) { this.maxCustomers = maxCustomers; }
    public Integer getMaxStorageMb() { return maxStorageMb; }
    public void setMaxStorageMb(Integer maxStorageMb) { this.maxStorageMb = maxStorageMb; }
    public String getFeaturesJson() { return featuresJson; }
    public void setFeaturesJson(String featuresJson) { this.featuresJson = featuresJson; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
