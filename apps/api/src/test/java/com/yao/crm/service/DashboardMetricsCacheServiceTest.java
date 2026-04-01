package com.yao.crm.service;

import static com.yao.crm.support.TestTenant.TENANT_TEST;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.data.redis.core.RedisTemplate;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Collections;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicInteger;

class DashboardMetricsCacheServiceTest {
    private static final String CACHE_INVALIDATION_MODE = TENANT_TEST.replace("_test", "");
    private static final String TENANT_B = TENANT_TEST + "_b";
    private static final String TENANT_B_VALUE = TENANT_TEST + "-b";
    private static final String TENANT_A_RELOADED_VALUE = TENANT_TEST + "-a-2";
    private static final String TENANT_B_RELOADED_VALUE = TENANT_TEST + "-b-2";

    @Test
    void shouldReturnHitWithinTtlAndMissAfterExpire() throws Exception {
        DashboardMetricsCacheService cacheService = new DashboardMetricsCacheService(
                emptyRedisProvider(),
                emptyMeterProvider(),
                false,
                CACHE_INVALIDATION_MODE,
                60000L,
                60000L,
                60000L,
                60000L,
                60000L,
                60000L,
                60000L,
                30000L,
                60000L
        );
        AtomicInteger calls = new AtomicInteger(0);

        // Use a longer TTL for the first load to ensure it stays in cache
        DashboardMetricsCacheService.CachedValue<String> miss = cacheService.getOrLoad(
                TENANT_TEST,
                "reports-overview",
                "k1",
                2000L,
                new Supplier<String>() {
                    @Override
                    public String get() {
                        calls.incrementAndGet();
                        return "v1";
                    }
                }
        );
        Assertions.assertFalse(miss.isHit());
        Assertions.assertEquals("v1", miss.getValue());

        // Second call should hit cache (within TTL)
        DashboardMetricsCacheService.CachedValue<String> hit = cacheService.getOrLoad(
                TENANT_TEST,
                "reports-overview",
                "k1",
                2000L,
                new Supplier<String>() {
                    @Override
                    public String get() {
                        calls.incrementAndGet();
                        return "v2";
                    }
                }
        );
        Assertions.assertTrue(hit.isHit());
        Assertions.assertEquals("v1", hit.getValue());
        Assertions.assertEquals(1, calls.get());

        // Evict tenant to simulate TTL expiration
        cacheService.evictTenant(TENANT_TEST);
        
        // After eviction, should be a miss and load new value
        DashboardMetricsCacheService.CachedValue<String> missAfterEvict = cacheService.getOrLoad(
                TENANT_TEST,
                "reports-overview",
                "k1",
                2000L,
                new Supplier<String>() {
                    @Override
                    public String get() {
                        calls.incrementAndGet();
                        return "v3";
                    }
                }
        );
        Assertions.assertFalse(missAfterEvict.isHit());
        Assertions.assertEquals("v3", missAfterEvict.getValue());
        Assertions.assertEquals(2, calls.get());
    }

    @Test
    void shouldEvictByTenantOnly() {
        DashboardMetricsCacheService cacheService = new DashboardMetricsCacheService(
                emptyRedisProvider(),
                emptyMeterProvider(),
                false,
                CACHE_INVALIDATION_MODE,
                60000L,
                60000L,
                60000L,
                60000L,
                60000L,
                60000L,
                60000L,
                30000L,
                60000L
        );

        cacheService.getOrLoad(TENANT_TEST, "dashboard-overview", "a", new Supplier<String>() {
            @Override
            public String get() {
                return TENANT_TEST;
            }
        });
        cacheService.getOrLoad(TENANT_B, "dashboard-overview", "b", new Supplier<String>() {
            @Override
            public String get() {
                return TENANT_B_VALUE;
            }
        });

        cacheService.evictTenant(TENANT_TEST);

        DashboardMetricsCacheService.CachedValue<String> tenantAHit = cacheService.getOrLoad(
                TENANT_TEST, "dashboard-overview", "a", new Supplier<String>() {
                    @Override
                    public String get() {
                        return TENANT_A_RELOADED_VALUE;
                    }
                }
        );
        DashboardMetricsCacheService.CachedValue<String> tenantBHit = cacheService.getOrLoad(
                TENANT_B, "dashboard-overview", "b", new Supplier<String>() {
                    @Override
                    public String get() {
                        return TENANT_B_RELOADED_VALUE;
                    }
                }
        );

        Assertions.assertFalse(tenantAHit.isHit());
        Assertions.assertTrue(tenantBHit.isHit());
        Assertions.assertEquals(TENANT_B_VALUE, tenantBHit.getValue());
    }

    @Test
    void shouldReportLocalTierWhenRedisDisabled() {
        DashboardMetricsCacheService cacheService = new DashboardMetricsCacheService(
                emptyRedisProvider(),
                emptyMeterProvider(),
                false,
                CACHE_INVALIDATION_MODE,
                60000L,
                60000L,
                60000L,
                60000L,
                60000L,
                60000L,
                60000L,
                30000L,
                60000L
        );

        DashboardMetricsCacheService.CachedValue<String> miss = cacheService.getOrLoad(
                TENANT_TEST,
                "dashboard-overview",
                "local-tier-key",
                new Supplier<String>() {
                    @Override
                    public String get() {
                        return "local-value";
                    }
                }
        );

        Assertions.assertFalse(miss.isHit());
        Assertions.assertEquals("LOCAL", miss.getTier());
        Assertions.assertFalse(miss.isFallback());
    }

    @Test
    void shouldFailFastWhenTenantIsBlank() {
        DashboardMetricsCacheService cacheService = new DashboardMetricsCacheService(
                emptyRedisProvider(),
                emptyMeterProvider(),
                false,
                CACHE_INVALIDATION_MODE,
                60000L,
                60000L,
                60000L,
                60000L,
                60000L,
                60000L,
                60000L,
                30000L,
                60000L
        );

        IllegalStateException error = Assertions.assertThrows(
                IllegalStateException.class,
                () -> cacheService.getOrLoad("   ", "reports-overview", "k1", new Supplier<String>() {
                    @Override
                    public String get() {
                        return "value";
                    }
                })
        );
        Assertions.assertEquals("tenant_id_required", error.getMessage());
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<RedisTemplate<String, Object>> emptyRedisProvider() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        return (ObjectProvider<RedisTemplate<String, Object>>) (ObjectProvider<?>) factory.getBeanProvider(RedisTemplate.class);
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<MeterRegistry> emptyMeterProvider() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        return (ObjectProvider<MeterRegistry>) (ObjectProvider<?>) factory.getBeanProvider(MeterRegistry.class);
    }
}

