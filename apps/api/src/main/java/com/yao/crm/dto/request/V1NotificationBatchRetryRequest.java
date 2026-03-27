package com.yao.crm.dto.request;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

public class V1NotificationBatchRetryRequest {

    @NotEmpty(message = "job_ids_required")
    @Size(max = 100, message = "job_ids_too_many")
    private List<String> jobIds;

    public List<String> getJobIds() {
        return jobIds;
    }

    public void setJobIds(List<String> jobIds) {
        this.jobIds = jobIds;
    }
}

