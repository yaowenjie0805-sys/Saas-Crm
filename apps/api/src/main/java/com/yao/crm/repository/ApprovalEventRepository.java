package com.yao.crm.repository;

import com.yao.crm.entity.ApprovalEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ApprovalEventRepository extends JpaRepository<ApprovalEvent, String> {
    List<ApprovalEvent> findByInstanceIdAndTenantIdOrderByCreatedAtAsc(String instanceId, String tenantId);
    Optional<ApprovalEvent> findTopByTenantIdAndTaskIdAndEventTypeOrderByCreatedAtDesc(String tenantId, String taskId, String eventType);
    long countByTenantIdAndTaskIdAndEventTypeAndCreatedAtBetween(String tenantId, String taskId, String eventType, LocalDateTime from, LocalDateTime to);
}
