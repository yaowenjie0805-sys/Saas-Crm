package com.yao.crm.repository;

import com.yao.crm.entity.ApprovalInstance;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import java.time.LocalDateTime;

public interface ApprovalInstanceRepository extends JpaRepository<ApprovalInstance, String> {
    Optional<ApprovalInstance> findByIdAndTenantId(String id, String tenantId);
    long countByTenantId(String tenantId);
    List<ApprovalInstance> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    List<ApprovalInstance> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);
    @Query("select upper(coalesce(i.status, 'UNKNOWN')), count(i) from ApprovalInstance i where i.tenantId = :tenantId group by upper(coalesce(i.status, 'UNKNOWN'))")
    List<Object[]> countGroupedByStatus(@Param("tenantId") String tenantId);
    @Query("select upper(coalesce(i.bizType, 'UNKNOWN')), count(i) from ApprovalInstance i where i.tenantId = :tenantId group by upper(coalesce(i.bizType, 'UNKNOWN'))")
    List<Object[]> countGroupedByBizType(@Param("tenantId") String tenantId);
    List<ApprovalInstance> findByTenantIdAndBizTypeIgnoreCaseAndBizIdInOrderByCreatedAtDesc(
            String tenantId, String bizType, Collection<String> bizIds);
    List<ApprovalInstance> findByTenantIdAndStatusInOrderByCreatedAtDesc(
            String tenantId, Collection<String> statuses);
    List<ApprovalInstance> findByTenantIdAndSubmitterInAndStatusInOrderByCreatedAtDesc(
            String tenantId, Collection<String> submitters, Collection<String> statuses);
    List<ApprovalInstance> findByTenantIdAndStatusInAndUpdatedAtBetweenOrderByCreatedAtDesc(
            String tenantId, Collection<String> statuses, LocalDateTime fromAt, LocalDateTime toAt);
    List<ApprovalInstance> findByTenantIdAndSubmitterInAndStatusInAndUpdatedAtBetweenOrderByCreatedAtDesc(
            String tenantId, Collection<String> submitters, Collection<String> statuses, LocalDateTime fromAt, LocalDateTime toAt);
    Optional<ApprovalInstance> findTopByTenantIdAndBizTypeAndBizIdAndStatusInOrderByCreatedAtDesc(
            String tenantId, String bizType, String bizId, Collection<String> statuses);
}
