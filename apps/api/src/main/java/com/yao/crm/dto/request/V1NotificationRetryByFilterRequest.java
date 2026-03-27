package com.yao.crm.dto.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

public class V1NotificationRetryByFilterRequest {

    @Size(max = 40, message = "status_too_long")
    private String status;

    @Min(value = 1, message = "page_gte_1")
    private Integer page;

    @Min(value = 1, message = "size_gte_1")
    @Max(value = 100, message = "size_lte_100")
    private Integer size;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}

