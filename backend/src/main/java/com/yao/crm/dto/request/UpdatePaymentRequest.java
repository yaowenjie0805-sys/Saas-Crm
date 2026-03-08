package com.yao.crm.dto.request;

import javax.validation.constraints.Min;

public class UpdatePaymentRequest {

    private String contractId;

    @Min(value = 0, message = "amount_gte_0")
    private Long amount;

    private String receivedDate;
    private String method;
    private String status;
    private String remark;

    public String getContractId() { return contractId; }
    public void setContractId(String contractId) { this.contractId = contractId; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public String getReceivedDate() { return receivedDate; }
    public void setReceivedDate(String receivedDate) { this.receivedDate = receivedDate; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
