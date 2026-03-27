package com.yao.crm.dto.request;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 报价单项请求DTO
 */
public class QuoteItemRequest {

    @NotBlank(message = "产品ID不能为空")
    @Size(max = 50, message = "产品ID长度不能超过50")
    private String productId;

    @Size(max = 200, message = "产品名称长度不能超过200")
    private String productName;

    @Min(value = 1, message = "数量必须大于等于1")
    private Integer quantity;

    @Min(value = 0, message = "单价必须大于等于0")
    private Long unitPrice;

    @Min(value = 0, message = "折扣率必须大于等于0")
    private Double discountRate;

    @Min(value = 0, message = "税率必须大于等于0")
    private Double taxRate;

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Long getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Long unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Double getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(Double discountRate) {
        this.discountRate = discountRate;
    }

    public Double getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(Double taxRate) {
        this.taxRate = taxRate;
    }
}
