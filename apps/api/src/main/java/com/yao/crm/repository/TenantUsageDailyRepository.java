package com.yao.crm.repository;

import com.yao.crm.entity.TenantUsageDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TenantUsageDailyRepository extends JpaRepository<TenantUsageDaily, String> {
    Optional<TenantUsageDaily> findByTenantIdAndUsageDateAndMetricKey(String tenantId, LocalDate usageDate, String metricKey);
    List<TenantUsageDaily> findAllByTenantIdAndUsageDateBetweenOrderByUsageDateAscMetricKeyAsc(String tenantId, LocalDate startDate, LocalDate endDate);
}
