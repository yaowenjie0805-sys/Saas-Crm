package com.yao.crm.repository;

import com.yao.crm.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, String>, JpaSpecificationExecutor<PaymentRecord> {
    java.util.List<PaymentRecord> findByTenantId(String tenantId);
    java.util.Optional<PaymentRecord> findByIdAndTenantId(String id, String tenantId);
    boolean existsByIdAndTenantId(String id, String tenantId);
}
