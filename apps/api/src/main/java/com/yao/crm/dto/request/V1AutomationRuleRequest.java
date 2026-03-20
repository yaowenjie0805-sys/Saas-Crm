package com.yao.crm.dto.request;

import javax.validation.constraints.NotBlank;

public class V1AutomationRuleRequest {
    @NotBlank(message = "automation_name_required")
    private String name;
    @NotBlank(message = "automation_trigger_required")
    private String triggerType;
    @NotBlank(message = "automation_trigger_required")
    private String triggerExpr;
    @NotBlank(message = "automation_action_required")
    private String actionType;
    @NotBlank(message = "automation_action_required")
    private String actionPayload;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public String getTriggerExpr() { return triggerExpr; }
    public void setTriggerExpr(String triggerExpr) { this.triggerExpr = triggerExpr; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getActionPayload() { return actionPayload; }
    public void setActionPayload(String actionPayload) { this.actionPayload = actionPayload; }
}
