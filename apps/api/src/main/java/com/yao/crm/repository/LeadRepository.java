package com.yao.crm.repository;

import com.yao.crm.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, String>, JpaSpecificationExecutor<Lead> {
    Optional<Lead> findByIdAndTenantId(String id, String tenantId);
    List<Lead> findByTenantId(String tenantId);
    List<Lead> findByTenantIdAndCreatedAtBetween(String tenantId, LocalDateTime from, LocalDateTime to);
    List<Lead> findByTenantIdAndOwnerIn(String tenantId, Collection<String> owners);
    List<Lead> findByTenantIdAndOwnerInAndCreatedAtBetween(String tenantId, Collection<String> owners, LocalDateTime from, LocalDateTime to);
    long countByTenantId(String tenantId);
    long countByTenantIdAndCreatedAtBetween(String tenantId, LocalDateTime from, LocalDateTime to);
    long countByTenantIdAndOwnerIn(String tenantId, Collection<String> owners);
    long countByTenantIdAndOwnerInAndCreatedAtBetween(String tenantId, Collection<String> owners, LocalDateTime from, LocalDateTime to);
}
