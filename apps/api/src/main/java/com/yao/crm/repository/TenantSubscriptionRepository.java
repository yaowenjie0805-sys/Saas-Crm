package com.yao.crm.repository;

import com.yao.crm.entity.TenantSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, String> {
    Optional<TenantSubscription> findByTenantId(String tenantId);
}
