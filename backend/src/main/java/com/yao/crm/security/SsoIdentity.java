package com.yao.crm.security;

public class SsoIdentity {
    private final String username;
    private final String displayName;

    public SsoIdentity(String username, String displayName) {
        this.username = username;
        this.displayName = displayName;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }
}
