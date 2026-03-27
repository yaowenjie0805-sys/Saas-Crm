package com.yao.crm.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 创建报价请求DTO
 */
public class QuoteCreateRequest {

    @Size(max = 50, message = "商机ID长度不能超过50")
    private String opportunityId;

    @Size(max = 50, message = "客户ID长度不能超过50")
    private String customerId;

    @Size(max = 200, message = "报价名称长度不能超过200")
    private String name;

    @Size(max = 500, message = "描述长度不能超过500")
    private String description;

    @Pattern(regexp = "DRAFT|SUBMITTED|APPROVED|REJECTED|ACCEPTED|EXPIRED|CANCELED", message = "状态无效")
    private String status;

    @Size(max = 100, message = "负责人长度不能超过100")
    private String owner;

    @Size(max = 10, message = "货币长度不能超过10")
    private String currency;

    private Long totalAmount;

    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "有效期格式必须为yyyy-MM-dd")
    private String validUntil;

    public String getOpportunityId() {
        return opportunityId;
    }

    public void setOpportunityId(String opportunityId) {
        this.opportunityId = opportunityId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
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

    public String getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(String validUntil) {
        this.validUntil = validUntil;
    }
}
