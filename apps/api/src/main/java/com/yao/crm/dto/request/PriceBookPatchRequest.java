package com.yao.crm.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 更新价格手册请求DTO
 */
public class PriceBookPatchRequest {

    @NotBlank(message = "价格手册ID不能为空")
    @Size(max = 50, message = "价格手册ID长度不能超过50")
    private String id;

    @Size(max = 200, message = "价格手册名称长度不能超过200")
    private String name;

    @Size(max = 500, message = "描述长度不能超过500")
    private String description;

    @Pattern(regexp = "ACTIVE|INACTIVE", message = "状态必须是ACTIVE或INACTIVE")
    private String status;

    private Boolean isDefault;

    @Size(max = 10, message = "货币长度不能超过10")
    private String currency;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
