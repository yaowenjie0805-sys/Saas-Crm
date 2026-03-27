package com.yao.crm.event;

public class ApprovalCompletedEvent extends DomainEvent {
    private final String approvalResult; // APPROVED / REJECTED
    private final String templateName;

    public ApprovalCompletedEvent(String tenantId, String entityId, String triggeredBy,
                                  String approvalResult, String templateName) {
        super("APPROVAL_COMPLETED", tenantId, entityId, "Approval", triggeredBy);
        this.approvalResult = approvalResult;
        this.templateName = templateName;
    }

    public String getApprovalResult() {
        return approvalResult;
    }

    public String getTemplateName() {
        return templateName;
    }
}
