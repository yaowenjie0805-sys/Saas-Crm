package com.yao.crm.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.entity.AutomationRule;
import com.yao.crm.entity.Lead;
import com.yao.crm.entity.NotificationJob;
import com.yao.crm.entity.TaskItem;
import com.yao.crm.repository.AutomationRuleRepository;
import com.yao.crm.repository.LeadRepository;
import com.yao.crm.repository.NotificationJobRepository;
import com.yao.crm.repository.TaskRepository;
import com.yao.crm.util.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LeadAutomationService {

    private final AutomationRuleRepository automationRuleRepository;
    private final TaskRepository taskRepository;
    private final LeadRepository leadRepository;
    private final NotificationJobRepository notificationJobRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final IdGenerator idGenerator;
    private final ConcurrentHashMap<String, Long> dedupeWindow = new ConcurrentHashMap<String, Long>();

    public LeadAutomationService(AutomationRuleRepository automationRuleRepository,
                                 TaskRepository taskRepository,
                                 LeadRepository leadRepository,
                                 NotificationJobRepository notificationJobRepository,
                                 AuditLogService auditLogService,
                                 ObjectMapper objectMapper,
                                 IdGenerator idGenerator) {
        this.automationRuleRepository = automationRuleRepository;
        this.taskRepository = taskRepository;
        this.leadRepository = leadRepository;
        this.notificationJobRepository = notificationJobRepository;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public void onLeadEvent(String tenantId, String triggerType, Lead lead, String operator) {
        String normalizedTrigger = normalize(triggerType);
        if (normalizedTrigger.isEmpty() || lead == null) return;
        List<AutomationRule> rules = automationRuleRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        long now = System.currentTimeMillis();
        prune(now);
        for (AutomationRule rule : rules) {
            if (!Boolean.TRUE.equals(rule.getEnabled())) continue;
            if (!normalizedTrigger.equalsIgnoreCase(normalize(rule.getTriggerType()))) continue;
            if (!matchTrigger(rule.getTriggerExpr(), lead)) continue;
            String dedupe = tenantId + "|" + lead.getId() + "|" + normalizedTrigger + "|" + rule.getId() + "|" + (now / 60000L);
            if (dedupeWindow.putIfAbsent(dedupe, now) != null) continue;
            executeRule(tenantId, lead, operator, rule);
        }
    }

    private boolean matchTrigger(String triggerExpr, Lead lead) {
        String expr = triggerExpr == null ? "" : triggerExpr.trim();
        if (expr.isEmpty() || "{}".equals(expr)) return true;
        try {
            Map<String, Object> map = objectMapper.readValue(expr, new TypeReference<Map<String, Object>>() {});
            String status = stringOf(map.get("status"));
            String source = stringOf(map.get("source"));
            if (!status.isEmpty() && !status.equalsIgnoreCase(stringOf(lead.getStatus()))) return false;
            if (!source.isEmpty() && !source.equalsIgnoreCase(stringOf(lead.getSource()))) return false;
            return true;
        } catch (Exception ex) {
            return true;
        }
    }

    private void executeRule(String tenantId, Lead lead, String operator, AutomationRule rule) {
        String action = normalize(rule.getActionType()).toUpperCase(Locale.ROOT);
        Map<String, Object> payload = parsePayload(rule.getActionPayload());
        if ("CREATE_TASK".equals(action)) {
            createTask(tenantId, lead, payload, operator);
            return;
        }
        if ("UPDATE_LEAD_STATUS".equals(action)) {
            updateLeadStatus(tenantId, lead, payload, operator);
            return;
        }
        if ("CREATE_NOTIFICATION".equals(action)) {
            createInAppNotification(tenantId, lead, payload, operator);
        }
    }

    private void createTask(String tenantId, Lead lead, Map<String, Object> payload, String operator) {
        TaskItem task = new TaskItem();
        task.setId(idGenerator.generate("t"));
        task.setTenantId(tenantId);
        task.setTitle(stringOr(payload.get("title"), "Follow up lead: " + lead.getName()));
        task.setTime(stringOr(payload.get("time"), "Today 18:00"));
        task.setLevel(stringOr(payload.get("level"), "Medium"));
        task.setDone(false);
        task.setOwner(stringOr(payload.get("owner"), lead.getOwner()));
        taskRepository.save(task);
        auditLogService.record(operator, "SYSTEM", "AUTOMATION_TASK_CREATED", "TASK", task.getId(), "Created from lead automation", tenantId);
    }

    private void updateLeadStatus(String tenantId, Lead lead, Map<String, Object> payload, String operator) {
        String status = stringOf(payload.get("status")).toUpperCase(Locale.ROOT);
        if (status.isEmpty()) return;
        lead.setStatus(status);
        leadRepository.save(lead);
        auditLogService.record(operator, "SYSTEM", "AUTOMATION_LEAD_STATUS", "LEAD", lead.getId(), "Updated status to " + status, tenantId);
    }

    private void createInAppNotification(String tenantId, Lead lead, Map<String, Object> payload, String operator) {
        String message = stringOr(payload.get("message"), "Lead event: " + lead.getName() + " / " + lead.getStatus());
        String dedupeKey = tenantId + "|" + lead.getId() + "|IN_APP|" + Integer.toHexString(message.hashCode());
        if (notificationJobRepository.findByTenantIdAndDedupeKey(tenantId, dedupeKey).isPresent()) return;
        NotificationJob job = new NotificationJob();
        job.setId(idGenerator.generate("noj"));
        job.setTenantId(tenantId);
        job.setEventType("lead_automation_notification");
        job.setTarget("IN_APP");
        job.setStatus("SUCCESS");
        job.setRetryCount(0);
        job.setMaxRetries(1);
        job.setDedupeKey(dedupeKey);
        job.setNextRetryAt(LocalDateTime.now());
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("message", message);
        body.put("leadId", lead.getId());
        body.put("owner", lead.getOwner());
        body.put("status", lead.getStatus());
        body.put("operator", operator);
        try {
            job.setPayload(objectMapper.writeValueAsString(body));
        } catch (Exception ex) {
            job.setPayload("{\"message\":\"" + escape(message) + "\"}");
        }
        notificationJobRepository.save(job);
        auditLogService.record(operator, "SYSTEM", "AUTOMATION_NOTIFICATION_CREATED", "NOTIFICATION_JOB", job.getId(), "Created in-app notification", tenantId);
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\"", "'");
    }

    private Map<String, Object> parsePayload(String actionPayload) {
        try {
            if (actionPayload == null || actionPayload.trim().isEmpty()) return new LinkedHashMap<String, Object>();
            return objectMapper.readValue(actionPayload, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            return new LinkedHashMap<String, Object>();
        }
    }

    private String stringOr(Object value, String fallback) {
        String text = stringOf(value);
        return text.isEmpty() ? fallback : text;
    }

    private String stringOf(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private void prune(long now) {
        dedupeWindow.entrySet().removeIf(entry -> {
            Long value = entry.getValue();
            return value == null || now - value > 5 * 60_000L;
        });
    }

}
