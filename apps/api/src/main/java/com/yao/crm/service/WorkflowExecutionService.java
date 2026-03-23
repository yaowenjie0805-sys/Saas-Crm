package com.yao.crm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yao.crm.entity.*;
import com.yao.crm.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 工作流执行引擎
 * 负责执行工作流中的各个节点，处理条件分支、审批流程等
 */
@Service
public class WorkflowExecutionService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionService.class);

    private final WorkflowDefinitionRepository workflowRepository;
    private final WorkflowNodeRepository nodeRepository;
    private final WorkflowConnectionRepository connectionRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final ApprovalNodeRepository approvalNodeRepository;
    private final ObjectMapper objectMapper;

    // 执行中的工作流缓存（用于处理异步审批等场景）
    private final Map<String, WorkflowExecutionContext> activeExecutions = new ConcurrentHashMap<>();

    public WorkflowExecutionService(
            WorkflowDefinitionRepository workflowRepository,
            WorkflowNodeRepository nodeRepository,
            WorkflowConnectionRepository connectionRepository,
            WorkflowExecutionRepository executionRepository,
            ApprovalNodeRepository approvalNodeRepository,
            ObjectMapper objectMapper) {
        this.workflowRepository = workflowRepository;
        this.nodeRepository = nodeRepository;
        this.connectionRepository = connectionRepository;
        this.executionRepository = executionRepository;
        this.approvalNodeRepository = approvalNodeRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 启动工作流执行
     */
    @Transactional
    public WorkflowExecution startExecution(String workflowId, String triggerType, String triggerSource,
                                            String triggerPayload, Map<String, Object> triggerData) {
        WorkflowDefinition workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        if (!"ACTIVE".equals(workflow.getStatus())) {
            throw new IllegalStateException("Workflow is not active: " + workflow.getStatus());
        }

        // 创建执行记录
        WorkflowExecution execution = new WorkflowExecution();
        execution.setId(UUID.randomUUID().toString());
        execution.setWorkflowId(workflowId);
        execution.setWorkflowVersion(workflow.getVersion());
        execution.setTriggerType(triggerType);
        execution.setTriggerSource(triggerSource);
        execution.setTriggerPayload(triggerPayload);
        execution.setStatus("RUNNING");
        execution.setStartedAt(LocalDateTime.now());

        // 初始化执行上下文
        WorkflowExecutionContext context = new WorkflowExecutionContext();
        context.setExecutionId(execution.getId());
        context.setWorkflowId(workflowId);
        context.setTriggerData(triggerData != null ? triggerData : new HashMap<>());
        context.setVariables(new HashMap<>());
        context.setNodeResults(new HashMap<>());
        context.setCurrentNodeId(null);
        context.setNextNodeIds(new ArrayList<>());

        try {
            execution.setExecutionContext(objectMapper.writeValueAsString(context));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize context", e);
            execution.setExecutionContext("{}");
        }

        execution = executionRepository.save(execution);
        activeExecutions.put(execution.getId(), context);

        // 更新工作流执行计数
        workflow.setExecutionCount(workflow.getExecutionCount() + 1);
        workflowRepository.save(workflow);

        // 异步执行工作流
        executeAsync(execution.getId());

        return execution;
    }

    /**
     * 异步执行工作流
     */
    @Transactional
    public void executeAsync(String executionId) {
        try {
            executeNextNodes(executionId);
        } catch (Exception e) {
            log.error("Workflow execution failed", e);
            failExecution(executionId, e.getMessage(), e.toString());
        }
    }

    /**
     * 执行下一个节点
     */
    @Transactional
    public void executeNextNodes(String executionId) {
        WorkflowExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

        if (!"RUNNING".equals(execution.getStatus())) {
            return;
        }

        WorkflowExecutionContext context = getContext(execution);
        if (context == null) {
            context = parseContext(execution);
            activeExecutions.put(executionId, context);
        }

        // 如果没有起始节点，找到触发器节点
        if (context.getCurrentNodeId() == null) {
            List<WorkflowNode> triggerNodes = nodeRepository.findByWorkflowIdAndNodeType(execution.getWorkflowId(), "TRIGGER");
            if (triggerNodes.isEmpty()) {
                completeExecution(executionId);
                return;
            }
            List<String> nextNodeIds = new ArrayList<>();
            nextNodeIds.add(triggerNodes.get(0).getId());
            context.setNextNodeIds(nextNodeIds);
        } else {
            // 从当前节点的连接中获取下一个节点
            List<WorkflowConnection> outgoing = connectionRepository.findBySourceNodeId(context.getCurrentNodeId());
            if (outgoing.isEmpty()) {
                completeExecution(executionId);
                return;
            }
            context.setNextNodeIds(outgoing.stream().map(WorkflowConnection::getTargetNodeId).collect(Collectors.toList()));
        }

        // 执行下一个节点
        for (String nodeId : context.getNextNodeIds()) {
            WorkflowNode node = nodeRepository.findById(nodeId).orElse(null);
            if (node == null) continue;

            context.setCurrentNodeId(nodeId);
            execution.setCurrentNodeId(nodeId);
            saveContext(execution, context);

            NodeExecutionResult result = executeNode(node, context, execution);

            context.getNodeResults().put(nodeId, result);

            if (!result.isSuccess()) {
                failExecution(executionId, result.getErrorMessage(), result.getErrorDetails());
                return;
            }

            // 如果是结束节点，完成执行
            if ("END".equals(node.getNodeType())) {
                completeExecution(executionId);
                return;
            }

            // 如果节点需要等待（如审批），暂停执行
            if (result.isWaiting()) {
                return;
            }
        }

        // 继续执行下一个节点
        executeNextNodes(executionId);
    }

    /**
     * 执行单个节点
     */
    private NodeExecutionResult executeNode(WorkflowNode node, WorkflowExecutionContext context,
                                            WorkflowExecution execution) {
        NodeExecutionResult result = new NodeExecutionResult();
        result.setNodeId(node.getId());
        result.setNodeType(node.getNodeType());
        result.setNodeSubtype(node.getNodeSubtype());

        try {
            switch (node.getNodeType()) {
                case "TRIGGER":
                    return executeTriggerNode(node, context, result);
                case "CONDITION":
                    return executeConditionNode(node, context, result);
                case "ACTION":
                    return executeActionNode(node, context, result);
                case "NOTIFICATION":
                    return executeNotificationNode(node, context, result);
                case "APPROVAL":
                    return executeApprovalNode(node, context, result, execution);
                case "WAIT":
                    return executeWaitNode(node, context, result);
                case "CC":
                    return executeCcNode(node, context, result);
                case "END":
                    return executeEndNode(node, context, result);
                default:
                    result.setSuccess(true);
                    return result;
            }
        } catch (Exception e) {
            log.error("Node execution failed: " + node.getId(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setErrorDetails(e.toString());
            return result;
        }
    }

    /**
     * 执行触发器节点
     */
    private NodeExecutionResult executeTriggerNode(WorkflowNode node, WorkflowExecutionContext context,
                                                   NodeExecutionResult result) {
        // 触发器节点不执行具体逻辑，只作为流程入口
        result.setSuccess(true);
        Map<String, Object> outputData = new HashMap<>();
        outputData.put("triggerType", node.getNodeSubtype() != null ? node.getNodeSubtype() : "MANUAL");
        outputData.put("triggerTime", LocalDateTime.now().toString());
        result.setOutputData(outputData);
        return result;
    }

    /**
     * 执行条件节点
     */
    private NodeExecutionResult executeConditionNode(WorkflowNode node, WorkflowExecutionContext context,
                                                     NodeExecutionResult result) {
        Map<String, Object> config = parseConfig(node.getConfigJson());
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) config.getOrDefault("conditions", new ArrayList<>());

        boolean matchResult = evaluateConditions(conditions, context);

        result.setSuccess(true);
        Map<String, Object> outputData = new HashMap<>();
        outputData.put("matched", matchResult);
        outputData.put("selectedBranch", matchResult ? "true" : "false");
        result.setOutputData(outputData);

        // 更新上下文中的下一个节点
        context.setNextNodeIds(new ArrayList<>());
        List<WorkflowConnection> outgoing = connectionRepository.findBySourceNodeId(node.getId());

        for (WorkflowConnection conn : outgoing) {
            String label = conn.getLabel();
            if (matchResult && ("true".equals(label) || "YES".equalsIgnoreCase(label) || "DEFAULT".equals(label))) {
                context.getNextNodeIds().add(conn.getTargetNodeId());
            } else if (!matchResult && ("false".equals(label) || "NO".equalsIgnoreCase(label))) {
                context.getNextNodeIds().add(conn.getTargetNodeId());
            }
        }

        return result;
    }

    /**
     * 评估条件
     */
    private boolean evaluateConditions(List<Map<String, Object>> conditions, WorkflowExecutionContext context) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        String logic = "AND";
        for (Map<String, Object> condition : conditions) {
            String field = (String) condition.get("field");
            String operator = (String) condition.get("operator");
            Object value = condition.get("value");

            Object fieldValue = getFieldValue(field, context);
            boolean conditionResult = evaluateSingleCondition(fieldValue, operator, value);

            if ("AND".equalsIgnoreCase(logic) && !conditionResult) {
                return false;
            }
            if ("OR".equalsIgnoreCase(logic) && conditionResult) {
                return true;
            }
        }
        return "OR".equalsIgnoreCase(logic) ? false : true;
    }

    /**
     * 评估单个条件
     */
    private boolean evaluateSingleCondition(Object fieldValue, String operator, Object compareValue) {
        if (fieldValue == null) {
            return "IS_NULL".equals(operator) || "IS_EMPTY".equals(operator);
        }

        switch (operator) {
            case "EQUALS":
                return fieldValue.toString().equals(compareValue.toString());
            case "NOT_EQUALS":
                return !fieldValue.toString().equals(compareValue.toString());
            case "CONTAINS":
                return fieldValue.toString().contains(compareValue.toString());
            case "NOT_CONTAINS":
                return !fieldValue.toString().contains(compareValue.toString());
            case "STARTS_WITH":
                return fieldValue.toString().startsWith(compareValue.toString());
            case "ENDS_WITH":
                return fieldValue.toString().endsWith(compareValue.toString());
            case "GREATER_THAN":
                return compareNumbers(fieldValue, compareValue) > 0;
            case "LESS_THAN":
                return compareNumbers(fieldValue, compareValue) < 0;
            case "GREATER_EQUAL":
                return compareNumbers(fieldValue, compareValue) >= 0;
            case "LESS_EQUAL":
                return compareNumbers(fieldValue, compareValue) <= 0;
            case "IS_NULL":
            case "IS_EMPTY":
                return fieldValue == null || fieldValue.toString().isEmpty();
            case "IS_NOT_NULL":
            case "IS_NOT_EMPTY":
                return fieldValue != null && !fieldValue.toString().isEmpty();
            default:
                return false;
        }
    }

    private int compareNumbers(Object a, Object b) {
        try {
            double numA = Double.parseDouble(a.toString());
            double numB = Double.parseDouble(b.toString());
            return Double.compare(numA, numB);
        } catch (NumberFormatException e) {
            return a.toString().compareTo(b.toString());
        }
    }

    /**
     * 获取字段值（支持嵌套和上下文变量）
     */
    private Object getFieldValue(String field, WorkflowExecutionContext context) {
        if (field == null) return null;

        // 检查是否是上下文变量
        if (field.startsWith("{{") && field.endsWith("}}")) {
            String varName = field.substring(2, field.length() - 2).trim();
            return context.getVariables().getOrDefault(varName,
                    context.getTriggerData().get(varName));
        }

        return context.getVariables().get(field);
    }

    /**
     * 执行动作节点
     */
    private NodeExecutionResult executeActionNode(WorkflowNode node, WorkflowExecutionContext context,
                                                  NodeExecutionResult result) {
        Map<String, Object> config = parseConfig(node.getConfigJson());
        String actionType = node.getNodeSubtype();

        Map<String, Object> output = new HashMap<>();

        switch (actionType) {
            case "CREATE_TASK":
                // 创建任务
                output.put("taskId", UUID.randomUUID().toString());
                output.put("taskName", config.getOrDefault("taskName", "自动化任务"));
                break;

            case "UPDATE_FIELD":
                // 更新字段
                String fieldName = (String) config.get("fieldName");
                Object fieldValue = config.get("fieldValue");
                context.getVariables().put(fieldName, fieldValue);
                output.put("updatedField", fieldName);
                output.put("newValue", fieldValue);
                break;

            case "SEND_EMAIL":
                // 发送邮件
                output.put("emailId", UUID.randomUUID().toString());
                output.put("recipient", config.getOrDefault("recipient", ""));
                output.put("subject", config.getOrDefault("subject", ""));
                break;

            case "CREATE_RECORD":
                // 创建记录
                String recordType = (String) config.getOrDefault("recordType", "Lead");
                output.put("recordId", UUID.randomUUID().toString());
                output.put("recordType", recordType);
                break;

            case "WEBHOOK":
                // 调用Webhook
                output.put("webhookUrl", config.getOrDefault("url", ""));
                output.put("status", "INVOKED");
                break;

            default:
                log.warn("Unknown action type: {}", actionType);
        }

        result.setSuccess(true);
        result.setOutputData(output);
        return result;
    }

    /**
     * 执行通知节点
     */
    private NodeExecutionResult executeNotificationNode(WorkflowNode node, WorkflowExecutionContext context,
                                                        NodeExecutionResult result) {
        Map<String, Object> config = parseConfig(node.getConfigJson());
        String notificationType = node.getNodeSubtype();

        Map<String, Object> output = new HashMap<>();

        switch (notificationType) {
            case "EMAIL":
                output.put("channel", "EMAIL");
                output.put("recipient", config.getOrDefault("to", ""));
                output.put("subject", config.getOrDefault("subject", ""));
                output.put("messageId", UUID.randomUUID().toString());
                break;

            case "WECHAT_WORK":
                // 企业微信通知
                output.put("channel", "WECHAT_WORK");
                output.put("agentId", config.getOrDefault("agentId", ""));
                output.put("userIds", config.getOrDefault("userIds", ""));
                output.put("messageId", UUID.randomUUID().toString());
                break;

            case "DINGTALK":
                // 钉钉通知
                output.put("channel", "DINGTALK");
                output.put("webhook", config.getOrDefault("webhook", ""));
                output.put("messageId", UUID.randomUUID().toString());
                break;

            case "IN_APP":
                // 站内通知
                output.put("channel", "IN_APP");
                output.put("title", config.getOrDefault("title", ""));
                output.put("recipientUserId", config.getOrDefault("recipientUserId", ""));
                output.put("notificationId", UUID.randomUUID().toString());
                break;

            case "SMS":
                // 短信通知（国内特色）
                output.put("channel", "SMS");
                output.put("phoneNumber", config.getOrDefault("phoneNumber", ""));
                output.put("messageId", UUID.randomUUID().toString());
                break;

            default:
                log.warn("Unknown notification type: {}", notificationType);
        }

        result.setSuccess(true);
        result.setOutputData(output);
        return result;
    }

    /**
     * 执行审批节点
     */
    private NodeExecutionResult executeApprovalNode(WorkflowNode node, WorkflowExecutionContext context,
                                                    NodeExecutionResult result, WorkflowExecution execution) {
        Map<String, Object> config = parseConfig(node.getConfigJson());
        String approvalType = node.getNodeSubtype();

        // 查找审批配置
        ApprovalNode approvalNode = approvalNodeRepository.findByWorkflowNodeId(node.getId())
                .orElse(null);

        if (approvalNode == null) {
            result.setSuccess(false);
            result.setErrorMessage("Approval configuration not found");
            return result;
        }

        Map<String, Object> output = new HashMap<>();
        output.put("approvalId", approvalNode.getId());
        output.put("approvalType", approvalType);
        output.put("status", "PENDING");

        // 根据审批类型处理
        switch (approvalType) {
            case "SINGLE":
                // 单人审批 - 查找审批人
                String approverId = (String) config.getOrDefault("approverId", "");
                if (approverId.isEmpty() && approvalNode.getApproverIds() != null) {
                    approverId = approvalNode.getApproverIds().split(",")[0];
                }
                output.put("approverId", approverId);
                break;

            case "SERIAL":
                // 逐级审批
                output.put("approvalOrder", approvalNode.getApproverIds() != null ?
                        approvalNode.getApproverIds().split(",") : new String[]{});
                output.put("currentLevel", 0);
                break;

            case "PARALLEL":
                // 会签审批
                String[] approvers = approvalNode.getApproverIds() != null ?
                        approvalNode.getApproverIds().split(",") : new String[]{};
                output.put("approverIds", approvers);
                output.put("requiredApprovals", approvers.length);
                output.put("currentApprovals", 0);
                break;
        }

        // 记录等待状态
        context.getVariables().put("approval_" + node.getId(), output);
        result.setSuccess(true);
        result.setWaiting(true);
        result.setOutputData(output);

        return result;
    }

    /**
     * 处理审批回调
     */
    @Transactional
    public void handleApprovalCallback(String executionId, String nodeId, String action,
                                       String approverId, String comments) {
        WorkflowExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found"));

        WorkflowExecutionContext context = parseContext(execution);
        WorkflowNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Node not found"));

        Map<String, Object> approvalData = (Map<String, Object>) context.getVariables()
                .getOrDefault("approval_" + nodeId, new HashMap<>());

        if ("APPROVE".equals(action)) {
            approvalData.put("status", "APPROVED");
            approvalData.put("approverId", approverId);
            approvalData.put("approvedAt", LocalDateTime.now().toString());
            approvalData.put("comments", comments);

            // 继续执行流程
            context.setCurrentNodeId(nodeId);
            saveContext(execution, context);
            executeNextNodes(executionId);
        } else if ("REJECT".equals(action)) {
            approvalData.put("status", "REJECTED");
            approvalData.put("approverId", approverId);
            approvalData.put("rejectedAt", LocalDateTime.now().toString());
            approvalData.put("rejectionReason", comments);

            // 工作流失败
            failExecution(executionId, "Approval rejected by " + approverId, comments);
        }
    }

    /**
     * 执行等待节点
     */
    private NodeExecutionResult executeWaitNode(WorkflowNode node, WorkflowExecutionContext context,
                                                 NodeExecutionResult result) {
        Map<String, Object> config = parseConfig(node.getConfigJson());
        String waitType = node.getNodeSubtype();

        switch (waitType) {
            case "DELAY":
                // 延时等待 - 在实际实现中应该使用定时任务
                long delaySeconds = Long.parseLong(config.getOrDefault("delaySeconds", "0").toString());
                result.setSuccess(true);
                result.setWaiting(true);
                Map<String, Object> outputData = new HashMap<>();
                outputData.put("waitType", "DELAY");
                outputData.put("delaySeconds", delaySeconds);
                outputData.put("resumeAt", LocalDateTime.now().plusSeconds(delaySeconds).toString());
                result.setOutputData(outputData);
                break;

            case "CONDITION":
                // 条件等待 - 检查条件是否满足
                List<Map<String, Object>> conditions = (List<Map<String, Object>>) config.get("conditions");
                boolean satisfied = evaluateConditions(conditions, context);

                if (satisfied) {
                    result.setSuccess(true);
                    result.setWaiting(false);
                } else {
                    result.setSuccess(true);
                    result.setWaiting(true);
                }
                Map<String, Object> conditionOutput = new HashMap<>();
                conditionOutput.put("conditionSatisfied", satisfied);
                result.setOutputData(conditionOutput);
                break;
        }

        return result;
    }

    /**
     * 执行抄送节点
     */
    private NodeExecutionResult executeCcNode(WorkflowNode node, WorkflowExecutionContext context,
                                               NodeExecutionResult result) {
        Map<String, Object> config = parseConfig(node.getConfigJson());
        String ccType = node.getNodeSubtype();

        Map<String, Object> output = new HashMap<>();
        output.put("ccType", ccType);
        output.put("ccUsers", config.getOrDefault("ccUserIds", ""));

        result.setSuccess(true);
        result.setOutputData(output);
        return result;
    }

    /**
     * 执行结束节点
     */
    private NodeExecutionResult executeEndNode(WorkflowNode node, WorkflowExecutionContext context,
                                               NodeExecutionResult result) {
        result.setSuccess(true);
        Map<String, Object> outputData = new HashMap<>();
        outputData.put("endType", node.getNodeSubtype() != null ? node.getNodeSubtype() : "NORMAL");
        outputData.put("endTime", LocalDateTime.now().toString());
        result.setOutputData(outputData);
        return result;
    }

    /**
     * 完成执行
     */
    @Transactional
    public void completeExecution(String executionId) {
        WorkflowExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found"));

        execution.setStatus("COMPLETED");
        execution.setCompletedAt(LocalDateTime.now());
        execution.setCurrentNodeId(null);

        // 计算执行时长
        if (execution.getStartedAt() != null) {
            long durationMs = java.time.Duration.between(
                    execution.getStartedAt(), execution.getCompletedAt()).toMillis();
            execution.setExecutionDurationMs((int) durationMs);
        }

        executionRepository.save(execution);
        activeExecutions.remove(executionId);

        log.info("Workflow execution completed: {}", executionId);
    }

    /**
     * 标记执行失败
     */
    @Transactional
    public void failExecution(String executionId, String errorMessage, String errorDetails) {
        WorkflowExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found"));

        execution.setStatus("FAILED");
        execution.setCompletedAt(LocalDateTime.now());
        execution.setErrorMessage(errorMessage);
        execution.setErrorDetails(errorDetails);

        if (execution.getStartedAt() != null) {
            long durationMs = java.time.Duration.between(
                    execution.getStartedAt(), execution.getCompletedAt()).toMillis();
            execution.setExecutionDurationMs((int) durationMs);
        }

        executionRepository.save(execution);
        activeExecutions.remove(executionId);

        log.error("Workflow execution failed: {} - {}", executionId, errorMessage);
    }

    /**
     * 取消执行
     */
    @Transactional
    public void cancelExecution(String executionId, String cancelledBy) {
        WorkflowExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found"));

        if (!"RUNNING".equals(execution.getStatus())) {
            throw new IllegalStateException("Cannot cancel execution with status: " + execution.getStatus());
        }

        execution.setStatus("CANCELLED");
        execution.setCompletedAt(LocalDateTime.now());
        execution.setErrorMessage("Cancelled by " + cancelledBy);

        if (execution.getStartedAt() != null) {
            long durationMs = java.time.Duration.between(
                    execution.getStartedAt(), execution.getCompletedAt()).toMillis();
            execution.setExecutionDurationMs((int) durationMs);
        }

        executionRepository.save(execution);
        activeExecutions.remove(executionId);

        log.info("Workflow execution cancelled: {} by {}", executionId, cancelledBy);
    }

    /**
     * 获取执行详情
     */
    public Map<String, Object> getExecutionDetail(String executionId) {
        WorkflowExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found"));

        Map<String, Object> detail = new HashMap<>();
        detail.put("execution", execution);

        // 解析上下文
        WorkflowExecutionContext context = parseContext(execution);
        detail.put("context", context);

        // 获取节点结果
        if (context != null && context.getNodeResults() != null) {
            detail.put("nodeResults", context.getNodeResults());
        }

        // 获取工作流信息
        WorkflowDefinition workflow = workflowRepository.findById(execution.getWorkflowId()).orElse(null);
        detail.put("workflow", workflow);

        return detail;
    }

    /**
     * 获取执行历史
     */
    public List<WorkflowExecution> getExecutionHistory(String workflowId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (status != null && !status.isEmpty()) {
            return executionRepository.findByWorkflowIdAndStatusOrderByStartedAtDesc(workflowId, status, pageable);
        }
        return executionRepository.findByWorkflowIdOrderByStartedAtDesc(workflowId, pageable);
    }

    /**
     * 重试失败的执行
     */
    @Transactional
    public WorkflowExecution retryExecution(String executionId) {
        WorkflowExecution oldExecution = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found"));

        if (!"FAILED".equals(oldExecution.getStatus())) {
            throw new IllegalStateException("Can only retry failed executions");
        }

        // 创建新的执行
        return startExecution(
                oldExecution.getWorkflowId(),
                oldExecution.getTriggerType(),
                oldExecution.getTriggerSource(),
                oldExecution.getTriggerPayload(),
                null
        );
    }

    // ========== Helper Methods ==========

    private Map<String, Object> parseConfig(String configJson) {
        if (configJson == null || configJson.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(configJson, Map.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse config JSON", e);
            return new HashMap<>();
        }
    }

    private WorkflowExecutionContext getContext(WorkflowExecution execution) {
        return activeExecutions.get(execution.getId());
    }

    private WorkflowExecutionContext parseContext(WorkflowExecution execution) {
        if (execution.getExecutionContext() == null || execution.getExecutionContext().isEmpty()) {
            return new WorkflowExecutionContext();
        }
        try {
            return objectMapper.readValue(execution.getExecutionContext(), WorkflowExecutionContext.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse context JSON", e);
            return new WorkflowExecutionContext();
        }
    }

    private void saveContext(WorkflowExecution execution, WorkflowExecutionContext context) {
        try {
            execution.setExecutionContext(objectMapper.writeValueAsString(context));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize context", e);
        }
    }

    /**
     * 执行上下文内部类
     */
    public static class WorkflowExecutionContext {
        private String executionId;
        private String workflowId;
        private String currentNodeId;
        private List<String> nextNodeIds;
        private Map<String, Object> triggerData;
        private Map<String, Object> variables;
        private Map<String, NodeExecutionResult> nodeResults;
        private LocalDateTime startedAt;

        public String getExecutionId() { return executionId; }
        public void setExecutionId(String executionId) { this.executionId = executionId; }

        public String getWorkflowId() { return workflowId; }
        public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

        public String getCurrentNodeId() { return currentNodeId; }
        public void setCurrentNodeId(String currentNodeId) { this.currentNodeId = currentNodeId; }

        public List<String> getNextNodeIds() {
            if (nextNodeIds == null) nextNodeIds = new ArrayList<>();
            return nextNodeIds;
        }
        public void setNextNodeIds(List<String> nextNodeIds) { this.nextNodeIds = nextNodeIds; }

        public Map<String, Object> getTriggerData() {
            if (triggerData == null) triggerData = new HashMap<>();
            return triggerData;
        }
        public void setTriggerData(Map<String, Object> triggerData) { this.triggerData = triggerData; }

        public Map<String, Object> getVariables() {
            if (variables == null) variables = new HashMap<>();
            return variables;
        }
        public void setVariables(Map<String, Object> variables) { this.variables = variables; }

        public Map<String, NodeExecutionResult> getNodeResults() {
            if (nodeResults == null) nodeResults = new HashMap<>();
            return nodeResults;
        }
        public void setNodeResults(Map<String, NodeExecutionResult> nodeResults) { this.nodeResults = nodeResults; }

        public LocalDateTime getStartedAt() { return startedAt; }
        public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    }

    /**
     * 节点执行结果内部类
     */
    public static class NodeExecutionResult {
        private String nodeId;
        private String nodeType;
        private String nodeSubtype;
        private boolean success;
        private boolean waiting;
        private String errorMessage;
        private String errorDetails;
        private Map<String, Object> outputData;

        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }

        public String getNodeType() { return nodeType; }
        public void setNodeType(String nodeType) { this.nodeType = nodeType; }

        public String getNodeSubtype() { return nodeSubtype; }
        public void setNodeSubtype(String nodeSubtype) { this.nodeSubtype = nodeSubtype; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public boolean isWaiting() { return waiting; }
        public void setWaiting(boolean waiting) { this.waiting = waiting; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public String getErrorDetails() { return errorDetails; }
        public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }

        public Map<String, Object> getOutputData() { return outputData; }
        public void setOutputData(Map<String, Object> outputData) { this.outputData = outputData; }
    }
}
