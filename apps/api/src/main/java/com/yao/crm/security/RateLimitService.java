package com.yao.crm.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static class Counter {
        long windowStart;
        int count;
        long blockedUntil;
    }

    private final boolean enabled;
    private final long windowMs;
    private final int maxRequests;
    private final long blockMs;
    private final Map<String, Counter> counters = new ConcurrentHashMap<String, Counter>();

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
    }

    public boolean allow(String key) {
        return allow(key, maxRequests);
    }

    public boolean allow(String key, int requestLimit) {
        if (!enabled) {
            return true;
        }

        long now = System.currentTimeMillis();
        Counter counter = counters.computeIfAbsent(key, k -> {
            Counter c = new Counter();
            c.windowStart = now;
            c.count = 0;
            c.blockedUntil = 0;
            return c;
        });

        synchronized (counter) {
            if (counter.blockedUntil > now) {
                return false;
            }

            if (now - counter.windowStart >= windowMs) {
                counter.windowStart = now;
                counter.count = 0;
            }

            counter.count += 1;
            int finalLimit = requestLimit <= 0 ? maxRequests : requestLimit;
            if (counter.count > finalLimit) {
                counter.blockedUntil = now + blockMs;
                return false;
            }
            return true;
        }
    }
}
