package com.yao.crm.repository;

import com.yao.crm.entity.Opportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface OpportunityRepository extends JpaRepository<Opportunity, String>, JpaSpecificationExecutor<Opportunity> {
    java.util.List<Opportunity> findByTenantId(String tenantId);
    org.springframework.data.domain.Page<Opportunity> findByTenantId(String tenantId, org.springframework.data.domain.Pageable pageable);
    java.util.List<Opportunity> findByTenantIdAndIdIn(String tenantId, Collection<String> ids);
    java.util.List<Opportunity> findByTenantIdAndCreatedAtBetween(String tenantId, LocalDateTime from, LocalDateTime to);
    java.util.List<Opportunity> findByTenantIdAndOwnerIn(String tenantId, Collection<String> owners);
    java.util.List<Opportunity> findByTenantIdAndOwnerInAndCreatedAtBetween(String tenantId, Collection<String> owners, LocalDateTime from, LocalDateTime to);
    java.util.Optional<Opportunity> findByIdAndTenantId(String id, String tenantId);
    boolean existsByIdAndTenantId(String id, String tenantId);
    long countByTenantId(String tenantId);
    long countByTenantIdAndOwnerIn(String tenantId, Collection<String> owners);
    long countByTenantIdAndCreatedAtBetween(String tenantId, LocalDateTime from, LocalDateTime to);
    long countByTenantIdAndOwnerInAndCreatedAtBetween(String tenantId, Collection<String> owners, LocalDateTime from, LocalDateTime to);
    java.util.List<Opportunity> findTop8ByTenantIdOrderByUpdatedAtDesc(String tenantId);

    @Query("select coalesce(sum(o.amount * o.progress), 0) from Opportunity o where o.tenantId = :tenantId")
    Long sumWeightedAmountRawByTenantId(@Param("tenantId") String tenantId);

    @Query("select coalesce(sum(o.amount * o.progress), 0) from Opportunity o where o.tenantId = :tenantId and lower(o.owner) in :owners")
    Long sumWeightedAmountRawByTenantIdAndOwnerIn(@Param("tenantId") String tenantId, @Param("owners") Collection<String> owners);

    @Query("select coalesce(sum(o.amount * o.progress), 0) from Opportunity o where o.tenantId = :tenantId and o.createdAt between :from and :to")
    Long sumWeightedAmountRawByTenantIdAndCreatedAtBetween(@Param("tenantId") String tenantId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select coalesce(sum(o.amount * o.progress), 0) from Opportunity o where o.tenantId = :tenantId and lower(o.owner) in :owners and o.createdAt between :from and :to")
    Long sumWeightedAmountRawByTenantIdAndOwnerInAndCreatedAtBetween(@Param("tenantId") String tenantId, @Param("owners") Collection<String> owners, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select count(o) from Opportunity o where o.tenantId = :tenantId and o.progress >= :threshold")
    long countByTenantIdAndProgressGte(@Param("tenantId") String tenantId, @Param("threshold") int threshold);

    @Query("select count(o) from Opportunity o where o.tenantId = :tenantId and lower(o.owner) in :owners and o.progress >= :threshold")
    long countByTenantIdAndOwnerInAndProgressGte(@Param("tenantId") String tenantId, @Param("owners") Collection<String> owners, @Param("threshold") int threshold);

    @Query("select count(o) from Opportunity o where o.tenantId = :tenantId and o.progress >= :threshold and o.createdAt between :from and :to")
    long countByTenantIdAndProgressGteAndCreatedAtBetween(@Param("tenantId") String tenantId, @Param("threshold") int threshold, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select count(o) from Opportunity o where o.tenantId = :tenantId and lower(o.owner) in :owners and o.progress >= :threshold and o.createdAt between :from and :to")
    long countByTenantIdAndOwnerInAndProgressGteAndCreatedAtBetween(@Param("tenantId") String tenantId, @Param("owners") Collection<String> owners, @Param("threshold") int threshold, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select coalesce(avg(100 - (case when o.progress < 0 then 0 when o.progress > 100 then 100 else o.progress end)), 0) from Opportunity o where o.tenantId = :tenantId")
    Double avgCycleDaysByTenantId(@Param("tenantId") String tenantId);

    @Query("select o.stage, count(o) from Opportunity o where o.tenantId = :tenantId group by o.stage")
    List<Object[]> countByStageGrouped(@Param("tenantId") String tenantId);

    @Query("select o.stage, count(o) from Opportunity o where o.tenantId = :tenantId and lower(o.owner) in :owners group by o.stage")
    List<Object[]> countByStageGroupedAndOwnerIn(@Param("tenantId") String tenantId, @Param("owners") Collection<String> owners);

    @Query("select o.stage, count(o) from Opportunity o where o.tenantId = :tenantId and o.createdAt between :from and :to group by o.stage")
    List<Object[]> countByStageGroupedAndCreatedAtBetween(@Param("tenantId") String tenantId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select o.stage, count(o) from Opportunity o where o.tenantId = :tenantId and lower(o.owner) in :owners and o.createdAt between :from and :to group by o.stage")
    List<Object[]> countByStageGroupedAndOwnerInAndCreatedAtBetween(@Param("tenantId") String tenantId, @Param("owners") Collection<String> owners, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
