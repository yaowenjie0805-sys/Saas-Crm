package com.yao.crm.repository;

import com.yao.crm.entity.LeadImportJobItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadImportJobItemRepository extends JpaRepository<LeadImportJobItem, String> {
    Page<LeadImportJobItem> findByTenantIdAndJobIdAndStatusOrderByLineNoAsc(String tenantId, String jobId, String status, Pageable pageable);
    long countByTenantIdAndJobIdAndStatus(String tenantId, String jobId, String status);
    void deleteByTenantIdAndJobId(String tenantId, String jobId);
}
