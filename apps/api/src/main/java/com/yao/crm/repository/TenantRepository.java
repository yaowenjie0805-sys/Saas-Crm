package com.yao.crm.repository;

import com.yao.crm.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenantRepository extends JpaRepository<Tenant, String> {
    List<Tenant> findAllByOrderByIdAsc();
}
