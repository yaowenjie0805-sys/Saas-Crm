package com.yao.crm.repository;

import com.yao.crm.entity.DashboardConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DashboardConfigRepository extends JpaRepository<DashboardConfig, String> {

    List<DashboardConfig> findByTenantId(String tenantId);

    List<DashboardConfig> findByTenantIdAndOwner(String tenantId, String owner);

    List<DashboardConfig> findByTenantIdAndVisibility(String tenantId, String visibility);

    Optional<DashboardConfig> findByTenantIdAndIsDefaultTrue(String tenantId);

    List<DashboardConfig> findByTenantIdAndIsSystemTrue(String tenantId);
}
