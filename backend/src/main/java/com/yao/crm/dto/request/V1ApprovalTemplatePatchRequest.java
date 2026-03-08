package com.yao.crm.dto.request;

import com.fasterxml.jackson.databind.JsonNode;

public class V1ApprovalTemplatePatchRequest {
    private String name;
    private Long amountMin;
    private Long amountMax;
    private String role;
    private String department;
    private String approverRoles;
    private Boolean enabled;
    private JsonNode flowDefinition;
    private String status;
    private Integer version;

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
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public JsonNode getFlowDefinition() { return flowDefinition; }
    public void setFlowDefinition(JsonNode flowDefinition) { this.flowDefinition = flowDefinition; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
