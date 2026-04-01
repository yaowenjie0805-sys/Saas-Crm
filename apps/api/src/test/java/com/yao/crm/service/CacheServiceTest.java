package com.yao.crm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private RedisConnection redisConnection;

    private CacheService cacheService;
    private Cache<String, ?> localCache;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheService = new CacheService(redisTemplate, new ObjectMapper());

        Field cacheField = CacheService.class.getDeclaredField("localCache");
        cacheField.setAccessible(true);
        localCache = (Cache<String, ?>) cacheField.get(cacheService);
    }

    @Test
    void setShouldNormalizePrefix() {
        cacheService.set("crm:cache:crm:cache:user:123", "value");
        verify(valueOperations).set(eq("crm:cache:user:123"), eq("\"value\""), eq(Duration.ofMinutes(10)));
    }

    @Test
    void deleteByPrefixShouldHandleLegacyVariants() throws Exception {
        Map<String, List<byte[]>> scanResults = Map.of(
            "crm:cache:dashboard:tenant*", List.of(bytes("crm:cache:dashboard:tenant:one")),
            "crm:cache:crm:cache:dashboard:tenant*", List.of(bytes("crm:cache:crm:cache:dashboard:tenant:legacy")),
            "crm:cache:crm:cache:crm:cache:dashboard:tenant*", Collections.emptyList()
        );

        List<String> deletedKeys = new ArrayList<>();

        doAnswer(invocation -> {
            RedisCallback<?> callback = invocation.getArgument(0);
            return callback.doInRedis(redisConnection);
        }).when(redisTemplate).execute(any(RedisCallback.class));

        when(redisConnection.scan(any(ScanOptions.class))).thenAnswer(invocation -> {
            ScanOptions options = invocation.getArgument(0);
            List<byte[]> entries = scanResults.getOrDefault(options.getPattern(), Collections.emptyList());
            return new TestCursor(entries);
        });

        lenient().doAnswer(invocation -> {
            byte[][] keys = invocation.getArgument(0);
            for (byte[] key : keys) {
                deletedKeys.add(new String(key, StandardCharsets.UTF_8));
            }
            return (long) keys.length;
        }).when(redisConnection).del(any(byte[][].class));

        cacheService.setLocal("dashboard:tenant:one", "v1", Duration.ofMinutes(5));
        putLegacyLocalEntry("crm:cache:crm:cache:dashboard:tenant:legacy");

        cacheService.deleteByPrefix("dashboard:tenant");

        verify(redisTemplate, times(3)).execute(any(RedisCallback.class));
        verify(redisConnection, times(3)).scan(any(ScanOptions.class));
        verify(redisTemplate, times(0)).keys(any());

        assertFalse(deletedKeys.stream().anyMatch(k -> k.startsWith("crm:cache:dashboard:tenant:") &&
            !k.equals("crm:cache:dashboard:tenant:one")));
        assertFalse(deletedKeys.stream().anyMatch(k -> k.startsWith("crm:cache:crm:cache:dashboard:tenant:") &&
            !k.equals("crm:cache:crm:cache:dashboard:tenant:legacy")));

        assertFalse(localCache.asMap().keySet().stream().anyMatch(k -> k.startsWith("crm:cache:dashboard:tenant")));
        assertFalse(localCache.asMap().keySet().stream().anyMatch(k -> k.startsWith("crm:cache:crm:cache:dashboard:tenant")));
        assertFalse(cacheService.getLocal("dashboard:tenant:one", String.class).isPresent());
    }

    @Test
    void invalidateAllShouldUseScanInsteadOfKeys() {
        doAnswer(invocation -> {
            RedisCallback<?> callback = invocation.getArgument(0);
            return callback.doInRedis(redisConnection);
        }).when(redisTemplate).execute(any(RedisCallback.class));

        when(redisConnection.scan(any(ScanOptions.class))).thenReturn(new TestCursor(List.of(
            bytes("crm:cache:user:1"),
            bytes("crm:cache:user:2")
        )));
        lenient().doAnswer(invocation -> {
            byte[][] keys = invocation.getArgument(0);
            return (long) keys.length;
        }).when(redisConnection).del(any(byte[][].class));

        cacheService.setLocal("user:1", "v1", Duration.ofMinutes(5));

        cacheService.invalidateAll();

        verify(redisTemplate).execute(any(RedisCallback.class));
        verify(redisConnection).scan(any(ScanOptions.class));
        verify(redisConnection).del(any(byte[].class), any(byte[].class));
        verify(redisTemplate, never()).keys(any());
        assertEquals(0L, localCache.estimatedSize());
    }

    @Test
    void getCacheStatsShouldUseScanInsteadOfKeys() {
        doAnswer(invocation -> {
            RedisCallback<?> callback = invocation.getArgument(0);
            return callback.doInRedis(redisConnection);
        }).when(redisTemplate).execute(any(RedisCallback.class));

        when(redisConnection.scan(any(ScanOptions.class))).thenReturn(new TestCursor(List.of(
            bytes("crm:cache:user:1"),
            bytes("crm:cache:user:2"),
            bytes("crm:cache:user:3")
        )));

        Map<String, Object> stats = cacheService.getCacheStats();

        assertEquals(3L, ((Number) stats.get("redisKeyCount")).longValue());
        verify(redisTemplate).execute(any(RedisCallback.class));
        verify(redisConnection).scan(any(ScanOptions.class));
        verify(redisTemplate, never()).keys(any());
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static class TestCursor implements Cursor<byte[]> {
        private final Iterator<byte[]> iterator;
        private boolean closed;
        private long position;

        TestCursor(List<byte[]> entries) {
            this.iterator = entries.iterator();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public byte[] next() {
            position++;
            return iterator.next();
        }

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public long getPosition() {
            return position;
        }

        @Override
        public Cursor<byte[]> open() {
            closed = false;
            return this;
        }

        @Override
        public long getCursorId() {
            return 0;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @SuppressWarnings("unchecked")
    private void putLegacyLocalEntry(String key) throws ReflectiveOperationException {
        Class<?> entryClass = Class.forName("com.yao.crm.service.CacheService$CacheEntry");
        Constructor<?> constructor = entryClass.getDeclaredConstructor(String.class, long.class);
        constructor.setAccessible(true);
        Object entry = constructor.newInstance("\"legacy\"", System.currentTimeMillis() + 60_000);
        ((Cache<String, Object>) localCache).put(key, entry);
    }
}
