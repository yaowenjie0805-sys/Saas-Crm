package com.yao.crm.repository;

import com.yao.crm.entity.NotificationJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationJobRepository extends JpaRepository<NotificationJob, String> {
    List<NotificationJob> findByStatusInAndNextRetryAtBeforeOrderByCreatedAtAsc(List<String> statuses, LocalDateTime now);
    List<NotificationJob> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    List<NotificationJob> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, String status);
    Page<NotificationJob> findByTenantId(String tenantId, Pageable pageable);
    Page<NotificationJob> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);
    Optional<NotificationJob> findByIdAndTenantId(String id, String tenantId);
    Optional<NotificationJob> findByTenantIdAndDedupeKey(String tenantId, String dedupeKey);
    List<NotificationJob> findByTenantIdAndDedupeKeyIn(String tenantId, Collection<String> dedupeKeys);
}
