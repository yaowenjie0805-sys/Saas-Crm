package com.yao.crm.dto.request;

import com.fasterxml.jackson.databind.JsonNode;

import javax.validation.constraints.NotBlank;

public class V1ApprovalTemplateRequest {
    @NotBlank(message = "approval_biz_type_required")
    private String bizType;
    @NotBlank(message = "approval_name_required")
    private String name;
    private Long amountMin;
    private Long amountMax;
    private String role;
    private String department;
    @NotBlank(message = "approval_approver_required")
    private String approverRoles;
    private JsonNode flowDefinition;

    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getAmountMin() { return amountMin; }
    public void setAmountMin(Long amountMin) { this.amountMin = amountMin; }
    public Long getAmountMax() { return amountMax; }
    public void setAmountMax(Long amountMax) { this.amountMax = amountMax; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getApproverRoles() { return approverRoles; }
    public void setApproverRoles(String approverRoles) { this.approverRoles = approverRoles; }
    public JsonNode getFlowDefinition() { return flowDefinition; }
    public void setFlowDefinition(JsonNode flowDefinition) { this.flowDefinition = flowDefinition; }
}
