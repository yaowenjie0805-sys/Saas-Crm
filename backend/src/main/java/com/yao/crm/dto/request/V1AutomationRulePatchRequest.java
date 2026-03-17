package com.yao.crm.dto.request;

public class V1AutomationRulePatchRequest {
    private String name;
    private String triggerType;
    private String triggerExpr;
    private String actionType;
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
