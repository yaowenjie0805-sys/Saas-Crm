package com.yao.crm.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_book_items")
public class PriceBookItem {
    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 64)
    private String priceBookId;

    @Column(nullable = false, length = 64)
    private String productId;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    private Double taxRate;

    @Column(nullable = false, length = 16)
    private String currency;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (tenantId == null || tenantId.trim().isEmpty()) throw new IllegalStateException("tenant_id_required");
        if (currency == null || currency.trim().isEmpty()) currency = "CNY";
        if (price == null) price = 0L;
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
    public String getPriceBookId() { return priceBookId; }
    public void setPriceBookId(String priceBookId) { this.priceBookId = priceBookId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }
    public Double getTaxRate() { return taxRate; }
    public void setTaxRate(Double taxRate) { this.taxRate = taxRate; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
