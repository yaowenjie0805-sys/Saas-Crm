package com.yao.crm.dto.request;

public class V1ApprovalTaskActionRequest {
    private String comment;
    private String transferTo;
    private String urgeChannel;

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getTransferTo() { return transferTo; }
    public void setTransferTo(String transferTo) { this.transferTo = transferTo; }
    public String getUrgeChannel() { return urgeChannel; }
    public void setUrgeChannel(String urgeChannel) { this.urgeChannel = urgeChannel; }
}
