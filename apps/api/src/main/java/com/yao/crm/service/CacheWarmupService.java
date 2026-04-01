package com.yao.crm.service;

import com.yao.crm.entity.Customer;
import com.yao.crm.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class CacheWarmupService {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmupService.class);
    private static final int WARMUP_BATCH_SIZE = 200;
    private static final int WARMUP_MAX_CUSTOMERS = 2000;

    private final CacheService cacheService;
    private final CustomerRepository customerRepository;

    @Autowired
    public CacheWarmupService(CacheService cacheService, CustomerRepository customerRepository) {
        this.cacheService = cacheService;
        this.customerRepository = customerRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void warmupCaches() {
        log.info("Starting cache warmup...");
        long startTime = System.currentTimeMillis();

        try {
            warmupCustomerCache();
            long endTime = System.currentTimeMillis();
            log.info("Cache warmup completed in {} ms", endTime - startTime);
        } catch (Exception e) {
            log.error("Failed to warmup caches", e);
        }
    }

    private void warmupCustomerCache() {
        try {
            int warmed = 0;
            for (int page = 0; warmed < WARMUP_MAX_CUSTOMERS; page++) {
                Page<Customer> batch = customerRepository.findAll(
                        PageRequest.of(page, WARMUP_BATCH_SIZE, Sort.by(Sort.Direction.DESC, "updatedAt"))
                );
                if (!batch.hasContent()) {
                    break;
                }

                for (Customer customer : batch.getContent()) {
                    cacheService.set("customer:" + customer.getId(), customer);
                    warmed++;
                    if (warmed >= WARMUP_MAX_CUSTOMERS) {
                        break;
                    }
                }

                if (!batch.hasNext()) {
                    break;
                }
            }

            log.info("Customer cache warmup completed, warmed {} records", warmed);
        } catch (Exception e) {
            log.error("Failed to warmup customer cache", e);
        }
    }

    @Async
    public void refreshHotCaches() {
        log.info("Refreshing hot caches...");
        try {
            warmupCustomerCache();
            log.info("Hot caches refreshed");
        } catch (Exception e) {
            log.error("Failed to refresh hot caches", e);
        }
    }
}
