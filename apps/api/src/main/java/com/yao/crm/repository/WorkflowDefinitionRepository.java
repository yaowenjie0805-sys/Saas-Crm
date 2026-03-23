package com.yao.crm.repository;

import com.yao.crm.entity.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, String> {

    List<WorkflowDefinition> findByTenantId(String tenantId);

    List<WorkflowDefinition> findByTenantIdAndStatus(String tenantId, String status);

    List<WorkflowDefinition> findByTenantIdAndCategory(String tenantId, String category);

    List<WorkflowDefinition> findByTenantIdAndOwner(String tenantId, String owner);

    @Query("SELECT w FROM WorkflowDefinition w WHERE w.tenantId = :tenantId AND w.status = 'ACTIVE'")
    List<WorkflowDefinition> findActiveByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT w FROM WorkflowDefinition w WHERE w.tenantId = :tenantId AND w.executionCount > 0 ORDER BY w.lastExecutedAt DESC")
    List<WorkflowDefinition> findMostExecutedByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT w FROM WorkflowDefinition w WHERE w.tenantId = :tenantId AND w.updatedAt > :since")
    List<WorkflowDefinition> findRecentlyUpdated(@Param("tenantId") String tenantId, @Param("since") LocalDateTime since);

    void deleteByTenantId(String tenantId);
}
