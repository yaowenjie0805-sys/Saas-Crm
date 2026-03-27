package com.yao.crm.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        service.recordFailure("stale", "10.0.0.1");
        service.recordFailure("active", "10.0.0.3");
        service.recordFailure("locked", "10.0.0.2");
        service.recordFailure("locked", "10.0.0.2");
        service.recordFailure("locked", "10.0.0.2");

        long now = System.currentTimeMillis();
        setLongField(getState("stale|10.0.0.1"), "lastSeen", now - TimeUnit.MINUTES.toMillis(10));
        setLongField(getState("active|10.0.0.3"), "lastSeen", now);
        setLongField(getState("locked|10.0.0.2"), "lastSeen", now - TimeUnit.MINUTES.toMillis(10));
        setLongField(service, "lastCleanupAt", 0L);

        assertFalse(service.isLocked("probe", "127.0.0.1"));

        assertNull(getState("stale|10.0.0.1"));
        assertNotNull(getState("active|10.0.0.3"));
        assertNotNull(getState("locked|10.0.0.2"));
        assertTrue(service.isLocked("locked", "10.0.0.2"));
        assertEquals(2, getStates().size());
    }

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<String, Object> getStates() throws Exception {
        Field field = LoginRiskService.class.getDeclaredField("states");
        field.setAccessible(true);
        return (ConcurrentHashMap<String, Object>) field.get(service);
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
