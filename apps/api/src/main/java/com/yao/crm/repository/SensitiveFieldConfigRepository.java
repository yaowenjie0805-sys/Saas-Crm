package com.yao.crm.repository;

import com.yao.crm.entity.SensitiveFieldConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SensitiveFieldConfigRepository extends JpaRepository<SensitiveFieldConfig, String> {

    Optional<SensitiveFieldConfig> findByTenantIdAndEntityTypeAndFieldName(
            String tenantId, String entityType, String fieldName);

    List<SensitiveFieldConfig> findByTenantIdAndEntityType(String tenantId, String entityType);

    List<SensitiveFieldConfig> findByTenantId(String tenantId);

    void deleteByTenantIdAndEntityTypeAndFieldName(String tenantId, String entityType, String fieldName);
}
