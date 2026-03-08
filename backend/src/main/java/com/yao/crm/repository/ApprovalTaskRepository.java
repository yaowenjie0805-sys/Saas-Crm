package com.yao.crm.repository;

import com.yao.crm.entity.ApprovalTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ApprovalTaskRepository extends JpaRepository<ApprovalTask, String> {
    List<ApprovalTask> findByInstanceIdAndTenantIdOrderBySeqAsc(String instanceId, String tenantId);
    Optional<ApprovalTask> findByIdAndTenantId(String id, String tenantId);
    List<ApprovalTask> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    List<ApprovalTask> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, String status);
    List<ApprovalTask> findByStatusAndDeadlineAtBefore(String status, LocalDateTime deadlineAt);
    List<ApprovalTask> findByTenantIdAndEscalationSourceTaskIdOrderByCreatedAtDesc(String tenantId, String escalationSourceTaskId);
}
