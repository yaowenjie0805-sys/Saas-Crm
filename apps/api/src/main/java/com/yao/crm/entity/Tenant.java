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

    @Column(nullable = false, length = 16)
    private String marketProfile;

    @Column(nullable = false, length = 32)
    private String taxRule;

    @Column(nullable = false, length = 32)
    private String approvalMode;

    @Column(nullable = false, length = 400)
    private String channelsJson;

    @Column(nullable = false, length = 40)
    private String dataResidency;

    @Column(nullable = false, length = 32)
    private String maskLevel;

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
        if (dateFormat == null || dateFormat.trim().isEmpty()) dateFormat = "yyyy-MM-dd";
        if (marketProfile == null || marketProfile.trim().isEmpty()) marketProfile = "CN";
        if (taxRule == null || taxRule.trim().isEmpty()) taxRule = "VAT_CN";
        if (approvalMode == null || approvalMode.trim().isEmpty()) approvalMode = "STRICT";
        if (channelsJson == null || channelsJson.trim().isEmpty()) channelsJson = "[\"WECOM\",\"DINGTALK\"]";
        if (dataResidency == null || dataResidency.trim().isEmpty()) dataResidency = "CN";
        if (maskLevel == null || maskLevel.trim().isEmpty()) maskLevel = "STANDARD";
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
    public String getMarketProfile() { return marketProfile; }
    public void setMarketProfile(String marketProfile) { this.marketProfile = marketProfile; }
    public String getTaxRule() { return taxRule; }
    public void setTaxRule(String taxRule) { this.taxRule = taxRule; }
    public String getApprovalMode() { return approvalMode; }
    public void setApprovalMode(String approvalMode) { this.approvalMode = approvalMode; }
    public String getChannelsJson() { return channelsJson; }
    public void setChannelsJson(String channelsJson) { this.channelsJson = channelsJson; }
    public String getDataResidency() { return dataResidency; }
    public void setDataResidency(String dataResidency) { this.dataResidency = dataResidency; }
    public String getMaskLevel() { return maskLevel; }
    public void setMaskLevel(String maskLevel) { this.maskLevel = maskLevel; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
