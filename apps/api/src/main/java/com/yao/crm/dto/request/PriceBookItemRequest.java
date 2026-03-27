package com.yao.crm.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 价格手册条目请求DTO
 */
public class PriceBookItemRequest {

    @NotBlank(message = "产品ID不能为空")
    @Size(max = 50, message = "产品ID长度不能超过50")
    private String productId;

    private Long price;

    private Double taxRate;

    @Size(max = 10, message = "货币长度不能超过10")
    private String currency;

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

    public Double getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(Double taxRate) {
        this.taxRate = taxRate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
