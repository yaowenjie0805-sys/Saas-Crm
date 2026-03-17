package com.yao.crm.repository;

import com.yao.crm.entity.OrderRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRecordRepository extends JpaRepository<OrderRecord, String> {
    Optional<OrderRecord> findByIdAndTenantId(String id, String tenantId);
    Optional<OrderRecord> findByTenantIdAndOrderNo(String tenantId, String orderNo);
    List<OrderRecord> findByTenantId(String tenantId);
    List<OrderRecord> findByTenantIdAndStatus(String tenantId, String status);
    List<OrderRecord> findByTenantIdAndOpportunityId(String tenantId, String opportunityId);
    List<OrderRecord> findByTenantIdAndStatusAndOpportunityId(String tenantId, String status, String opportunityId);
    Page<OrderRecord> findByTenantId(String tenantId, Pageable pageable);
    Page<OrderRecord> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);
    Page<OrderRecord> findByTenantIdAndOpportunityId(String tenantId, String opportunityId, Pageable pageable);
    Page<OrderRecord> findByTenantIdAndStatusAndOpportunityId(String tenantId, String status, String opportunityId, Pageable pageable);
    Page<OrderRecord> findByTenantIdAndOwnerIn(String tenantId, Collection<String> owners, Pageable pageable);
    Page<OrderRecord> findByTenantIdAndStatusAndOwnerIn(String tenantId, String status, Collection<String> owners, Pageable pageable);
    Page<OrderRecord> findByTenantIdAndOpportunityIdAndOwnerIn(String tenantId, String opportunityId, Collection<String> owners, Pageable pageable);
    Page<OrderRecord> findByTenantIdAndStatusAndOpportunityIdAndOwnerIn(String tenantId, String status, String opportunityId, Collection<String> owners, Pageable pageable);
    long countByTenantIdAndStatus(String tenantId, String status);
}
