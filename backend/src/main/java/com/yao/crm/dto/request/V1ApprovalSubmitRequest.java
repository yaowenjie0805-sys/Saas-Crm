package com.yao.crm.dto.request;

import javax.validation.constraints.NotBlank;

public class V1ApprovalSubmitRequest {
    @NotBlank(message = "approval_biz_type_required")
    private String bizType;
    @NotBlank(message = "approval_biz_id_required")
    private String bizId;
    private Long amount;
    private String role;
    private String department;
    private String comment;

    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public String getBizId() { return bizId; }
    public void setBizId(String bizId) { this.bizId = bizId; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
