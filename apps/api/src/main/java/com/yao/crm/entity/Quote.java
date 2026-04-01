package com.yao.crm.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "quotes")
public class Quote {
    @Id
    @Column(length = 64)
    private String id;
    @Column(nullable = false, length = 64)
    private String tenantId;
    @Column(nullable = false, length = 80)
    private String quoteNo;
    @Column(nullable = false, length = 64)
    private String customerId;
    @Column(length = 64)
    private String opportunityId;
    @Column(length = 64)
    private String priceBookId;
    @Column(nullable = false, length = 120)
    private String owner;
    @Column(nullable = false, length = 32)
    private String status;
    @Column(nullable = false)
    private Long subtotalAmount;
    @Column(nullable = false)
    private Long taxAmount;
    @Column(nullable = false)
    private Long totalAmount;
    @Column(nullable = false, length = 16)
    private String settlementCurrency;
    @Column(nullable = false, length = 40)
    private String exchangeRateSnapshot;
    @Column(nullable = false, length = 24)
    private String invoiceStatus;
    @Column(nullable = false, length = 24)
    private String taxDisplayMode;
    @Column(nullable = false, length = 32)
    private String complianceTag;
    @Column(nullable = false)
    private Integer version;
    private LocalDate validUntil;
    @Column(length = 500)
    private String notes;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (tenantId == null || tenantId.trim().isEmpty()) throw new IllegalStateException("tenant_id_required");
        if (status == null || status.trim().isEmpty()) status = "DRAFT";
        if (subtotalAmount == null) subtotalAmount = 0L;
        if (taxAmount == null) taxAmount = 0L;
        if (totalAmount == null) totalAmount = 0L;
        if (settlementCurrency == null || settlementCurrency.trim().isEmpty()) settlementCurrency = "CNY";
        if (exchangeRateSnapshot == null || exchangeRateSnapshot.trim().isEmpty()) exchangeRateSnapshot = "1.000000@SYSTEM";
        if (invoiceStatus == null || invoiceStatus.trim().isEmpty()) invoiceStatus = "NOT_REQUIRED";
        if (taxDisplayMode == null || taxDisplayMode.trim().isEmpty()) taxDisplayMode = "TAX_INCLUSIVE";
        if (complianceTag == null || complianceTag.trim().isEmpty()) complianceTag = "STANDARD";
        if (version == null || version < 1) version = 1;
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }
    @PreUpdate
    public void preUpdate() { updatedAt = LocalDateTime.now(); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getQuoteNo() { return quoteNo; }
    public void setQuoteNo(String quoteNo) { this.quoteNo = quoteNo; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getOpportunityId() { return opportunityId; }
    public void setOpportunityId(String opportunityId) { this.opportunityId = opportunityId; }
    public String getPriceBookId() { return priceBookId; }
    public void setPriceBookId(String priceBookId) { this.priceBookId = priceBookId; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getSubtotalAmount() { return subtotalAmount; }
    public void setSubtotalAmount(Long subtotalAmount) { this.subtotalAmount = subtotalAmount; }
    public Long getTaxAmount() { return taxAmount; }
    public void setTaxAmount(Long taxAmount) { this.taxAmount = taxAmount; }
    public Long getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Long totalAmount) { this.totalAmount = totalAmount; }
    public String getSettlementCurrency() { return settlementCurrency; }
    public void setSettlementCurrency(String settlementCurrency) { this.settlementCurrency = settlementCurrency; }
    public String getExchangeRateSnapshot() { return exchangeRateSnapshot; }
    public void setExchangeRateSnapshot(String exchangeRateSnapshot) { this.exchangeRateSnapshot = exchangeRateSnapshot; }
    public String getInvoiceStatus() { return invoiceStatus; }
    public void setInvoiceStatus(String invoiceStatus) { this.invoiceStatus = invoiceStatus; }
    public String getTaxDisplayMode() { return taxDisplayMode; }
    public void setTaxDisplayMode(String taxDisplayMode) { this.taxDisplayMode = taxDisplayMode; }
    public String getComplianceTag() { return complianceTag; }
    public void setComplianceTag(String complianceTag) { this.complianceTag = complianceTag; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
