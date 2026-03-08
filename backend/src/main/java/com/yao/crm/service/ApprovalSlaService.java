package com.yao.crm.service;

import com.yao.crm.entity.ApprovalEvent;
import com.yao.crm.entity.ApprovalInstance;
import com.yao.crm.entity.ApprovalTask;
import com.yao.crm.repository.ApprovalEventRepository;
import com.yao.crm.repository.ApprovalInstanceRepository;
import com.yao.crm.repository.ApprovalTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ApprovalSlaService {

    private final ApprovalTaskRepository taskRepository;
    private final ApprovalInstanceRepository instanceRepository;
    private final ApprovalEventRepository eventRepository;
    private final AuditLogService auditLogService;
    private final NotificationJobService notificationJobService;

    public ApprovalSlaService(ApprovalTaskRepository taskRepository,
                              ApprovalInstanceRepository instanceRepository,
                              ApprovalEventRepository eventRepository,
                              AuditLogService auditLogService,
                              NotificationJobService notificationJobService) {
        this.taskRepository = taskRepository;
        this.instanceRepository = instanceRepository;
        this.eventRepository = eventRepository;
        this.auditLogService = auditLogService;
        this.notificationJobService = notificationJobService;
    }

    @Transactional
    public int scanOverdueAndEscalate() {
        List<ApprovalTask> overdue = taskRepository.findByStatusAndDeadlineAtBefore("PENDING", LocalDateTime.now());
        int handled = 0;
        for (ApprovalTask task : overdue) {
            if ("ESCALATED".equalsIgnoreCase(task.getStatus())) {
                continue;
            }
            if (task.getNotifiedAt() == null) {
                task.setNotifiedAt(LocalDateTime.now());
                taskRepository.save(task);
                recordEvent(task, "SLA_REMINDER", "system", "Pending task overdue");
                auditLogService.record("system", "SYSTEM", "SLA_REMINDER", "APPROVAL_TASK", task.getId(), "Pending task overdue", task.getTenantId());
            }
            List<ApprovalTask> existingEsc = taskRepository.findByTenantIdAndEscalationSourceTaskIdOrderByCreatedAtDesc(task.getTenantId(), task.getId());
            if (!existingEsc.isEmpty()) {
                continue;
            }
            task.setStatus("ESCALATED");
            task.setEscalationLevel((task.getEscalationLevel() == null ? 0 : task.getEscalationLevel()) + 1);
            taskRepository.save(task);

            String targets = task.getEscalateToRoles();
            if (targets == null || targets.trim().isEmpty()) {
                targets = "MANAGER,ADMIN";
            }
            String[] roles = targets.split(",");
            for (int i = 0; i < roles.length; i++) {
                String role = roles[i] == null ? "" : roles[i].trim().toUpperCase();
                if (role.isEmpty()) continue;
                ApprovalTask esc = new ApprovalTask();
                esc.setId(newId("aptk"));
                esc.setTenantId(task.getTenantId());
                esc.setInstanceId(task.getInstanceId());
                esc.setApproverRole(role);
                esc.setSeq(task.getSeq() + i + 1);
                esc.setNodeKey(task.getNodeKey());
                esc.setSlaMinutes(Math.max(10, task.getSlaMinutes() == null ? 60 : task.getSlaMinutes() / 2));
                esc.setEscalateToRoles(task.getEscalateToRoles());
                esc.setEscalationLevel(task.getEscalationLevel());
                esc.setEscalationSourceTaskId(task.getId());
                esc.setStatus(i == 0 ? "PENDING" : "WAITING");
                esc.setDeadlineAt(LocalDateTime.now().plusMinutes(esc.getSlaMinutes()));
                taskRepository.save(esc);
            }

            Optional<ApprovalInstance> instOpt = instanceRepository.findByIdAndTenantId(task.getInstanceId(), task.getTenantId());
            if (instOpt.isPresent()) {
                ApprovalInstance inst = instOpt.get();
                inst.setStatus("PENDING");
                instanceRepository.save(inst);
            }
            recordEvent(task, "SLA_ESCALATED", "system", "approval_sla_escalated");
            auditLogService.record("system", "SYSTEM", "SLA_ESCALATED", "APPROVAL_TASK", task.getId(), "approval_sla_escalated", task.getTenantId());
            notificationJobService.enqueueSlaEscalated(task.getTenantId(), task.getInstanceId(), task.getId(), task.getApproverRole());
            handled++;
        }
        return handled;
    }

    private void recordEvent(ApprovalTask task, String eventType, String operator, String detail) {
        ApprovalEvent event = new ApprovalEvent();
        event.setId(newId("apev"));
        event.setTenantId(task.getTenantId());
        event.setInstanceId(task.getInstanceId());
        event.setTaskId(task.getId());
        event.setEventType(eventType);
        event.setOperatorUser(operator);
        event.setDetail(detail);
        event.setRequestId("scheduler");
        eventRepository.save(event);
    }

    private String newId(String prefix) {
        return prefix + "_" + Long.toString(System.currentTimeMillis(), 36) + String.format("%03d", (int) (Math.random() * 1000));
    }
}
