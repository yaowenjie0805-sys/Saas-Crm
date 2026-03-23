package com.yao.crm.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 80)
    private String code;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(length = 80)
    private String category;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private Long standardPrice;

    @Column(nullable = false)
    private Double taxRate;

    @Column(nullable = false, length = 16)
    private String currency;

    @Column(length = 32)
    private String unit;

    @Column(length = 80)
    private String saleRegion;

    private LocalDate validFrom;
    private LocalDate validTo;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (tenantId == null || tenantId.trim().isEmpty()) tenantId = "tenant_default";
        if (status == null || status.trim().isEmpty()) status = "ACTIVE";
        if (currency == null || currency.trim().isEmpty()) currency = "CNY";
        if (standardPrice == null) standardPrice = 0L;
        if (taxRate == null) taxRate = 0.0;
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getStandardPrice() { return standardPrice; }
    public void setStandardPrice(Long standardPrice) { this.standardPrice = standardPrice; }
    public Double getTaxRate() { return taxRate; }
    public void setTaxRate(Double taxRate) { this.taxRate = taxRate; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getSaleRegion() { return saleRegion; }
    public void setSaleRegion(String saleRegion) { this.saleRegion = saleRegion; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    // Java 8 兼容：添加 setPrice 方法接受 BigDecimal
    public void setPrice(BigDecimal price) {
        this.standardPrice = price != null ? price.longValue() : 0L;
    }
}

