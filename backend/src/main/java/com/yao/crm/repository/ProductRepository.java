package com.yao.crm.repository;

import com.yao.crm.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {
    Optional<Product> findByIdAndTenantId(String id, String tenantId);
    Optional<Product> findByTenantIdAndCode(String tenantId, String code);
    Page<Product> findByTenantId(String tenantId, Pageable pageable);
    Page<Product> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);
}

