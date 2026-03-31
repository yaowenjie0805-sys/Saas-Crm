package com.yao.crm.repository;

import com.yao.crm.entity.ApprovalTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
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
    List<ApprovalTask> findByEscalationSourceTaskIdIn(List<String> escalationSourceTaskIds);

    @Transactional
    @Modifying
    @Query(value = "UPDATE approval_tasks SET status = :nextStatus, approver_user = :approverUser, comment = :comment, updated_at = CURRENT_TIMESTAMP WHERE id = :id AND tenant_id = :tenantId AND (status = 'PENDING' OR status = 'WAITING')", nativeQuery = true)
    int closeTaskIfOpen(@Param("id") String id,
                        @Param("tenantId") String tenantId,
                        @Param("nextStatus") String nextStatus,
                        @Param("approverUser") String approverUser,
                        @Param("comment") String comment);

    @Transactional
    @Modifying
    @Query(value = "UPDATE approval_tasks SET status = 'TRANSFERRED', approver_user = :approverUser, comment = :comment, updated_at = CURRENT_TIMESTAMP WHERE id = :id AND tenant_id = :tenantId AND (status = 'PENDING' OR status = 'WAITING')", nativeQuery = true)
    int transferTaskIfOpen(@Param("id") String id,
                           @Param("tenantId") String tenantId,
                           @Param("approverUser") String approverUser,
                           @Param("comment") String comment);

    @Transactional
    @Modifying
    @Query(value = "UPDATE approval_tasks SET notified_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = :id AND tenant_id = :tenantId AND (status = 'PENDING' OR status = 'WAITING')", nativeQuery = true)
    int markTaskUrgedIfOpen(@Param("id") String id,
                            @Param("tenantId") String tenantId);
}
