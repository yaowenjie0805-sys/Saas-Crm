package com.yao.crm.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "quote_versions")
public class QuoteVersion {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 64)
    private String quoteId;

    @Column(nullable = false)
    private Integer versionNo;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false)
    private Long totalAmount;

    @Column(nullable = false, length = 120)
    private String createdBy;

    @Column(nullable = false, length = 2000)
    private String snapshotJson;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (tenantId == null || tenantId.trim().isEmpty()) throw new IllegalStateException("tenant_id_required");
        if (versionNo == null || versionNo < 1) versionNo = 1;
        if (status == null || status.trim().isEmpty()) status = "DRAFT";
        if (totalAmount == null) totalAmount = 0L;
        if (createdBy == null || createdBy.trim().isEmpty()) createdBy = "unknown";
        if (snapshotJson == null) snapshotJson = "{}";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getQuoteId() { return quoteId; }
    public void setQuoteId(String quoteId) { this.quoteId = quoteId; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Long totalAmount) { this.totalAmount = totalAmount; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getSnapshotJson() { return snapshotJson; }
    public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
