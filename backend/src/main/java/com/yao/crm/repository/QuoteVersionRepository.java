package com.yao.crm.repository;

import com.yao.crm.entity.QuoteVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuoteVersionRepository extends JpaRepository<QuoteVersion, String> {
    List<QuoteVersion> findByTenantIdAndQuoteIdOrderByVersionNoDesc(String tenantId, String quoteId);
    Optional<QuoteVersion> findTopByTenantIdAndQuoteIdOrderByVersionNoDesc(String tenantId, String quoteId);
}

