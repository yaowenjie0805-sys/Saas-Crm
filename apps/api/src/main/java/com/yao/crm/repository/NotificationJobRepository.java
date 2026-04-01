package com.yao.crm.repository;

import com.yao.crm.entity.NotificationJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationJobRepository extends JpaRepository<NotificationJob, String> {
    List<NotificationJob> findByStatusInAndNextRetryAtBeforeOrderByCreatedAtAsc(List<String> statuses, LocalDateTime now);
    Page<NotificationJob> findByStatusInAndNextRetryAtBefore(List<String> statuses, LocalDateTime now, Pageable pageable);
    List<NotificationJob> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    List<NotificationJob> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, String status);
    Page<NotificationJob> findByTenantId(String tenantId, Pageable pageable);
    Page<NotificationJob> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);
    Optional<NotificationJob> findByIdAndTenantId(String id, String tenantId);
    Optional<NotificationJob> findByTenantIdAndDedupeKey(String tenantId, String dedupeKey);
    List<NotificationJob> findByTenantIdAndDedupeKeyIn(String tenantId, Collection<String> dedupeKeys);
    @Query("select upper(coalesce(n.status, 'UNKNOWN')), count(n) from NotificationJob n where n.tenantId = :tenantId group by upper(coalesce(n.status, 'UNKNOWN'))")
    List<Object[]> countGroupedByStatus(@Param("tenantId") String tenantId);
    @Query("select case " +
            "when n.retryCount is null or n.retryCount <= 0 then 'RETRY0' " +
            "when n.retryCount <= 2 then 'RETRY1TO2' " +
            "else 'RETRY3PLUS' end, count(n) " +
            "from NotificationJob n where n.tenantId = :tenantId " +
            "group by case " +
            "when n.retryCount is null or n.retryCount <= 0 then 'RETRY0' " +
            "when n.retryCount <= 2 then 'RETRY1TO2' " +
            "else 'RETRY3PLUS' end")
    List<Object[]> countRetryBuckets(@Param("tenantId") String tenantId);
    long countByTenantIdAndStatusIn(String tenantId, Collection<String> statuses);
    long countByTenantIdAndRetryCountIsNull(String tenantId);
    long countByTenantIdAndRetryCountLessThanEqual(String tenantId, Integer maxRetryCount);
    long countByTenantIdAndRetryCountBetween(String tenantId, Integer minRetryCount, Integer maxRetryCount);
    long countByTenantIdAndRetryCountGreaterThan(String tenantId, Integer minRetryCount);
}
