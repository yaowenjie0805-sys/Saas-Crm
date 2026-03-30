package com.yao.crm.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 创建订单请求DTO
 */
public class OrderCreateRequest {

    @Size(max = 50, message = "报价ID长度不能超过50")
    private String quoteId;

    @NotBlank(message = "客户ID不能为空")
    @Size(max = 50, message = "客户ID长度不能超过50")
    private String customerId;

    @Size(max = 50, message = "商机ID长度不能超过50")
    private String opportunityId;

    @Size(max = 200, message = "订单名称长度不能超过200")
    private String name;

    @Size(max = 500, message = "描述长度不能超过500")
    private String description;

    @Pattern(regexp = "DRAFT|CONFIRMED|FULFILLING|COMPLETED|CANCELED", message = "状态无效")
    private String status;

    @Size(max = 100, message = "负责人长度不能超过100")
    private String owner;

    @Size(max = 10, message = "货币长度不能超过10")
    private String currency;

    private Long totalAmount;

    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "日期格式必须为yyyy-MM-dd")
    private String orderDate;

    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "日期格式必须为yyyy-MM-dd")
    private String deliveryDate;

    // ========== 扩展字段 ==========

    /**
     * 结算货币（如 CNY, USD）
     */
    @Size(max = 16, message = "结算货币长度不能超过16")
    private String settlementCurrency;

    /**
     * 汇率快照（格式：汇率@来源，如 1.000000@SYSTEM）
     */
    @Size(max = 40, message = "汇率快照长度不能超过40")
    private String exchangeRateSnapshot;

    /**
     * 发票状态（如 NOT_REQUIRED, PENDING, ISSUED, PAID）
     */
    @Size(max = 24, message = "发票状态长度不能超过24")
    private String invoiceStatus;

    /**
     * 税额显示模式（如 TAX_INCLUSIVE, TAX_EXCLUSIVE, NO_TAX）
     */
    @Size(max = 24, message = "税额显示模式长度不能超过24")
    private String taxDisplayMode;

    /**
     * 合规标签（如 STANDARD, COMPLIANCE_REQUIRED, EXPORT_CONTROLLED）
     */
    @Size(max = 32, message = "合规标签长度不能超过32")
    private String complianceTag;

    public String getQuoteId() {
        return quoteId;
    }

    public void setQuoteId(String quoteId) {
        this.quoteId = quoteId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getOpportunityId() {
        return opportunityId;
    }

    public void setOpportunityId(String opportunityId) {
        this.opportunityId = opportunityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    public String getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(String deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public String getSettlementCurrency() {
        return settlementCurrency;
    }

    public void setSettlementCurrency(String settlementCurrency) {
        this.settlementCurrency = settlementCurrency;
    }

    public String getExchangeRateSnapshot() {
        return exchangeRateSnapshot;
    }

    public void setExchangeRateSnapshot(String exchangeRateSnapshot) {
        this.exchangeRateSnapshot = exchangeRateSnapshot;
    }

    public String getInvoiceStatus() {
        return invoiceStatus;
    }

    public void setInvoiceStatus(String invoiceStatus) {
        this.invoiceStatus = invoiceStatus;
    }

    public String getTaxDisplayMode() {
        return taxDisplayMode;
    }

    public void setTaxDisplayMode(String taxDisplayMode) {
        this.taxDisplayMode = taxDisplayMode;
    }

    public String getComplianceTag() {
        return complianceTag;
    }

    public void setComplianceTag(String complianceTag) {
        this.complianceTag = complianceTag;
    }
}
