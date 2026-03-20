package com.yao.crm.repository;

import com.yao.crm.entity.PriceBook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PriceBookRepository extends JpaRepository<PriceBook, String> {
    Optional<PriceBook> findByIdAndTenantId(String id, String tenantId);
    Page<PriceBook> findByTenantId(String tenantId, Pageable pageable);
    Page<PriceBook> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);
    List<PriceBook> findByTenantIdOrderByUpdatedAtDesc(String tenantId);
}
