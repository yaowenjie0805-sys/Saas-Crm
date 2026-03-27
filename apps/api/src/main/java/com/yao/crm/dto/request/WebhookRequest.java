package com.yao.crm.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Webhook 请求DTO
 * 用于接收第三方集成平台的回调消息
 */
public class WebhookRequest {

    @Size(max = 500, message = "标题长度不能超过500")
    private String title;

    @Size(max = 500, message = "主题长度不能超过500")
    private String subject;

    @Size(max = 5000, message = "内容长度不能超过5000")
    private String text;

    @Size(max = 5000, message = "内容长度不能超过5000")
    private String content;

    @Size(max = 5000, message = "消息长度不能超过5000")
    private String message;

    @Size(max = 5000, message = "消息体长度不能超过5000")
    private String body;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
