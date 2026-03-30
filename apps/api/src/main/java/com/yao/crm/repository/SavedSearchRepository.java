package com.yao.crm.repository;

import com.yao.crm.entity.SavedSearch;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedSearchRepository extends JpaRepository<SavedSearch, String> {

    List<SavedSearch> findByTenantIdAndOwner(String tenantId, String owner);

    List<SavedSearch> findByTenantIdAndOwnerOrderByLastUsedAtDesc(String tenantId, String owner, Pageable pageable);

    List<SavedSearch> findByTenantIdAndOwnerAndIsSharedTrue(String tenantId, String owner);

    List<SavedSearch> findByTenantIdAndOwnerAndSearchType(String tenantId, String owner, String searchType);

    Optional<SavedSearch> findByIdAndTenantIdAndOwner(String id, String tenantId, String owner);

    @Query("select distinct s.name from SavedSearch s " +
            "where s.tenantId = :tenantId and lower(s.name) like lower(concat(:q, '%')) " +
            "order by s.name asc")
    List<String> findDistinctNamesByTenantIdAndNameStartingWithIgnoreCase(
            @Param("tenantId") String tenantId,
            @Param("q") String q,
            Pageable pageable);

    @Query("select distinct s.name from SavedSearch s " +
            "where s.tenantId = :tenantId and lower(s.name) like lower(concat('%', :q, '%')) " +
            "order by s.name asc")
    List<String> findDistinctNamesByTenantIdAndNameContainingIgnoreCase(
            @Param("tenantId") String tenantId,
            @Param("q") String q,
            Pageable pageable);

    long deleteByIdAndTenantIdAndOwner(String id, String tenantId, String owner);

    void deleteByTenantIdAndOwner(String tenantId, String owner);
}
