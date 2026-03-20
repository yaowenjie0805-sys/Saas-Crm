package com.yao.crm.repository;

import com.yao.crm.entity.QuoteItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuoteItemRepository extends JpaRepository<QuoteItem, String> {
    Optional<QuoteItem> findByIdAndTenantId(String id, String tenantId);
    List<QuoteItem> findByTenantIdAndQuoteIdOrderByCreatedAtAsc(String tenantId, String quoteId);
    void deleteByTenantIdAndQuoteId(String tenantId, String quoteId);
}

