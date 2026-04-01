package com.yao.crm.repository;

import com.yao.crm.entity.ApprovalTask;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApprovalTaskRepository extends JpaRepository<ApprovalTask, String> {
    // Tenant-scoped core reads
    List<ApprovalTask> findByInstanceIdAndTenantIdOrderBySeqAsc(String instanceId, String tenantId);
    Optional<ApprovalTask> findByIdAndTenantId(String id, String tenantId);
    // Kept for backward compatibility in existing tests/call contracts.
    List<ApprovalTask> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    List<ApprovalTask> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);
    List<ApprovalTask> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, String status, Pageable pageable);
    List<ApprovalTask> findByTenantIdAndStatusInOrderByCreatedAtDesc(String tenantId, Collection<String> statuses, Pageable pageable);
    long countByTenantId(String tenantId);
    long countByTenantIdAndStatusIgnoreCase(String tenantId, String status);
    long countByTenantIdAndStatusIn(String tenantId, Collection<String> statuses);
    long countByTenantIdAndStatusInAndDeadlineAtBefore(String tenantId, Collection<String> statuses, LocalDateTime deadlineAt);

    // Tenant-scoped aggregates for approval dashboard
    @Query("select upper(coalesce(t.status, 'UNKNOWN')), count(t) from ApprovalTask t where t.tenantId = :tenantId group by upper(coalesce(t.status, 'UNKNOWN'))")
    List<Object[]> countGroupedByStatus(@Param("tenantId") String tenantId);
    @Query("select upper(coalesce(t.approverRole, 'UNKNOWN')), count(t) from ApprovalTask t where t.tenantId = :tenantId and upper(coalesce(t.status, '')) in ('PENDING', 'WAITING') group by upper(coalesce(t.approverRole, 'UNKNOWN'))")
    List<Object[]> countBacklogByRole(@Param("tenantId") String tenantId);
    @Query("select count(t) from ApprovalTask t where t.tenantId = :tenantId and upper(coalesce(t.status, '')) = 'PENDING' and t.deadlineAt is not null and t.deadlineAt < :now")
    long countOverduePendingTasks(@Param("tenantId") String tenantId, @Param("now") LocalDateTime now);
    @Query("select count(t) from ApprovalTask t where t.tenantId = :tenantId and (upper(coalesce(t.status, '')) = 'ESCALATED' or coalesce(t.escalationLevel, 0) > 0)")
    long countEscalatedTasks(@Param("tenantId") String tenantId);

    // SLA/escalation scanning
    List<ApprovalTask> findByStatusAndDeadlineAtBefore(String status, LocalDateTime deadlineAt);
    // Kept for tenant-safe drill-down even though current flow prefers batch tenant lookup.
    List<ApprovalTask> findByTenantIdAndEscalationSourceTaskIdOrderByCreatedAtDesc(String tenantId, String escalationSourceTaskId);
    List<ApprovalTask> findByEscalationSourceTaskIdIn(List<String> escalationSourceTaskIds);
    List<ApprovalTask> findByTenantIdAndEscalationSourceTaskIdIn(String tenantId, List<String> escalationSourceTaskIds);

    // Atomic status transitions
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
