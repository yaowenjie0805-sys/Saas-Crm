package com.yao.crm.repository;

import com.yao.crm.entity.QuickFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuickFilterRepository extends JpaRepository<QuickFilter, String> {

    List<QuickFilter> findByTenantIdAndEntityTypeOrderByDisplayOrderAsc(String tenantId, String entityType);

    List<QuickFilter> findByTenantIdAndOwnerAndEntityTypeOrderByDisplayOrderAsc(
            String tenantId, String owner, String entityType);

    List<QuickFilter> findByTenantIdAndOwner(String tenantId, String owner);

    void deleteByTenantIdAndOwnerAndId(String tenantId, String owner, String id);
}
