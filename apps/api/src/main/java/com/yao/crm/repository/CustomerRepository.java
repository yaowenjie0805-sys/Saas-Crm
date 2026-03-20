package com.yao.crm.repository;

import com.yao.crm.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, String>, JpaSpecificationExecutor<Customer> {
    java.util.List<Customer> findByTenantId(String tenantId);
    java.util.List<Customer> findByTenantIdAndIdIn(String tenantId, Collection<String> ids);
    java.util.List<Customer> findByTenantIdAndCreatedAtBetween(String tenantId, LocalDateTime from, LocalDateTime to);
    java.util.List<Customer> findByTenantIdAndOwnerIn(String tenantId, Collection<String> owners);
    java.util.List<Customer> findByTenantIdAndOwnerInAndCreatedAtBetween(String tenantId, Collection<String> owners, LocalDateTime from, LocalDateTime to);
    java.util.Optional<Customer> findByIdAndTenantId(String id, String tenantId);
    boolean existsByIdAndTenantId(String id, String tenantId);
    long countByTenantId(String tenantId);
    long countByTenantIdAndOwnerIn(String tenantId, Collection<String> owners);
    long countByTenantIdAndCreatedAtBetween(String tenantId, LocalDateTime from, LocalDateTime to);
    long countByTenantIdAndOwnerInAndCreatedAtBetween(String tenantId, Collection<String> owners, LocalDateTime from, LocalDateTime to);
    java.util.List<Customer> findTop8ByTenantIdOrderByUpdatedAtDesc(String tenantId);

    @Query("select coalesce(sum(c.value), 0) from Customer c where c.tenantId = :tenantId")
    Long sumValueByTenantId(@Param("tenantId") String tenantId);

    @Query("select coalesce(sum(c.value), 0) from Customer c where c.tenantId = :tenantId and lower(c.owner) in :owners")
    Long sumValueByTenantIdAndOwnerIn(@Param("tenantId") String tenantId, @Param("owners") Collection<String> owners);

    @Query("select coalesce(sum(c.value), 0) from Customer c where c.tenantId = :tenantId and c.createdAt between :from and :to")
    Long sumValueByTenantIdAndCreatedAtBetween(@Param("tenantId") String tenantId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select coalesce(sum(c.value), 0) from Customer c where c.tenantId = :tenantId and lower(c.owner) in :owners and c.createdAt between :from and :to")
    Long sumValueByTenantIdAndOwnerInAndCreatedAtBetween(@Param("tenantId") String tenantId, @Param("owners") Collection<String> owners, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select c.owner, count(c) from Customer c where c.tenantId = :tenantId group by c.owner")
    List<Object[]> countByOwnerGrouped(@Param("tenantId") String tenantId);

    @Query("select c.owner, count(c) from Customer c where c.tenantId = :tenantId and lower(c.owner) in :owners group by c.owner")
    List<Object[]> countByOwnerGroupedAndOwnerIn(@Param("tenantId") String tenantId, @Param("owners") Collection<String> owners);

    @Query("select c.owner, count(c) from Customer c where c.tenantId = :tenantId and c.createdAt between :from and :to group by c.owner")
    List<Object[]> countByOwnerGroupedAndCreatedAtBetween(@Param("tenantId") String tenantId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select c.owner, count(c) from Customer c where c.tenantId = :tenantId and lower(c.owner) in :owners and c.createdAt between :from and :to group by c.owner")
    List<Object[]> countByOwnerGroupedAndOwnerInAndCreatedAtBetween(@Param("tenantId") String tenantId, @Param("owners") Collection<String> owners, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select c.status, coalesce(sum(c.value), 0) from Customer c where c.tenantId = :tenantId group by c.status")
    List<Object[]> sumValueByStatusGrouped(@Param("tenantId") String tenantId);

    @Query("select c.status, coalesce(sum(c.value), 0) from Customer c where c.tenantId = :tenantId and lower(c.owner) in :owners group by c.status")
    List<Object[]> sumValueByStatusGroupedAndOwnerIn(@Param("tenantId") String tenantId, @Param("owners") Collection<String> owners);

    @Query("select c.status, coalesce(sum(c.value), 0) from Customer c where c.tenantId = :tenantId and c.createdAt between :from and :to group by c.status")
    List<Object[]> sumValueByStatusGroupedAndCreatedAtBetween(@Param("tenantId") String tenantId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select c.status, coalesce(sum(c.value), 0) from Customer c where c.tenantId = :tenantId and lower(c.owner) in :owners and c.createdAt between :from and :to group by c.status")
    List<Object[]> sumValueByStatusGroupedAndOwnerInAndCreatedAtBetween(@Param("tenantId") String tenantId, @Param("owners") Collection<String> owners, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
