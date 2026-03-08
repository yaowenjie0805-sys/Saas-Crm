package com.yao.crm.dto.request;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

public class CreateContractRequest {

    @NotBlank(message = "contract_customer_title_required")
    private String customerId;

    private String contractNo;

    @NotBlank(message = "contract_customer_title_required")
    private String title;

    @Min(value = 0, message = "amount_gte_0")
    private Long amount;

    @NotBlank(message = "contract_status_required")
    private String status;

    private String signDate;
    private String owner;

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getContractNo() { return contractNo; }
    public void setContractNo(String contractNo) { this.contractNo = contractNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSignDate() { return signDate; }
    public void setSignDate(String signDate) { this.signDate = signDate; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
}
