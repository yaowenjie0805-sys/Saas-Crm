package com.yao.crm.repository;

import com.yao.crm.entity.ContractRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.Collection;

public interface ContractRecordRepository extends JpaRepository<ContractRecord, String>, JpaSpecificationExecutor<ContractRecord> {
    java.util.List<ContractRecord> findByTenantId(String tenantId);
    java.util.List<ContractRecord> findByTenantIdAndCustomerId(String tenantId, String customerId);
    java.util.List<ContractRecord> findByTenantIdAndIdIn(String tenantId, Collection<String> ids);
    java.util.List<ContractRecord> findByTenantIdAndCreatedAtBetween(String tenantId, LocalDateTime from, LocalDateTime to);
    java.util.List<ContractRecord> findByTenantIdAndOwnerIn(String tenantId, Collection<String> owners);
    java.util.List<ContractRecord> findByTenantIdAndOwnerInAndCreatedAtBetween(String tenantId, Collection<String> owners, LocalDateTime from, LocalDateTime to);
    java.util.Optional<ContractRecord> findByIdAndTenantId(String id, String tenantId);
    long deleteByIdAndTenantId(String id, String tenantId);
}
