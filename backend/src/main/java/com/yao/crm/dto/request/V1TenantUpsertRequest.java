package com.yao.crm.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class V1TenantUpsertRequest {
    @NotBlank(message = "tenant_name_required")
    private String name;
    private String status;
    @NotNull(message = "tenant_quota_required")
    private Integer quotaUsers;
    private String timezone;
    private String currency;
    private String dateFormat;
    private String marketProfile;
    private String taxRule;
    private String approvalMode;
    private String channels;
    private String dataResidency;
    private String maskLevel;

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
    public String getChannels() { return channels; }
    public void setChannels(String channels) { this.channels = channels; }
    public String getDataResidency() { return dataResidency; }
    public void setDataResidency(String dataResidency) { this.dataResidency = dataResidency; }
    public String getMaskLevel() { return maskLevel; }
    public void setMaskLevel(String maskLevel) { this.maskLevel = maskLevel; }
}
