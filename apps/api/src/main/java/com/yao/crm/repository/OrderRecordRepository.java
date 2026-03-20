package com.yao.crm.repository;

import com.yao.crm.entity.OrderRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRecordRepository extends JpaRepository<OrderRecord, String> {
    Optional<OrderRecord> findByIdAndTenantId(String id, String tenantId);
    Optional<OrderRecord> findByTenantIdAndOrderNo(String tenantId, String orderNo);
    List<OrderRecord> findByTenantId(String tenantId);
    List<OrderRecord> findByTenantIdAndIdIn(String tenantId, Collection<String> ids);
    List<OrderRecord> findByTenantIdAndCreatedAtBetween(String tenantId, LocalDateTime from, LocalDateTime to);
    List<OrderRecord> findByTenantIdAndOwnerIn(String tenantId, Collection<String> owners);
    List<OrderRecord> findByTenantIdAndOwnerInAndCreatedAtBetween(String tenantId, Collection<String> owners, LocalDateTime from, LocalDateTime to);
    List<OrderRecord> findByTenantIdAndStatus(String tenantId, String status);
    List<OrderRecord> findByTenantIdAndCustomerId(String tenantId, String customerId);
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
    long countByTenantId(String tenantId);
    long countByTenantIdAndOwnerIn(String tenantId, Collection<String> owners);
    long countByTenantIdAndCreatedAtBetween(String tenantId, LocalDateTime from, LocalDateTime to);
    long countByTenantIdAndOwnerInAndCreatedAtBetween(String tenantId, Collection<String> owners, LocalDateTime from, LocalDateTime to);
    long countByTenantIdAndStatus(String tenantId, String status);
    long countByTenantIdAndStatusAndCreatedAtBetween(String tenantId, String status, LocalDateTime from, LocalDateTime to);
    long countByTenantIdAndOwnerInAndStatusAndCreatedAtBetween(String tenantId, Collection<String> owners, String status, LocalDateTime from, LocalDateTime to);

    @Query("select coalesce(sum(o.amount), 0) from OrderRecord o where o.tenantId = :tenantId")
    Long sumAmountByTenantId(@Param("tenantId") String tenantId);

    @Query("select coalesce(sum(o.amount), 0) from OrderRecord o where o.tenantId = :tenantId and lower(o.owner) in :owners")
    Long sumAmountByTenantIdAndOwnerIn(@Param("tenantId") String tenantId, @Param("owners") Collection<String> owners);

    @Query("select coalesce(sum(o.amount), 0) from OrderRecord o where o.tenantId = :tenantId and o.createdAt between :from and :to")
    Long sumAmountByTenantIdAndCreatedAtBetween(@Param("tenantId") String tenantId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select coalesce(sum(o.amount), 0) from OrderRecord o where o.tenantId = :tenantId and lower(o.owner) in :owners and o.createdAt between :from and :to")
    Long sumAmountByTenantIdAndOwnerInAndCreatedAtBetween(@Param("tenantId") String tenantId, @Param("owners") Collection<String> owners, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select count(o) from OrderRecord o where o.tenantId = :tenantId and lower(o.owner) in :owners and upper(o.status) = upper(:status)")
    long countByTenantIdAndOwnerInAndStatus(@Param("tenantId") String tenantId, @Param("owners") Collection<String> owners, @Param("status") String status);

    @Query("select o.status, count(o) from OrderRecord o where o.tenantId = :tenantId group by o.status")
    List<Object[]> countByStatusGrouped(@Param("tenantId") String tenantId);

    @Query("select o.status, count(o) from OrderRecord o where o.tenantId = :tenantId and lower(o.owner) in :owners group by o.status")
    List<Object[]> countByStatusGroupedAndOwnerIn(@Param("tenantId") String tenantId, @Param("owners") Collection<String> owners);

    @Query("select o.status, count(o) from OrderRecord o where o.tenantId = :tenantId and o.createdAt between :from and :to group by o.status")
    List<Object[]> countByStatusGroupedAndCreatedAtBetween(@Param("tenantId") String tenantId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select o.status, count(o) from OrderRecord o where o.tenantId = :tenantId and lower(o.owner) in :owners and o.createdAt between :from and :to group by o.status")
    List<Object[]> countByStatusGroupedAndOwnerInAndCreatedAtBetween(@Param("tenantId") String tenantId, @Param("owners") Collection<String> owners, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
