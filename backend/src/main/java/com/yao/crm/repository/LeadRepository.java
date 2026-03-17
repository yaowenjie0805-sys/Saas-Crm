package com.yao.crm.repository;

import com.yao.crm.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, String>, JpaSpecificationExecutor<Lead> {
    Optional<Lead> findByIdAndTenantId(String id, String tenantId);
    List<Lead> findByTenantId(String tenantId);
}
