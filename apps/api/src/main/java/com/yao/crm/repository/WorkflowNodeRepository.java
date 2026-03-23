package com.yao.crm.repository;

import com.yao.crm.entity.WorkflowNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowNodeRepository extends JpaRepository<WorkflowNode, String> {

    List<WorkflowNode> findByWorkflowIdOrderByPositionXAscPositionYAsc(String workflowId);

    List<WorkflowNode> findByWorkflowIdAndNodeType(String workflowId, String nodeType);

    List<WorkflowNode> findByWorkflowIdAndConfigValidation(String workflowId, String validation);

    void deleteByWorkflowId(String workflowId);

    int countByWorkflowId(String workflowId);
}
