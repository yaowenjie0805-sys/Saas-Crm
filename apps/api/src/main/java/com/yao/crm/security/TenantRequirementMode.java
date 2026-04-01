package com.yao.crm.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TenantRequirementMode {

    private static final Logger log = LoggerFactory.getLogger(TenantRequirementMode.class);
    private static boolean rejectMissingTenant = false;

    public TenantRequirementMode(@Value("${tenant.reject-missing:false}") boolean rejectMissingTenant) {
        TenantRequirementMode.rejectMissingTenant = rejectMissingTenant;
        log.debug("tenant.reject-missing flag initialized to {}", rejectMissingTenant);
    }

    public static boolean isRejectMissingTenant() {
        return rejectMissingTenant;
    }

    static void setRejectMissingTenantForTesting(boolean rejectMissingTenant) {
        TenantRequirementMode.rejectMissingTenant = rejectMissingTenant;
    }
}
