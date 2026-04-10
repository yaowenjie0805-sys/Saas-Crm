package com.yao.crm.controller;

import com.yao.crm.entity.*;
import com.yao.crm.service.WorkflowService;
import com.yao.crm.service.WorkflowExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * 工作流控制器
 * 提供工作流定义、节点、连接管理API
 */
@RestController
@RequestMapping("/api/v2/workflows")
public class WorkflowController {

    private static final String CALLBACK_TOKEN_HEADER = "X-Workflow-Callback-Token";
    private static final Set<String> READ_ROLES = Set.of("ADMIN", "MANAGER", "ANALYST", "SALES");
    private static final Set<String> WRITE_ROLES = Set.of("ADMIN", "MANAGER");

    private final WorkflowService workflowService;
    private final WorkflowExecutionService executionService;
    private final List<String> approvalCallbackTokens;

    public WorkflowController(WorkflowService workflowService, WorkflowExecutionService executionService) {
        this(workflowService, executionService, "", "");
    }

    public WorkflowController(WorkflowService workflowService,
                              WorkflowExecutionService executionService,
                              @Value("${workflow.approval-callback.token:}") String approvalCallbackToken) {
        this(workflowService, executionService, approvalCallbackToken, "");
    }

    @Autowired
    public WorkflowController(WorkflowService workflowService,
                              WorkflowExecutionService executionService,
                              @Value("${workflow.approval-callback.token:}") String approvalCallbackToken,
                              @Value("${workflow.approval-callback.tokens:}") String approvalCallbackTokens) {
        this.workflowService = workflowService;
        this.executionService = executionService;
        this.approvalCallbackTokens = resolveCallbackTokens(approvalCallbackToken, approvalCallbackTokens);
    }

    /**
     * 获取工作流列表
     */
    @GetMapping
    public ResponseEntity<?> getWorkflows(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        ResponseEntity<?> authFailure = ensureReadAccess();
        if (authFailure != null) {
            return authFailure;
        }

        String normalizedTenantId = normalizeRequired(tenantId);
        if (normalizedTenantId == null) {
            return ResponseEntity.badRequest().build();
        }

        List<WorkflowDefinition> workflows = workflowService.getWorkflows(
                normalizedTenantId,
                normalizeOptional(status),
                normalizeOptional(category));
        return ResponseEntity.ok(workflows);
    }

    /**
     * 获取工作流详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getWorkflowDetail(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id) {
        ResponseEntity<?> authFailure = ensureReadAccess();
        if (authFailure != null) {
            return authFailure;
        }

        String normalizedTenantId = normalizeRequired(tenantId);
        String normalizedId = normalizeRequired(id);
        if (normalizedTenantId == null) {
            return ResponseEntity.badRequest().build();
        }
        if (normalizedId == null) {
            return ResponseEntity.badRequest().build();
        }
        Map<String, Object> detail = workflowService.getWorkflowDetail(normalizedTenantId, normalizedId);
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detail);
    }

    /**
     * 创建工作流
     */
    @PostMapping
    public ResponseEntity<?> createWorkflow(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestBody CreateWorkflowRequest request) {
        ResponseEntity<?> authFailure = ensureWriteAccess();
        if (authFailure != null) {
            return authFailure;
        }

        String normalizedTenantId = normalizeRequired(tenantId);
        if (normalizedTenantId == null) {
            return ResponseEntity.badRequest().build();
        }

        WorkflowDefinition workflow = workflowService.createWorkflow(
                normalizedTenantId,
                request.name,
                request.description,
                request.category,
                request.owner
        );

        return ResponseEntity.ok(workflow);
    }

    /**
     * 更新工作流
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateWorkflow(
            @PathVariable String id,
            @RequestBody UpdateWorkflowRequest request) {
        ResponseEntity<?> authFailure = ensureWriteAccess();
        if (authFailure != null) {
            return authFailure;
        }

        String normalizedId = normalizeRequired(id);
        if (normalizedId == null) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("id", normalizedId);
        result.put("message", "Workflow updated");
        return ResponseEntity.ok(result);
    }

    /**
     * 删除工作流
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWorkflow(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id) {
        ResponseEntity<?> authFailure = ensureWriteAccess();
        if (authFailure != null) {
            return authFailure;
        }

        String normalizedTenantId = normalizeRequired(tenantId);
        String normalizedId = normalizeRequired(id);
        if (normalizedTenantId == null || normalizedId == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            workflowService.deleteWorkflow(normalizedTenantId, normalizedId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return mapServiceException(e);
        }
    }

    /**
     * 激活工作流
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<?> activateWorkflow(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id,
            @RequestBody ActivateRequest request) {
        ResponseEntity<?> authFailure = ensureWriteAccess();
        if (authFailure != null) {
            return authFailure;
        }

        String normalizedTenantId = normalizeRequired(tenantId);
        String normalizedId = normalizeRequired(id);
        if (normalizedTenantId == null) {
            return ResponseEntity.badRequest().build();
        }
        if (normalizedId == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            WorkflowDefinition workflow = workflowService.activateWorkflow(normalizedTenantId, normalizedId, request.activatedBy);
            return ResponseEntity.ok(workflow);
        } catch (Exception e) {
            return mapServiceException(e);
        }
    }

    /**
     * 停用工作流
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateWorkflow(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id) {
        ResponseEntity<?> authFailure = ensureWriteAccess();
        if (authFailure != null) {
            return authFailure;
        }

        String normalizedTenantId = normalizeRequired(tenantId);
        String normalizedId = normalizeRequired(id);
        if (normalizedTenantId == null) {
            return ResponseEntity.badRequest().build();
        }
        if (normalizedId == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            WorkflowDefinition workflow = workflowService.deactivateWorkflow(normalizedTenantId, normalizedId);
            return ResponseEntity.ok(workflow);
        } catch (Exception e) {
            return mapServiceException(e);
        }
    }

    /**
     * 验证工作流
     */
    @PostMapping("/{id}/validate")
    public ResponseEntity<?> validateWorkflow(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id) {
        ResponseEntity<?> authFailure = ensureWriteAccess();
        if (authFailure != null) {
            return authFailure;
        }

        String normalizedTenantId = normalizeRequired(tenantId);
        String normalizedId = normalizeRequired(id);
        if (normalizedTenantId == null) {
            return ResponseEntity.badRequest().build();
        }
        if (normalizedId == null) {
            return ResponseEntity.badRequest().build();
        }
        Map<String, Object> validation = workflowService.validateWorkflow(normalizedTenantId, normalizedId);
        return ResponseEntity.ok(validation);
    }

    /**
     * 获取工作流统计
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<?> getWorkflowStats(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id) {
        ResponseEntity<?> authFailure = ensureReadAccess();
        if (authFailure != null) {
            return authFailure;
        }

        String normalizedTenantId = normalizeRequired(tenantId);
        String normalizedId = normalizeRequired(id);
        if (normalizedTenantId == null) {
            return ResponseEntity.badRequest().build();
        }
        if (normalizedId == null) {
            return ResponseEntity.badRequest().build();
        }
        Map<String, Object> stats = workflowService.getWorkflowStats(normalizedTenantId, normalizedId);
        if (stats == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stats);
    }

    /**
     * 添加节点
     */
    @PostMapping("/{id}/nodes")
    public ResponseEntity<?> addNode(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id,
            @RequestBody AddNodeRequest request) {
        ResponseEntity<?> authFailure = ensureWriteAccess();
        if (authFailure != null) {
            return authFailure;
        }

        String normalizedTenantId = normalizeRequired(tenantId);
        String normalizedId = normalizeRequired(id);
        if (normalizedTenantId == null || normalizedId == null) {
            return ResponseEntity.badRequest().build();
        }

        WorkflowNode node = workflowService.addNode(
                normalizedTenantId,
                normalizedId,
                request.nodeType,
                request.nodeSubtype,
                request.name,
                request.positionX,
                request.positionY,
                request.configJson
        );

        return ResponseEntity.ok(node);
    }

    /**
     * 更新节点
     */
    @PutMapping("/{workflowId}/nodes/{nodeId}")
    public ResponseEntity<?> updateNode(
            @PathVariable String workflowId,
            @PathVariable String nodeId,
            @RequestBody UpdateNodeRequest request) {
        ResponseEntity<?> authFailure = ensureWriteAccess();
        if (authFailure != null) {
            return authFailure;
        }

        String normalizedWorkflowId = normalizeRequired(workflowId);
        String normalizedNodeId = normalizeRequired(nodeId);
        if (normalizedWorkflowId == null || normalizedNodeId == null) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("nodeId", normalizedNodeId);
        return ResponseEntity.ok(result);
    }

    /**
     * 删除节点
     */
    @DeleteMapping("/{workflowId}/nodes/{nodeId}")
    public ResponseEntity<?> deleteNode(
            @PathVariable String workflowId,
            @PathVariable String nodeId) {
        ResponseEntity<?> authFailure = ensureWriteAccess();
        if (authFailure != null) {
            return authFailure;
        }

        String normalizedWorkflowId = normalizeRequired(workflowId);
        String normalizedNodeId = normalizeRequired(nodeId);
        if (normalizedWorkflowId == null || normalizedNodeId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * 添加连接
     */
    @PostMapping("/{id}/connections")
    public ResponseEntity<?> addConnection(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id,
            @RequestBody AddConnectionRequest request) {
        ResponseEntity<?> authFailure = ensureWriteAccess();
        if (authFailure != null) {
            return authFailure;
        }

        String normalizedTenantId = normalizeRequired(tenantId);
        String normalizedId = normalizeRequired(id);
        if (normalizedTenantId == null || normalizedId == null) {
            return ResponseEntity.badRequest().build();
        }

        WorkflowConnection connection = workflowService.addConnection(
                normalizedTenantId,
                normalizedId,
                request.sourceNodeId,
                request.targetNodeId,
                request.connectionType,
                request.label
        );

        return ResponseEntity.ok(connection);
    }

    /**
     * 删除连接
     */
    @DeleteMapping("/{workflowId}/connections/{connectionId}")
    public ResponseEntity<?> deleteConnection(
            @PathVariable String workflowId,
            @PathVariable String connectionId) {
        ResponseEntity<?> authFailure = ensureWriteAccess();
        if (authFailure != null) {
            return authFailure;
        }

        String normalizedWorkflowId = normalizeRequired(workflowId);
        String normalizedConnectionId = normalizeRequired(connectionId);
        if (normalizedWorkflowId == null || normalizedConnectionId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取节点类型选项
     */
    @GetMapping("/node-types")
    public ResponseEntity<?> getNodeTypes() {
        ResponseEntity<?> authFailure = ensureReadAccess();
        if (authFailure != null) {
            return authFailure;
        }

        Map<String, List<Map<String, String>>> nodeTypes = workflowService.getNodeTypes();
        return ResponseEntity.ok(nodeTypes);
    }

    /**
     * 测试工作流
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<?> testWorkflow(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id,
            @RequestBody TestWorkflowRequest request) {
        ResponseEntity<?> authFailure = ensureWriteAccess();
        if (authFailure != null) {
            return authFailure;
        }

        String normalizedTenantId = normalizeRequired(tenantId);
        String normalizedId = normalizeRequired(id);
        if (normalizedTenantId == null || normalizedId == null) {
            return ResponseEntity.badRequest().build();
        }

        workflowService.getWorkflowDetail(normalizedTenantId, normalizedId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Workflow test started");
        result.put("executionId", UUID.randomUUID().toString());
        return ResponseEntity.ok(result);
    }

    // Request DTOs
    public static class CreateWorkflowRequest {
        public String name;
        public String description;
        public String category;
        public String owner;
    }

    public static class UpdateWorkflowRequest {
        public String name;
        public String description;
        public String category;
    }

    public static class ActivateRequest {
        public String activatedBy;
    }

    public static class AddNodeRequest {
        public String nodeType;
        public String nodeSubtype;
        public String name;
        public int positionX;
        public int positionY;
        public String configJson;
    }

    public static class UpdateNodeRequest {
        public String name;
        public String configJson;
        public int positionX;
        public int positionY;
    }

    public static class AddConnectionRequest {
        public String sourceNodeId;
        public String targetNodeId;
        public String connectionType;
        public String label;
    }

    public static class TestWorkflowRequest {
        public Map<String, Object> payload;
    }

    // ========== 工作流执行 API ==========

    /**
     * 启动工作流执行
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<?> executeWorkflow(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id,
            @RequestBody ExecuteWorkflowRequest request) {
        ResponseEntity<?> authFailure = ensureWriteAccess();
        if (authFailure != null) {
            return authFailure;
        }

        String normalizedTenantId = normalizeRequired(tenantId);
        String normalizedId = normalizeRequired(id);
        if (normalizedTenantId == null || normalizedId == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            WorkflowExecution execution = executionService.startExecution(
                    normalizedTenantId,
                    normalizedId,
                    request.triggerType != null ? request.triggerType : "MANUAL",
                    request.triggerSource,
                    request.payload != null ? new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request.payload) : null,
                    request.payload
            );
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("executionId", execution.getId());
            result.put("status", execution.getStatus());
            result.put("startedAt", execution.getStartedAt().toString());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return mapServiceException(e);
        }
    }

    /**
     * 获取执行详情
     */
    @GetMapping("/executions/{executionId}")
    public ResponseEntity<?> getExecutionDetail(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String executionId) {
        ResponseEntity<?> authFailure = ensureReadAccess();
        if (authFailure != null) {
            return authFailure;
        }

        String normalizedTenantId = normalizeRequired(tenantId);
        String normalizedExecutionId = normalizeRequired(executionId);
        if (normalizedTenantId == null || normalizedExecutionId == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Map<String, Object> detail = executionService.getExecutionDetail(normalizedTenantId, normalizedExecutionId);
            if (detail == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            return mapServiceException(e);
        }
    }

    /**
     * 获取工作流执行历史
     */
    @GetMapping("/{id}/executions")
    public ResponseEntity<?> getExecutionHistory(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ResponseEntity<?> authFailure = ensureReadAccess();
        if (authFailure != null) {
            return authFailure;
        }

        String normalizedTenantId = normalizeRequired(tenantId);
        String normalizedId = normalizeRequired(id);
        if (normalizedTenantId == null || normalizedId == null) {
            return ResponseEntity.badRequest().build();
        }

        List<WorkflowExecution> executions = executionService.getExecutionHistory(
                normalizedTenantId,
                normalizedId,
                normalizeOptional(status),
                page,
                size);
        return ResponseEntity.ok(executions);
    }

    /**
     * 取消执行
     */
    @PostMapping("/executions/{executionId}/cancel")
    public ResponseEntity<?> cancelExecution(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String executionId,
            @RequestBody CancelExecutionRequest request) {
        ResponseEntity<?> authFailure = ensureWriteAccess();
        if (authFailure != null) {
            return authFailure;
        }

        String normalizedTenantId = normalizeRequired(tenantId);
        String normalizedExecutionId = normalizeRequired(executionId);
        if (normalizedTenantId == null || normalizedExecutionId == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            executionService.cancelExecution(normalizedTenantId, normalizedExecutionId, request.cancelledBy);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Execution cancelled");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return mapServiceException(e);
        }
    }

    /**
     * 重试失败的执行
     */
    @PostMapping("/executions/{executionId}/retry")
    public ResponseEntity<?> retryExecution(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String executionId) {
        ResponseEntity<?> authFailure = ensureWriteAccess();
        if (authFailure != null) {
            return authFailure;
        }

        String normalizedTenantId = normalizeRequired(tenantId);
        String normalizedExecutionId = normalizeRequired(executionId);
        if (normalizedTenantId == null || normalizedExecutionId == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            WorkflowExecution execution = executionService.retryExecution(normalizedTenantId, normalizedExecutionId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("newExecutionId", execution.getId());
            result.put("status", execution.getStatus());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return mapServiceException(e);
        }
    }

    /**
     * 处理审批回调
     */
    @PostMapping("/executions/{executionId}/approval-callback")
    public ResponseEntity<?> handleApprovalCallback(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String executionId,
            @RequestBody ApprovalCallbackRequest request) {
        ResponseEntity<?> authFailure = ensureWriteAccessOrCallbackToken();
        if (authFailure != null) {
            return authFailure;
        }

        String normalizedTenantId = normalizeRequired(tenantId);
        String normalizedExecutionId = normalizeRequired(executionId);
        if (normalizedTenantId == null || normalizedExecutionId == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            executionService.handleApprovalCallback(
                    normalizedTenantId,
                    normalizedExecutionId,
                    request.nodeId,
                    request.action,
                    request.approverId,
                    request.comments
            );
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Approval processed");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return mapServiceException(e);
        }
    }

    // 执行相关请求 DTOs
    public static class ExecuteWorkflowRequest {
        public String triggerType;
        public String triggerSource;
        public Map<String, Object> payload;
    }

    public static class CancelExecutionRequest {
        public String cancelledBy;
    }

    public static class ApprovalCallbackRequest {
        public String nodeId;
        public String action; // APPROVE or REJECT
        public String approverId;
        public String comments;
    }

    private ResponseEntity<?> mapServiceException(Exception e) {
        if (e instanceof IllegalArgumentException) {
            if (isNotFoundError(e.getMessage())) {
                return buildErrorResponse(HttpStatus.NOT_FOUND, e);
            }
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e);
        }
        if (e instanceof IllegalStateException) {
            return buildErrorResponse(HttpStatus.CONFLICT, e);
        }
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, Exception e) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", resolveErrorMessage(status, e));
        return ResponseEntity.status(status).body(error);
    }

    private String resolveErrorMessage(HttpStatus status, Exception e) {
        String message = e == null ? null : e.getMessage();
        if (message != null && !message.trim().isEmpty()) {
            return message;
        }
        if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
            return "Internal server error";
        }
        return status.getReasonPhrase();
    }

    private boolean isNotFoundError(String message) {
        if (message == null) {
            return false;
        }
        return message.toLowerCase(Locale.ROOT).contains("not found");
    }

    private ResponseEntity<?> ensureReadAccess() {
        return ensureRole(READ_ROLES);
    }

    private ResponseEntity<?> ensureWriteAccess() {
        return ensureRole(WRITE_ROLES);
    }

    private ResponseEntity<?> ensureWriteAccessOrCallbackToken() {
        ResponseEntity<?> roleFailure = ensureWriteAccess();
        if (roleFailure == null) {
            return null;
        }
        if (hasValidCallbackToken()) {
            return null;
        }
        return roleFailure;
    }

    private ResponseEntity<?> ensureRole(Set<String> allowedRoles) {
        String role = resolveAuthRole();
        if (role == null) {
            return forbidden();
        }
        String normalizedRole = role.toUpperCase(Locale.ROOT);
        if (!allowedRoles.contains(normalizedRole)) {
            return forbidden();
        }
        return null;
    }

    private String resolveAuthRole() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        Object role = attributes.getAttribute("authRole", RequestAttributes.SCOPE_REQUEST);
        if (!(role instanceof String)) {
            return null;
        }
        return normalizeRequired((String) role);
    }

    private boolean hasValidCallbackToken() {
        if (approvalCallbackTokens.isEmpty()) {
            return false;
        }
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            return false;
        }
        String headerValue = normalizeRequired(((ServletRequestAttributes) attributes).getRequest().getHeader(CALLBACK_TOKEN_HEADER));
        if (headerValue == null) {
            return false;
        }
        byte[] headerBytes = headerValue.getBytes(StandardCharsets.UTF_8);
        for (String token : approvalCallbackTokens) {
            if (MessageDigest.isEqual(headerBytes, token.getBytes(StandardCharsets.UTF_8))) {
                return true;
            }
        }
        return false;
    }

    private ResponseEntity<Map<String, Object>> forbidden() {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "forbidden");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizeRequired(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeOptional(String value) {
        String normalized = normalizeRequired(value);
        return normalized == null ? null : normalized;
    }

    private List<String> resolveCallbackTokens(String singleTokenConfig, String multipleTokensConfig) {
        List<String> tokens = parseCommaSeparatedTokens(multipleTokensConfig);
        if (!tokens.isEmpty()) {
            return tokens;
        }
        String singleToken = normalizeOptional(singleTokenConfig);
        if (singleToken == null) {
            return List.of();
        }
        return List.of(singleToken);
    }

    private List<String> parseCommaSeparatedTokens(String rawTokens) {
        String normalized = normalizeOptional(rawTokens);
        if (normalized == null) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String token : normalized.split(",")) {
            String normalizedToken = normalizeOptional(token);
            if (normalizedToken != null) {
                tokens.add(normalizedToken);
            }
        }
        return tokens;
    }
}
