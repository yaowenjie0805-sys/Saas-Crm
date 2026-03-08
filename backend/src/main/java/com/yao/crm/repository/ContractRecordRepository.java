package com.yao.crm.repository;

import com.yao.crm.entity.ContractRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ContractRecordRepository extends JpaRepository<ContractRecord, String>, JpaSpecificationExecutor<ContractRecord> {
    java.util.List<ContractRecord> findByTenantId(String tenantId);
}
