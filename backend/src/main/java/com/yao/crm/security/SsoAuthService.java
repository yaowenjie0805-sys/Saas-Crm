package com.yao.crm.security;

import com.yao.crm.dto.request.SsoLoginRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class SsoAuthService {

    private final boolean enabled;
    private final String providerName;
    private final String mode;
    private final String mockCode;
    private final String defaultRole;
    private final boolean autoProvision;
    private final OidcAuthService oidcAuthService;

    public SsoAuthService(@Value("${security.sso.enabled:false}") boolean enabled,
                          @Value("${security.sso.provider-name:Enterprise SSO}") String providerName,
                          @Value("${security.sso.mode:mock}") String mode,
                          @Value("${security.sso.mock-code:SSO-ACCESS}") String mockCode,
                          @Value("${security.sso.default-role:ANALYST}") String defaultRole,
                          @Value("${security.sso.auto-provision:true}") boolean autoProvision,
                          OidcAuthService oidcAuthService) {
        this.enabled = enabled;
        this.providerName = providerName;
        this.mode = mode == null ? "mock" : mode.trim().toLowerCase(Locale.ROOT);
        this.mockCode = mockCode;
        this.defaultRole = defaultRole == null ? "ANALYST" : defaultRole.trim().toUpperCase(Locale.ROOT);
        this.autoProvision = autoProvision;
        this.oidcAuthService = oidcAuthService;
    }

    public Map<String, Object> config() {
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("enabled", enabled);
        out.put("providerName", providerName);
        out.put("mode", mode);
        out.put("redirectUri", oidcAuthService.redirectUri());
        out.put("authorizeEndpoint", oidcAuthService.authorizeEndpoint());
        out.put("clientId", oidcAuthService.clientId());
        out.put("scope", oidcAuthService.scope());
        return out;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String providerName() {
        return providerName;
    }

    public String mode() {
        return mode;
    }

    public boolean allowAutoProvision() {
        return autoProvision;
    }

    public String defaultRole() {
        return defaultRole;
    }

    public SsoIdentity resolveIdentity(SsoLoginRequest payload) {
        if (!enabled || payload == null || payload.getCode() == null) {
            return null;
        }
        if ("oidc".equals(mode)) {
            return oidcAuthService.resolveByAuthorizationCode(payload.getCode());
        }

        if (!mockCode.equals(payload.getCode().trim())) {
            return null;
        }
        String username = payload.getUsername() == null ? "" : payload.getUsername().trim();
        if (username.isEmpty()) {
            return null;
        }
        String displayName = payload.getDisplayName() == null || payload.getDisplayName().trim().isEmpty()
                ? username
                : payload.getDisplayName().trim();
        return new SsoIdentity(username, displayName);
    }
}
