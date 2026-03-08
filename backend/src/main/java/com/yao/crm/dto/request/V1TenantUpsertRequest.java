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
}
