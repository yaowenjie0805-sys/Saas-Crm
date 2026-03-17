package com.yao.crm.repository;

import com.yao.crm.entity.FollowUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FollowUpRepository extends JpaRepository<FollowUp, String>, JpaSpecificationExecutor<FollowUp> {
    java.util.List<FollowUp> findByTenantId(String tenantId);
    java.util.Optional<FollowUp> findByIdAndTenantId(String id, String tenantId);
    boolean existsByIdAndTenantId(String id, String tenantId);
}
