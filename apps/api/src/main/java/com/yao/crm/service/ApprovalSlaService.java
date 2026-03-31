package com.yao.crm.service;

import com.yao.crm.entity.ApprovalEvent;
import com.yao.crm.entity.ApprovalInstance;
import com.yao.crm.entity.ApprovalTask;
import com.yao.crm.repository.ApprovalEventRepository;
import com.yao.crm.repository.ApprovalInstanceRepository;
import com.yao.crm.repository.ApprovalTaskRepository;
import com.yao.crm.enums.ApprovalStatus;
import com.yao.crm.util.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ApprovalSlaService {

    private final ApprovalTaskRepository taskRepository;
    private final ApprovalInstanceRepository instanceRepository;
    private final ApprovalEventRepository eventRepository;
    private final AuditLogService auditLogService;
    private final NotificationJobService notificationJobService;
    private final IdGenerator idGenerator;

    public ApprovalSlaService(ApprovalTaskRepository taskRepository,
                              ApprovalInstanceRepository instanceRepository,
                              ApprovalEventRepository eventRepository,
                              AuditLogService auditLogService,
                              NotificationJobService notificationJobService,
                              IdGenerator idGenerator) {
        this.taskRepository = taskRepository;
        this.instanceRepository = instanceRepository;
        this.eventRepository = eventRepository;
        this.auditLogService = auditLogService;
        this.notificationJobService = notificationJobService;
        this.idGenerator = idGenerator;
    }

    @Transactional(timeout = 30)
    public ScanResult scanOverdueAndEscalate() {
        LocalDateTime now = LocalDateTime.now();
        List<ApprovalTask> overdue = taskRepository.findByStatusAndDeadlineAtBefore(ApprovalStatus.PENDING.name(), now);

        // Batch load all existing escalations for overdue tasks to avoid N+1 queries
        List<String> overdueTaskIds = overdue.stream()
                .map(ApprovalTask::getId)
                .collect(Collectors.toList());
        Map<String, List<ApprovalTask>> existingEscalationsMap = new LinkedHashMap<>();
        if (!overdueTaskIds.isEmpty()) {
            List<ApprovalTask> allExistingEsc = taskRepository.findByEscalationSourceTaskIdIn(overdueTaskIds);
            existingEscalationsMap = allExistingEsc.stream()
                    .collect(Collectors.groupingBy(ApprovalTask::getEscalationSourceTaskId, LinkedHashMap::new, Collectors.toList()));
        }

        int handled = 0;
        Map<String, Integer> tierStats = new LinkedHashMap<String, Integer>();
        tierStats.put("P1", 0);
        tierStats.put("P2", 0);
        tierStats.put("P3", 0);
        for (ApprovalTask task : overdue) {
            if (ApprovalStatus.isEscalated(task.getStatus())) {
                continue;
            }
            int overdueMinutes = resolveOverdueMinutes(task, now);
            String tier = resolveTier(overdueMinutes);
            if (task.getNotifiedAt() == null) {
                task.setNotifiedAt(LocalDateTime.now());
                taskRepository.save(task);
                recordEvent(task, "SLA_REMINDER", "system", "Pending task overdue | tier=" + tier + " | overdueMinutes=" + overdueMinutes);
                auditLogService.record("system", "SYSTEM", "SLA_REMINDER", "APPROVAL_TASK", task.getId(), "Pending task overdue | tier=" + tier + " | overdueMinutes=" + overdueMinutes, task.getTenantId());
            }
            List<ApprovalTask> existingEsc = existingEscalationsMap.getOrDefault(task.getId(), List.of());
            if (!existingEsc.isEmpty()) {
                continue;
            }
            task.setStatus(ApprovalStatus.ESCALATED.name());
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
                esc.setId(idGenerator.generate("aptk"));
                esc.setTenantId(task.getTenantId());
                esc.setInstanceId(task.getInstanceId());
                esc.setApproverRole(role);
                esc.setSeq(task.getSeq() + i + 1);
                esc.setNodeKey(task.getNodeKey());
                esc.setSlaMinutes(Math.max(10, task.getSlaMinutes() == null ? 60 : task.getSlaMinutes() / 2));
                esc.setEscalateToRoles(task.getEscalateToRoles());
                esc.setEscalationLevel(task.getEscalationLevel());
                esc.setEscalationSourceTaskId(task.getId());
                esc.setStatus(i == 0 ? ApprovalStatus.PENDING.name() : ApprovalStatus.WAITING.name());
                esc.setDeadlineAt(LocalDateTime.now().plusMinutes(esc.getSlaMinutes()));
                taskRepository.save(esc);
            }

            Optional<ApprovalInstance> instOpt = instanceRepository.findByIdAndTenantId(task.getInstanceId(), task.getTenantId());
            if (instOpt.isPresent()) {
                ApprovalInstance inst = instOpt.get();
                inst.setStatus(ApprovalStatus.PENDING.name());
                instanceRepository.save(inst);
            }
            recordEvent(task, "SLA_ESCALATED", "system", "approval_sla_escalated | tier=" + tier + " | overdueMinutes=" + overdueMinutes);
            auditLogService.record("system", "SYSTEM", "SLA_ESCALATED", "APPROVAL_TASK", task.getId(), "approval_sla_escalated | tier=" + tier + " | overdueMinutes=" + overdueMinutes, task.getTenantId());
            notificationJobService.enqueueSlaEscalated(task.getTenantId(), task.getInstanceId(), task.getId(), task.getApproverRole());
            tierStats.put(tier, tierStats.get(tier) + 1);
            handled++;
        }
        ScanResult result = new ScanResult();
        result.setAffected(handled);
        result.setTierStats(tierStats);
        return result;
    }

    private int resolveOverdueMinutes(ApprovalTask task, LocalDateTime now) {
        if (task == null || task.getDeadlineAt() == null) return 0;
        long mins = Duration.between(task.getDeadlineAt(), now).toMinutes();
        if (mins <= 0) return 0;
        if (mins > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) mins;
    }

    private String resolveTier(int overdueMinutes) {
        if (overdueMinutes >= 24 * 60) return "P3";
        if (overdueMinutes >= 2 * 60) return "P2";
        return "P1";
    }

    public static class ScanResult {
        private int affected;
        private Map<String, Integer> tierStats;

        public int getAffected() {
            return affected;
        }

        public void setAffected(int affected) {
            this.affected = affected;
        }

        public Map<String, Integer> getTierStats() {
            return tierStats;
        }

        public void setTierStats(Map<String, Integer> tierStats) {
            this.tierStats = tierStats;
        }
    }

    private void recordEvent(ApprovalTask task, String eventType, String operator, String detail) {
        ApprovalEvent event = new ApprovalEvent();
        event.setId(idGenerator.generate("apev"));
        event.setTenantId(task.getTenantId());
        event.setInstanceId(task.getInstanceId());
        event.setTaskId(task.getId());
        event.setEventType(eventType);
        event.setOperatorUser(operator);
        event.setDetail(detail);
        event.setRequestId("scheduler");
        eventRepository.save(event);
    }

}
