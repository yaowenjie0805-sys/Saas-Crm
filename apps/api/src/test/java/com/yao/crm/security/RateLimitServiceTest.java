package com.yao.crm.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitServiceTest {

    private RateLimitService service;

    @BeforeEach
    void setUp() {
        service = new RateLimitService(true, 60000L, 5, 60000L);
    }

    @Test
    @DisplayName("shouldCleanupExpiredCountersWhenAgedOut")
    void shouldCleanupExpiredCountersWhenAgedOut() throws Exception {
        assertTrue(service.allow("stale-key", 2));

        ConcurrentHashMap<String, Object> counters = getCounters();
        Object staleCounter = counters.get("stale-key");
        assertTrue(staleCounter != null);

        setLongField(staleCounter, "lastSeen", System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10));
        setLongField(service, "lastCleanupAt", 0L);

        assertTrue(service.allow("fresh-key", 2));

        assertFalse(counters.containsKey("stale-key"));
        assertTrue(counters.containsKey("fresh-key"));
        assertEquals(1, counters.size());
    }

    @Test
    @DisplayName("shouldReclaimStaleCountersUnderHighCardinalityWithoutBreakingLimitChecks")
    void shouldReclaimStaleCountersUnderHighCardinalityWithoutBreakingLimitChecks() throws Exception {
        String hotKey = "hot-key";
        assertTrue(service.allow(hotKey, 2));
        assertTrue(service.allow(hotKey, 2));

        long now = System.currentTimeMillis();
        for (int i = 0; i < 1050; i++) {
            assertTrue(service.allow("stale-" + i, 2));
        }

        ConcurrentHashMap<String, Object> counters = getCounters();
        for (int i = 0; i < 1050; i++) {
            Object staleCounter = counters.get("stale-" + i);
            assertTrue(staleCounter != null);
            setLongField(staleCounter, "lastSeen", now - TimeUnit.MINUTES.toMillis(10));
        }
        setLongField(service, "lastCleanupAt", now - TimeUnit.SECONDS.toMillis(20));

        assertTrue(service.allow("trigger-key", 2));

        assertTrue(counters.containsKey(hotKey));
        assertFalse(counters.containsKey("stale-0"));
        assertTrue(counters.size() <= 3);

        assertFalse(service.allow(hotKey, 2));
    }

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<String, Object> getCounters() throws Exception {
        Field field = RateLimitService.class.getDeclaredField("counters");
        field.setAccessible(true);
        return (ConcurrentHashMap<String, Object>) field.get(service);
    }

    private void setLongField(Object target, String fieldName, long value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setLong(target, value);
    }
}
