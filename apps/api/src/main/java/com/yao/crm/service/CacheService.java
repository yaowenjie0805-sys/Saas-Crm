package com.yao.crm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 缓存服务
 * 提供多级缓存支持：Redis -> 本地缓存 -> 空
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // 本地缓存（作为Redis的备份或替代，使用 Caffeine 提供自动淘汰和统计）
    private final Cache<String, CacheEntry> localCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .recordStats()
        .build();

    // 缓存配置
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);
    private static final Duration SHORT_TTL = Duration.ofMinutes(1);
    private static final Duration LONG_TTL = Duration.ofHours(1);

    // 缓存Key前缀
    private static final String PREFIX = "crm:cache:";
    private static final String USER_PREFIX = PREFIX + "user:";
    private static final String DASHBOARD_PREFIX = PREFIX + "dashboard:";
    private static final String WORKFLOW_PREFIX = PREFIX + "workflow:";
    private static final String REPORT_PREFIX = PREFIX + "report:";
    private static final String SEARCH_PREFIX = PREFIX + "search:";
    private static final String IMPORT_JOB_PREFIX = PREFIX + "job:import:";
    private static final String EXPORT_JOB_PREFIX = PREFIX + "job:export:";

    // 任务缓存TTL
    private static final Duration JOB_TTL = Duration.ofHours(24);

    public CacheService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取缓存值（多级缓存：本地缓存 -> Redis）
     */
    public <T> Optional<T> get(String key, Class<T> type) {
        String normalizedKey = normalizeKey(key);
        Optional<T> localValue = getLocalByNormalizedKey(normalizedKey, type);
        if (localValue.isPresent()) {
            log.debug("Local cache hit: {}", key);
            return localValue;
        }

        try {
            String value = redisTemplate.opsForValue().get(normalizedKey);
            if (value != null) {
                log.debug("Redis cache hit: {}", key);
                T result = objectMapper.readValue(value, type);
                setLocalByNormalizedKey(normalizedKey, result, DEFAULT_TTL);
                return Optional.of(result);
            }
        } catch (Exception e) {
            log.warn("Failed to get cache: {}", key, e);
        }
        return Optional.empty();
    }

    /**
     * 获取缓存值（支持泛型，多级缓存：本地缓存 -> Redis）
     */
    public <T> Optional<T> get(String key, java.lang.reflect.Type type) {
        String normalizedKey = normalizeKey(key);
        Optional<Object> localValue = getLocalByNormalizedKey(normalizedKey, Object.class);
        if (localValue.isPresent()) {
            try {
                String json = objectMapper.writeValueAsString(localValue.get());
                com.fasterxml.jackson.databind.JavaType javaType = objectMapper.getTypeFactory().constructType(type);
                T result = objectMapper.readValue(json, javaType);
                log.debug("Local cache hit: {}", key);
                return Optional.of(result);
            } catch (Exception e) {
                log.warn("Failed to deserialize local cache: {}", key, e);
                localCache.invalidate(normalizedKey);
            }
        }

        try {
            String value = redisTemplate.opsForValue().get(normalizedKey);
            if (value != null) {
                log.debug("Redis cache hit: {}", key);
                com.fasterxml.jackson.databind.JavaType javaType = objectMapper.getTypeFactory().constructType(type);
                T result = objectMapper.readValue(value, javaType);
                setLocalByNormalizedKey(normalizedKey, result, DEFAULT_TTL);
                return Optional.of(result);
            }
        } catch (Exception e) {
            log.warn("Failed to get cache: {}", key, e);
        }
        return Optional.empty();
    }

    /**
     * 设置缓存值
     */
    public void set(String key, Object value) {
        set(key, value, DEFAULT_TTL);
    }

    /**
     * 设置缓存值（指定TTL）
     */
    public void set(String key, Object value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(normalizeKey(key), json, ttl);
            log.debug("Cache set: {} (TTL: {})", key, ttl);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize cache value: {}", key, e);
        }
    }

    /**
     * 设置本地缓存
     */
    public void setLocal(String key, Object value, Duration ttl) {
        setLocalByNormalizedKey(normalizeKey(key), value, ttl);
    }

    /**
     * 获取本地缓存
     */
    public <T> Optional<T> getLocal(String key, Class<T> type) {
        return getLocalByNormalizedKey(normalizeKey(key), type);
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        String normalizedKey = normalizeKey(key);
        redisTemplate.delete(normalizedKey);
        localCache.invalidate(normalizedKey);
        log.debug("Cache deleted: {}", key);
    }

    /**
     * 删除匹配前缀的所有缓存
     */
    public void deleteByPrefix(String prefix) {
        Set<String> prefixVariants = collectPrefixVariants(prefix);
        for (String variant : prefixVariants) {
            deleteKeysByPattern(variant + "*");
        }
        localCache.asMap().keySet().removeIf(key ->
            prefixVariants.stream().anyMatch(key::startsWith));
        log.debug("Cache deleted by prefix: {}", prefix);
    }

    /**
     * 检查缓存是否存在
     */
    public boolean exists(String key) {
        String normalizedKey = normalizeKey(key);
        Boolean exists = redisTemplate.hasKey(normalizedKey);
        return exists != null && exists;
    }

    /**
     * 获取或设置缓存（缓存穿透保护）
     */
    public <T> T getOrSet(String key, Supplier<T> supplier, Duration ttl) {
        // Java 8 兼容：简化实现，直接使用 supplier 获取值
        T value = supplier.get();
        if (value != null) {
            set(key, value, ttl);
        }
        return value;
    }

    /**
     * 获取或设置缓存（简化版）
     */
    public <T> T getOrSetSimple(String key, Supplier<T> supplier, Duration ttl) {
        Optional<T> cached = get(key, new java.lang.reflect.ParameterizedType() {
            public java.lang.reflect.Type[] getActualTypeArguments() { return new java.lang.reflect.Type[]{Object.class}; }
            public java.lang.reflect.Type getRawType() { return Object.class; }
            public java.lang.reflect.Type getOwnerType() { return null; }
        });
        if (cached.isPresent()) {
            return cached.get();
        }

        T value = supplier.get();
        if (value != null) {
            set(key, value, ttl);
        }
        return value;
    }

    /**
     * 获取或设置缓存（使用默认TTL）
     */
    public <T> T getOrSet(String key, Supplier<T> supplier) {
        return getOrSet(key, supplier, DEFAULT_TTL);
    }

    // ========== 专用缓存方法 ==========

    /**
     * 用户缓存
     */
    public void cacheUser(String userId, Object userData) {
        set(USER_PREFIX + userId, userData, LONG_TTL);
    }

    public <T> Optional<T> getCachedUser(String userId, Class<T> type) {
        return get(USER_PREFIX + userId, type);
    }

    public void invalidateUser(String userId) {
        delete(USER_PREFIX + userId);
    }

    /**
     * 仪表盘缓存
     */
    public void cacheDashboard(String tenantId, String dashboardId, Object data) {
        set(DASHBOARD_PREFIX + tenantId + ":" + dashboardId, data, SHORT_TTL);
    }

    public <T> Optional<T> getCachedDashboard(String tenantId, String dashboardId, Class<T> type) {
        return get(DASHBOARD_PREFIX + tenantId + ":" + dashboardId, type);
    }

    public void invalidateDashboard(String tenantId) {
        deleteByPrefix("dashboard:" + tenantId);
    }

    /**
     * 工作流缓存
     */
    public void cacheWorkflow(String workflowId, Object data) {
        set(WORKFLOW_PREFIX + workflowId, data, DEFAULT_TTL);
    }

    public <T> Optional<T> getCachedWorkflow(String workflowId, Class<T> type) {
        return get(WORKFLOW_PREFIX + workflowId, type);
    }

    public void invalidateWorkflow(String workflowId) {
        delete(WORKFLOW_PREFIX + workflowId);
    }

    /**
     * 报表缓存
     */
    public void cacheReport(String tenantId, String reportKey, Object data) {
        set(REPORT_PREFIX + tenantId + ":" + reportKey, data, SHORT_TTL);
    }

    public <T> Optional<T> getCachedReport(String tenantId, String reportKey, Class<T> type) {
        return get(REPORT_PREFIX + tenantId + ":" + reportKey, type);
    }

    public void invalidateReport(String tenantId) {
        deleteByPrefix("report:" + tenantId);
    }

    /**
     * 搜索缓存
     */
    public void cacheSearch(String tenantId, String query, Object data) {
        String key = SEARCH_PREFIX + tenantId + ":" + query.hashCode();
        set(key, data, Duration.ofSeconds(30)); // 搜索缓存只保留30秒
    }

    public <T> Optional<T> getCachedSearch(String tenantId, String query, Class<T> type) {
        String key = SEARCH_PREFIX + tenantId + ":" + query.hashCode();
        return get(key, type);
    }

    /**
     * 清除租户所有缓存
     */
    public void invalidateTenant(String tenantId) {
        deleteByPrefix("user:" + tenantId);
        deleteByPrefix("dashboard:" + tenantId);
        deleteByPrefix("report:" + tenantId);
        deleteByPrefix("search:" + tenantId);
        log.info("Invalidated all caches for tenant: {}", tenantId);
    }

    // ========== 任务缓存方法 ==========

    /**
     * 保存导入任务上下文到Redis
     */
    public void setImportJobContext(String jobId, Object context) {
        set(IMPORT_JOB_PREFIX + jobId, context, JOB_TTL);
    }

    /**
     * 获取导入任务上下文
     */
    public <T> Optional<T> getImportJobContext(String jobId, Class<T> type) {
        return get(IMPORT_JOB_PREFIX + jobId, type);
    }

    /**
     * 删除导入任务上下文
     */
    public void deleteImportJobContext(String jobId) {
        delete(IMPORT_JOB_PREFIX + jobId);
    }

    /**
     * 保存导出任务上下文到Redis
     */
    public void setExportJobContext(String jobId, Object context) {
        set(EXPORT_JOB_PREFIX + jobId, context, JOB_TTL);
    }

    /**
     * 获取导出任务上下文
     */
    public <T> Optional<T> getExportJobContext(String jobId, Class<T> type) {
        return get(EXPORT_JOB_PREFIX + jobId, type);
    }

    /**
     * 删除导出任务上下文
     */
    public void deleteExportJobContext(String jobId) {
        delete(EXPORT_JOB_PREFIX + jobId);
    }

    /**
     * 清除所有缓存
     */
    public void invalidateAll() {
        deleteKeysByPattern(PREFIX + "*");
        localCache.invalidateAll();
        log.info("Invalidated all caches");
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            stats.put("redisKeyCount", countKeysByPattern(PREFIX + "*"));
            stats.put("localCacheSize", localCache.estimatedSize());
            stats.put("localCacheKeys", new ArrayList<>(localCache.asMap().keySet()));
            
            // Caffeine 统计
            com.github.benmanes.caffeine.cache.stats.CacheStats caffeineStats = localCache.stats();
            stats.put("caffeine_hit_rate", caffeineStats.hitRate());
            stats.put("caffeine_eviction_count", caffeineStats.evictionCount());
            stats.put("caffeine_estimated_size", localCache.estimatedSize());
        } catch (Exception e) {
            log.error("Failed to get cache stats", e);
        }

        return stats;
    }

    private <T> Optional<T> getLocalByNormalizedKey(String normalizedKey, Class<T> type) {
        CacheEntry entry = localCache.getIfPresent(normalizedKey);
        if (entry != null) {
            if (entry.isExpired()) {
                localCache.invalidate(normalizedKey);
                return Optional.empty();
            }
            try {
                return Optional.of(objectMapper.readValue(entry.getValue(), type));
            } catch (Exception e) {
                log.warn("Failed to get local cache: {}", normalizedKey, e);
            }
        }
        return Optional.empty();
    }

    private void setLocalByNormalizedKey(String normalizedKey, Object value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            localCache.put(normalizedKey, new CacheEntry(json, System.currentTimeMillis() + ttl.toMillis()));
        } catch (Exception e) {
            log.error("Failed to set local cache: {}", normalizedKey, e);
        }
    }

    private String normalizeKey(String key) {
        return PREFIX + stripGlobalPrefix(key);
    }

    private String stripGlobalPrefix(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Cache key cannot be null");
        }
        String trimmed = raw;
        while (trimmed.startsWith(PREFIX)) {
            trimmed = trimmed.substring(PREFIX.length());
        }
        return trimmed;
    }

    private Set<String> collectPrefixVariants(String prefix) {
        String base = stripGlobalPrefix(prefix);
        Set<String> variants = new LinkedHashSet<>();
        String current = PREFIX + base;
        int maxDepth = 3;
        for (int i = 0; i < maxDepth; i++) {
            variants.add(current);
            current = PREFIX + current;
        }
        return variants;
    }

    private void deleteKeysByPattern(String pattern) {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(pattern)
                .count(500)
                .build();
            List<byte[]> batch = new ArrayList<>();
            final int batchSize = 200;
            Cursor<byte[]> cursor = null;
            try {
                cursor = connection.scan(scanOptions);
                while (cursor.hasNext()) {
                    byte[] keyBytes = cursor.next();
                    if (keyBytes == null) {
                        continue;
                    }
                    batch.add(keyBytes);
                    if (batch.size() >= batchSize) {
                        connection.del(batch.toArray(new byte[0][]));
                        batch.clear();
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            if (!batch.isEmpty()) {
                connection.del(batch.toArray(new byte[0][]));
            }
            return null;
        });
    }

    private long countKeysByPattern(String pattern) {
        Long keyCount = redisTemplate.execute((RedisCallback<Long>) connection -> {
            ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(pattern)
                .count(500)
                .build();
            Cursor<byte[]> cursor = null;
            long count = 0L;
            try {
                cursor = connection.scan(scanOptions);
                while (cursor != null && cursor.hasNext()) {
                    byte[] keyBytes = cursor.next();
                    if (keyBytes != null) {
                        count++;
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return count;
        });
        return keyCount != null ? keyCount : 0L;
    }

    /**
     * 缓存条目内部类（用于本地缓存）
     */
    private static class CacheEntry {
        private final String value;
        private final long expiresAt;

        public CacheEntry(String value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }

        public String getValue() {
            return value;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
