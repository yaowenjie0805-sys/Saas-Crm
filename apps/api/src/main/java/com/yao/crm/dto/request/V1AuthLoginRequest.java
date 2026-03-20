package com.yao.crm.dto.request;

import javax.validation.constraints.NotBlank;

public class V1AuthLoginRequest {
    @NotBlank(message = "username_password_required")
    private String username;
    @NotBlank(message = "username_password_required")
    private String password;
    @NotBlank(message = "tenant_header_required")
    private String tenantId;
    private String mfaCode;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getMfaCode() { return mfaCode; }
    public void setMfaCode(String mfaCode) { this.mfaCode = mfaCode; }
}
