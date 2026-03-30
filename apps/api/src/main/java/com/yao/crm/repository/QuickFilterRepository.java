package com.yao.crm.repository;

import com.yao.crm.entity.QuickFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuickFilterRepository extends JpaRepository<QuickFilter, String> {

    List<QuickFilter> findByTenantIdAndEntityTypeOrderByDisplayOrderAsc(String tenantId, String entityType);

    List<QuickFilter> findByTenantIdAndOwnerAndEntityTypeOrderByDisplayOrderAsc(
            String tenantId, String owner, String entityType);

    List<QuickFilter> findByTenantIdAndOwner(String tenantId, String owner);

    Optional<QuickFilter> findByTenantIdAndId(String tenantId, String id);

    @Transactional
    @Modifying
    @Query("delete from QuickFilter q where q.tenantId = :tenantId and q.id = :id")
    int deleteByTenantIdAndId(String tenantId, String id);
}
