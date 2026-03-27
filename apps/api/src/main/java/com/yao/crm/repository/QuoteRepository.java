package com.yao.crm.repository;

import com.yao.crm.entity.Quote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, String> {
    Optional<Quote> findByIdAndTenantId(String id, String tenantId);
    Optional<Quote> findByTenantIdAndQuoteNo(String tenantId, String quoteNo);
    List<Quote> findByTenantId(String tenantId);
    List<Quote> findByTenantIdAndIdIn(String tenantId, Collection<String> ids);
    List<Quote> findByTenantIdAndCreatedAtBetween(String tenantId, LocalDateTime from, LocalDateTime to);
    List<Quote> findByTenantIdAndOwnerIn(String tenantId, Collection<String> owners);
    List<Quote> findByTenantIdAndOwnerInAndCreatedAtBetween(String tenantId, Collection<String> owners, LocalDateTime from, LocalDateTime to);
    List<Quote> findByTenantIdAndStatus(String tenantId, String status);
    List<Quote> findByTenantIdAndCustomerId(String tenantId, String customerId);
    List<Quote> findByTenantIdAndOpportunityId(String tenantId, String opportunityId);
    List<Quote> findByTenantIdAndStatusAndOpportunityId(String tenantId, String status, String opportunityId);
    Page<Quote> findByTenantId(String tenantId, Pageable pageable);
    Page<Quote> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);
    Page<Quote> findByTenantIdAndOpportunityId(String tenantId, String opportunityId, Pageable pageable);
    Page<Quote> findByTenantIdAndStatusAndOpportunityId(String tenantId, String status, String opportunityId, Pageable pageable);
    Page<Quote> findByTenantIdAndOwnerIn(String tenantId, Collection<String> owners, Pageable pageable);
    Page<Quote> findByTenantIdAndStatusAndOwnerIn(String tenantId, String status, Collection<String> owners, Pageable pageable);
    Page<Quote> findByTenantIdAndOpportunityIdAndOwnerIn(String tenantId, String opportunityId, Collection<String> owners, Pageable pageable);
    Page<Quote> findByTenantIdAndStatusAndOpportunityIdAndOwnerIn(String tenantId, String status, String opportunityId, Collection<String> owners, Pageable pageable);
    long countByTenantId(String tenantId);
    long countByTenantIdAndOwnerIn(String tenantId, Collection<String> owners);
    long countByTenantIdAndCreatedAtBetween(String tenantId, LocalDateTime from, LocalDateTime to);
    long countByTenantIdAndOwnerInAndCreatedAtBetween(String tenantId, Collection<String> owners, LocalDateTime from, LocalDateTime to);
    long countByTenantIdAndStatus(String tenantId, String status);

    @Query("select q.status, count(q) from Quote q where q.tenantId = :tenantId group by q.status")
    List<Object[]> countByStatusGrouped(@Param("tenantId") String tenantId);

    // Note: owners 参数已由 Service 层统一转为小写，可直接使用索引
    @Query("select q.status, count(q) from Quote q where q.tenantId = :tenantId and q.owner in :owners group by q.status")
    List<Object[]> countByStatusGroupedAndOwnerIn(@Param("tenantId") String tenantId, @Param("owners") Collection<String> owners);

    @Query("select q.status, count(q) from Quote q where q.tenantId = :tenantId and q.createdAt between :from and :to group by q.status")
    List<Object[]> countByStatusGroupedAndCreatedAtBetween(@Param("tenantId") String tenantId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Note: owners 参数已由 Service 层统一转为小写，可直接使用索引
    @Query("select q.status, count(q) from Quote q where q.tenantId = :tenantId and q.owner in :owners and q.createdAt between :from and :to group by q.status")
    List<Object[]> countByStatusGroupedAndOwnerInAndCreatedAtBetween(@Param("tenantId") String tenantId, @Param("owners") Collection<String> owners, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select count(q) from Quote q where q.tenantId = :tenantId and upper(q.status) in :statuses")
    long countByTenantIdAndStatusInUppercase(@Param("tenantId") String tenantId, @Param("statuses") List<String> statuses);

    // Note: owners 参数已由 Service 层统一转为小写，可直接使用索引
    @Query("select count(q) from Quote q where q.tenantId = :tenantId and q.owner in :owners and upper(q.status) in :statuses")
    long countByTenantIdAndOwnerInAndStatusInUppercase(@Param("tenantId") String tenantId, @Param("owners") Collection<String> owners, @Param("statuses") List<String> statuses);

    @Query("select count(q) from Quote q where q.tenantId = :tenantId and upper(q.status) in :statuses and q.createdAt between :from and :to")
    long countByTenantIdAndStatusInUppercaseAndCreatedAtBetween(@Param("tenantId") String tenantId, @Param("statuses") List<String> statuses, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Note: owners 参数已由 Service 层统一转为小写，可直接使用索引
    @Query("select count(q) from Quote q where q.tenantId = :tenantId and q.owner in :owners and upper(q.status) in :statuses and q.createdAt between :from and :to")
    long countByTenantIdAndOwnerInAndStatusInUppercaseAndCreatedAtBetween(@Param("tenantId") String tenantId, @Param("owners") Collection<String> owners, @Param("statuses") List<String> statuses, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
