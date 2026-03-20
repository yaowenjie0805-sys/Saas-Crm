package com.yao.crm.repository;

import com.yao.crm.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ContactRepository extends JpaRepository<Contact, String>, JpaSpecificationExecutor<Contact> {
    java.util.List<Contact> findByTenantId(String tenantId);
    java.util.Optional<Contact> findByIdAndTenantId(String id, String tenantId);
    boolean existsByIdAndTenantId(String id, String tenantId);
}
