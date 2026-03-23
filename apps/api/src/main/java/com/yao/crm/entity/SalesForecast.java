package com.yao.crm.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售预测实体
 */
@Entity
@Table(name = "sales_forecasts")
public class SalesForecast {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(length = 80)
    private String owner;

    @Column(nullable = false, length = 20)
    private String forecastPeriod;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false)
    private Long predictedAmount;

    @Column(nullable = false)
    private Integer predictedCount;

    @Column(nullable = false)
    private Long confirmedAmount;

    @Column(precision = 5, scale = 2)
    private BigDecimal confidenceLevel;

    @Column(nullable = false)
    private Long pipelineAmount;

    @Column(length = 40)
    private String modelVersion;

    @Column(nullable = false)
    private LocalDateTime computedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (id == null) id = java.util.UUID.randomUUID().toString();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (computedAt == null) computedAt = now;
        if (predictedAmount == null) predictedAmount = 0L;
        if (predictedCount == null) predictedCount = 0;
        if (confirmedAmount == null) confirmedAmount = 0L;
        if (pipelineAmount == null) pipelineAmount = 0L;
        if (tenantId == null || tenantId.trim().isEmpty()) tenantId = "tenant_default";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getForecastPeriod() {
        return forecastPeriod;
    }

    public void setForecastPeriod(String forecastPeriod) {
        this.forecastPeriod = forecastPeriod;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public Long getPredictedAmount() {
        return predictedAmount;
    }

    public void setPredictedAmount(Long predictedAmount) {
        this.predictedAmount = predictedAmount;
    }

    public Integer getPredictedCount() {
        return predictedCount;
    }

    public void setPredictedCount(Integer predictedCount) {
        this.predictedCount = predictedCount;
    }

    public Long getConfirmedAmount() {
        return confirmedAmount;
    }

    public void setConfirmedAmount(Long confirmedAmount) {
        this.confirmedAmount = confirmedAmount;
    }

    public BigDecimal getConfidenceLevel() {
        return confidenceLevel;
    }

    public void setConfidenceLevel(BigDecimal confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }

    public Long getPipelineAmount() {
        return pipelineAmount;
    }

    public void setPipelineAmount(Long pipelineAmount) {
        this.pipelineAmount = pipelineAmount;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public LocalDateTime getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(LocalDateTime computedAt) {
        this.computedAt = computedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
