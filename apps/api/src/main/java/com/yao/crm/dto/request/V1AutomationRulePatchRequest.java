package com.yao.crm.dto.request;

import javax.validation.constraints.Size;

public class V1AutomationRulePatchRequest {

    @Size(max = 120, message = "name_too_long")
    private String name;

    @Size(max = 40, message = "trigger_type_too_long")
    private String triggerType;

    @Size(max = 500, message = "trigger_expr_too_long")
    private String triggerExpr;

    @Size(max = 40, message = "action_type_too_long")
    private String actionType;

    @Size(max = 1000, message = "action_payload_too_long")
    private String actionPayload;

    private Boolean enabled;

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
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
