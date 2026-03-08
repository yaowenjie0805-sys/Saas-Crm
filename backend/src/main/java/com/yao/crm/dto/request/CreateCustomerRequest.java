package com.yao.crm.dto.request;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

public class CreateCustomerRequest {

    @NotBlank(message = "name_owner_status_required")
    private String name;

    @NotBlank(message = "name_owner_status_required")
    private String owner;

    private String tag;

    @NotBlank(message = "name_owner_status_required")
    private String status;

    @Min(value = 0, message = "value_gte_0")
    private Long value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }
}