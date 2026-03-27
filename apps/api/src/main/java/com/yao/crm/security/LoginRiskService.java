package com.yao.crm.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * 登录风险控制服务 - 使用无锁算法优化并发性能
 */
@Service
public class LoginRiskService {

    private static class AttemptState {
        volatile int failures;
        volatile long firstFailureAt;
        volatile long lockedUntil;
        volatile long lastSeen;
    }

    private static final AtomicIntegerFieldUpdater<AttemptState> FAILURES_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(AttemptState.class, "failures");
    private static final AtomicLongFieldUpdater<AttemptState> FIRST_FAILURE_UPDATER =
            AtomicLongFieldUpdater.newUpdater(AttemptState.class, "firstFailureAt");
    private static final AtomicLongFieldUpdater<AttemptState> LOCKED_UNTIL_UPDATER =
            AtomicLongFieldUpdater.newUpdater(AttemptState.class, "lockedUntil");
    private static final AtomicLongFieldUpdater<AttemptState> LAST_SEEN_UPDATER =
            AtomicLongFieldUpdater.newUpdater(AttemptState.class, "lastSeen");

    private final int maxFailures;
    private final long windowMs;
    private final long lockMs;
    private final long cleanupIntervalMs;
    private final ConcurrentHashMap<String, AttemptState> states = new ConcurrentHashMap<>();
    private final Object cleanupLock = new Object();
    private volatile long lastCleanupAt;

    public LoginRiskService(
            @Value("${security.login-risk.max-failures:5}") int maxFailures,
            @Value("${security.login-risk.window-ms:300000}") long windowMs,
            @Value("${security.login-risk.lock-ms:600000}") long lockMs
    ) {
        this.maxFailures = Math.max(1, maxFailures);
        this.windowMs = Math.max(1000, windowMs);
        this.lockMs = Math.max(1000, lockMs);
        long cleanupBase = this.windowMs > 0 ? this.windowMs : 60000L;
        long cleanupWindow = this.lockMs > 0 ? Math.min(cleanupBase, this.lockMs) : cleanupBase;
        this.cleanupIntervalMs = Math.max(5000L, Math.min(cleanupWindow, 60000L));
    }

    public boolean isLocked(String username, String ip) {
        long now = System.currentTimeMillis();
        cleanupIfNeeded(now);
        AttemptState state = states.get(key(username, ip));
        if (state == null) {
            return false;
        }
        return state.lockedUntil > now;
    }

    public boolean isUserLocked(String username) {
        long now = System.currentTimeMillis();
        cleanupIfNeeded(now);
        String prefix = (username == null ? "" : username.trim().toLowerCase()) + "|";
        return states.keySet().stream().anyMatch(k -> {
            if (!k.startsWith(prefix)) return false;
            AttemptState state = states.get(k);
            return state != null && state.lockedUntil > now;
        });
    }

    public long remainingSeconds(String username, String ip) {
        long now = System.currentTimeMillis();
        cleanupIfNeeded(now);
        AttemptState state = states.get(key(username, ip));
        if (state == null) {
            return 0;
        }
        long lockedUntil = state.lockedUntil;
        if (lockedUntil <= now) {
            return 0;
        }
        return (lockedUntil - now + 999) / 1000;
    }

    public long remainingUserSeconds(String username) {
        long now = System.currentTimeMillis();
        cleanupIfNeeded(now);
        String prefix = (username == null ? "" : username.trim().toLowerCase()) + "|";
        return states.keySet().stream()
                .filter(k -> k.startsWith(prefix))
                .map(k -> states.get(k))
                .filter(state -> state != null && state.lockedUntil > now)
                .mapToLong(state -> (state.lockedUntil - now + 999) / 1000)
                .max()
                .orElse(0);
    }

    public void recordFailure(String username, String ip) {
        long now = System.currentTimeMillis();
        cleanupIfNeeded(now);
        String k = key(username, ip);
        
        states.compute(k, (key, existing) -> {
            AttemptState state = existing;
            if (state == null) {
                state = new AttemptState();
                state.failures = 0;
                state.firstFailureAt = 0;
                state.lockedUntil = 0;
                state.lastSeen = 0;
            }
            
            if (state.firstFailureAt == 0 || now - state.firstFailureAt > windowMs) {
                FAILURES_UPDATER.set(state, 0);
                FIRST_FAILURE_UPDATER.set(state, now);
            }
            LAST_SEEN_UPDATER.set(state, now);
            int failures = FAILURES_UPDATER.incrementAndGet(state);
            
            if (failures >= maxFailures) {
                LOCKED_UNTIL_UPDATER.set(state, now + lockMs);
                FAILURES_UPDATER.set(state, 0);
                FIRST_FAILURE_UPDATER.set(state, 0);
            }
            
            return state;
        });
    }

    public void clear(String username, String ip) {
        states.remove(key(username, ip));
    }

    public void clearUser(String username) {
        String prefix = (username == null ? "" : username.trim().toLowerCase()) + "|";
        states.keySet().removeIf(k -> k.startsWith(prefix));
    }

    private void cleanupIfNeeded(long now) {
        if (now - lastCleanupAt < cleanupIntervalMs) {
            return;
        }
        synchronized (cleanupLock) {
            if (now - lastCleanupAt < cleanupIntervalMs) {
                return;
            }
            cleanupExpiredStates(now);
            lastCleanupAt = now;
        }
    }

    private void cleanupExpiredStates(long now) {
        long retentionMs = Math.max(1L, Math.max(windowMs, lockMs));
        for (String key : states.keySet()) {
            AttemptState state = states.get(key);
            if (state == null) {
                continue;
            }
            if (state.lockedUntil > now) {
                continue;
            }
            if (now - state.lastSeen >= retentionMs) {
                states.remove(key, state);
            }
        }
    }

    private String key(String username, String ip) {
        return (username == null ? "" : username.trim().toLowerCase()) + "|" + (ip == null ? "unknown" : ip);
    }
}
