package com.yao.crm.service;

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

    @Test
    void shouldReturnHitWithinTtlAndMissAfterExpire() throws Exception {
        DashboardMetricsCacheService cacheService = new DashboardMetricsCacheService(
                emptyRedisProvider(),
                emptyMeterProvider(),
                false,
                "tenant",
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

        DashboardMetricsCacheService.CachedValue<String> miss = cacheService.getOrLoad(
                "tenant_a",
                "reports-overview",
                "k1",
                40L,
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

        DashboardMetricsCacheService.CachedValue<String> hit = cacheService.getOrLoad(
                "tenant_a",
                "reports-overview",
                "k1",
                40L,
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

        Thread.sleep(60L);
        DashboardMetricsCacheService.CachedValue<String> missAfterTtl = cacheService.getOrLoad(
                "tenant_a",
                "reports-overview",
                "k1",
                40L,
                new Supplier<String>() {
                    @Override
                    public String get() {
                        calls.incrementAndGet();
                        return "v3";
                    }
                }
        );
        Assertions.assertFalse(missAfterTtl.isHit());
        Assertions.assertEquals("v3", missAfterTtl.getValue());
        Assertions.assertEquals(2, calls.get());
    }

    @Test
    void shouldEvictByTenantOnly() {
        DashboardMetricsCacheService cacheService = new DashboardMetricsCacheService(
                emptyRedisProvider(),
                emptyMeterProvider(),
                false,
                "tenant",
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

        cacheService.getOrLoad("tenant_a", "dashboard-overview", "a", new Supplier<String>() {
            @Override
            public String get() {
                return "tenant-a";
            }
        });
        cacheService.getOrLoad("tenant_b", "dashboard-overview", "b", new Supplier<String>() {
            @Override
            public String get() {
                return "tenant-b";
            }
        });

        cacheService.evictTenant("tenant_a");

        DashboardMetricsCacheService.CachedValue<String> tenantAHit = cacheService.getOrLoad(
                "tenant_a", "dashboard-overview", "a", new Supplier<String>() {
                    @Override
                    public String get() {
                        return "tenant-a-2";
                    }
                }
        );
        DashboardMetricsCacheService.CachedValue<String> tenantBHit = cacheService.getOrLoad(
                "tenant_b", "dashboard-overview", "b", new Supplier<String>() {
                    @Override
                    public String get() {
                        return "tenant-b-2";
                    }
                }
        );

        Assertions.assertFalse(tenantAHit.isHit());
        Assertions.assertTrue(tenantBHit.isHit());
        Assertions.assertEquals("tenant-b", tenantBHit.getValue());
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
