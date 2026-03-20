package com.yao.crm.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRiskService {

    private static class AttemptState {
        private int failures;
        private long firstFailureAt;
        private long lockedUntil;
    }

    private final int maxFailures;
    private final long windowMs;
    private final long lockMs;
    private final Map<String, AttemptState> states = new ConcurrentHashMap<String, AttemptState>();

    public LoginRiskService(
            @Value("${security.login-risk.max-failures:5}") int maxFailures,
            @Value("${security.login-risk.window-ms:300000}") long windowMs,
            @Value("${security.login-risk.lock-ms:600000}") long lockMs
    ) {
        this.maxFailures = Math.max(1, maxFailures);
        this.windowMs = Math.max(1000, windowMs);
        this.lockMs = Math.max(1000, lockMs);
    }

    public boolean isLocked(String username, String ip) {
        AttemptState state = states.get(key(username, ip));
        if (state == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        synchronized (state) {
            return state.lockedUntil > now;
        }
    }

    public boolean isUserLocked(String username) {
        long now = System.currentTimeMillis();
        String prefix = (username == null ? "" : username.trim().toLowerCase()) + "|";
        for (Map.Entry<String, AttemptState> entry : states.entrySet()) {
            if (!entry.getKey().startsWith(prefix)) {
                continue;
            }
            AttemptState state = entry.getValue();
            synchronized (state) {
                if (state.lockedUntil > now) {
                    return true;
                }
            }
        }
        return false;
    }

    public long remainingSeconds(String username, String ip) {
        AttemptState state = states.get(key(username, ip));
        if (state == null) {
            return 0;
        }
        long now = System.currentTimeMillis();
        synchronized (state) {
            if (state.lockedUntil <= now) {
                return 0;
            }
            return (state.lockedUntil - now + 999) / 1000;
        }
    }

    public long remainingUserSeconds(String username) {
        long now = System.currentTimeMillis();
        long max = 0;
        String prefix = (username == null ? "" : username.trim().toLowerCase()) + "|";
        for (Map.Entry<String, AttemptState> entry : states.entrySet()) {
            if (!entry.getKey().startsWith(prefix)) {
                continue;
            }
            AttemptState state = entry.getValue();
            synchronized (state) {
                if (state.lockedUntil > now) {
                    long v = (state.lockedUntil - now + 999) / 1000;
                    if (v > max) {
                        max = v;
                    }
                }
            }
        }
        return max;
    }

    public void recordFailure(String username, String ip) {
        long now = System.currentTimeMillis();
        AttemptState state = states.computeIfAbsent(key(username, ip), k -> new AttemptState());
        synchronized (state) {
            if (state.firstFailureAt == 0 || now - state.firstFailureAt > windowMs) {
                state.failures = 0;
                state.firstFailureAt = now;
            }
            state.failures += 1;
            if (state.failures >= maxFailures) {
                state.lockedUntil = now + lockMs;
                state.failures = 0;
                state.firstFailureAt = 0;
            }
        }
    }

    public void clear(String username, String ip) {
        states.remove(key(username, ip));
    }

    public void clearUser(String username) {
        String prefix = (username == null ? "" : username.trim().toLowerCase()) + "|";
        for (String k : states.keySet()) {
            if (k.startsWith(prefix)) {
                states.remove(k);
            }
        }
    }

    private String key(String username, String ip) {
        return (username == null ? "" : username.trim().toLowerCase()) + "|" + (ip == null ? "unknown" : ip);
    }
}
