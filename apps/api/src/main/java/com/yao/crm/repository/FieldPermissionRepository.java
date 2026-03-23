package com.yao.crm.repository;

import com.yao.crm.entity.FieldPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FieldPermissionRepository extends JpaRepository<FieldPermission, String> {

    Optional<FieldPermission> findByTenantIdAndRoleAndEntityTypeAndFieldName(
            String tenantId, String role, String entityType, String fieldName);

    List<FieldPermission> findByTenantIdAndRoleAndEntityType(
            String tenantId, String role, String entityType);

    List<FieldPermission> findByTenantId(String tenantId);

    List<FieldPermission> findByTenantIdAndEntityType(String tenantId, String entityType);

    void deleteByTenantIdAndRoleAndEntityTypeAndFieldName(
            String tenantId, String role, String entityType, String fieldName);
}
