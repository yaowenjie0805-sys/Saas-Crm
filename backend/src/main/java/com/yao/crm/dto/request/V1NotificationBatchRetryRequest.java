package com.yao.crm.dto.request;

import java.util.List;

public class V1NotificationBatchRetryRequest {
    private List<String> jobIds;

    public List<String> getJobIds() {
        return jobIds;
    }

    public void setJobIds(List<String> jobIds) {
        this.jobIds = jobIds;
    }
}

