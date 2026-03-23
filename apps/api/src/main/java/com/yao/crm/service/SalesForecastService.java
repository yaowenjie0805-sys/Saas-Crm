package com.yao.crm.service;

import com.yao.crm.entity.SalesForecast;
import com.yao.crm.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 销售预测服务
 * 基于历史数据提供销售预测功能
 */
@Service
public class SalesForecastService {

    private final SalesForecastRepository salesForecastRepository;
    private final OpportunityRepository opportunityRepository;
    private final OrderRecordRepository orderRecordRepository;
    private final QuoteRepository quoteRepository;

    // 预测模型版本
    private static final String MODEL_VERSION = "V1.0";

    // 预测权重配置
    private static final Map<String, Double> STAGE_WEIGHTS = new LinkedHashMap<>();

    static {
        STAGE_WEIGHTS.put("LEAD", 0.05);
        STAGE_WEIGHTS.put("QUALIFIED", 0.15);
        STAGE_WEIGHTS.put("PROPOSAL", 0.30);
        STAGE_WEIGHTS.put("NEGOTIATION", 0.50);
        STAGE_WEIGHTS.put("CLOSED_WON", 1.00);
    }

    public SalesForecastService(
            SalesForecastRepository salesForecastRepository,
            OpportunityRepository opportunityRepository,
            OrderRecordRepository orderRecordRepository,
            QuoteRepository quoteRepository) {
        this.salesForecastRepository = salesForecastRepository;
        this.opportunityRepository = opportunityRepository;
        this.orderRecordRepository = orderRecordRepository;
        this.quoteRepository = quoteRepository;
    }

    /**
     * 计算销售预测
     */
    public SalesForecast calculateForecast(String tenantId, String periodType, LocalDate periodStart) {
        LocalDate periodEnd = calculatePeriodEnd(periodType, periodStart);
        LocalDateTime now = LocalDateTime.now();

        // 获取当前Pipeline金额
        long pipelineAmount = calculatePipelineAmount(tenantId, periodEnd);

        // 计算加权金额
        long weightedAmount = calculateWeightedPipelineAmount(tenantId);

        // 计算已确认金额
        long confirmedAmount = calculateConfirmedAmount(tenantId, periodStart, periodEnd);

        // 计算预测金额（加权金额 * 转化率 + 已确认金额）
        long predictedAmount = calculatePredictedAmount(weightedAmount, confirmedAmount);

        // 计算置信度
        double confidence = calculateConfidence(pipelineAmount, confirmedAmount);

        // 预测数量
        int predictedCount = calculatePredictedCount(tenantId, periodEnd);

        // 保存预测结果
        SalesForecast forecast = new SalesForecast();
        forecast.setId(UUID.randomUUID().toString());
        forecast.setTenantId(tenantId);
        forecast.setForecastPeriod(periodType);
        forecast.setPeriodStart(periodStart);
        forecast.setPeriodEnd(periodEnd);
        forecast.setPredictedAmount(predictedAmount);
        forecast.setPredictedCount(predictedCount);
        forecast.setConfirmedAmount(confirmedAmount);
        forecast.setPipelineAmount(pipelineAmount);
        forecast.setConfidenceLevel(BigDecimal.valueOf(confidence).setScale(2, RoundingMode.HALF_UP));
        forecast.setModelVersion(MODEL_VERSION);
        forecast.setComputedAt(now);
        forecast.setCreatedAt(now);
        forecast.setUpdatedAt(now);

        return salesForecastRepository.save(forecast);
    }

    /**
     * 计算Pipeline总金额
     */
    private long calculatePipelineAmount(String tenantId, LocalDate endDate) {
        // 获取所有活跃商机
        List<String> excludedStages = new ArrayList<>();
        excludedStages.add("CLOSED_WON");
        excludedStages.add("CLOSED_LOST");
        List<Object[]> opportunities = opportunityRepository.sumAmountByTenantIdAndStageNotIn(
                tenantId, excludedStages);

        if (opportunities.isEmpty()) {
            return 0L;
        }

        long total = 0L;
        for (Object[] row : opportunities) {
            if (row[0] != null) {
                total += ((Number) row[0]).longValue();
            }
        }

        return total;
    }

    /**
     * 计算Pipeline加权金额
     */
    private long calculateWeightedPipelineAmount(String tenantId) {
        // 获取各阶段商机
        long weightedAmount = 0L;

        for (Map.Entry<String, Double> entry : STAGE_WEIGHTS.entrySet()) {
            String stage = entry.getKey();
            double weight = entry.getValue();

            Long stageAmount = opportunityRepository.sumAmountByTenantIdAndStage(tenantId, stage);

            if (stageAmount != null && stageAmount > 0) {
                weightedAmount += (long) (stageAmount * weight);
            }
        }

        return weightedAmount;
    }

    /**
     * 计算已确认金额
     */
    private long calculateConfirmedAmount(String tenantId, LocalDate startDate, LocalDate endDate) {
        // 获取已完成订单金额
        Long totalAmount = orderRecordRepository.sumAmountByTenantIdAndStatusAndCreatedAtBetween(
                tenantId, "COMPLETED", startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

        return totalAmount != null ? totalAmount : 0L;
    }

    /**
     * 计算预测金额
     */
    private long calculatePredictedAmount(long weightedAmount, long confirmedAmount) {
        // 简单预测模型：已确认金额 + 加权金额 * 调整系数
        double adjustmentFactor = 0.7; // 调整系数，考虑历史转化率
        return confirmedAmount + (long) (weightedAmount * adjustmentFactor);
    }

    /**
     * 计算置信度
     */
    private double calculateConfidence(long pipelineAmount, long confirmedAmount) {
        // 基于Pipeline和已确认金额的比例计算置信度
        if (pipelineAmount == 0) {
            return 50.0; // 默认50%
        }

        double ratio = (double) confirmedAmount / pipelineAmount;

        // 置信度范围：50% - 95%
        double confidence = 50.0 + (ratio * 45.0);

        // 限制范围
        return Math.min(Math.max(confidence, 50.0), 95.0);
    }

    /**
     * 计算预测数量
     */
    private int calculatePredictedCount(String tenantId, LocalDate endDate) {
        // 统计预计在periodEnd前完成的商机数量
        long count = opportunityRepository.countByStageAndTenantId("NEGOTIATION", tenantId);
        return (int) count;
    }

    /**
     * 计算周期结束日期
     */
    private LocalDate calculatePeriodEnd(String periodType, LocalDate periodStart) {
        switch (periodType.toUpperCase()) {
            case "WEEK":
                return periodStart.plusDays(6);
            case "MONTH":
                return periodStart.plusMonths(1).minusDays(1);
            case "QUARTER":
                return periodStart.plusMonths(3).minusDays(1);
            case "YEAR":
                return periodStart.plusYears(1).minusDays(1);
            default:
                return periodStart.plusMonths(1).minusDays(1);
        }
    }

    /**
     * 获取预测列表
     */
    public List<SalesForecast> getForecasts(String tenantId, String periodType) {
        if (periodType != null && !periodType.isEmpty()) {
            return salesForecastRepository.findByTenantIdAndForecastPeriod(tenantId, periodType);
        }
        return salesForecastRepository.findByTenantId(tenantId);
    }

    /**
     * 获取预测对比（同比/环比）
     */
    public Map<String, Object> getForecastComparison(String tenantId, LocalDate currentPeriodStart) {
        Map<String, Object> comparison = new HashMap<>();

        // 当前预测
        SalesForecast currentForecast = salesForecastRepository
                .findByTenantIdAndForecastPeriodAndPeriodStart(tenantId, "MONTH", currentPeriodStart)
                .orElse(null);

        // 上一期预测
        LocalDate previousPeriodStart = currentPeriodStart.minusMonths(1);
        SalesForecast previousForecast = salesForecastRepository
                .findByTenantIdAndForecastPeriodAndPeriodStart(tenantId, "MONTH", previousPeriodStart)
                .orElse(null);

        // 去年同期
        LocalDate lastYearStart = currentPeriodStart.minusYears(1);
        SalesForecast lastYearForecast = salesForecastRepository
                .findByTenantIdAndForecastPeriodAndPeriodStart(tenantId, "MONTH", lastYearStart)
                .orElse(null);

        comparison.put("current", currentForecast);
        comparison.put("previous", previousForecast);
        comparison.put("lastYear", lastYearForecast);

        // 计算变化
        if (currentForecast != null && previousForecast != null) {
            long amountDiff = currentForecast.getPredictedAmount() - previousForecast.getPredictedAmount();
            double amountChange = previousForecast.getPredictedAmount() > 0
                    ? (amountDiff * 100.0 / previousForecast.getPredictedAmount())
                    : 0;

            comparison.put("amountChange", amountChange);
            comparison.put("amountChangeValue", amountDiff);
        }

        if (currentForecast != null && lastYearForecast != null) {
            long yearDiff = currentForecast.getPredictedAmount() - lastYearForecast.getPredictedAmount();
            double yearOverYear = lastYearForecast.getPredictedAmount() > 0
                    ? (yearDiff * 100.0 / lastYearForecast.getPredictedAmount())
                    : 0;

            comparison.put("yearOverYearChange", yearOverYear);
            comparison.put("yearOverYearValue", yearDiff);
        }

        return comparison;
    }

    /**
     * 手动调整预测
     */
    public SalesForecast adjustForecast(String forecastId, String adjustedBy, long adjustmentAmount, String reason) {
        SalesForecast forecast = salesForecastRepository.findById(forecastId).orElse(null);
        if (forecast == null) {
            throw new IllegalArgumentException("Forecast not found");
        }

        long newAmount = forecast.getPredictedAmount() + adjustmentAmount;
        forecast.setPredictedAmount(Math.max(0, newAmount));
        forecast.setUpdatedAt(LocalDateTime.now());

        return salesForecastRepository.save(forecast);
    }

    /**
     * 获取预测趋势
     */
    public List<Map<String, Object>> getForecastTrend(String tenantId, int periods) {
        List<Map<String, Object>> trend = new ArrayList<>();

        LocalDate currentDate = LocalDate.now();

        for (int i = periods - 1; i >= 0; i--) {
            LocalDate periodStart = currentDate.minusMonths(i);

            Optional<SalesForecast> forecast = salesForecastRepository
                    .findByTenantIdAndForecastPeriodAndPeriodStart(tenantId, "MONTH", periodStart.withDayOfMonth(1));

            Map<String, Object> periodData = new HashMap<>();
            periodData.put("period", periodStart.toString());

            if (forecast.isPresent()) {
                SalesForecast f = forecast.get();
                periodData.put("predicted", f.getPredictedAmount());
                periodData.put("confirmed", f.getConfirmedAmount());
                periodData.put("pipeline", f.getPipelineAmount());
                periodData.put("confidence", f.getConfidenceLevel());
            } else {
                periodData.put("predicted", 0);
                periodData.put("confirmed", 0);
                periodData.put("pipeline", 0);
                periodData.put("confidence", 0);
            }

            trend.add(periodData);
        }

        return trend;
    }
}
