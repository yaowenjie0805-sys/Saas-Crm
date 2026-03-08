package com.yao.crm.repository;

import com.yao.crm.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, String> {
    Optional<UserAccount> findByUsernameAndEnabledTrue(String username);
    Optional<UserAccount> findByUsername(String username);
    Optional<UserAccount> findByUsernameAndTenantIdAndEnabledTrue(String username, String tenantId);
    Optional<UserAccount> findByUsernameAndTenantId(String username, String tenantId);
    List<UserAccount> findAllByTenantIdOrderByUsernameAsc(String tenantId);
}
