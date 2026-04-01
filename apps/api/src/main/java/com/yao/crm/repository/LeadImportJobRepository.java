package com.yao.crm.repository;

import com.yao.crm.entity.LeadImportJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LeadImportJobRepository extends JpaRepository<LeadImportJob, String> {
    Optional<LeadImportJob> findByIdAndTenantId(String id, String tenantId);
    List<LeadImportJob> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    Page<LeadImportJob> findByTenantId(String tenantId, Pageable pageable);
    Page<LeadImportJob> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);
    long countByTenantIdAndStatusIn(String tenantId, Collection<String> statuses);
    List<LeadImportJob> findByTenantIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(String tenantId, LocalDateTime from);
    List<LeadImportJob> findByTenantIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(String tenantId, LocalDateTime from, Pageable pageable);
}
