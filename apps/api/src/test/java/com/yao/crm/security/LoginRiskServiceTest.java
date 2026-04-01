package com.yao.crm.security;

import static com.yao.crm.support.TestTenant.TENANT_TEST;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginRiskServiceTest {

    private LoginRiskService service;

    @BeforeEach
    void setUp() {
        service = new LoginRiskService(3, 60000L, 120000L);
    }

    @Test
    @DisplayName("shouldCleanupExpiredUnlockedStatesWithoutRemovingLockedStates")
    void shouldCleanupExpiredUnlockedStatesWithoutRemovingLockedStates() throws Exception {
        service.recordFailure(TENANT_TEST, "stale", "10.0.0.1");
        service.recordFailure(TENANT_TEST, "active", "10.0.0.3");
        service.recordFailure(TENANT_TEST, "locked", "10.0.0.2");
        service.recordFailure(TENANT_TEST, "locked", "10.0.0.2");
        service.recordFailure(TENANT_TEST, "locked", "10.0.0.2");

        long now = System.currentTimeMillis();
        setLongField(getState(key(TENANT_TEST, "stale", "10.0.0.1")), "lastSeen", now - TimeUnit.MINUTES.toMillis(10));
        setLongField(getState(key(TENANT_TEST, "active", "10.0.0.3")), "lastSeen", now);
        setLongField(getState(key(TENANT_TEST, "locked", "10.0.0.2")), "lastSeen", now - TimeUnit.MINUTES.toMillis(10));
        setLongField(service, "lastCleanupAt", 0L);

        IllegalStateException noTenantError = assertThrows(
                IllegalStateException.class,
                () -> service.isLocked("probe", "127.0.0.1")
        );
        assertEquals("tenant_id_required", noTenantError.getMessage());
        assertFalse(service.isLocked(TENANT_TEST, "probe", "127.0.0.1"));

        assertNull(getState(key(TENANT_TEST, "stale", "10.0.0.1")));
        assertNotNull(getState(key(TENANT_TEST, "active", "10.0.0.3")));
        assertNotNull(getState(key(TENANT_TEST, "locked", "10.0.0.2")));
        assertTrue(service.isLocked(TENANT_TEST, "locked", "10.0.0.2"));
        assertEquals(2, getStates().size());
    }

    @Test
    @DisplayName("shouldFailFastWhenTenantlessOverloadIsUsed")
    void shouldFailFastWhenTenantlessOverloadIsUsed() {
        IllegalStateException recordError = assertThrows(
                IllegalStateException.class,
                () -> service.recordFailure("legacy-user", "127.0.0.1")
        );
        assertEquals("tenant_id_required", recordError.getMessage());

        IllegalStateException clearError = assertThrows(
                IllegalStateException.class,
                () -> service.clear("legacy-user", "127.0.0.1")
        );
        assertEquals("tenant_id_required", clearError.getMessage());

        IllegalStateException userLockedError = assertThrows(
                IllegalStateException.class,
                () -> service.isUserLocked("legacy-user")
        );
        assertEquals("tenant_id_required", userLockedError.getMessage());

        IllegalStateException userRemainingError = assertThrows(
                IllegalStateException.class,
                () -> service.remainingUserSeconds("legacy-user")
        );
        assertEquals("tenant_id_required", userRemainingError.getMessage());

        IllegalStateException clearUserError = assertThrows(
                IllegalStateException.class,
                () -> service.clearUser("legacy-user")
        );
        assertEquals("tenant_id_required", clearUserError.getMessage());
    }

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<String, Object> getStates() throws Exception {
        Field field = LoginRiskService.class.getDeclaredField("states");
        field.setAccessible(true);
        return (ConcurrentHashMap<String, Object>) field.get(service);
    }

    private String key(String tenantId, String username, String ip) throws Exception {
        Method method = LoginRiskService.class.getDeclaredMethod("key", String.class, String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(service, tenantId, username, ip);
    }

    private Object getState(String key) throws Exception {
        return getStates().get(key);
    }

    private void setLongField(Object target, String fieldName, long value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setLong(target, value);
    }
}
