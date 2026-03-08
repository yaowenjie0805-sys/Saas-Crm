package com.yao.crm.repository;

import com.yao.crm.entity.ApprovalEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalEventRepository extends JpaRepository<ApprovalEvent, String> {
    List<ApprovalEvent> findByInstanceIdAndTenantIdOrderByCreatedAtAsc(String instanceId, String tenantId);
}
