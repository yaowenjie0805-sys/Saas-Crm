package com.yao.crm.repository;

import com.yao.crm.entity.SalesForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesForecastRepository extends JpaRepository<SalesForecast, String> {

    List<SalesForecast> findByTenantId(String tenantId);

    List<SalesForecast> findByTenantIdAndForecastPeriod(String tenantId, String forecastPeriod);

    @Query("SELECT sf FROM SalesForecast sf WHERE sf.tenantId = :tenantId AND sf.owner = :owner AND sf.forecastPeriod = :period")
    List<SalesForecast> findByTenantIdAndOwnerAndPeriod(
            @Param("tenantId") String tenantId,
            @Param("owner") String owner,
            @Param("period") String forecastPeriod);

    @Query("SELECT sf FROM SalesForecast sf WHERE sf.tenantId = :tenantId AND sf.periodStart >= :startDate AND sf.periodEnd <= :endDate")
    List<SalesForecast> findByTenantIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    Optional<SalesForecast> findByTenantIdAndForecastPeriodAndPeriodStart(
            String tenantId, String forecastPeriod, LocalDate periodStart);

    void deleteByTenantIdAndForecastPeriodAndPeriodStart(
            String tenantId, String forecastPeriod, LocalDate periodStart);
}
