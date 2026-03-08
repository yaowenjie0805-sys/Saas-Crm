package com.yao.crm.repository;

import com.yao.crm.entity.TaskItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TaskRepository extends JpaRepository<TaskItem, String>, JpaSpecificationExecutor<TaskItem> {
    java.util.List<TaskItem> findByTenantId(String tenantId);
}
