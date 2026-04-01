package com.yao.crm.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "quote_items")
public class QuoteItem {
    @Id
    @Column(length = 64)
    private String id;
    @Column(nullable = false, length = 64)
    private String tenantId;
    @Column(nullable = false, length = 64)
    private String quoteId;
    @Column(nullable = false, length = 64)
    private String productId;
    @Column(nullable = false, length = 180)
    private String productName;
    @Column(nullable = false)
    private Integer quantity;
    @Column(nullable = false)
    private Long unitPrice;
    @Column(nullable = false)
    private Double discountRate;
    @Column(nullable = false)
    private Double taxRate;
    @Column(nullable = false)
    private Long subtotalAmount;
    @Column(nullable = false)
    private Long taxAmount;
    @Column(nullable = false)
    private Long totalAmount;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (tenantId == null || tenantId.trim().isEmpty()) throw new IllegalStateException("tenant_id_required");
        if (quantity == null || quantity < 1) quantity = 1;
        if (unitPrice == null) unitPrice = 0L;
        if (discountRate == null) discountRate = 0.0;
        if (taxRate == null) taxRate = 0.0;
        if (subtotalAmount == null) subtotalAmount = 0L;
        if (taxAmount == null) taxAmount = 0L;
        if (totalAmount == null) totalAmount = 0L;
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() { updatedAt = LocalDateTime.now(); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getQuoteId() { return quoteId; }
    public void setQuoteId(String quoteId) { this.quoteId = quoteId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Long getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Long unitPrice) { this.unitPrice = unitPrice; }
    public Double getDiscountRate() { return discountRate; }
    public void setDiscountRate(Double discountRate) { this.discountRate = discountRate; }
    public Double getTaxRate() { return taxRate; }
    public void setTaxRate(Double taxRate) { this.taxRate = taxRate; }
    public Long getSubtotalAmount() { return subtotalAmount; }
    public void setSubtotalAmount(Long subtotalAmount) { this.subtotalAmount = subtotalAmount; }
    public Long getTaxAmount() { return taxAmount; }
    public void setTaxAmount(Long taxAmount) { this.taxAmount = taxAmount; }
    public Long getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Long totalAmount) { this.totalAmount = totalAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
