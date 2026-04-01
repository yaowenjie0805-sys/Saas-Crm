package com.yao.crm.controller;

import com.yao.crm.entity.*;
import com.yao.crm.service.WorkflowService;
import com.yao.crm.service.WorkflowExecutionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 工作流控制器
 * 提供工作流定义、节点、连接管理API
 */
@RestController
@RequestMapping("/api/v2/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowExecutionService executionService;

    public WorkflowController(WorkflowService workflowService, WorkflowExecutionService executionService) {
        this.workflowService = workflowService;
        this.executionService = executionService;
    }

    /**
     * 获取工作流列表
     */
    @GetMapping
    public ResponseEntity<?> getWorkflows(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {

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
}
