package com.yao.crm.repository;

import com.yao.crm.entity.WorkflowConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowConnectionRepository extends JpaRepository<WorkflowConnection, String> {

    List<WorkflowConnection> findByWorkflowId(String workflowId);

    List<WorkflowConnection> findBySourceNodeId(String sourceNodeId);

    List<WorkflowConnection> findByTargetNodeId(String targetNodeId);

    List<WorkflowConnection> findByWorkflowIdAndConnectionType(String workflowId, String connectionType);

    void deleteByWorkflowId(String workflowId);

    void deleteBySourceNodeId(String sourceNodeId);

    void deleteByTargetNodeId(String targetNodeId);
}
