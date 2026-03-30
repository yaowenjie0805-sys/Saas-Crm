package com.yao.crm.repository;

import com.yao.crm.entity.FollowUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface FollowUpRepository extends JpaRepository<FollowUp, String>, JpaSpecificationExecutor<FollowUp> {
    java.util.List<FollowUp> findByTenantId(String tenantId);
    java.util.List<FollowUp> findByTenantIdAndCustomerId(String tenantId, String customerId);
    java.util.List<FollowUp> findByTenantIdAndCreatedAtBetween(String tenantId, LocalDateTime from, LocalDateTime to);
    java.util.List<FollowUp> findByTenantIdAndAuthorIn(String tenantId, Collection<String> authors);
    java.util.List<FollowUp> findByTenantIdAndAuthorInAndCreatedAtBetween(String tenantId, Collection<String> authors, LocalDateTime from, LocalDateTime to);
    java.util.Optional<FollowUp> findByIdAndTenantId(String id, String tenantId);
    boolean existsByIdAndTenantId(String id, String tenantId);
    long deleteByIdAndTenantId(String id, String tenantId);

    @Query("select f.channel, count(f) from FollowUp f where f.tenantId = :tenantId group by f.channel")
    List<Object[]> countByChannelGrouped(@Param("tenantId") String tenantId);

    // Note: authors 参数已由 Service 层统一转为小写，可直接使用索引
    @Query("select f.channel, count(f) from FollowUp f where f.tenantId = :tenantId and f.author in :authors group by f.channel")
    List<Object[]> countByChannelGroupedAndAuthorIn(@Param("tenantId") String tenantId, @Param("authors") Collection<String> authors);

    @Query("select f.channel, count(f) from FollowUp f where f.tenantId = :tenantId and f.createdAt between :from and :to group by f.channel")
    List<Object[]> countByChannelGroupedAndCreatedAtBetween(@Param("tenantId") String tenantId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Note: authors 参数已由 Service 层统一转为小写，可直接使用索引
    @Query("select f.channel, count(f) from FollowUp f where f.tenantId = :tenantId and f.author in :authors and f.createdAt between :from and :to group by f.channel")
    List<Object[]> countByChannelGroupedAndAuthorInAndCreatedAtBetween(@Param("tenantId") String tenantId, @Param("authors") Collection<String> authors, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    long countByTenantId(String tenantId);
    long countByTenantIdAndAuthorIn(String tenantId, Collection<String> authors);
    long countByTenantIdAndCreatedAtBetween(String tenantId, LocalDateTime from, LocalDateTime to);
    long countByTenantIdAndAuthorInAndCreatedAtBetween(String tenantId, Collection<String> authors, LocalDateTime from, LocalDateTime to);
}
