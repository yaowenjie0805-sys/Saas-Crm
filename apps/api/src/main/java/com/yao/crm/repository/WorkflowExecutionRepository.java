package com.yao.crm.repository;

import com.yao.crm.entity.WorkflowExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, String> {

    java.util.Optional<WorkflowExecution> findByIdAndTenantId(String id, String tenantId);

    List<WorkflowExecution> findByWorkflowId(String workflowId);

    List<WorkflowExecution> findByWorkflowIdAndStatus(String workflowId, String status);

    @Query("SELECT e FROM WorkflowExecution e WHERE e.workflowId = :workflowId ORDER BY e.startedAt DESC")
    List<WorkflowExecution> findByWorkflowIdOrderByStartedAtDesc(@Param("workflowId") String workflowId);

    @Query("SELECT e FROM WorkflowExecution e WHERE e.workflowId = :workflowId AND e.status = 'RUNNING'")
    List<WorkflowExecution> findRunningByWorkflowId(@Param("workflowId") String workflowId);

    // 分页查询
    @Query(value = "SELECT e FROM WorkflowExecution e WHERE e.workflowId = :workflowId ORDER BY e.startedAt DESC",
           countQuery = "SELECT COUNT(e) FROM WorkflowExecution e WHERE e.workflowId = :workflowId")
    List<WorkflowExecution> findByWorkflowIdOrderByStartedAtDesc(@Param("workflowId") String workflowId,
                                                                org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT e FROM WorkflowExecution e WHERE e.workflowId = :workflowId AND e.status = :status ORDER BY e.startedAt DESC",
           countQuery = "SELECT COUNT(e) FROM WorkflowExecution e WHERE e.workflowId = :workflowId AND e.status = :status")
    List<WorkflowExecution> findByWorkflowIdAndStatusOrderByStartedAtDesc(@Param("workflowId") String workflowId,
                                                                          @Param("status") String status,
                                                                          org.springframework.data.domain.Pageable pageable);

    @Query("SELECT e FROM WorkflowExecution e WHERE e.tenantId = :tenantId ORDER BY e.startedAt DESC")
    List<WorkflowExecution> findRecentByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT e FROM WorkflowExecution e WHERE e.startedAt >= :since AND e.startedAt <= :until")
    List<WorkflowExecution> findByDateRange(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until);

    long countByWorkflowIdAndStatus(String workflowId, String status);

    @Query("SELECT COUNT(e) FROM WorkflowExecution e WHERE e.workflowId = :workflowId AND e.status = :status AND e.startedAt >= :since")
    long countRecentByStatus(@Param("workflowId") String workflowId, @Param("status") String status, @Param("since") LocalDateTime since);
}
