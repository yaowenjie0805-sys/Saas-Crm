package com.yao.crm.repository;

import com.yao.crm.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, String>, JpaSpecificationExecutor<PaymentRecord> {
    java.util.List<PaymentRecord> findByTenantId(String tenantId);
    java.util.List<PaymentRecord> findByTenantIdAndCustomerId(String tenantId, String customerId);
    java.util.List<PaymentRecord> findByTenantIdAndIdIn(String tenantId, Collection<String> ids);
    java.util.List<PaymentRecord> findByTenantIdAndCreatedAtBetween(String tenantId, LocalDateTime from, LocalDateTime to);
    java.util.List<PaymentRecord> findByTenantIdAndOwnerIn(String tenantId, Collection<String> owners);
    java.util.List<PaymentRecord> findByTenantIdAndOwnerInAndCreatedAtBetween(String tenantId, Collection<String> owners, LocalDateTime from, LocalDateTime to);
    java.util.Optional<PaymentRecord> findByIdAndTenantId(String id, String tenantId);
    boolean existsByIdAndTenantId(String id, String tenantId);
    long deleteByIdAndTenantId(String id, String tenantId);
    long countByTenantId(String tenantId);

    @Query("select coalesce(sum(p.amount), 0) from PaymentRecord p where p.tenantId = :tenantId and upper(p.status) in :statuses")
    Long sumAmountByTenantIdAndStatusInUppercase(@Param("tenantId") String tenantId, @Param("statuses") List<String> statuses);

    // Note: owners 参数已由 Service 层统一转为小写，可直接使用索引
    @Query("select coalesce(sum(p.amount), 0) from PaymentRecord p where p.tenantId = :tenantId and p.owner in :owners and upper(p.status) in :statuses")
    Long sumAmountByTenantIdAndOwnerInAndStatusInUppercase(@Param("tenantId") String tenantId, @Param("owners") Collection<String> owners, @Param("statuses") List<String> statuses);

    @Query("select coalesce(sum(p.amount), 0) from PaymentRecord p where p.tenantId = :tenantId and upper(p.status) in :statuses and p.createdAt between :from and :to")
    Long sumAmountByTenantIdAndStatusInUppercaseAndCreatedAtBetween(@Param("tenantId") String tenantId, @Param("statuses") List<String> statuses, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Note: owners 参数已由 Service 层统一转为小写，可直接使用索引
    @Query("select coalesce(sum(p.amount), 0) from PaymentRecord p where p.tenantId = :tenantId and p.owner in :owners and upper(p.status) in :statuses and p.createdAt between :from and :to")
    Long sumAmountByTenantIdAndOwnerInAndStatusInUppercaseAndCreatedAtBetween(@Param("tenantId") String tenantId, @Param("owners") Collection<String> owners, @Param("statuses") List<String> statuses, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select coalesce(sum(p.amount), 0) from PaymentRecord p where p.tenantId = :tenantId and upper(p.status) not in :statuses")
    Long sumAmountByTenantIdAndStatusNotInUppercase(@Param("tenantId") String tenantId, @Param("statuses") List<String> statuses);
}
