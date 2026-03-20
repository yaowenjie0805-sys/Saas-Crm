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
    Optional<UserAccount> findByUsernameAndTenantIdAndEnabledTrue(String username, String tenantId);
    Optional<UserAccount> findByUsernameAndTenantId(String username, String tenantId);
    List<UserAccount> findAllByTenantId(String tenantId);
    List<UserAccount> findAllByTenantIdOrderByUsernameAsc(String tenantId);

    @Query("select u.username, u.displayName from UserAccount u where u.tenantId = :tenantId and upper(coalesce(u.role, '')) = upper(:role)")
    List<Object[]> findIdentityPairsByTenantIdAndRole(@Param("tenantId") String tenantId, @Param("role") String role);

    @Query("select u.username, u.displayName from UserAccount u where u.tenantId = :tenantId and lower(coalesce(u.department, '')) = lower(:department)")
    List<Object[]> findIdentityPairsByTenantIdAndDepartment(@Param("tenantId") String tenantId, @Param("department") String department);
}
