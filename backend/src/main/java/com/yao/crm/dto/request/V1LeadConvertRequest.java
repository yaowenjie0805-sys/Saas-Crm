package com.yao.crm.dto.request;

import javax.validation.constraints.Size;

public class V1LeadConvertRequest {

    @Size(max = 120, message = "bad_request")
    private String customerOwner;

    @Size(max = 120, message = "bad_request")
    private String contactName;

    @Size(max = 80, message = "bad_request")
    private String opportunityStage;

    @Size(max = 80, message = "bad_request")
    private String contactTitle;

    public String getCustomerOwner() { return customerOwner; }
    public void setCustomerOwner(String customerOwner) { this.customerOwner = customerOwner; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getOpportunityStage() { return opportunityStage; }
    public void setOpportunityStage(String opportunityStage) { this.opportunityStage = opportunityStage; }
    public String getContactTitle() { return contactTitle; }
    public void setContactTitle(String contactTitle) { this.contactTitle = contactTitle; }
}
