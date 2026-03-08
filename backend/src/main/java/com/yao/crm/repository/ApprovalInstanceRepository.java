package com.yao.crm.repository;

import com.yao.crm.entity.ApprovalInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalInstanceRepository extends JpaRepository<ApprovalInstance, String> {
    Optional<ApprovalInstance> findByIdAndTenantId(String id, String tenantId);
    List<ApprovalInstance> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
