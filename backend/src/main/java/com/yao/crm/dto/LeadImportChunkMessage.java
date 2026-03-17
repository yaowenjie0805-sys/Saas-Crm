package com.yao.crm.dto;

import java.util.List;

public class LeadImportChunkMessage {
    private String tenantId;
    private String jobId;
    private Integer chunkNo;
    private List<String> rows;
    private Integer retryCount;
    private String requestId;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public Integer getChunkNo() { return chunkNo; }
    public void setChunkNo(Integer chunkNo) { this.chunkNo = chunkNo; }
    public List<String> getRows() { return rows; }
    public void setRows(List<String> rows) { this.rows = rows; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}
