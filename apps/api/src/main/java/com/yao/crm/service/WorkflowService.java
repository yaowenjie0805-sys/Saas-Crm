package com.yao.crm.service;

import com.yao.crm.entity.*;
import com.yao.crm.repository.*;
import com.yao.crm.enums.WorkflowStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 工作流服务
 * 提供工作流定义、节点、连接管理
 */
@Service
public class WorkflowService {

    private final WorkflowDefinitionRepository workflowRepository;
    private final WorkflowNodeRepository nodeRepository;
    private final WorkflowConnectionRepository connectionRepository;
    private final WorkflowExecutionRepository executionRepository;

    public WorkflowService(
            WorkflowDefinitionRepository workflowRepository,
            WorkflowNodeRepository nodeRepository,
            WorkflowConnectionRepository connectionRepository,
            WorkflowExecutionRepository executionRepository) {
        this.workflowRepository = workflowRepository;
        this.nodeRepository = nodeRepository;
        this.connectionRepository = connectionRepository;
        this.executionRepository = executionRepository;
    }

    /**
     * 创建工作流
     */
    @Transactional(timeout = 30)
    public WorkflowDefinition createWorkflow(String tenantId, String name, String description, String category, String owner) {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(UUID.randomUUID().toString());
        workflow.setTenantId(tenantId);
        workflow.setName(name);
        workflow.setDescription(description);
        workflow.setCategory(category);
        workflow.setOwner(owner);
        workflow.setStatus(WorkflowStatus.DRAFT.name());
        workflow.setVersion(1);
        workflow.setIsSystem(false);
        workflow.setExecutionCount(0);
        workflow.setCreatedAt(LocalDateTime.now());
        workflow.setUpdatedAt(LocalDateTime.now());

        return workflowRepository.save(workflow);
    }

    /**
     * 获取工作流详情
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getWorkflowDetail(String tenantId, String workflowId) {
        WorkflowDefinition workflow = workflowRepository.findByIdAndTenantId(workflowId, tenantId).orElse(null);
        if (workflow == null) {
            return null;
        }

        List<WorkflowNode> nodes = nodeRepository.findByWorkflowIdOrderByPositionXAscPositionYAsc(workflowId);
        List<WorkflowConnection> connections = connectionRepository.findByWorkflowId(workflowId);

        Map<String, Object> detail = new HashMap<>();
        detail.put("workflow", workflow);
        detail.put("nodes", nodes);
        detail.put("connections", connections);

        return detail;
    }

    /**
     * 添加节点
     */
    @Transactional(timeout = 30)
    public WorkflowNode addNode(String tenantId, String workflowId, String nodeType, String nodeSubtype, String name,
                                int positionX, int positionY, String configJson) {
        requireWorkflow(tenantId, workflowId);
        WorkflowNode node = new WorkflowNode();
        node.setId(UUID.randomUUID().toString());
        node.setWorkflowId(workflowId);
        node.setNodeType(nodeType);
        node.setNodeSubtype(nodeSubtype);
        node.setName(name);
        node.setPositionX(positionX);
        node.setPositionY(positionY);
        node.setConfigJson(configJson);
        node.setConfigValidation("VALID");
        node.setCreatedAt(LocalDateTime.now());
        node.setUpdatedAt(LocalDateTime.now());

        return nodeRepository.save(node);
    }

    /**
     * 添加连接
     */
    @Transactional(timeout = 30)
    public WorkflowConnection addConnection(String tenantId, String workflowId, String sourceNodeId, String targetNodeId,
                                            String connectionType, String label) {
        requireWorkflow(tenantId, workflowId);
        WorkflowConnection connection = new WorkflowConnection();
        connection.setId(UUID.randomUUID().toString());
        connection.setWorkflowId(workflowId);
        connection.setSourceNodeId(sourceNodeId);
        connection.setTargetNodeId(targetNodeId);
        connection.setConnectionType(connectionType != null ? connectionType : "DEFAULT");
        connection.setLabel(label);
        connection.setDisplayOrder(0);
        connection.setCreatedAt(LocalDateTime.now());

        return connectionRepository.save(connection);
    }

    /**
     * 验证工作流
     */
    @Transactional(readOnly = true)
    public Map<String, Object> validateWorkflow(String tenantId, String workflowId) {
        requireWorkflow(tenantId, workflowId);
        List<WorkflowNode> nodes = nodeRepository.findByWorkflowIdOrderByPositionXAscPositionYAsc(workflowId);
        List<WorkflowConnection> connections = connectionRepository.findByWorkflowId(workflowId);

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 检查是否有触发器节点
        boolean hasTrigger = nodes.stream().anyMatch(n -> "TRIGGER".equals(n.getNodeType()));
        if (!hasTrigger) {
            errors.add("工作流必须包含至少一个触发器节点");
        }

        // 检查是否有结束节点
        boolean hasEnd = nodes.stream().anyMatch(n -> "END".equals(n.getNodeType()));
        if (!hasEnd) {
            warnings.add("建议添加结束节点");
        }

        // 检查孤立节点
        Set<String> connectedNodes = new HashSet<>();
        connections.forEach(c -> {
            connectedNodes.add(c.getSourceNodeId());
            connectedNodes.add(c.getTargetNodeId());
        });
        nodes.forEach(n -> {
            if (!connectedNodes.contains(n.getId()) && nodes.size() > 1) {
                warnings.add("节点 '" + n.getName() + "' 未连接到任何其他节点");
            }
        });

        // 检查无效配置
        nodes.forEach(n -> {
            if ("INVALID".equals(n.getConfigValidation())) {
                errors.add("节点 '" + n.getName() + "' 配置无效: " + n.getValidationMessage());
            }
        });

        Map<String, Object> result = new HashMap<>();
        result.put("valid", errors.isEmpty());
        result.put("errors", errors);
        result.put("warnings", warnings);

        return result;
    }

    /**
     * 激活工作流
     */
    @Transactional(timeout = 30)
    public WorkflowDefinition activateWorkflow(String tenantId, String workflowId, String activatedBy) {
        WorkflowDefinition workflow = requireWorkflow(tenantId, workflowId);

        // 验证工作流
        Map<String, Object> validation = validateWorkflow(tenantId, workflowId);
        if (!(Boolean) validation.get("valid")) {
            throw new IllegalStateException("Workflow validation failed: " + validation.get("errors"));
        }

        workflow.setStatus(WorkflowStatus.ACTIVE.name());
        workflow.setActivatedAt(LocalDateTime.now());
        workflow.setPublishedBy(activatedBy);

        return workflowRepository.save(workflow);
    }

    /**
     * 停用工作流
     */
    @Transactional(timeout = 30)
    public WorkflowDefinition deactivateWorkflow(String tenantId, String workflowId) {
        WorkflowDefinition workflow = requireWorkflow(tenantId, workflowId);

        // 检查是否有正在运行的执行
        List<WorkflowExecution> running = executionRepository.findRunningByWorkflowId(workflowId);
        if (!running.isEmpty()) {
            throw new IllegalStateException("Cannot deactivate workflow with running executions");
        }

        workflow.setStatus(WorkflowStatus.PAUSED.name());
        return workflowRepository.save(workflow);
    }

    /**
     * 删除工作流
     */
    @Transactional(timeout = 30)
    public void deleteWorkflow(String tenantId, String workflowId) {
        requireWorkflow(tenantId, workflowId);
        // 先删除关联数据
        connectionRepository.deleteByWorkflowId(workflowId);
        nodeRepository.deleteByWorkflowId(workflowId);
        workflowRepository.deleteByIdAndTenantId(workflowId, tenantId);
    }

    /**
     * 获取工作流列表
     */
    @Transactional(readOnly = true)
    public List<WorkflowDefinition> getWorkflows(String tenantId, String status, String category) {
        if (status != null && !status.isEmpty()) {
            return workflowRepository.findByTenantIdAndStatus(tenantId, status);
        }
        if (category != null && !category.isEmpty()) {
            return workflowRepository.findByTenantIdAndCategory(tenantId, category);
        }
        return workflowRepository.findByTenantId(tenantId);
    }

    /**
     * 获取工作流统计
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getWorkflowStats(String tenantId, String workflowId) {
        WorkflowDefinition workflow = workflowRepository.findByIdAndTenantId(workflowId, tenantId).orElse(null);
        if (workflow == null) {
            return null;
        }

        long totalExecutions = executionRepository.countByWorkflowIdAndStatus(workflowId, "COMPLETED");
        long failedExecutions = executionRepository.countByWorkflowIdAndStatus(workflowId, "FAILED");
        long runningExecutions = executionRepository.countByWorkflowIdAndStatus(workflowId, "RUNNING");

        Map<String, Object> stats = new HashMap<>();
        stats.put("workflow", workflow);
        stats.put("totalExecutions", totalExecutions);
        stats.put("failedExecutions", failedExecutions);
        stats.put("runningExecutions", runningExecutions);
        stats.put("successRate", totalExecutions > 0 ? (totalExecutions - failedExecutions) * 100.0 / totalExecutions : 0);

        return stats;
    }

    /**
     * 获取节点类型选项
     */
    @Transactional(readOnly = true)
    public Map<String, List<Map<String, String>>> getNodeTypes() {
        Map<String, List<Map<String, String>>> nodeTypes = new HashMap<>();

        // 触发器类型
        List<Map<String, String>> triggerTypes = new ArrayList<>();
        triggerTypes.add(createNodeType("RECORD_CREATED", "记录创建", "📝"));
        triggerTypes.add(createNodeType("RECORD_UPDATED", "记录更新", "✏️"));
        triggerTypes.add(createNodeType("FIELD_CHANGED", "字段变更", "🔄"));
        triggerTypes.add(createNodeType("MANUAL", "手动触发", "👆"));
        nodeTypes.put("TRIGGER", triggerTypes);

        // 动作类型
        List<Map<String, String>> actionTypes = new ArrayList<>();
        actionTypes.add(createNodeType("CREATE_TASK", "创建任务", "✅"));
        actionTypes.add(createNodeType("UPDATE_FIELD", "更新字段", "📊"));
        actionTypes.add(createNodeType("SEND_EMAIL", "发送邮件", "📧"));
        actionTypes.add(createNodeType("CREATE_RECORD", "创建记录", "➕"));
        nodeTypes.put("ACTION", actionTypes);

        // 条件类型
        List<Map<String, String>> conditionTypes = new ArrayList<>();
        conditionTypes.add(createNodeType("IF", "条件判断", "❓"));
        conditionTypes.add(createNodeType("SWITCH", "多条件分支", "🔀"));
        nodeTypes.put("CONDITION", conditionTypes);

        // 通知类型
        List<Map<String, String>> notificationTypes = new ArrayList<>();
        notificationTypes.add(createNodeType("EMAIL", "邮件通知", "📧"));
        notificationTypes.add(createNodeType("WECHAT_WORK", "企业微信", "💬")); // 国内特色
        notificationTypes.add(createNodeType("DINGTALK", "钉钉通知", "🔔")); // 国内特色
        notificationTypes.add(createNodeType("IN_APP", "站内通知", "🔔"));
        nodeTypes.put("NOTIFICATION", notificationTypes);

        // 审批类型（国内特色）
        List<Map<String, String>> approvalTypes = new ArrayList<>();
        approvalTypes.add(createNodeType("SINGLE", "单人审批", "👤"));
        approvalTypes.add(createNodeType("SERIAL", "逐级审批", "📋"));
        approvalTypes.add(createNodeType("PARALLEL", "会签审批", "👥"));
        nodeTypes.put("APPROVAL", approvalTypes);

        // 等待类型
        List<Map<String, String>> waitTypes = new ArrayList<>();
        waitTypes.add(createNodeType("DELAY", "延时等待", "⏰"));
        waitTypes.add(createNodeType("CONDITION", "条件等待", "⏳"));
        nodeTypes.put("WAIT", waitTypes);

        // 抄送类型（国内特色）
        List<Map<String, String>> ccTypes = new ArrayList<>();
        ccTypes.add(createNodeType("EMAIL_CC", "邮件抄送", "📧"));
        ccTypes.add(createNodeType("WECHAT_CC", "微信抄送", "💬"));
        nodeTypes.put("CC", ccTypes);

        // 辅助类型
        List<Map<String, String>> auxiliaryTypes = new ArrayList<>();
        auxiliaryTypes.add(createNodeType("START", "开始", "▶️"));
        auxiliaryTypes.add(createNodeType("END", "结束", "🏁"));
        nodeTypes.put("AUXILIARY", auxiliaryTypes);

        return nodeTypes;
    }

    private Map<String, String> createNodeType(String value, String label, String icon) {
        Map<String, String> type = new HashMap<>();
        type.put("value", value);
        type.put("label", label);
        type.put("icon", icon);
        return type;
    }

    private WorkflowDefinition requireWorkflow(String tenantId, String workflowId) {
        return workflowRepository.findByIdAndTenantId(workflowId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found"));
    }
}
