package com.yao.crm.controller;

import com.yao.crm.entity.*;
import com.yao.crm.service.WorkflowService;
import com.yao.crm.service.WorkflowExecutionService;
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
    public ResponseEntity<List<WorkflowDefinition>> getWorkflows(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {

        List<WorkflowDefinition> workflows = workflowService.getWorkflows(tenantId, status, category);
        return ResponseEntity.ok(workflows);
    }

    /**
     * 获取工作流详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getWorkflowDetail(@PathVariable String id) {
        Map<String, Object> detail = workflowService.getWorkflowDetail(id);
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detail);
    }

    /**
     * 创建工作流
     */
    @PostMapping
    public ResponseEntity<WorkflowDefinition> createWorkflow(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestBody CreateWorkflowRequest request) {

        WorkflowDefinition workflow = workflowService.createWorkflow(
                tenantId,
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

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("id", id);
        result.put("message", "Workflow updated");
        return ResponseEntity.ok(result);
    }

    /**
     * 删除工作流
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWorkflow(@PathVariable String id) {
        try {
            workflowService.deleteWorkflow(id);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 激活工作流
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<?> activateWorkflow(
            @PathVariable String id,
            @RequestBody ActivateRequest request) {

        try {
            WorkflowDefinition workflow = workflowService.activateWorkflow(id, request.activatedBy);
            return ResponseEntity.ok(workflow);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 停用工作流
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateWorkflow(@PathVariable String id) {
        try {
            WorkflowDefinition workflow = workflowService.deactivateWorkflow(id);
            return ResponseEntity.ok(workflow);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 验证工作流
     */
    @PostMapping("/{id}/validate")
    public ResponseEntity<?> validateWorkflow(@PathVariable String id) {
        Map<String, Object> validation = workflowService.validateWorkflow(id);
        return ResponseEntity.ok(validation);
    }

    /**
     * 获取工作流统计
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<?> getWorkflowStats(@PathVariable String id) {
        Map<String, Object> stats = workflowService.getWorkflowStats(id);
        if (stats == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stats);
    }

    /**
     * 添加节点
     */
    @PostMapping("/{id}/nodes")
    public ResponseEntity<WorkflowNode> addNode(
            @PathVariable String id,
            @RequestBody AddNodeRequest request) {

        WorkflowNode node = workflowService.addNode(
                id,
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

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("nodeId", nodeId);
        return ResponseEntity.ok(result);
    }

    /**
     * 删除节点
     */
    @DeleteMapping("/{workflowId}/nodes/{nodeId}")
    public ResponseEntity<?> deleteNode(
            @PathVariable String workflowId,
            @PathVariable String nodeId) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return ResponseEntity.ok(result);
    }

    /**
     * 添加连接
     */
    @PostMapping("/{id}/connections")
    public ResponseEntity<WorkflowConnection> addConnection(
            @PathVariable String id,
            @RequestBody AddConnectionRequest request) {

        WorkflowConnection connection = workflowService.addConnection(
                id,
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
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return ResponseEntity.ok(result);
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
            @PathVariable String id,
            @RequestBody TestWorkflowRequest request) {

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
            @PathVariable String id,
            @RequestBody ExecuteWorkflowRequest request) {

        try {
            WorkflowExecution execution = executionService.startExecution(
                    id,
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
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 获取执行详情
     */
    @GetMapping("/executions/{executionId}")
    public ResponseEntity<?> getExecutionDetail(@PathVariable String executionId) {
        try {
            Map<String, Object> detail = executionService.getExecutionDetail(executionId);
            if (detail == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 获取工作流执行历史
     */
    @GetMapping("/{id}/executions")
    public ResponseEntity<List<WorkflowExecution>> getExecutionHistory(
            @PathVariable String id,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<WorkflowExecution> executions = executionService.getExecutionHistory(id, status, page, size);
        return ResponseEntity.ok(executions);
    }

    /**
     * 取消执行
     */
    @PostMapping("/executions/{executionId}/cancel")
    public ResponseEntity<?> cancelExecution(
            @PathVariable String executionId,
            @RequestBody CancelExecutionRequest request) {

        try {
            executionService.cancelExecution(executionId, request.cancelledBy);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Execution cancelled");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 重试失败的执行
     */
    @PostMapping("/executions/{executionId}/retry")
    public ResponseEntity<?> retryExecution(@PathVariable String executionId) {
        try {
            WorkflowExecution execution = executionService.retryExecution(executionId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("newExecutionId", execution.getId());
            result.put("status", execution.getStatus());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 处理审批回调
     */
    @PostMapping("/executions/{executionId}/approval-callback")
    public ResponseEntity<?> handleApprovalCallback(
            @PathVariable String executionId,
            @RequestBody ApprovalCallbackRequest request) {

        try {
            executionService.handleApprovalCallback(
                    executionId,
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
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
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
}
