package com.yao.crm.repository;

import com.yao.crm.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, String> {
    Optional<UserAccount> findByUsernameAndEnabledTrue(String username);
    Optional<UserAccount> findByUsername(String username);
    Optional<UserAccount> findByIdAndTenantId(String id, String tenantId);
    Optional<UserAccount> findByUsernameAndTenantIdAndEnabledTrue(String username, String tenantId);
    Optional<UserAccount> findByUsernameAndTenantId(String username, String tenantId);
    List<UserAccount> findAllByTenantId(String tenantId);
    List<UserAccount> findAllByTenantIdOrderByUsernameAsc(String tenantId);

    @Query("select u.username, u.displayName from UserAccount u where u.tenantId = :tenantId and upper(u.role) = upper(:role)")
    List<Object[]> findIdentityPairsByTenantIdAndRoleIgnoreCase(@Param("tenantId") String tenantId, @Param("role") String role);

    @Query("select u.username, u.displayName from UserAccount u where u.tenantId = :tenantId and upper(u.department) = upper(:department)")
    List<Object[]> findIdentityPairsByTenantIdAndDepartmentIgnoreCase(@Param("tenantId") String tenantId, @Param("department") String department);
}
