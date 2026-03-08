package com.yao.crm.repository;

import com.yao.crm.entity.Opportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OpportunityRepository extends JpaRepository<Opportunity, String>, JpaSpecificationExecutor<Opportunity> {
    java.util.List<Opportunity> findByTenantId(String tenantId);
}
