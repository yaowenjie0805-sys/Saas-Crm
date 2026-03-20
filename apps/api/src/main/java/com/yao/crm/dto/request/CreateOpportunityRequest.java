package com.yao.crm.dto.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

public class CreateOpportunityRequest {

    @NotBlank(message = "stage_required")
    private String stage;

    @Min(value = 0, message = "count_gte_0")
    private Integer count;

    @Min(value = 0, message = "amount_gte_0")
    private Long amount;

    @Min(value = 0, message = "progress_range")
    @Max(value = 100, message = "progress_range")
    private Integer progress;

    private String owner;

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
