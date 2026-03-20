package com.yao.crm.repository;

import com.yao.crm.entity.PriceBookItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PriceBookItemRepository extends JpaRepository<PriceBookItem, String> {
    Optional<PriceBookItem> findByIdAndTenantId(String id, String tenantId);
    List<PriceBookItem> findByTenantIdAndPriceBookIdOrderByUpdatedAtDesc(String tenantId, String priceBookId);
    Optional<PriceBookItem> findByTenantIdAndPriceBookIdAndProductId(String tenantId, String priceBookId, String productId);
}

