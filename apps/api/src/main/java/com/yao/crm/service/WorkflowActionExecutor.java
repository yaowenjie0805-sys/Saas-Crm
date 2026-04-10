package com.yao.crm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkflowActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(WorkflowActionExecutor.class);

    public Map<String, Object> execute(String actionType, Map<String, Object> config, Map<String, Object> variables) {
        Map<String, Object> output = new HashMap<>();
        Map<String, Object> safeConfig = config == null ? new HashMap<String, Object>() : config;
        Map<String, Object> safeVariables = variables == null ? new HashMap<String, Object>() : variables;
        String normalizedType = actionType == null ? "" : actionType.trim().toUpperCase(Locale.ROOT);

        switch (normalizedType) {
            case "CREATE_TASK":
                output.put("taskId", UUID.randomUUID().toString());
                output.put("taskName", safeConfig.getOrDefault("taskName", "Auto task"));
                break;
            case "UPDATE_FIELD":
                String fieldName = (String) safeConfig.get("fieldName");
                Object fieldValue = safeConfig.get("fieldValue");
                if (fieldName == null || fieldName.trim().isEmpty()) {
                    break;
                }
                safeVariables.put(fieldName, fieldValue);
                output.put("updatedField", fieldName);
                output.put("newValue", fieldValue);
                break;
            case "SEND_EMAIL":
                output.put("emailId", UUID.randomUUID().toString());
                output.put("recipient", safeConfig.getOrDefault("recipient", ""));
                output.put("subject", safeConfig.getOrDefault("subject", ""));
                break;
            case "CREATE_RECORD":
                String recordType = (String) safeConfig.getOrDefault("recordType", "Lead");
                output.put("recordId", UUID.randomUUID().toString());
                output.put("recordType", recordType);
                break;
            case "WEBHOOK":
                output.put("webhookUrl", safeConfig.getOrDefault("url", ""));
                output.put("status", "INVOKED");
                break;
            default:
                log.warn("Unknown action type: {}", actionType);
        }

        return output;
    }
}
