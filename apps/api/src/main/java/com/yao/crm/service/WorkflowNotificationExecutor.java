package com.yao.crm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkflowNotificationExecutor {

    private static final Logger log = LoggerFactory.getLogger(WorkflowNotificationExecutor.class);

    public Map<String, Object> execute(String notificationType, Map<String, Object> config) {
        Map<String, Object> output = new HashMap<>();
        Map<String, Object> safeConfig = config == null ? new HashMap<String, Object>() : config;
        String normalizedType = notificationType == null ? "" : notificationType.trim().toUpperCase(Locale.ROOT);

        switch (normalizedType) {
            case "EMAIL":
                output.put("channel", "EMAIL");
                output.put("recipient", safeConfig.getOrDefault("to", ""));
                output.put("subject", safeConfig.getOrDefault("subject", ""));
                output.put("messageId", UUID.randomUUID().toString());
                break;
            case "WECHAT_WORK":
                output.put("channel", "WECHAT_WORK");
                output.put("agentId", safeConfig.getOrDefault("agentId", ""));
                output.put("userIds", safeConfig.getOrDefault("userIds", ""));
                output.put("messageId", UUID.randomUUID().toString());
                break;
            case "DINGTALK":
                output.put("channel", "DINGTALK");
                output.put("webhook", safeConfig.getOrDefault("webhook", ""));
                output.put("messageId", UUID.randomUUID().toString());
                break;
            case "IN_APP":
                output.put("channel", "IN_APP");
                output.put("title", safeConfig.getOrDefault("title", ""));
                output.put("recipientUserId", safeConfig.getOrDefault("recipientUserId", ""));
                output.put("notificationId", UUID.randomUUID().toString());
                break;
            case "SMS":
                output.put("channel", "SMS");
                output.put("phoneNumber", safeConfig.getOrDefault("phoneNumber", ""));
                output.put("messageId", UUID.randomUUID().toString());
                break;
            default:
                log.warn("Unknown notification type: {}", notificationType);
        }

        return output;
    }
}
