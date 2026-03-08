package com.yao.crm.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private Integer quotaUsers;

    @Column(nullable = false, length = 40)
    private String timezone;

    @Column(nullable = false, length = 16)
    private String currency;

    @Column(nullable = false, length = 20)
    private String dateFormat;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (status == null || status.trim().isEmpty()) status = "ACTIVE";
        if (quotaUsers == null || quotaUsers < 1) quotaUsers = 50;
        if (timezone == null || timezone.trim().isEmpty()) timezone = "Asia/Shanghai";
        if (currency == null || currency.trim().isEmpty()) currency = "CNY";
        if (dateFormat == null || dateFormat.trim().isEmpty()) dateFormat = "YYYY-MM-DD";
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getQuotaUsers() { return quotaUsers; }
    public void setQuotaUsers(Integer quotaUsers) { this.quotaUsers = quotaUsers; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getDateFormat() { return dateFormat; }
    public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
