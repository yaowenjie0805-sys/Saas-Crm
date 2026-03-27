package com.yao.crm.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 更新产品请求DTO
 */
public class ProductPatchRequest {

    @NotBlank(message = "产品ID不能为空")
    @Size(max = 50, message = "产品ID长度不能超过50")
    private String id;

    @Size(max = 50, message = "产品编码长度不能超过50")
    private String code;

    @Size(max = 200, message = "产品名称长度不能超过200")
    private String name;

    @Size(max = 1000, message = "描述长度不能超过1000")
    private String description;

    @Pattern(regexp = "ACTIVE|INACTIVE", message = "状态必须是ACTIVE或INACTIVE")
    private String status;

    private Long standardPrice;

    private Double taxRate;

    @Size(max = 10, message = "货币长度不能超过10")
    private String currency;

    @Size(max = 50, message = "类别长度不能超过50")
    private String category;

    @Size(max = 100, message = "单位长度不能超过100")
    private String unit;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public Long getStandardPrice() {
        return standardPrice;
    }

    public void setStandardPrice(Long standardPrice) {
        this.standardPrice = standardPrice;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
