package com.yao.crm.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class DashboardMetricsCacheService {

    public static final class CachedValue<T> {
        private final T value;
        private final boolean hit;
        private final String tier;
        private final boolean fallback;

        public CachedValue(T value, boolean hit, String tier, boolean fallback) {
            this.value = value;
            this.hit = hit;
            this.tier = tier == null ? "LOCAL" : tier;
            this.fallback = fallback;
        }

        public T getValue() {
            return value;
        }

        public boolean isHit() {
            return hit;
        }

        public String getTier() {
            return tier;
        }

        public boolean isFallback() {
            return fallback;
        }
    }

    private static final class CacheEntry {
        private final Object value;
        private final long expireAt;

        private CacheEntry(Object value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }

    private final Map<String, CacheEntry> localCache = new ConcurrentHashMap<String, CacheEntry>();
    private final Map<String, Long> localTenantVersions = new ConcurrentHashMap<String, Long>();
    private final Map<String, Long> localDomainVersions = new ConcurrentHashMap<String, Long>();
    private final RedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry meterRegistry;
    private final boolean redisEnabled;
    private final boolean domainInvalidationEnabled;
    private final long defaultTtlMs;
    private final long dashboardTtlMs;
    private final long reportsTtlMs;
    private final long workbenchTtlMs;
    private final long timelineTtlMs;
    private final long commerceTtlMs;
    private final long listTtlMs;
    private final long redisRetryBackoffMs;
    private final long localCleanupIntervalMs;
    private volatile long redisRetryAfterMs = 0L;

    public DashboardMetricsCacheService(
            ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider,
            ObjectProvider<MeterRegistry> meterRegistryProvider,
            @Value("${crm.cache.redis.enabled:true}") boolean redisEnabled,
            @Value("${crm.cache.invalidation.mode:tenant}") String invalidationMode,
            @Value("${crm.cache.default.ttl-ms:60000}") long defaultTtlMs,
            @Value("${crm.cache.dashboard.ttl-ms:90000}") long dashboardTtlMs,
            @Value("${crm.cache.reports.ttl-ms:120000}") long reportsTtlMs,
            @Value("${crm.cache.workbench.ttl-ms:90000}") long workbenchTtlMs,
            @Value("${crm.cache.timeline.ttl-ms:180000}") long timelineTtlMs,
            @Value("${crm.cache.commerce.ttl-ms:90000}") long commerceTtlMs,
            @Value("${crm.cache.list.ttl-ms:45000}") long listTtlMs,
            @Value("${crm.cache.redis.retry-backoff-ms:30000}") long redisRetryBackoffMs,
            @Value("${crm.cache.local.cleanup-interval-ms:60000}") long localCleanupIntervalMs) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
        this.redisEnabled = redisEnabled;
        this.domainInvalidationEnabled = "domain".equalsIgnoreCase(String.valueOf(invalidationMode));
        this.defaultTtlMs = Math.max(1000L, defaultTtlMs);
        this.dashboardTtlMs = Math.max(1000L, dashboardTtlMs);
        this.reportsTtlMs = Math.max(1000L, reportsTtlMs);
        this.workbenchTtlMs = Math.max(1000L, workbenchTtlMs);
        this.timelineTtlMs = Math.max(1000L, timelineTtlMs);
        this.commerceTtlMs = Math.max(1000L, commerceTtlMs);
        this.listTtlMs = Math.max(1000L, listTtlMs);
        this.redisRetryBackoffMs = Math.max(1000L, redisRetryBackoffMs);
        this.localCleanupIntervalMs = Math.max(1000L, localCleanupIntervalMs);
    }

    public <T> CachedValue<T> getOrLoad(String tenantId, String namespace, String key, Supplier<T> loader) {
        return getOrLoad(tenantId, namespace, key, 0L, loader);
    }

    public <T> CachedValue<T> getOrLoad(String tenantId, String namespace, String key, long ttlMsOverride, Supplier<T> loader) {
        String tenant = normalizeTenant(tenantId);
        String ns = normalizeNamespace(namespace);
        String body = key == null ? "" : key.trim();
        long ttlMs = ttlMsOverride > 0 ? ttlMsOverride : namespaceTtlMs(ns);
        long now = System.currentTimeMillis();
        pruneExpiredEntries(now, 64);
        boolean fallback = false;

        long version = localTenantVersions.containsKey(tenant) ? localTenantVersions.get(tenant) : 0L;
        if (canUseRedis()) {
            try {
                Object raw = redisTemplate.opsForValue().get(redisVersionKey(tenant));
                if (raw != null) {
                    version = parseLong(raw, 0L);
                }
                localTenantVersions.put(tenant, version);
            } catch (Exception ex) {
                markRedisFailure();
                fallback = true;
            }
        }
        long domainVersion = loadDomainVersion(tenant, namespaceDomain(ns));
        String finalKey = cacheDataKey(tenant, ns, version, domainVersion, body);

        CacheEntry local = localCache.get(finalKey);
        if (local != null && local.expireAt >= now) {
            @SuppressWarnings("unchecked")
            T value = (T) local.value;
            recordCacheMetric("hit", "LOCAL", fallback, ns);
            return new CachedValue<T>(value, true, "LOCAL", fallback);
        }

        if (canUseRedis() && !fallback) {
            try {
                Object cached = redisTemplate.opsForValue().get(finalKey);
                if (cached != null) {
                    @SuppressWarnings("unchecked")
                    T value = (T) cached;
                    localCache.put(finalKey, new CacheEntry(value, now + ttlMs));
                    recordCacheMetric("hit", "REDIS", false, ns);
                    return new CachedValue<T>(value, true, "REDIS", false);
                }
            } catch (Exception ex) {
                markRedisFailure();
                fallback = true;
            }
        }

        T loaded = loader.get();
        localCache.put(finalKey, new CacheEntry(loaded, now + ttlMs));
        if (canUseRedis() && !fallback) {
            try {
                redisTemplate.opsForValue().set(finalKey, loaded, ttlMs, TimeUnit.MILLISECONDS);
            } catch (Exception ex) {
                markRedisFailure();
                fallback = true;
            }
        }
        recordCacheMetric("miss", fallback ? "LOCAL" : "REDIS", fallback, ns);
        return new CachedValue<T>(loaded, false, fallback ? "LOCAL" : "REDIS", fallback);
    }

    @Scheduled(fixedDelayString = "${crm.cache.local.cleanup-interval-ms:60000}")
    public void cleanupExpiredLocalCacheEntries() {
        pruneExpiredEntries(System.currentTimeMillis(), Integer.MAX_VALUE);
    }

    public void evictTenant(String tenantId) {
        bumpTenantVersion(tenantId);
    }

    public void evictDomain(String tenantId, String domain) {
        if (!domainInvalidationEnabled) {
            bumpTenantVersion(tenantId);
            return;
        }
        bumpDomainVersion(tenantId, domain);
    }

    public void bumpTenantVersion(String tenantId) {
        String tenant = normalizeTenant(tenantId);
        long nextVersion = localTenantVersions.containsKey(tenant) ? localTenantVersions.get(tenant) + 1L : 1L;
        if (canUseRedis()) {
            try {
                Long value = redisTemplate.opsForValue().increment(redisVersionKey(tenant), 1L);
                nextVersion = value == null ? nextVersion : value.longValue();
            } catch (Exception ex) {
                markRedisFailure();
                // degrade to local-only versioning
            }
        }
        localTenantVersions.put(tenant, nextVersion);
        String prefix = cacheDataPrefix(tenant);
        Iterator<String> it = localCache.keySet().iterator();
        while (it.hasNext()) {
            String cacheKey = it.next();
            if (cacheKey.startsWith(prefix)) {
                it.remove();
            }
        }
    }

    private void bumpDomainVersion(String tenantId, String domainRaw) {
        String tenant = normalizeTenant(tenantId);
        String domain = normalizeDomain(domainRaw);
        String domainKey = localDomainVersionKey(tenant, domain);
        long nextVersion = localDomainVersions.containsKey(domainKey) ? localDomainVersions.get(domainKey) + 1L : 1L;
        if (canUseRedis()) {
            try {
                Long value = redisTemplate.opsForValue().increment(redisDomainVersionKey(tenant, domain), 1L);
                nextVersion = value == null ? nextVersion : value.longValue();
            } catch (Exception ex) {
                markRedisFailure();
                // degrade to local-only versioning
            }
        }
        localDomainVersions.put(domainKey, nextVersion);
        String prefix = cacheDataPrefix(tenant) + domain + ":";
        Iterator<String> it = localCache.keySet().iterator();
        while (it.hasNext()) {
            String cacheKey = it.next();
            if (cacheKey.startsWith(prefix)) {
                it.remove();
            }
        }
    }

    private boolean canUseRedis() {
        if (!redisEnabled || redisTemplate == null) {
            return false;
        }
        return System.currentTimeMillis() >= redisRetryAfterMs;
    }

    private long namespaceTtlMs(String namespace) {
        if ("dashboard-overview".equals(namespace)) return dashboardTtlMs;
        if ("reports-overview".equals(namespace) || namespace.startsWith("reports-")) return reportsTtlMs;
        if ("workbench-today".equals(namespace)) return workbenchTtlMs;
        if (namespace.endsWith("-timeline")) return timelineTtlMs;
        if ("commerce-overview".equals(namespace)) return commerceTtlMs;
        if (namespace.endsWith("-list")) return listTtlMs;
        return defaultTtlMs;
    }

    private String namespaceDomain(String namespace) {
        if ("dashboard-overview".equals(namespace)) return "dashboard";
        if ("reports-overview".equals(namespace) || namespace.startsWith("reports-")) return "reports";
        if ("workbench-today".equals(namespace)) return "workbench";
        if (namespace.endsWith("-timeline")) return "timeline";
        if ("commerce-overview".equals(namespace)) return "commerce";
        if (namespace.endsWith("-list")) return "list";
        return "default";
    }

    private String redisVersionKey(String tenantId) {
        return "crm:cache:version:" + tenantId;
    }

    private String redisDomainVersionKey(String tenantId, String domain) {
        return "crm:cache:version:" + tenantId + ":domain:" + domain;
    }

    private String localDomainVersionKey(String tenantId, String domain) {
        return tenantId + ":" + domain;
    }

    private String cacheDataPrefix(String tenantId) {
        return "crm:cache:data:" + tenantId + ":";
    }

    private String cacheDataKey(String tenantId, String namespace, long version, long domainVersion, String body) {
        String domain = namespaceDomain(namespace);
        return cacheDataPrefix(tenantId) + domain + ":" + namespace + ":v" + version + ":d" + domainVersion + ":" + hashKey(body);
    }

    private String hashKey(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(String.valueOf(raw).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (Exception ex) {
            return Integer.toHexString(String.valueOf(raw).hashCode());
        }
    }

    private String normalizeTenant(String tenantId) {
        return tenantId == null || tenantId.trim().isEmpty() ? "tenant_default" : tenantId.trim();
    }

    private String normalizeNamespace(String namespace) {
        if (namespace == null || namespace.trim().isEmpty()) return "default";
        return namespace.trim();
    }

    private String normalizeDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) return "default";
        return domain.trim().toLowerCase();
    }

    private long parseLong(Object value, long fallback) {
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private long loadDomainVersion(String tenantId, String domainRaw) {
        String domain = normalizeDomain(domainRaw);
        String mapKey = localDomainVersionKey(tenantId, domain);
        long version = localDomainVersions.containsKey(mapKey) ? localDomainVersions.get(mapKey) : 0L;
        if (canUseRedis()) {
            try {
                Object raw = redisTemplate.opsForValue().get(redisDomainVersionKey(tenantId, domain));
                if (raw != null) {
                    version = parseLong(raw, 0L);
                }
                localDomainVersions.put(mapKey, version);
            } catch (Exception ex) {
                markRedisFailure();
                // degrade to local
            }
        }
        return version;
    }

    private void markRedisFailure() {
        redisRetryAfterMs = System.currentTimeMillis() + redisRetryBackoffMs;
    }

    private void recordCacheMetric(String result, String tier, boolean fallback, String namespace) {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.counter(
                "crm.cache.requests",
                "result", String.valueOf(result),
                "tier", String.valueOf(tier),
                "fallback", fallback ? "1" : "0",
                "namespace", namespaceDomain(namespace)
        ).increment();
    }

    private void pruneExpiredEntries(long now, int maxRemovals) {
        if (localCache.isEmpty()) {
            return;
        }
        int removed = 0;
        Iterator<Map.Entry<String, CacheEntry>> it = localCache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, CacheEntry> entry = it.next();
            CacheEntry cacheEntry = entry.getValue();
            if (cacheEntry != null && cacheEntry.expireAt < now) {
                it.remove();
                removed++;
                if (removed >= maxRemovals) {
                    return;
                }
            }
        }
    }
}
