package com.yao.crm.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class MfaService {

    private final boolean enabled;
    private final Set<String> protectedRoles;
    private final String staticCode;

    public MfaService(
            @Value("${security.mfa.enabled:false}") boolean enabled,
            @Value("${security.mfa.protected-roles:ADMIN,MANAGER}") String protectedRoles,
            @Value("${security.mfa.static-code:000000}") String staticCode
    ) {
        this.enabled = enabled;
        this.protectedRoles = new HashSet<String>();
        for (String role : Arrays.asList(protectedRoles.split(","))) {
            if (role != null && !role.trim().isEmpty()) {
                this.protectedRoles.add(role.trim().toUpperCase(Locale.ROOT));
            }
        }
        this.staticCode = staticCode == null ? "" : staticCode.trim();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean requiresMfa(String role) {
        if (!enabled || role == null) {
            return false;
        }
        return protectedRoles.contains(role.toUpperCase(Locale.ROOT));
    }

    public boolean verify(String code) {
        if (!enabled) {
            return true;
        }
        String value = code == null ? "" : code.trim();
        return !value.isEmpty() && value.equals(staticCode);
    }
}
