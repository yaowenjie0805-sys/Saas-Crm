package com.yao.crm.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.dto.request.V1ApprovalSubmitRequest;
import com.yao.crm.dto.request.V1ApprovalTaskActionRequest;
import com.yao.crm.dto.request.V1ApprovalTemplateRequest;
import com.yao.crm.dto.request.V1ApprovalTemplatePatchRequest;
import com.yao.crm.entity.*;
import com.yao.crm.repository.*;
import com.yao.crm.service.ApprovalSlaService;
import com.yao.crm.service.ApprovalTemplateVersionService;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/approval")
public class V1ApprovalController extends BaseApiController {

    private final ApprovalTemplateRepository templateRepository;
    private final ApprovalInstanceRepository instanceRepository;
    private final ApprovalTaskRepository taskRepository;
    private final ApprovalEventRepository eventRepository;
    private final ContractRecordRepository contractRepository;
    private final PaymentRecordRepository paymentRepository;
    private final AuditLogService auditLogService;
    private final ApprovalSlaService approvalSlaService;
    private final ApprovalTemplateVersionService templateVersionService;
    private final ObjectMapper objectMapper;

    public V1ApprovalController(ApprovalTemplateRepository templateRepository,
                                ApprovalInstanceRepository instanceRepository,
                                ApprovalTaskRepository taskRepository,
                                ApprovalEventRepository eventRepository,
                                ContractRecordRepository contractRepository,
                                PaymentRecordRepository paymentRepository,
                                AuditLogService auditLogService,
                                ApprovalSlaService approvalSlaService,
                                ApprovalTemplateVersionService templateVersionService,
                                ObjectMapper objectMapper,
                                I18nService i18nService) {
        super(i18nService);
        this.templateRepository = templateRepository;
        this.instanceRepository = instanceRepository;
        this.taskRepository = taskRepository;
        this.eventRepository = eventRepository;
        this.contractRepository = contractRepository;
        this.paymentRepository = paymentRepository;
        this.auditLogService = auditLogService;
        this.approvalSlaService = approvalSlaService;
        this.templateVersionService = templateVersionService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/templates")
    public ResponseEntity<?> createTemplate(HttpServletRequest request, @Valid @RequestBody V1ApprovalTemplateRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        try {
            String tenantId = currentTenant(request);
            ApprovalTemplate t = new ApprovalTemplate();
            t.setId(newId("apt"));
            t.setTenantId(tenantId);
            t.setBizType(payload.getBizType().trim().toUpperCase(Locale.ROOT));
            t.setName(payload.getName().trim());
            t.setAmountMin(payload.getAmountMin());
            t.setAmountMax(payload.getAmountMax());
            t.setRole(isBlank(payload.getRole()) ? null : payload.getRole().trim().toUpperCase(Locale.ROOT));
            t.setDepartment(isBlank(payload.getDepartment()) ? null : payload.getDepartment().trim());
            CompiledFlow compiled = compileFlow(payload.getFlowDefinition(), payload.getApproverRoles());
            t.setApproverRoles(compiled.flatRoles);
            t.setFlowDefinition(compiled.json);
            t.setVersion(1);
            t.setEnabled(true);
            t.setStatus("PUBLISHED");
            t = templateRepository.save(t);
            auditLogService.record(currentUser(request), currentRole(request), "CREATE", "APPROVAL_TEMPLATE", t.getId(), "Created approval template", tenantId);
            return ResponseEntity.status(201).body(toTemplateView(t));
        } catch (IllegalArgumentException ex) {
            String code = normalizeCode(ex.getMessage(), "bad_request");
            return ResponseEntity.badRequest().body(errorBody(request, code, msg(request, code), null));
        }
    }

    @GetMapping("/templates")
    public ResponseEntity<?> listTemplates(HttpServletRequest request,
                                           @RequestParam(defaultValue = "") String bizType,
                                           @RequestParam(defaultValue = "50") int limit) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        int finalLimit = Math.max(1, Math.min(limit, 200));
        List<ApprovalTemplate> rows;
        if (isBlank(bizType)) {
            rows = templateRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        } else {
            rows = templateRepository.findByTenantIdAndBizTypeAndEnabledTrueOrderByCreatedAtAsc(tenantId, bizType.trim().toUpperCase(Locale.ROOT));
        }
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (ApprovalTemplate row : rows) {
            items.add(toTemplateView(row));
            if (items.size() >= finalLimit) break;
        }
        return ResponseEntity.ok(Collections.singletonMap("items", items));
    }

    @PatchMapping("/templates/{id}")
    public ResponseEntity<?> patchTemplate(HttpServletRequest request,
                                           @PathVariable String id,
                                           @RequestBody V1ApprovalTemplatePatchRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Optional<ApprovalTemplate> optional = templateRepository.findByIdAndTenantId(id, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "approval_template_not_found", msg(request, "approval_template_not_found"), null));
        }
        ApprovalTemplate row = optional.get();
        try {
            if (payload.getName() != null && !isBlank(payload.getName())) row.setName(payload.getName().trim());
            if (payload.getAmountMin() != null) row.setAmountMin(payload.getAmountMin());
            if (payload.getAmountMax() != null) row.setAmountMax(payload.getAmountMax());
            if (payload.getRole() != null) row.setRole(isBlank(payload.getRole()) ? null : payload.getRole().trim().toUpperCase(Locale.ROOT));
            if (payload.getDepartment() != null) row.setDepartment(isBlank(payload.getDepartment()) ? null : payload.getDepartment().trim());
            if (payload.getEnabled() != null) row.setEnabled(payload.getEnabled());
            if (payload.getStatus() != null && !isBlank(payload.getStatus())) row.setStatus(payload.getStatus().trim().toUpperCase(Locale.ROOT));
            if (payload.getFlowDefinition() != null || (payload.getApproverRoles() != null && !isBlank(payload.getApproverRoles()))) {
                CompiledFlow compiled = compileFlow(payload.getFlowDefinition(), payload.getApproverRoles() == null ? row.getApproverRoles() : payload.getApproverRoles());
                row.setApproverRoles(compiled.flatRoles);
                row.setFlowDefinition(compiled.json);
                row.setVersion((row.getVersion() == null ? 0 : row.getVersion()) + 1);
            }
            row = templateRepository.save(row);
            auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "APPROVAL_TEMPLATE", row.getId(), "Updated approval template", tenantId);
            return ResponseEntity.ok(toTemplateView(row));
        } catch (IllegalArgumentException ex) {
            String code = normalizeCode(ex.getMessage(), "bad_request");
            return ResponseEntity.badRequest().body(errorBody(request, code, msg(request, code), null));
        }
    }

    @PostMapping("/templates/{id}/publish")
    public ResponseEntity<?> publishTemplate(HttpServletRequest request, @PathVariable String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Optional<ApprovalTemplate> optional = templateRepository.findByIdAndTenantId(id, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "approval_template_not_found", msg(request, "approval_template_not_found"), null));
        }
        ApprovalTemplate updated = templateVersionService.publish(tenantId, currentUser(request), currentRole(request), optional.get());
        return ResponseEntity.ok(toTemplateView(updated));
    }

    @GetMapping("/templates/{id}/versions")
    public ResponseEntity<?> templateVersions(HttpServletRequest request, @PathVariable String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        return ResponseEntity.ok(Collections.singletonMap("items", templateVersionService.listVersions(tenantId, id)));
    }

    @PostMapping("/templates/{id}/rollback/{version}")
    public ResponseEntity<?> rollbackTemplate(HttpServletRequest request, @PathVariable String id, @PathVariable Integer version) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Optional<ApprovalTemplate> optional = templateRepository.findByIdAndTenantId(id, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "approval_template_not_found", msg(request, "approval_template_not_found"), null));
        }
        try {
            ApprovalTemplate updated = templateVersionService.rollback(tenantId, currentUser(request), currentRole(request), optional.get(), version == null ? 0 : version);
            return ResponseEntity.ok(toTemplateView(updated));
        } catch (IllegalArgumentException ex) {
            String code = normalizeCode(ex.getMessage(), "bad_request");
            return ResponseEntity.badRequest().body(errorBody(request, code, msg(request, code), null));
        }
    }

    @PostMapping("/instances/{bizType}/{bizId}/submit")
    public ResponseEntity<?> submitInstance(HttpServletRequest request,
                                            @PathVariable String bizType,
                                            @PathVariable String bizId,
                                            @Valid @RequestBody V1ApprovalSubmitRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }

        String tenantId = currentTenant(request);
        String bizTypeUpper = bizType.trim().toUpperCase(Locale.ROOT);
        Long amount = payload.getAmount() == null ? resolveAmount(tenantId, bizTypeUpper, bizId) : payload.getAmount();
        String role = isBlank(payload.getRole()) ? currentRole(request) : payload.getRole().trim().toUpperCase(Locale.ROOT);
        String department = isBlank(payload.getDepartment()) ? "DEFAULT" : payload.getDepartment().trim();

        ApprovalTemplate selected = selectTemplate(tenantId, bizTypeUpper, amount, role, department);
        if (selected == null) {
            return ResponseEntity.badRequest().body(errorBody(request, "approval_template_not_found", msg(request, "approval_template_not_found"), null));
        }
        List<NodeDef> effectiveNodes = filterEffectiveNodes(selected, amount, role, department);
        if (effectiveNodes.isEmpty()) {
            return ResponseEntity.badRequest().body(errorBody(request, "approval_flow_no_path", msg(request, "approval_flow_no_path"), null));
        }

        ApprovalInstance instance = new ApprovalInstance();
        instance.setId("api_" + Long.toString(System.currentTimeMillis(), 36));
        instance.setTenantId(tenantId);
        instance.setTemplateId(selected.getId());
        instance.setBizType(bizTypeUpper);
        instance.setBizId(bizId);
        instance.setSubmitter(currentUser(request));
        instance.setComment(payload.getComment());
        instance.setStatus("PENDING");
        instance.setCurrentSeq(1);
        instance.setTemplateVersion(selected.getVersion());
        instance = instanceRepository.save(instance);

        for (int i = 0; i < effectiveNodes.size(); i++) {
            NodeDef node = effectiveNodes.get(i);
            ApprovalTask task = new ApprovalTask();
            task.setId(newId("aptk"));
            task.setTenantId(tenantId);
            task.setInstanceId(instance.getId());
            task.setApproverRole(node.approverRoles.isEmpty() ? "MANAGER" : node.approverRoles.get(0));
            task.setSeq(i + 1);
            task.setNodeKey(node.id);
            task.setSlaMinutes(node.slaMinutes);
            task.setEscalateToRoles(String.join(",", node.escalateToRoles));
            task.setStatus(i == 0 ? "PENDING" : "WAITING");
            task.setDeadlineAt(LocalDateTime.now().plusMinutes(node.slaMinutes));
            taskRepository.save(task);
            recordEvent(tenantId, instance.getId(), task.getId(), "TASK_CREATED", currentUser(request), "node=" + node.id, traceId(request));
        }

        recordEvent(tenantId, instance.getId(), null, "SUBMITTED", currentUser(request), "submitted", traceId(request));
        auditLogService.record(currentUser(request), currentRole(request), "SUBMIT", "APPROVAL_INSTANCE", instance.getId(), "Submitted approval", tenantId);
        return ResponseEntity.status(201).body(instance);
    }

    @PostMapping("/tasks/{taskId}/approve")
    public ResponseEntity<?> approveTask(HttpServletRequest request, @PathVariable String taskId, @RequestBody(required = false) V1ApprovalTaskActionRequest payload) {
        return handleTaskAction(request, taskId, "APPROVED", payload);
    }

    @PostMapping("/tasks/{taskId}/reject")
    public ResponseEntity<?> rejectTask(HttpServletRequest request, @PathVariable String taskId, @RequestBody(required = false) V1ApprovalTaskActionRequest payload) {
        return handleTaskAction(request, taskId, "REJECTED", payload);
    }

    @PostMapping("/tasks/{taskId}/transfer")
    public ResponseEntity<?> transferTask(HttpServletRequest request, @PathVariable String taskId, @RequestBody(required = false) V1ApprovalTaskActionRequest payload) {
        String tenantId = currentTenant(request);
        Optional<ApprovalTask> optional = taskRepository.findByIdAndTenantId(taskId, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "approval_task_not_found", msg(request, "approval_task_not_found"), null));
        }
        ApprovalTask task = optional.get();
        String transferTo = payload == null ? "" : payload.getTransferTo();
        if (isBlank(transferTo)) {
            return ResponseEntity.badRequest().body(errorBody(request, "approval_transfer_required", msg(request, "approval_transfer_required"), null));
        }
        task.setApproverUser(currentUser(request));
        task.setStatus("TRANSFERRED");
        if (payload != null) task.setComment(payload.getComment());
        taskRepository.save(task);

        ApprovalTask next = new ApprovalTask();
        next.setId(newId("aptk"));
        next.setTenantId(task.getTenantId());
        next.setInstanceId(task.getInstanceId());
        next.setApproverRole(task.getApproverRole());
        next.setApproverUser(transferTo.trim());
        next.setSeq(task.getSeq());
        next.setNodeKey(task.getNodeKey());
        next.setSlaMinutes(task.getSlaMinutes());
        next.setEscalateToRoles(task.getEscalateToRoles());
        next.setEscalationLevel(task.getEscalationLevel());
        next.setStatus("PENDING");
        next.setDeadlineAt(LocalDateTime.now().plusMinutes(task.getSlaMinutes() == null ? 60 : Math.max(1, task.getSlaMinutes())));
        taskRepository.save(next);
        recordEvent(tenantId, task.getInstanceId(), task.getId(), "TRANSFERRED", currentUser(request), "transferTo=" + transferTo, traceId(request));
        recordEvent(tenantId, task.getInstanceId(), next.getId(), "PENDING", transferTo.trim(), "transfer accepted", traceId(request));
        auditLogService.record(currentUser(request), currentRole(request), "TRANSFER", "APPROVAL_TASK", task.getId(), "Transferred approval task", tenantId);
        return ResponseEntity.ok(toTaskView(next));
    }

    @PostMapping("/tasks/{taskId}/urge")
    public ResponseEntity<?> urgeTask(HttpServletRequest request, @PathVariable String taskId, @RequestBody(required = false) V1ApprovalTaskActionRequest payload) {
        String tenantId = currentTenant(request);
        Optional<ApprovalTask> optional = taskRepository.findByIdAndTenantId(taskId, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "approval_task_not_found", msg(request, "approval_task_not_found"), null));
        }
        ApprovalTask task = optional.get();
        if (!("PENDING".equals(task.getStatus()) || "WAITING".equals(task.getStatus()))) {
            return ResponseEntity.badRequest().body(errorBody(request, "approval_task_closed", msg(request, "approval_task_closed"), null));
        }
        String channel = payload == null || isBlank(payload.getUrgeChannel()) ? "IN_APP" : payload.getUrgeChannel().trim().toUpperCase(Locale.ROOT);
        task.setNotifiedAt(LocalDateTime.now());
        taskRepository.save(task);
        recordEvent(tenantId, task.getInstanceId(), task.getId(), "URGED", currentUser(request), "channel=" + channel, traceId(request));
        auditLogService.record(currentUser(request), currentRole(request), "URGE", "APPROVAL_TASK", task.getId(), "Urged task " + channel, tenantId);
        return ResponseEntity.ok(toTaskView(task));
    }

    @GetMapping("/tasks")
    public ResponseEntity<?> listTasks(HttpServletRequest request,
                                       @RequestParam(defaultValue = "") String status,
                                       @RequestParam(defaultValue = "false") boolean overdue,
                                       @RequestParam(defaultValue = "false") boolean escalated,
                                       @RequestParam(defaultValue = "20") int limit) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        List<ApprovalTask> rows = isBlank(status)
                ? taskRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                : taskRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status.trim().toUpperCase(Locale.ROOT));
        int finalLimit = Math.max(1, Math.min(limit, 100));
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (ApprovalTask row : rows) {
            boolean isOverdue = "PENDING".equals(row.getStatus()) && row.getDeadlineAt() != null && row.getDeadlineAt().isBefore(now);
            boolean isEscalated = "ESCALATED".equals(row.getStatus()) || (row.getEscalationLevel() != null && row.getEscalationLevel() > 0);
            if (overdue && !isOverdue) continue;
            if (escalated && !isEscalated) continue;
            items.add(toTaskView(row));
            if (items.size() >= finalLimit) break;
        }
        return ResponseEntity.ok(Collections.singletonMap("items", items));
    }

    @GetMapping("/instances")
    public ResponseEntity<?> listInstances(HttpServletRequest request,
                                           @RequestParam(defaultValue = "20") int limit) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        List<ApprovalInstance> rows = instanceRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        int finalLimit = Math.max(1, Math.min(limit, 100));
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (ApprovalInstance row : rows) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("id", row.getId());
            item.put("bizType", row.getBizType());
            item.put("bizId", row.getBizId());
            item.put("status", row.getStatus());
            item.put("submitter", row.getSubmitter());
            item.put("comment", row.getComment());
            item.put("currentSeq", row.getCurrentSeq());
            item.put("templateVersion", row.getTemplateVersion());
            item.put("createdAt", row.getCreatedAt());
            items.add(item);
            if (items.size() >= finalLimit) break;
        }
        return ResponseEntity.ok(Collections.singletonMap("items", items));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats(HttpServletRequest request) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        List<ApprovalInstance> instances = instanceRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        List<ApprovalTask> tasks = taskRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);

        Map<String, Integer> instanceByStatus = new LinkedHashMap<String, Integer>();
        Map<String, Integer> taskByStatus = new LinkedHashMap<String, Integer>();
        Map<String, Integer> bizByType = new LinkedHashMap<String, Integer>();
        Map<String, Integer> backlogByRole = new LinkedHashMap<String, Integer>();
        int overdueCount = 0;
        int escalatedCount = 0;
        long processingMinutesTotal = 0L;
        int processedCount = 0;
        LocalDateTime now = LocalDateTime.now();
        for (ApprovalInstance row : instances) {
            String status = isBlank(row.getStatus()) ? "UNKNOWN" : row.getStatus().trim().toUpperCase(Locale.ROOT);
            instanceByStatus.put(status, instanceByStatus.containsKey(status) ? instanceByStatus.get(status) + 1 : 1);
            String biz = isBlank(row.getBizType()) ? "UNKNOWN" : row.getBizType().trim().toUpperCase(Locale.ROOT);
            bizByType.put(biz, bizByType.containsKey(biz) ? bizByType.get(biz) + 1 : 1);
        }
        for (ApprovalTask row : tasks) {
            String status = isBlank(row.getStatus()) ? "UNKNOWN" : row.getStatus().trim().toUpperCase(Locale.ROOT);
            taskByStatus.put(status, taskByStatus.containsKey(status) ? taskByStatus.get(status) + 1 : 1);
            if ("PENDING".equals(status) && row.getDeadlineAt() != null && row.getDeadlineAt().isBefore(now)) overdueCount++;
            if ("ESCALATED".equals(status) || (row.getEscalationLevel() != null && row.getEscalationLevel() > 0)) escalatedCount++;
            if ("PENDING".equals(status) || "WAITING".equals(status)) {
                String role = isBlank(row.getApproverRole()) ? "UNKNOWN" : row.getApproverRole().trim().toUpperCase(Locale.ROOT);
                backlogByRole.put(role, backlogByRole.containsKey(role) ? backlogByRole.get(role) + 1 : 1);
            }
            if ("APPROVED".equals(status) || "REJECTED".equals(status) || "TRANSFERRED".equals(status) || "CANCELED".equals(status) || "EXPIRED".equals(status)) {
                processingMinutesTotal += Math.max(0L, Duration.between(row.getCreatedAt(), row.getUpdatedAt()).toMinutes());
                processedCount++;
            }
        }
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("templates", templateRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).size());
        summary.put("instances", instances.size());
        summary.put("tasks", tasks.size());
        summary.put("pendingTasks", taskByStatus.containsKey("PENDING") ? taskByStatus.get("PENDING") : 0);
        Map<String, Object> sla = new LinkedHashMap<String, Object>();
        sla.put("overdueCount", overdueCount);
        sla.put("escalatedCount", escalatedCount);
        sla.put("avgProcessingMinutes", processedCount == 0 ? 0 : processingMinutesTotal / processedCount);
        sla.put("escalationRate", tasks.isEmpty() ? 0.0 : ((double) escalatedCount / (double) tasks.size()));
        sla.put("backlogByRole", backlogByRole);

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("summary", summary);
        body.put("sla", sla);
        body.put("instanceByStatus", instanceByStatus);
        body.put("taskByStatus", taskByStatus);
        body.put("bizByType", bizByType);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/instances/{id}")
    public ResponseEntity<?> instanceDetail(HttpServletRequest request, @PathVariable String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Optional<ApprovalInstance> optional = instanceRepository.findByIdAndTenantId(id, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "approval_task_not_found", msg(request, "approval_task_not_found"), null));
        }
        ApprovalInstance row = optional.get();
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("id", row.getId());
        body.put("templateId", row.getTemplateId());
        body.put("bizType", row.getBizType());
        body.put("bizId", row.getBizId());
        body.put("status", row.getStatus());
        body.put("submitter", row.getSubmitter());
        body.put("comment", row.getComment());
        body.put("currentSeq", row.getCurrentSeq());
        body.put("templateVersion", row.getTemplateVersion());
        body.put("createdAt", row.getCreatedAt());
        body.put("updatedAt", row.getUpdatedAt());
        List<ApprovalTask> tasks = taskRepository.findByInstanceIdAndTenantIdOrderBySeqAsc(id, tenantId);
        List<Map<String, Object>> taskItems = new ArrayList<Map<String, Object>>();
        for (ApprovalTask task : tasks) taskItems.add(toTaskView(task));
        body.put("tasks", taskItems);
        List<ApprovalEvent> events = eventRepository.findByInstanceIdAndTenantIdOrderByCreatedAtAsc(id, tenantId);
        List<Map<String, Object>> timeline = new ArrayList<Map<String, Object>>();
        for (ApprovalEvent event : events) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("id", event.getId());
            item.put("eventType", event.getEventType());
            item.put("taskId", event.getTaskId());
            item.put("operatorUser", event.getOperatorUser());
            item.put("detail", event.getDetail());
            item.put("requestId", event.getRequestId());
            item.put("createdAt", event.getCreatedAt());
            timeline.add(item);
        }
        body.put("timeline", timeline);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/sla/scan")
    public ResponseEntity<?> runSlaScan(HttpServletRequest request) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        int affected = approvalSlaService.scanOverdueAndEscalate();
        return ResponseEntity.ok(Collections.singletonMap("affected", affected));
    }

    private ResponseEntity<?> handleTaskAction(HttpServletRequest request, String taskId, String actionStatus, V1ApprovalTaskActionRequest payload) {
        String tenantId = currentTenant(request);
        Optional<ApprovalTask> optional = taskRepository.findByIdAndTenantId(taskId, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "approval_task_not_found", msg(request, "approval_task_not_found"), null));
        }
        ApprovalTask task = optional.get();
        if (!("PENDING".equals(task.getStatus()) || "WAITING".equals(task.getStatus()))) {
            return ResponseEntity.badRequest().body(errorBody(request, "approval_task_closed", msg(request, "approval_task_closed"), null));
        }

        task.setStatus(actionStatus);
        task.setApproverUser(currentUser(request));
        if (payload != null) task.setComment(payload.getComment());
        taskRepository.save(task);

        Optional<ApprovalInstance> instOpt = instanceRepository.findByIdAndTenantId(task.getInstanceId(), tenantId);
        if (instOpt.isPresent()) {
            ApprovalInstance inst = instOpt.get();
            List<ApprovalTask> tasks = taskRepository.findByInstanceIdAndTenantIdOrderBySeqAsc(inst.getId(), tenantId);
            if ("REJECTED".equals(actionStatus)) {
                inst.setStatus("REJECTED");
                for (ApprovalTask t : tasks) {
                    if ("WAITING".equals(t.getStatus())) {
                        t.setStatus("CANCELED");
                        taskRepository.save(t);
                    }
                }
            } else {
                ApprovalTask nextWaiting = null;
                boolean allApproved = true;
                for (ApprovalTask t : tasks) {
                    if ("WAITING".equals(t.getStatus()) && nextWaiting == null) nextWaiting = t;
                    if (!"APPROVED".equals(t.getStatus())) allApproved = false;
                }
                if (nextWaiting != null) {
                    nextWaiting.setStatus("PENDING");
                    if (nextWaiting.getDeadlineAt() == null) {
                        int sla = nextWaiting.getSlaMinutes() == null ? 60 : Math.max(1, nextWaiting.getSlaMinutes());
                        nextWaiting.setDeadlineAt(LocalDateTime.now().plusMinutes(sla));
                    }
                    taskRepository.save(nextWaiting);
                    inst.setStatus("PENDING");
                    inst.setCurrentSeq(nextWaiting.getSeq());
                } else if (allApproved) {
                    inst.setStatus("APPROVED");
                }
            }
            instanceRepository.save(inst);
        }

        recordEvent(tenantId, task.getInstanceId(), task.getId(), actionStatus, currentUser(request), payload == null ? "" : payload.getComment(), traceId(request));
        auditLogService.record(currentUser(request), currentRole(request), actionStatus, "APPROVAL_TASK", task.getId(), "Approval task action", tenantId);
        return ResponseEntity.ok(toTaskView(task));
    }

    private ApprovalTemplate selectTemplate(String tenantId, String bizType, Long amount, String role, String department) {
        List<ApprovalTemplate> templates = templateRepository.findByTenantIdAndBizTypeAndEnabledTrueOrderByCreatedAtAsc(tenantId, bizType);
        for (ApprovalTemplate t : templates) {
            if (t.getAmountMin() != null && amount != null && amount < t.getAmountMin()) continue;
            if (t.getAmountMax() != null && amount != null && amount > t.getAmountMax()) continue;
            if (!isBlank(t.getRole()) && !t.getRole().equalsIgnoreCase(role)) continue;
            if (!isBlank(t.getDepartment()) && !t.getDepartment().equalsIgnoreCase(department)) continue;
            if ("INACTIVE".equalsIgnoreCase(t.getStatus())) continue;
            return t;
        }
        return null;
    }

    private Map<String, Object> toTaskView(ApprovalTask row) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("id", row.getId());
        item.put("instanceId", row.getInstanceId());
        item.put("approverRole", row.getApproverRole());
        item.put("approverUser", row.getApproverUser());
        item.put("status", row.getStatus());
        item.put("comment", row.getComment());
        item.put("seq", row.getSeq());
        item.put("nodeKey", row.getNodeKey());
        item.put("slaMinutes", row.getSlaMinutes());
        item.put("deadlineAt", row.getDeadlineAt());
        item.put("escalateToRoles", row.getEscalateToRoles());
        item.put("escalationLevel", row.getEscalationLevel());
        item.put("escalationSourceTaskId", row.getEscalationSourceTaskId());
        item.put("notifiedAt", row.getNotifiedAt());
        item.put("overdue", "PENDING".equals(row.getStatus()) && row.getDeadlineAt() != null && row.getDeadlineAt().isBefore(LocalDateTime.now()));
        item.put("createdAt", row.getCreatedAt());
        item.put("updatedAt", row.getUpdatedAt());
        return item;
    }

    private Map<String, Object> toTemplateView(ApprovalTemplate row) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("id", row.getId());
        item.put("tenantId", row.getTenantId());
        item.put("bizType", row.getBizType());
        item.put("name", row.getName());
        item.put("amountMin", row.getAmountMin());
        item.put("amountMax", row.getAmountMax());
        item.put("role", row.getRole());
        item.put("department", row.getDepartment());
        item.put("approverRoles", row.getApproverRoles());
        item.put("flowDefinition", parseFlowJson(row.getFlowDefinition()));
        item.put("version", row.getVersion());
        item.put("activeVersion", row.getVersion());
        item.put("status", row.getStatus());
        item.put("enabled", row.getEnabled());
        item.put("createdAt", row.getCreatedAt());
        item.put("updatedAt", row.getUpdatedAt());
        return item;
    }

    private Map<String, Object> parseFlowJson(String flowJson) {
        if (isBlank(flowJson)) return new LinkedHashMap<String, Object>();
        try {
            return objectMapper.readValue(flowJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            return new LinkedHashMap<String, Object>();
        }
    }

    private List<NodeDef> filterEffectiveNodes(ApprovalTemplate template, Long amount, String role, String department) {
        List<NodeDef> nodes = parseNodes(template.getFlowDefinition());
        if (nodes.isEmpty()) nodes = parseNodes(buildDefaultFlow(template.getApproverRoles()));
        List<NodeDef> out = new ArrayList<NodeDef>();
        for (NodeDef node : nodes) if (node.matches(amount, role, department)) out.add(node);
        return out;
    }

    private CompiledFlow compileFlow(JsonNode flowDefinition, String approverRoles) {
        String fallback = buildDefaultFlow(approverRoles);
        List<NodeDef> nodes = parseNodes(flowDefinition == null ? fallback : flowDefinition.toString());
        if (nodes.isEmpty()) nodes = parseNodes(fallback);
        if (nodes.isEmpty()) throw new IllegalArgumentException("approval_flow_invalid");
        Set<Integer> seqSet = new HashSet<Integer>();
        Set<String> roleSet = new LinkedHashSet<String>();
        for (NodeDef node : nodes) {
            if (node.seq < 1 || !seqSet.add(node.seq)) throw new IllegalArgumentException("approval_flow_invalid");
            if (node.slaMinutes < 1 || node.slaMinutes > 43200) throw new IllegalArgumentException("approval_sla_invalid");
            if (node.approverRoles.isEmpty()) throw new IllegalArgumentException("approval_approver_required");
            roleSet.addAll(node.approverRoles);
        }
        Collections.sort(nodes, new Comparator<NodeDef>() {
            @Override
            public int compare(NodeDef a, NodeDef b) {
                return Integer.compare(a.seq, b.seq);
            }
        });
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        List<Map<String, Object>> normalized = new ArrayList<Map<String, Object>>();
        for (NodeDef node : nodes) {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", node.id);
            m.put("seq", node.seq);
            m.put("approverRoles", node.approverRoles);
            Map<String, Object> cond = new LinkedHashMap<String, Object>();
            if (node.amountMin != null) cond.put("amountMin", node.amountMin);
            if (node.amountMax != null) cond.put("amountMax", node.amountMax);
            if (!isBlank(node.role)) cond.put("role", node.role);
            if (!isBlank(node.department)) cond.put("department", node.department);
            m.put("conditions", cond);
            m.put("slaMinutes", node.slaMinutes);
            m.put("escalateToRoles", node.escalateToRoles);
            normalized.add(m);
        }
        root.put("nodes", normalized);
        try {
            return new CompiledFlow(objectMapper.writeValueAsString(root), String.join(",", roleSet));
        } catch (Exception ex) {
            throw new IllegalArgumentException("approval_flow_invalid");
        }
    }

    private List<NodeDef> parseNodes(String flowJson) {
        if (isBlank(flowJson)) return new ArrayList<NodeDef>();
        try {
            Map<String, Object> root = objectMapper.readValue(flowJson, new TypeReference<Map<String, Object>>() {});
            Object nodesObj = root.get("nodes");
            if (!(nodesObj instanceof List)) return new ArrayList<NodeDef>();
            List<?> list = (List<?>) nodesObj;
            List<NodeDef> out = new ArrayList<NodeDef>();
            for (int i = 0; i < list.size(); i++) {
                if (!(list.get(i) instanceof Map)) continue;
                @SuppressWarnings("unchecked") Map<String, Object> m = (Map<String, Object>) list.get(i);
                out.add(NodeDef.fromMap(m, i + 1));
            }
            return out;
        } catch (Exception ex) {
            return new ArrayList<NodeDef>();
        }
    }

    private String buildDefaultFlow(String approverRoles) {
        String raw = isBlank(approverRoles) ? "MANAGER,ADMIN" : approverRoles.toUpperCase(Locale.ROOT);
        String[] roles = raw.split(",");
        List<Map<String, Object>> nodes = new ArrayList<Map<String, Object>>();
        int seq = 1;
        for (String role : roles) {
            String r = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
            if (r.isEmpty()) continue;
            Map<String, Object> node = new LinkedHashMap<String, Object>();
            node.put("id", "node_" + seq);
            node.put("seq", seq);
            node.put("approverRoles", Collections.singletonList(r));
            node.put("conditions", new LinkedHashMap<String, Object>());
            node.put("slaMinutes", 240);
            node.put("escalateToRoles", Collections.singletonList("ADMIN"));
            nodes.add(node);
            seq++;
        }
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("nodes", nodes);
        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            return "{\"nodes\":[]}";
        }
    }

    private void recordEvent(String tenantId, String instanceId, String taskId, String eventType, String operatorUser, String detail, String requestId) {
        ApprovalEvent event = new ApprovalEvent();
        event.setId(newId("apev"));
        event.setTenantId(tenantId);
        event.setInstanceId(instanceId);
        event.setTaskId(taskId);
        event.setEventType(eventType);
        event.setOperatorUser(operatorUser);
        event.setDetail(detail);
        event.setRequestId(requestId);
        eventRepository.save(event);
    }

    private String newId(String prefix) {
        return prefix + "_" + Long.toString(System.currentTimeMillis(), 36) + String.format("%03d", (int) (Math.random() * 1000));
    }

    private static class CompiledFlow {
        private final String json;
        private final String flatRoles;

        private CompiledFlow(String json, String flatRoles) {
            this.json = json;
            this.flatRoles = flatRoles;
        }
    }

    private static class NodeDef {
        private String id;
        private int seq;
        private List<String> approverRoles;
        private Long amountMin;
        private Long amountMax;
        private String role;
        private String department;
        private int slaMinutes;
        private List<String> escalateToRoles;

        static NodeDef fromMap(Map<String, Object> map, int fallbackSeq) {
            NodeDef node = new NodeDef();
            node.id = map.containsKey("id") ? String.valueOf(map.get("id")) : ("node_" + fallbackSeq);
            node.seq = parseInt(map.get("seq"), fallbackSeq);
            node.approverRoles = parseList(map.get("approverRoles"));
            @SuppressWarnings("unchecked")
            Map<String, Object> conditions = map.get("conditions") instanceof Map ? (Map<String, Object>) map.get("conditions") : new LinkedHashMap<String, Object>();
            node.amountMin = parseLong(conditions.get("amountMin"), null);
            node.amountMax = parseLong(conditions.get("amountMax"), null);
            node.role = conditions.get("role") == null ? null : String.valueOf(conditions.get("role")).trim().toUpperCase(Locale.ROOT);
            node.department = conditions.get("department") == null ? null : String.valueOf(conditions.get("department")).trim();
            node.slaMinutes = parseInt(map.get("slaMinutes"), 240);
            node.escalateToRoles = parseList(map.get("escalateToRoles"));
            if (node.escalateToRoles.isEmpty()) node.escalateToRoles = Collections.singletonList("ADMIN");
            return node;
        }

        boolean matches(Long amount, String submitRole, String submitDepartment) {
            if (amountMin != null && amount != null && amount < amountMin) return false;
            if (amountMax != null && amount != null && amount > amountMax) return false;
            if (role != null && !role.isEmpty() && !role.equalsIgnoreCase(submitRole)) return false;
            if (department != null && !department.isEmpty() && !department.equalsIgnoreCase(submitDepartment)) return false;
            return true;
        }

        private static List<String> parseList(Object obj) {
            List<String> out = new ArrayList<String>();
            if (obj instanceof List) {
                for (Object each : (List<?>) obj) {
                    String s = each == null ? "" : String.valueOf(each).trim().toUpperCase(Locale.ROOT);
                    if (!s.isEmpty() && !out.contains(s)) out.add(s);
                }
            } else if (obj != null) {
                String[] parts = String.valueOf(obj).split(",");
                for (String part : parts) {
                    String s = part == null ? "" : part.trim().toUpperCase(Locale.ROOT);
                    if (!s.isEmpty() && !out.contains(s)) out.add(s);
                }
            }
            return out;
        }

        private static int parseInt(Object value, int fallback) {
            if (value == null) return fallback;
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (Exception ex) {
                return fallback;
            }
        }

        private static Long parseLong(Object value, Long fallback) {
            if (value == null) return fallback;
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (Exception ex) {
                return fallback;
            }
        }
    }

    private Long resolveAmount(String tenantId, String bizType, String bizId) {
        if ("CONTRACT".equals(bizType)) {
            Optional<ContractRecord> c = contractRepository.findById(bizId);
            if (c.isPresent() && tenantId.equals(c.get().getTenantId())) return c.get().getAmount();
        }
        if ("PAYMENT".equals(bizType)) {
            Optional<PaymentRecord> p = paymentRepository.findById(bizId);
            if (p.isPresent() && tenantId.equals(p.get().getTenantId())) return p.get().getAmount();
        }
        return 0L;
    }
}

