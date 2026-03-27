package com.yao.crm.dto.request;

import javax.validation.constraints.Size;

/**
 * 创建报价版本请求DTO
 */
public class QuoteVersionCreateRequest {

    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
