package com.yao.crm.repository;

import com.yao.crm.entity.ApprovalNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 审批节点配置Repository
 */
@Repository
public interface ApprovalNodeRepository extends JpaRepository<ApprovalNode, String> {

    /**
     * 根据工作流节点ID查询审批节点配置
     */
    Optional<ApprovalNode> findByWorkflowNodeId(String workflowNodeId);

    /**
     * 根据审批类型查询
     */
    List<ApprovalNode> findByApprovalType(String approvalType);

    /**
     * 检查工作流节点是否有审批配置
     */
    boolean existsByWorkflowNodeId(String workflowNodeId);

    /**
     * 删除工作流节点的审批配置
     */
    void deleteByWorkflowNodeId(String workflowNodeId);
}
