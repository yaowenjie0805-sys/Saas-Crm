package com.yao.crm.dto.request;

public class UpdateTenantSubscriptionRequest {
    private String planCode;
    private String status;
    private String expiresAt;
    private String trialEndsAt;

    public String getPlanCode() { return planCode; }
    public void setPlanCode(String planCode) { this.planCode = planCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
    public String getTrialEndsAt() { return trialEndsAt; }
    public void setTrialEndsAt(String trialEndsAt) { this.trialEndsAt = trialEndsAt; }
}
