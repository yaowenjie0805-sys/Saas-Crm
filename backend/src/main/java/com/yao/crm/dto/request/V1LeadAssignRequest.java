package com.yao.crm.dto.request;

import javax.validation.constraints.Size;

public class V1LeadAssignRequest {

    @Size(max = 120, message = "bad_request")
    private String owner;

    private Boolean useRule;

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public Boolean getUseRule() { return useRule; }
    public void setUseRule(Boolean useRule) { this.useRule = useRule; }
}
