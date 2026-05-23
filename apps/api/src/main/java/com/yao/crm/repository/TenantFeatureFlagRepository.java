package com.yao.crm.repository;

import com.yao.crm.entity.TenantFeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantFeatureFlagRepository extends JpaRepository<TenantFeatureFlag, String> {
    Optional<TenantFeatureFlag> findByTenantIdAndFlagKey(String tenantId, String flagKey);
    List<TenantFeatureFlag> findAllByTenantIdOrderByFlagKeyAsc(String tenantId);
}
