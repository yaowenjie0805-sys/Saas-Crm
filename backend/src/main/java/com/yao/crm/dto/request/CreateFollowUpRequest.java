package com.yao.crm.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class CreateFollowUpRequest {

    @NotBlank(message = "customer_id_summary_required")
    private String customerId;

    private String author;

    @NotBlank(message = "customer_id_summary_required")
    private String summary;

    private String channel;

    private String result;

    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "next_action_date_format")
    private String nextActionDate;

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getNextActionDate() {
        return nextActionDate;
    }

    public void setNextActionDate(String nextActionDate) {
        this.nextActionDate = nextActionDate;
    }
}