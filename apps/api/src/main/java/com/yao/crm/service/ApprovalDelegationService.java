package com.yao.crm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.entity.ApprovalEvent;
import com.yao.crm.entity.ApprovalTask;
import com.yao.crm.repository.ApprovalEventRepository;
import com.yao.crm.repository.ApprovalInstanceRepository;
import com.yao.crm.repository.ApprovalTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ApprovalDelegationService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalDelegationService.class);

    private final ApprovalTaskRepository taskRepository;
    private final ApprovalInstanceRepository instanceRepository;
    private final ApprovalEventRepository eventRepository;
    private final ObjectMapper objectMapper;
    private final NotificationDispatchService notificationService;

    public ApprovalDelegationService(
            ApprovalTaskRepository taskRepository,
            ApprovalInstanceRepository instanceRepository,
            ApprovalEventRepository eventRepository,
            ObjectMapper objectMapper,
            NotificationDispatchService notificationService) {
        this.taskRepository = taskRepository;
        this.instanceRepository = instanceRepository;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    @Transactional(timeout = 30)
    public DelegationResult delegateTask(String taskId, String fromUserId, String toUserId, String reason) {
        ApprovalTask task = requireTask(taskId);
        return delegateTaskInternal(task, fromUserId, toUserId, reason);
    }

    @Transactional(timeout = 30)
    public DelegationResult delegateTask(
            String tenantId, String taskId, String fromUserId, String toUserId, String reason) {
        ApprovalTask task = requireTask(tenantId, taskId);
        return delegateTaskInternal(task, fromUserId, toUserId, reason);
    }

    private DelegationResult delegateTaskInternal(
            ApprovalTask task, String fromUserId, String toUserId, String reason) {
        if (!task.getAssigneeId().equals(fromUserId)) {
            throw new IllegalStateException("Only the assigned user can delegate this task");
        }

        if (!"PENDING".equals(task.getStatus())) {
            throw new IllegalStateException("Cannot delegate a non-pending task");
        }

        DelegationRecord delegation = new DelegationRecord();
        delegation.setDelegationId(UUID.randomUUID().toString());
        delegation.setTaskId(task.getId());
        delegation.setFromUserId(fromUserId);
        delegation.setToUserId(toUserId);
        delegation.setReason(reason);
        delegation.setDelegatedAt(LocalDateTime.now());
        delegation.setStatus("ACTIVE");

        String originalAssignee = task.getAssigneeId();
        task.setAssigneeId(toUserId);
        task.setDelegatedFrom(originalAssignee);
        task.setDelegatedAt(LocalDateTime.now());
        taskRepository.save(task);

        ApprovalEvent event = new ApprovalEvent();
        event.setId(UUID.randomUUID().toString());
        event.setTenantId(task.getTenantId());
        event.setInstanceId(task.getInstanceId());
        event.setTaskId(task.getId());
        event.setEventType("DELEGATED");
        event.setActorId(fromUserId);
        event.setTargetId(toUserId);
        event.setDescription("委托给 " + toUserId + "，原因：" + reason);
        event.setCreatedAt(LocalDateTime.now());
        eventRepository.save(event);

        sendDelegationNotification(task, toUserId, fromUserId, reason);

        log.info("Task {} delegated from {} to {}", task.getId(), fromUserId, toUserId);

        DelegationResult result = new DelegationResult();
        result.setSuccess(true);
        result.setTaskId(task.getId());
        result.setDelegationId(delegation.getDelegationId());
        result.setMessage("审批任务已委托给 " + toUserId);
        return result;
    }

    @Transactional(timeout = 30)
    public AddSignResult addSign(String taskId, String approverId, String addSignUserId, String reason, String type) {
        ApprovalTask task = requireTask(taskId);
        return addSignInternal(task, approverId, addSignUserId, reason, type);
    }

    @Transactional(timeout = 30)
    public AddSignResult addSign(
            String tenantId, String taskId, String approverId, String addSignUserId, String reason, String type) {
        ApprovalTask task = requireTask(tenantId, taskId);
        return addSignInternal(task, approverId, addSignUserId, reason, type);
    }

    private AddSignResult addSignInternal(
            ApprovalTask task, String approverId, String addSignUserId, String reason, String type) {
        if (!"PENDING".equals(task.getStatus())) {
            throw new IllegalStateException("Cannot add sign for a non-pending task");
        }

        ApprovalTask addSignTask = new ApprovalTask();
        addSignTask.setId(UUID.randomUUID().toString());
        addSignTask.setInstanceId(task.getInstanceId());
        addSignTask.setTemplateId(task.getTemplateId());
        addSignTask.setAssigneeId(addSignUserId);
        addSignTask.setStatus("PENDING");
        addSignTask.setPriority(task.getPriority());
        addSignTask.setSlaDeadline(LocalDateTime.now().plusHours(24));
        addSignTask.setCreatedAt(LocalDateTime.now());
        addSignTask.setAddSignType(type);
        addSignTask.setParentTaskId(task.getId());
        addSignTask.setTenantId(task.getTenantId());
        taskRepository.save(addSignTask);

        ApprovalEvent event = new ApprovalEvent();
        event.setId(UUID.randomUUID().toString());
        event.setTenantId(addSignTask.getTenantId());
        event.setInstanceId(addSignTask.getInstanceId());
        event.setTaskId(addSignTask.getId());
        event.setEventType("ADD_SIGN_" + type);
        event.setActorId(approverId);
        event.setTargetId(addSignUserId);
        event.setDescription("加签给 " + addSignUserId + "（" + type + "），原因：" + reason);
        event.setCreatedAt(LocalDateTime.now());
        eventRepository.save(event);

        sendAddSignNotification(addSignTask, approverId, reason);

        log.info("Add-sign task {} created for user {}", addSignTask.getId(), addSignUserId);

        AddSignResult result = new AddSignResult();
        result.setSuccess(true);
        result.setAddSignTaskId(addSignTask.getId());
        result.setMessage("已成功加签给 " + addSignUserId);
        return result;
    }

    @Transactional(timeout = 30)
    public TransferResult transferTask(String taskId, String fromUserId, String toUserId, String reason) {
        ApprovalTask task = requireTask(taskId);
        return transferTaskInternal(task, fromUserId, toUserId, reason);
    }

    @Transactional(timeout = 30)
    public TransferResult transferTask(
            String tenantId, String taskId, String fromUserId, String toUserId, String reason) {
        ApprovalTask task = requireTask(tenantId, taskId);
        return transferTaskInternal(task, fromUserId, toUserId, reason);
    }

    private TransferResult transferTaskInternal(
            ApprovalTask task, String fromUserId, String toUserId, String reason) {
        if (!task.getAssigneeId().equals(fromUserId)) {
            throw new IllegalStateException("Only the assigned user can transfer this task");
        }

        if (!"PENDING".equals(task.getStatus())) {
            throw new IllegalStateException("Cannot transfer a non-pending task");
        }

        List<String> transferHistory = task.getTransferHistory() != null
                ? new ArrayList<>(Arrays.asList(task.getTransferHistory().split(",")))
                : new ArrayList<>();
        transferHistory.add(fromUserId + ":" + LocalDateTime.now());

        task.setAssigneeId(toUserId);
        task.setTransferHistory(String.join(",", transferHistory));
        task.setTransferredAt(LocalDateTime.now());
        taskRepository.save(task);

        ApprovalEvent event = new ApprovalEvent();
        event.setId(UUID.randomUUID().toString());
        event.setTenantId(task.getTenantId());
        event.setInstanceId(task.getInstanceId());
        event.setTaskId(task.getId());
        event.setEventType("TRANSFERRED");
        event.setActorId(fromUserId);
        event.setTargetId(toUserId);
        event.setDescription("转交给 " + toUserId + "，原因：" + reason);
        event.setCreatedAt(LocalDateTime.now());
        eventRepository.save(event);

        sendTransferNotification(task, toUserId, fromUserId, reason);

        log.info("Task {} transferred from {} to {}", task.getId(), fromUserId, toUserId);

        TransferResult result = new TransferResult();
        result.setSuccess(true);
        result.setTaskId(task.getId());
        result.setMessage("审批任务已转交给 " + toUserId);
        return result;
    }

    @Transactional(readOnly = true)
    public List<DelegationRecord> getDelegationHistory(String taskId) {
        return mapDelegationHistory(eventRepository.findByTaskIdOrderByCreatedAtDesc(taskId));
    }

    @Transactional(readOnly = true)
    public List<DelegationRecord> getDelegationHistory(String tenantId, String taskId) {
        return mapDelegationHistory(eventRepository.findByTaskIdAndTenantIdOrderByCreatedAtDesc(taskId, tenantId));
    }

    private List<DelegationRecord> mapDelegationHistory(List<ApprovalEvent> events) {
        return events.stream()
                .filter(e -> "DELEGATED".equals(e.getEventType()))
                .map(e -> {
                    DelegationRecord record = new DelegationRecord();
                    record.setDelegationId(e.getId());
                    record.setTaskId(e.getTaskId());
                    record.setFromUserId(e.getActorId());
                    record.setToUserId(e.getTargetId());
                    record.setReason(e.getDescription());
                    record.setDelegatedAt(e.getCreatedAt());
                    record.setStatus("COMPLETED");
                    return record;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransferRecord> getTransferHistory(String taskId) {
        return mapTransferHistory(requireTask(taskId));
    }

    @Transactional(readOnly = true)
    public List<TransferRecord> getTransferHistory(String tenantId, String taskId) {
        return mapTransferHistory(requireTask(tenantId, taskId));
    }

    private List<TransferRecord> mapTransferHistory(ApprovalTask task) {
        List<TransferRecord> records = new ArrayList<>();

        if (task.getTransferHistory() != null && !task.getTransferHistory().isEmpty()) {
            String[] entries = task.getTransferHistory().split(",");
            for (String entry : entries) {
                String[] parts = entry.split(":");
                if (parts.length >= 2) {
                    TransferRecord record = new TransferRecord();
                    record.setTransferId(UUID.randomUUID().toString());
                    record.setTaskId(task.getId());
                    record.setFromUserId(parts[0]);
                    record.setTransferredAt(parts[1]);
                    records.add(record);
                }
            }
        }

        TransferRecord current = new TransferRecord();
        current.setTransferId(UUID.randomUUID().toString());
        current.setTaskId(task.getId());
        current.setToUserId(task.getAssigneeId());
        current.setFromUserId(task.getDelegatedFrom());
        current.setTransferredAt(task.getTransferredAt() != null
                ? task.getTransferredAt().toString()
                : task.getCreatedAt().toString());
        records.add(current);

        return records;
    }

    @Transactional(timeout = 30)
    public boolean recallDelegation(String delegationId, String userId) {
        ApprovalEvent delegationEvent = eventRepository
                .findByIdAndEventType(delegationId, "DELEGATED")
                .orElse(null);
        return recallDelegationInternal(delegationEvent, userId);
    }

    @Transactional(timeout = 30)
    public boolean recallDelegation(String tenantId, String delegationId, String userId) {
        ApprovalEvent delegationEvent = eventRepository
                .findByIdAndTenantIdAndEventType(delegationId, tenantId, "DELEGATED")
                .orElse(null);
        return recallDelegationInternal(delegationEvent, userId);
    }

    private boolean recallDelegationInternal(ApprovalEvent delegationEvent, String userId) {
        if (delegationEvent == null) {
            throw new IllegalArgumentException("Delegation not found");
        }

        ApprovalTask task;
        if (isBlank(delegationEvent.getTenantId())) {
            task = requireTask(delegationEvent.getTaskId());
        } else {
            task = requireTask(delegationEvent.getTenantId(), delegationEvent.getTaskId());
        }

        if (!delegationEvent.getActorId().equals(userId)) {
            throw new IllegalStateException("Only the delegator can recall the delegation");
        }

        if (!"PENDING".equals(task.getStatus())) {
            throw new IllegalStateException("Cannot recall delegation for a non-pending task");
        }

        task.setAssigneeId(delegationEvent.getActorId());
        task.setDelegatedFrom(null);
        task.setDelegatedAt(null);
        taskRepository.save(task);

        ApprovalEvent recallEvent = new ApprovalEvent();
        recallEvent.setId(UUID.randomUUID().toString());
        recallEvent.setTenantId(task.getTenantId());
        recallEvent.setInstanceId(task.getInstanceId());
        recallEvent.setTaskId(task.getId());
        recallEvent.setEventType("DELEGATION_RECALLED");
        recallEvent.setActorId(userId);
        recallEvent.setDescription("撤回委托");
        recallEvent.setCreatedAt(LocalDateTime.now());
        eventRepository.save(recallEvent);

        log.info("Delegation {} recalled for task {}", delegationEvent.getId(), task.getId());
        return true;
    }

    @Transactional(readOnly = true)
    public List<Map<String, String>> getDelegatableUsers(String tenantId, String currentUserId) {
        List<Map<String, String>> users = new ArrayList<>();

        Map<String, String> user1 = new HashMap<>();
        user1.put("userId", "manager_" + tenantId);
        user1.put("userName", "Department Manager");
        user1.put("department", "Management");
        users.add(user1);

        Map<String, String> user2 = new HashMap<>();
        user2.put("userId", "director_" + tenantId);
        user2.put("userName", "Director");
        user2.put("department", "Management");
        users.add(user2);

        return users;
    }

    private void sendDelegationNotification(ApprovalTask task, String toUserId, String fromUserId, String reason) {
        try {
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("taskId", task.getId());
            notificationData.put("type", "DELEGATION");
            notificationData.put("fromUserId", fromUserId);
            notificationData.put("reason", reason);
            notificationData.put("title", "审批委托提醒");
            notificationData.put("content", "您收到了一项委托审批任务，请及时处理。");

            notificationService.sendNotification(toUserId, "IN_APP", notificationData);
            notificationService.sendNotification(toUserId, "WECHAT_WORK", notificationData);
        } catch (Exception e) {
            log.error("Failed to send delegation notification", e);
        }
    }

    private void sendAddSignNotification(ApprovalTask task, String approverId, String reason) {
        try {
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("taskId", task.getId());
            notificationData.put("type", "ADD_SIGN");
            notificationData.put("fromUserId", approverId);
            notificationData.put("reason", reason);
            notificationData.put("title", "加签审批提醒");
            notificationData.put("content", "您收到了一项加签审批任务，请及时处理。");

            notificationService.sendNotification(task.getAssigneeId(), "IN_APP", notificationData);
            notificationService.sendNotification(task.getAssigneeId(), "WECHAT_WORK", notificationData);
        } catch (Exception e) {
            log.error("Failed to send add-sign notification", e);
        }
    }

    private void sendTransferNotification(ApprovalTask task, String toUserId, String fromUserId, String reason) {
        try {
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("taskId", task.getId());
            notificationData.put("type", "TRANSFER");
            notificationData.put("fromUserId", fromUserId);
            notificationData.put("reason", reason);
            notificationData.put("title", "审批转交通知");
            notificationData.put("content", "您收到了一项转交审批任务，请及时处理。");

            notificationService.sendNotification(toUserId, "IN_APP", notificationData);
            notificationService.sendNotification(toUserId, "WECHAT_WORK", notificationData);
            notificationService.sendNotification(toUserId, "DINGTALK", notificationData);
        } catch (Exception e) {
            log.error("Failed to send transfer notification", e);
        }
    }

    private ApprovalTask requireTask(String taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
    }

    private ApprovalTask requireTask(String tenantId, String taskId) {
        return taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class DelegationRecord {
        private String delegationId;
        private String taskId;
        private String fromUserId;
        private String toUserId;
        private String reason;
        private LocalDateTime delegatedAt;
        private String status;

        public String getDelegationId() { return delegationId; }
        public void setDelegationId(String delegationId) { this.delegationId = delegationId; }
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getFromUserId() { return fromUserId; }
        public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }
        public String getToUserId() { return toUserId; }
        public void setToUserId(String toUserId) { this.toUserId = toUserId; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public LocalDateTime getDelegatedAt() { return delegatedAt; }
        public void setDelegatedAt(LocalDateTime delegatedAt) { this.delegatedAt = delegatedAt; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class TransferRecord {
        private String transferId;
        private String taskId;
        private String fromUserId;
        private String toUserId;
        private String transferredAt;

        public String getTransferId() { return transferId; }
        public void setTransferId(String transferId) { this.transferId = transferId; }
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getFromUserId() { return fromUserId; }
        public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }
        public String getToUserId() { return toUserId; }
        public void setToUserId(String toUserId) { this.toUserId = toUserId; }
        public String getTransferredAt() { return transferredAt; }
        public void setTransferredAt(String transferredAt) { this.transferredAt = transferredAt; }
    }

    public static class DelegationResult {
        private boolean success;
        private String taskId;
        private String delegationId;
        private String message;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getDelegationId() { return delegationId; }
        public void setDelegationId(String delegationId) { this.delegationId = delegationId; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class AddSignResult {
        private boolean success;
        private String addSignTaskId;
        private String message;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getAddSignTaskId() { return addSignTaskId; }
        public void setAddSignTaskId(String addSignTaskId) { this.addSignTaskId = addSignTaskId; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class TransferResult {
        private boolean success;
        private String taskId;
        private String message;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
