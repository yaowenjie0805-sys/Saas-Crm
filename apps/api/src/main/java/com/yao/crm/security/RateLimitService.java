package com.yao.crm.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static final int CLEANUP_HIGH_WATER_MARK = 1024;

    private static class Counter {
        volatile long windowStart;
        volatile int count;
        volatile long blockedUntil;
        volatile long lastSeen;
    }

    private final boolean enabled;
    private final long windowMs;
    private final int maxRequests;
    private final long blockMs;
    private final long cleanupIntervalMs;
    private final long aggressiveCleanupIntervalMs;
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<String, Counter>();
    private final Object cleanupLock = new Object();
    private volatile long lastCleanupAt;

    public RateLimitService(
            @Value("${security.rate-limit.enabled:true}") boolean enabled,
            @Value("${security.rate-limit.window-ms:60000}") long windowMs,
            @Value("${security.rate-limit.max-requests:180}") int maxRequests,
            @Value("${security.rate-limit.block-ms:60000}") long blockMs
    ) {
        this.enabled = enabled;
        this.windowMs = windowMs;
        this.maxRequests = maxRequests;
        this.blockMs = blockMs;
        long cleanupBase = windowMs > 0 ? windowMs : 60000L;
        long cleanupWindow = blockMs > 0 ? Math.min(cleanupBase, blockMs) : cleanupBase;
        this.cleanupIntervalMs = Math.max(5000L, cleanupWindow);
        this.aggressiveCleanupIntervalMs = Math.max(1000L, this.cleanupIntervalMs / 4);
    }

    public boolean allow(String key) {
        return allow(key, maxRequests);
    }

    public boolean allow(String key, int requestLimit) {
        if (!enabled) {
            return true;
        }

        long now = System.currentTimeMillis();
        cleanupIfNeeded(now);
        int finalLimit = requestLimit <= 0 ? maxRequests : requestLimit;
        String counterKey = key == null ? "" : key;

        Counter result = counters.compute(counterKey, (k, existing) -> {
            Counter counter = existing;
            if (counter == null) {
                counter = new Counter();
                counter.windowStart = now;
                counter.count = 0;
                counter.blockedUntil = 0;
            }
            counter.lastSeen = now;

            if (counter.blockedUntil > now) {
                return counter;
            }

            if (now - counter.windowStart >= windowMs) {
                counter.windowStart = now;
                counter.count = 0;
                counter.blockedUntil = 0;
            }

            counter.count += 1;
            if (counter.count > finalLimit) {
                counter.blockedUntil = now + blockMs;
            }

            return counter;
        });

        return result.blockedUntil <= now;
    }

    private void cleanupIfNeeded(long now) {
        boolean standardCleanupDue = now - lastCleanupAt >= cleanupIntervalMs;
        boolean aggressiveCleanupDue = counters.size() > CLEANUP_HIGH_WATER_MARK
                && now - lastCleanupAt >= aggressiveCleanupIntervalMs;

        if (!standardCleanupDue && !aggressiveCleanupDue) {
            return;
        }
        synchronized (cleanupLock) {
            standardCleanupDue = now - lastCleanupAt >= cleanupIntervalMs;
            aggressiveCleanupDue = counters.size() > CLEANUP_HIGH_WATER_MARK
                    && now - lastCleanupAt >= aggressiveCleanupIntervalMs;
            if (!standardCleanupDue && !aggressiveCleanupDue) {
                return;
            }
            cleanupExpiredCounters(now);
            lastCleanupAt = now;
        }
    }

    private void cleanupExpiredCounters(long now) {
        long retentionMs = Math.max(1L, Math.max(windowMs, blockMs));
        for (String key : counters.keySet()) {
            Counter counter = counters.get(key);
            if (counter == null) {
                continue;
            }
            if (counter.blockedUntil > now) {
                continue;
            }
            if (now - counter.lastSeen >= retentionMs) {
                counters.remove(key, counter);
            }
        }
    }
}
