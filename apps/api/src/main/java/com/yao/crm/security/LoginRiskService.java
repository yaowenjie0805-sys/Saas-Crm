package com.yao.crm.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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
    private final ConcurrentHashMap<String, Set<String>> tenantUserIndex = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> usernameIndex = new ConcurrentHashMap<>();
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
        throw new IllegalStateException("tenant_id_required");
    }

    public boolean isLocked(String tenantId, String username, String ip) {
        long now = System.currentTimeMillis();
        cleanupIfNeeded(now);
        AttemptState state = states.get(key(tenantId, username, ip));
        if (state == null) {
            return false;
        }
        return state.lockedUntil > now;
    }

    public boolean isUserLocked(String username) {
        throw new IllegalStateException("tenant_id_required");
    }

    public long remainingSeconds(String username, String ip) {
        throw new IllegalStateException("tenant_id_required");
    }

    public long remainingSeconds(String tenantId, String username, String ip) {
        long now = System.currentTimeMillis();
        cleanupIfNeeded(now);
        AttemptState state = states.get(key(tenantId, username, ip));
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
        throw new IllegalStateException("tenant_id_required");
    }

    public void recordFailure(String username, String ip) {
        throw new IllegalStateException("tenant_id_required");
    }

    public void recordFailure(String tenantId, String username, String ip) {
        long now = System.currentTimeMillis();
        cleanupIfNeeded(now);
        String k = key(tenantId, username, ip);
        
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
        indexKey(k, tenantId, username);
    }

    public void clear(String username, String ip) {
        throw new IllegalStateException("tenant_id_required");
    }

    public void clear(String tenantId, String username, String ip) {
        String fullKey = key(tenantId, username, ip);
        AttemptState removed = states.remove(fullKey);
        if (removed != null) {
            removeKeyFromIndexes(fullKey);
        }
    }

    public boolean isUserLocked(String tenantId, String username) {
        long now = System.currentTimeMillis();
        cleanupIfNeeded(now);
        Set<String> keys = tenantUserIndex.get(tenantUserPrefix(tenantId, username));
        if (keys == null || keys.isEmpty()) {
            return false;
        }
        for (String key : keys) {
            AttemptState state = states.get(key);
            if (state != null && state.lockedUntil > now) {
                return true;
            }
        }
        return false;
    }

    public long remainingUserSeconds(String tenantId, String username) {
        long now = System.currentTimeMillis();
        cleanupIfNeeded(now);
        Set<String> keys = tenantUserIndex.get(tenantUserPrefix(tenantId, username));
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        long max = 0;
        for (String key : keys) {
            AttemptState state = states.get(key);
            if (state == null || state.lockedUntil <= now) {
                continue;
            }
            long remaining = (state.lockedUntil - now + 999) / 1000;
            if (remaining > max) {
                max = remaining;
            }
        }
        return max;
    }

    public void clearUser(String username) {
        throw new IllegalStateException("tenant_id_required");
    }

    public void clearUser(String tenantId, String username) {
        String prefix = tenantUserPrefix(tenantId, username);
        Set<String> keys = tenantUserIndex.get(prefix);
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys.toArray(new String[0])) {
            AttemptState removed = states.remove(key);
            if (removed != null) {
                removeKeyFromIndexes(key);
            }
        }
    }

    private void indexKey(String key, String tenantId, String username) {
        String prefix = tenantUserPrefix(tenantId, username);
        tenantUserIndex.computeIfAbsent(prefix, p -> ConcurrentHashMap.newKeySet()).add(key);
        String normalized = normalizeUsername(username);
        usernameIndex.computeIfAbsent(normalized, p -> ConcurrentHashMap.newKeySet()).add(key);
    }

    private void removeKeyFromIndexes(String key) {
        String[] parts = key.split("\\|", 3);
        if (parts.length < 2) {
            return;
        }
        String prefix = parts[0] + "|" + parts[1] + "|";
        removeFromIndex(tenantUserIndex, prefix, key);
        removeFromIndex(usernameIndex, normalizeUsername(parts[1]), key);
    }

    private void removeFromIndex(ConcurrentHashMap<String, Set<String>> index, String mapKey, String value) {
        Set<String> keys = index.get(mapKey);
        if (keys == null) {
            return;
        }
        keys.remove(value);
        if (keys.isEmpty()) {
            index.remove(mapKey, keys);
        }
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
        Iterator<Map.Entry<String, AttemptState>> iterator = states.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, AttemptState> entry = iterator.next();
            AttemptState state = entry.getValue();
            if (state == null) {
                continue;
            }
            if (state.lockedUntil > now) {
                continue;
            }
            if (now - state.lastSeen >= retentionMs) {
                String key = entry.getKey();
                iterator.remove();
                removeKeyFromIndexes(key);
            }
        }
    }

    private String key(String tenantId, String username, String ip) {
        return normalizeTenantId(tenantId) + "|" + normalizeUsername(username) + "|" + (ip == null ? "unknown" : ip);
    }

    private String tenantUserPrefix(String tenantId, String username) {
        return normalizeTenantId(tenantId) + "|" + normalizeUsername(username) + "|";
    }

    private String normalizeTenantId(String tenantId) {
        if (tenantId == null) throw new IllegalStateException("tenant_id_required");
        String normalized = tenantId.trim().toLowerCase();
        if (normalized.isEmpty()) throw new IllegalStateException("tenant_id_required");
        return normalized;
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
