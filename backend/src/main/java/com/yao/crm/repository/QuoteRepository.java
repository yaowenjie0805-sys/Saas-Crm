package com.yao.crm.repository;

import com.yao.crm.entity.Quote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, String> {
    Optional<Quote> findByIdAndTenantId(String id, String tenantId);
    Optional<Quote> findByTenantIdAndQuoteNo(String tenantId, String quoteNo);
    List<Quote> findByTenantId(String tenantId);
    List<Quote> findByTenantIdAndStatus(String tenantId, String status);
    List<Quote> findByTenantIdAndOpportunityId(String tenantId, String opportunityId);
    List<Quote> findByTenantIdAndStatusAndOpportunityId(String tenantId, String status, String opportunityId);
    Page<Quote> findByTenantId(String tenantId, Pageable pageable);
    Page<Quote> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);
    Page<Quote> findByTenantIdAndOpportunityId(String tenantId, String opportunityId, Pageable pageable);
    Page<Quote> findByTenantIdAndStatusAndOpportunityId(String tenantId, String status, String opportunityId, Pageable pageable);
    Page<Quote> findByTenantIdAndOwnerIn(String tenantId, Collection<String> owners, Pageable pageable);
    Page<Quote> findByTenantIdAndStatusAndOwnerIn(String tenantId, String status, Collection<String> owners, Pageable pageable);
    Page<Quote> findByTenantIdAndOpportunityIdAndOwnerIn(String tenantId, String opportunityId, Collection<String> owners, Pageable pageable);
    Page<Quote> findByTenantIdAndStatusAndOpportunityIdAndOwnerIn(String tenantId, String status, String opportunityId, Collection<String> owners, Pageable pageable);
    long countByTenantIdAndStatus(String tenantId, String status);
}
