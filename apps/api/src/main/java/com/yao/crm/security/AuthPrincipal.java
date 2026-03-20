package com.yao.crm.security;

public class AuthPrincipal {
    private final String username;
    private final String role;
    private final String ownerScope;
    private final String tenantId;
    private final boolean mfaVerified;

    public AuthPrincipal(String username, String role, String ownerScope, String tenantId, boolean mfaVerified) {
        this.username = username;
        this.role = role;
        this.ownerScope = ownerScope;
        this.tenantId = tenantId;
        this.mfaVerified = mfaVerified;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getOwnerScope() {
        return ownerScope;
    }

    public String getTenantId() {
        return tenantId;
    }

    public boolean isMfaVerified() {
        return mfaVerified;
    }
}
