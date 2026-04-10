package com.yao.crm.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class SsoLoginRequest {

    @NotBlank(message = "sso_code_required")
    @Size(max = 2048, message = "bad_request")
    private String code;

    @Size(max = 64, message = "sso_username_required")
    private String username;

    @Size(max = 64, message = "bad_request")
    private String displayName;

    @Size(max = 64, message = "bad_request")
    private String tenantId;

    @Size(max = 512, message = "bad_request")
    private String nonce;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }
}
