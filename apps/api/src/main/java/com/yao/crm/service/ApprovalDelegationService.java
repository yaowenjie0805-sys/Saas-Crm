package com.yao.crm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.entity.ApprovalEvent;
import com.yao.crm.entity.ApprovalInstance;
import com.yao.crm.entity.ApprovalTask;
import com.yao.crm.repository.ApprovalEventRepository;
import com.yao.crm.repository.ApprovalInstanceRepository;
import com.yao.crm.repository.ApprovalTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 审批委托服务 - 国内特色功能
 * 支持审批任务委托、加签、转交等操作
 */
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

    /**
     * 委托审批任务
     */
    @Transactional(timeout = 30)
    public DelegationResult delegateTask(String taskId, String fromUserId, String toUserId, String reason) {
        ApprovalTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        // 验证审批人是否有权限
        if (!task.getAssigneeId().equals(fromUserId)) {
            throw new IllegalStateException("Only the assigned user can delegate this task");
        }

        // 检查任务状态
        if (!"PENDING".equals(task.getStatus())) {
            throw new IllegalStateException("Cannot delegate a non-pending task");
        }

        // 创建委托记录
        DelegationRecord delegation = new DelegationRecord();
        delegation.setDelegationId(UUID.randomUUID().toString());
        delegation.setTaskId(taskId);
        delegation.setFromUserId(fromUserId);
        delegation.setToUserId(toUserId);
        delegation.setReason(reason);
        delegation.setDelegatedAt(LocalDateTime.now());
        delegation.setStatus("ACTIVE");

        // 更新任务分配人
        String originalAssignee = task.getAssigneeId();
        task.setAssigneeId(toUserId);
        task.setDelegatedFrom(originalAssignee);
        task.setDelegatedAt(LocalDateTime.now());
        taskRepository.save(task);

        // 记录委托事件
        ApprovalEvent event = new ApprovalEvent();
        event.setId(UUID.randomUUID().toString());
        event.setTaskId(taskId);
        event.setEventType("DELEGATED");
        event.setActorId(fromUserId);
        event.setTargetId(toUserId);
        event.setDescription("委托给 " + toUserId + "，原因：" + reason);
        event.setCreatedAt(LocalDateTime.now());
        eventRepository.save(event);

        // 发送通知给被委托人
        sendDelegationNotification(task, toUserId, fromUserId, reason);

        log.info("Task {} delegated from {} to {}", taskId, fromUserId, toUserId);

        DelegationResult result = new DelegationResult();
        result.setSuccess(true);
        result.setTaskId(taskId);
        result.setDelegationId(delegation.getDelegationId());
        result.setMessage("审批任务已委托给 " + toUserId);

        return result;
    }

    /**
     * 加签 - 审批时指定额外审批人
     */
    @Transactional(timeout = 30)
    public AddSignResult addSign(String taskId, String approverId, String addSignUserId, String reason, String type) {
        ApprovalTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (!"PENDING".equals(task.getStatus())) {
            throw new IllegalStateException("Cannot add sign for a non-pending task");
        }

        // 创建加签任务
        ApprovalTask addSignTask = new ApprovalTask();
        addSignTask.setId(UUID.randomUUID().toString());
        addSignTask.setInstanceId(task.getInstanceId());
        addSignTask.setTemplateId(task.getTemplateId());
        addSignTask.setAssigneeId(addSignUserId);
        addSignTask.setStatus("PENDING");
        addSignTask.setPriority(task.getPriority());
        addSignTask.setSlaDeadline(LocalDateTime.now().plusHours(24)); // 默认24小时
        addSignTask.setCreatedAt(LocalDateTime.now());
        addSignTask.setAddSignType(type); // BEFORE 或 AFTER
        addSignTask.setParentTaskId(taskId);
        addSignTask.setTenantId(task.getTenantId());

        taskRepository.save(addSignTask);

        // 记录加签事件
        ApprovalEvent event = new ApprovalEvent();
        event.setId(UUID.randomUUID().toString());
        event.setTaskId(addSignTask.getId());
        event.setEventType("ADD_SIGN_" + type);
        event.setActorId(approverId);
        event.setTargetId(addSignUserId);
        event.setDescription("加签给 " + addSignUserId + "（" + type + "），原因：" + reason);
        event.setCreatedAt(LocalDateTime.now());
        eventRepository.save(event);

        // 发送加签通知
        sendAddSignNotification(addSignTask, approverId, reason);

        log.info("Add sign task created: {} for user {}", addSignTask.getId(), addSignUserId);

        AddSignResult result = new AddSignResult();
        result.setSuccess(true);
        result.setAddSignTaskId(addSignTask.getId());
        result.setMessage("已成功加签给 " + addSignUserId);

        return result;
    }

    /**
     * 转交审批任务
     * 与委托不同，转交后原审批人不再有权限查看
     */
    @Transactional(timeout = 30)
    public TransferResult transferTask(String taskId, String fromUserId, String toUserId, String reason) {
        ApprovalTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (!task.getAssigneeId().equals(fromUserId)) {
            throw new IllegalStateException("Only the assigned user can transfer this task");
        }

        if (!"PENDING".equals(task.getStatus())) {
            throw new IllegalStateException("Cannot transfer a non-pending task");
        }

        // 记录转交历史
        List<String> transferHistory = task.getTransferHistory() != null ?
                new ArrayList<>(Arrays.asList(task.getTransferHistory().split(","))) : new ArrayList<>();
        transferHistory.add(fromUserId + ":" + LocalDateTime.now().toString());

        // 更新任务
        task.setAssigneeId(toUserId);
        task.setTransferHistory(String.join(",", transferHistory));
        task.setTransferredAt(LocalDateTime.now());
        taskRepository.save(task);

        // 记录转交事件
        ApprovalEvent event = new ApprovalEvent();
        event.setId(UUID.randomUUID().toString());
        event.setTaskId(taskId);
        event.setEventType("TRANSFERRED");
        event.setActorId(fromUserId);
        event.setTargetId(toUserId);
        event.setDescription("转交给 " + toUserId + "，原因：" + reason);
        event.setCreatedAt(LocalDateTime.now());
        eventRepository.save(event);

        // 发送转交通知
        sendTransferNotification(task, toUserId, fromUserId, reason);

        log.info("Task {} transferred from {} to {}", taskId, fromUserId, toUserId);

        TransferResult result = new TransferResult();
        result.setSuccess(true);
        result.setTaskId(taskId);
        result.setMessage("审批任务已转交给 " + toUserId);

        return result;
    }

    /**
     * 获取委托历史
     */
    @Transactional(readOnly = true)
    public List<DelegationRecord> getDelegationHistory(String taskId) {
        List<ApprovalEvent> events = eventRepository.findByTaskIdOrderByCreatedAtDesc(taskId);

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

    /**
     * 获取转交历史
     */
    @Transactional(readOnly = true)
    public List<TransferRecord> getTransferHistory(String taskId) {
        ApprovalTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        List<TransferRecord> records = new ArrayList<>();

        // 从任务中解析转交历史
        if (task.getTransferHistory() != null && !task.getTransferHistory().isEmpty()) {
            String[] entries = task.getTransferHistory().split(",");
            for (String entry : entries) {
                String[] parts = entry.split(":");
                if (parts.length >= 2) {
                    TransferRecord record = new TransferRecord();
                    record.setTransferId(UUID.randomUUID().toString());
                    record.setTaskId(taskId);
                    record.setFromUserId(parts[0]);
                    record.setTransferredAt(parts[1]);
                    records.add(record);
                }
            }
        }

        // 添加当前持有人
        TransferRecord current = new TransferRecord();
        current.setTransferId(UUID.randomUUID().toString());
        current.setTaskId(taskId);
        current.setToUserId(task.getAssigneeId());
        current.setFromUserId(task.getDelegatedFrom());
        current.setTransferredAt(task.getTransferredAt() != null ?
                task.getTransferredAt().toString() : task.getCreatedAt().toString());
        records.add(current);

        return records;
    }

    /**
     * 撤回委托
     */
    @Transactional(timeout = 30)
    public boolean recallDelegation(String delegationId, String userId) {
        List<ApprovalEvent> events = eventRepository.findAll();
        ApprovalEvent delegationEvent = events.stream()
                .filter(e -> e.getId().equals(delegationId) && "DELEGATED".equals(e.getEventType()))
                .findFirst()
                .orElse(null);

        if (delegationEvent == null) {
            throw new IllegalArgumentException("Delegation not found");
        }

        ApprovalTask task = taskRepository.findById(delegationEvent.getTaskId())
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        // 只能撤回自己的委托
        if (!delegationEvent.getActorId().equals(userId)) {
            throw new IllegalStateException("Only the delegator can recall the delegation");
        }

        // 任务状态检查
        if (!"PENDING".equals(task.getStatus())) {
            throw new IllegalStateException("Cannot recall delegation for a non-pending task");
        }

        // 恢复到原审批人
        String originalUser = delegationEvent.getActorId();
        task.setAssigneeId(originalUser);
        task.setDelegatedFrom(null);
        task.setDelegatedAt(null);
        taskRepository.save(task);

        // 记录撤回事件
        ApprovalEvent recallEvent = new ApprovalEvent();
        recallEvent.setId(UUID.randomUUID().toString());
        recallEvent.setTaskId(task.getId());
        recallEvent.setEventType("DELEGATION_RECALLED");
        recallEvent.setActorId(userId);
        recallEvent.setDescription("撤回委托");
        recallEvent.setCreatedAt(LocalDateTime.now());
        eventRepository.save(recallEvent);

        log.info("Delegation {} recalled for task {}", delegationId, task.getId());

        return true;
    }

    /**
     * 获取可委托的用户列表
     */
    @Transactional(readOnly = true)
    public List<Map<String, String>> getDelegatableUsers(String tenantId, String currentUserId) {
        // 在实际实现中，应该从用户服务获取同部门或有审批权限的用户
        // 这里返回模拟数据
        List<Map<String, String>> users = new ArrayList<>();

        Map<String, String> user1 = new HashMap<>();
        user1.put("userId", "manager_" + tenantId);
        user1.put("userName", "部门经理");
        user1.put("department", "管理部门");
        users.add(user1);

        Map<String, String> user2 = new HashMap<>();
        user2.put("userId", "director_" + tenantId);
        user2.put("userName", "总监");
        user2.put("department", "管理层");
        users.add(user2);

        return users;
    }

    // ========== 通知方法 ==========

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
            log.error("Failed to send add sign notification", e);
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

    // ========== 内部类 ==========

    public static class DelegationRecord {
        private String delegationId;
        private String taskId;
        private String fromUserId;
        private String toUserId;
        private String reason;
        private LocalDateTime delegatedAt;
        private String status;

        // Getters and Setters
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

        // Getters and Setters
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
