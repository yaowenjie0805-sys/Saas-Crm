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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 缂撳瓨鏈嶅姟
 * 鎻愪緵澶氱骇缂撳瓨鏀寔锛歊edis -> 鏈湴缂撳瓨 -> 绌?
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // 鏈湴缂撳瓨锛堜綔涓篟edis鐨勫浠芥垨鏇夸唬锛屼娇鐢?Caffeine 鎻愪緵鑷姩娣樻卑鍜岀粺璁★級
    private final Cache<String, CacheEntry> localCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .recordStats()
        .build();

    // 缂撳瓨閰嶇疆
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);
    private static final Duration SHORT_TTL = Duration.ofMinutes(1);
    private static final Duration LONG_TTL = Duration.ofHours(1);

    // 缂撳瓨Key鍓嶇紑
    private static final String PREFIX = "crm:cache:";
    private static final String USER_PREFIX = PREFIX + "user:";
    private static final String DASHBOARD_PREFIX = PREFIX + "dashboard:";
    private static final String WORKFLOW_PREFIX = PREFIX + "workflow:";
    private static final String REPORT_PREFIX = PREFIX + "report:";
    private static final String SEARCH_PREFIX = PREFIX + "search:";
    private static final String IMPORT_JOB_PREFIX = PREFIX + "job:import:";
    private static final String EXPORT_JOB_PREFIX = PREFIX + "job:export:";

    // 浠诲姟缂撳瓨TTL
    private static final Duration JOB_TTL = Duration.ofHours(24);
    private static final Duration REDIS_UNAVAILABLE_COOLDOWN = Duration.ofSeconds(30);
    private final AtomicLong redisUnavailableUntil = new AtomicLong(0L);

    public CacheService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 鑾峰彇缂撳瓨鍊硷紙澶氱骇缂撳瓨锛氭湰鍦扮紦瀛?-> Redis锛?     */
    public <T> Optional<T> get(String key, Class<T> type) {
        String normalizedKey = normalizeKey(key);
        Optional<T> localValue = getLocalByNormalizedKey(normalizedKey, type);
        if (localValue.isPresent()) {
            log.debug("Local cache hit: {}", key);
            return localValue;
        }

        if (isRedisTemporarilyUnavailable()) {
            log.debug("Skip Redis get due cooldown: {}", key);
            return Optional.empty();
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
            markRedisTemporarilyUnavailable("get", key, e);
        }
        return Optional.empty();
    }

    /**
     * 鑾峰彇缂撳瓨鍊硷紙鏀寔娉涘瀷锛屽绾х紦瀛橈細鏈湴缂撳瓨 -> Redis锛?     */
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

        if (isRedisTemporarilyUnavailable()) {
            log.debug("Skip Redis get(Type) due cooldown: {}", key);
            return Optional.empty();
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
            markRedisTemporarilyUnavailable("getType", key, e);
        }
        return Optional.empty();
    }

    /**
     * 璁剧疆缂撳瓨鍊?
     */
    public void set(String key, Object value) {
        set(key, value, DEFAULT_TTL);
    }

    /**
     * 璁剧疆缂撳瓨鍊硷紙鎸囧畾TTL锛?
     */
    public void set(String key, Object value, Duration ttl) {
        String normalizedKey = normalizeKey(key);
        if (isRedisTemporarilyUnavailable()) {
            setLocalByNormalizedKey(normalizedKey, value, ttl);
            log.debug("Skip Redis set due cooldown, local cache only: {}", key);
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(normalizedKey, json, ttl);
            setLocalByNormalizedKey(normalizedKey, value, ttl);
            log.debug("Cache set: {} (TTL: {})", key, ttl);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize cache value: {}", key, e);
        } catch (Exception e) {
            markRedisTemporarilyUnavailable("set", key, e);
            setLocalByNormalizedKey(normalizedKey, value, ttl);
        }
    }

    /**
     * 璁剧疆鏈湴缂撳瓨
     */
    public void setLocal(String key, Object value, Duration ttl) {
        setLocalByNormalizedKey(normalizeKey(key), value, ttl);
    }

    /**
     * 鑾峰彇鏈湴缂撳瓨
     */
    public <T> Optional<T> getLocal(String key, Class<T> type) {
        return getLocalByNormalizedKey(normalizeKey(key), type);
    }

    /**
     * 鍒犻櫎缂撳瓨
     */
    public void delete(String key) {
        String normalizedKey = normalizeKey(key);
        if (!isRedisTemporarilyUnavailable()) {
            try {
                redisTemplate.delete(normalizedKey);
            } catch (Exception e) {
                markRedisTemporarilyUnavailable("delete", key, e);
            }
        } else {
            log.debug("Skip Redis delete due cooldown: {}", key);
        }
        localCache.invalidate(normalizedKey);
        log.debug("Cache deleted: {}", key);
    }

    /**
     * 鍒犻櫎鍖归厤鍓嶇紑鐨勬墍鏈夌紦瀛?
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
     * 妫€鏌ョ紦瀛樻槸鍚﹀瓨鍦?
     */
    public boolean exists(String key) {
        String normalizedKey = normalizeKey(key);
        if (isRedisTemporarilyUnavailable()) {
            return getLocalByNormalizedKey(normalizedKey, Object.class).isPresent();
        }
        try {
            Boolean exists = redisTemplate.hasKey(normalizedKey);
            return exists != null && exists;
        } catch (Exception e) {
            markRedisTemporarilyUnavailable("exists", key, e);
            return getLocalByNormalizedKey(normalizedKey, Object.class).isPresent();
        }
    }

    /**
     * 鑾峰彇鎴栬缃紦瀛橈紙缂撳瓨绌块€忎繚鎶わ級
     */
    public <T> T getOrSet(String key, Supplier<T> supplier, Duration ttl) {
        // Check cache first, then fallback to supplier
        Optional<Object> cached = get(key, Object.class);
        if (cached.isPresent()) {
            @SuppressWarnings("unchecked")
            T typed = (T) cached.get();
            return typed;
        }
        T value = supplier.get();
        if (value != null) {
            set(key, value, ttl);
        }
        return value;
    }

    /**
     * 鑾峰彇鎴栬缃紦瀛橈紙绠€鍖栫増锛?
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
     * 鑾峰彇鎴栬缃紦瀛橈紙浣跨敤榛樿TTL锛?
     */
    public <T> T getOrSet(String key, Supplier<T> supplier) {
        return getOrSet(key, supplier, DEFAULT_TTL);
    }

    // ========== 涓撶敤缂撳瓨鏂规硶 ==========

    /**
     * 鐢ㄦ埛缂撳瓨
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
     * 浠〃鐩樼紦瀛?
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
     * 宸ヤ綔娴佺紦瀛?
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
     * 鎶ヨ〃缂撳瓨
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
     * 鎼滅储缂撳瓨
     */
    public void cacheSearch(String tenantId, String query, Object data) {
        String key = SEARCH_PREFIX + tenantId + ":" + query.hashCode();
        set(key, data, Duration.ofSeconds(30)); // 鎼滅储缂撳瓨鍙繚鐣?0绉?
    }

    public <T> Optional<T> getCachedSearch(String tenantId, String query, Class<T> type) {
        String key = SEARCH_PREFIX + tenantId + ":" + query.hashCode();
        return get(key, type);
    }

    /**
     * 娓呴櫎绉熸埛鎵€鏈夌紦瀛?
     */
    public void invalidateTenant(String tenantId) {
        deleteByPrefix("user:" + tenantId);
        deleteByPrefix("dashboard:" + tenantId);
        deleteByPrefix("report:" + tenantId);
        deleteByPrefix("search:" + tenantId);
        log.info("Invalidated all caches for tenant: {}", tenantId);
    }

    // ========== 浠诲姟缂撳瓨鏂规硶 ==========

    /**
     * 淇濆瓨瀵煎叆浠诲姟涓婁笅鏂囧埌Redis
     */
    public void setImportJobContext(String jobId, Object context) {
        set(IMPORT_JOB_PREFIX + jobId, context, JOB_TTL);
    }

    /**
     * 鑾峰彇瀵煎叆浠诲姟涓婁笅鏂?
     */
    public <T> Optional<T> getImportJobContext(String jobId, Class<T> type) {
        return get(IMPORT_JOB_PREFIX + jobId, type);
    }

    /**
     * 鍒犻櫎瀵煎叆浠诲姟涓婁笅鏂?
     */
    public void deleteImportJobContext(String jobId) {
        delete(IMPORT_JOB_PREFIX + jobId);
    }

    /**
     * 淇濆瓨瀵煎嚭浠诲姟涓婁笅鏂囧埌Redis
     */
    public void setExportJobContext(String jobId, Object context) {
        set(EXPORT_JOB_PREFIX + jobId, context, JOB_TTL);
    }

    /**
     * 鑾峰彇瀵煎嚭浠诲姟涓婁笅鏂?
     */
    public <T> Optional<T> getExportJobContext(String jobId, Class<T> type) {
        return get(EXPORT_JOB_PREFIX + jobId, type);
    }

    /**
     * 鍒犻櫎瀵煎嚭浠诲姟涓婁笅鏂?
     */
    public void deleteExportJobContext(String jobId) {
        delete(EXPORT_JOB_PREFIX + jobId);
    }

    /**
     * 娓呴櫎鎵€鏈夌紦瀛?
     */
    public void invalidateAll() {
        deleteKeysByPattern(PREFIX + "*");
        localCache.invalidateAll();
        log.info("Invalidated all caches");
    }

    /**
     * 鑾峰彇缂撳瓨缁熻淇℃伅
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            stats.put("redisKeyCount", countKeysByPattern(PREFIX + "*"));
            stats.put("localCacheSize", localCache.estimatedSize());
            stats.put("localCacheKeys", new ArrayList<>(localCache.asMap().keySet()));
            
            // Caffeine 缁熻
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
            localCache.put(normalizedKey, new CacheEntry(json));
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
                        connection.unlink(batch.toArray(new byte[0][]));
                        batch.clear();
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            if (!batch.isEmpty()) {
                connection.unlink(batch.toArray(new byte[0][]));
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

    private boolean isRedisTemporarilyUnavailable() {
        return System.currentTimeMillis() < redisUnavailableUntil.get();
    }

    private void markRedisTemporarilyUnavailable(String operation, String key, Exception e) {
        long now = System.currentTimeMillis();
        long target = now + REDIS_UNAVAILABLE_COOLDOWN.toMillis();
        while (true) {
            long current = redisUnavailableUntil.get();
            long next = Math.max(current, target);
            if (redisUnavailableUntil.compareAndSet(current, next)) {
                boolean enteringCooldown = current <= now;
                if (enteringCooldown) {
                    log.warn("Redis unavailable, entering {}s cooldown; op={}, key={}, reason={}",
                        REDIS_UNAVAILABLE_COOLDOWN.getSeconds(), operation, key, e.getMessage());
                } else {
                    log.debug("Redis still in cooldown; op={}, key={}, reason={}", operation, key, e.getMessage());
                }
                break;
            }
        }
    }

    /**
     * 缂撳瓨鏉＄洰鍐呴儴绫伙紙鐢ㄤ簬鏈湴缂撳瓨锛?     */
    private static class CacheEntry {
        private final String value;

        public CacheEntry(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}

