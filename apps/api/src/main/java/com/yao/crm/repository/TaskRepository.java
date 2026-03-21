package com.yao.crm.repository;

import com.yao.crm.entity.TaskItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface TaskRepository extends JpaRepository<TaskItem, String>, JpaSpecificationExecutor<TaskItem> {
    List<TaskItem> findByTenantId(String tenantId);
    Page<TaskItem> findByTenantId(String tenantId, Pageable pageable);
    List<TaskItem> findByTenantIdAndCreatedAtBetween(String tenantId, LocalDateTime from, LocalDateTime to);
    List<TaskItem> findByTenantIdAndOwnerIn(String tenantId, Collection<String> owners);
    List<TaskItem> findByTenantIdAndOwnerInAndCreatedAtBetween(String tenantId, Collection<String> owners, LocalDateTime from, LocalDateTime to);
    Page<TaskItem> findByTenantIdAndDoneOrderByUpdatedAtDesc(String tenantId, Boolean done, Pageable pageable);
    Page<TaskItem> findByTenantIdAndOwnerInAndDoneOrderByUpdatedAtDesc(String tenantId, Collection<String> owners, Boolean done, Pageable pageable);
    List<TaskItem> findTop200ByTenantIdAndDoneOrderByUpdatedAtDesc(String tenantId, Boolean done);
    List<TaskItem> findTop200ByTenantIdAndOwnerInAndDoneOrderByUpdatedAtDesc(String tenantId, Collection<String> owners, Boolean done);
    List<TaskItem> findTop200ByTenantIdAndDoneAndUpdatedAtBetweenOrderByUpdatedAtDesc(
            String tenantId,
            Boolean done,
            LocalDateTime from,
            LocalDateTime to
    );
    List<TaskItem> findTop200ByTenantIdAndOwnerInAndDoneAndUpdatedAtBetweenOrderByUpdatedAtDesc(
            String tenantId,
            Collection<String> owners,
            Boolean done,
            LocalDateTime from,
            LocalDateTime to
    );
    List<TaskItem> findTop8ByTenantIdOrderByUpdatedAtDesc(String tenantId);
    long countByTenantIdAndDoneFalse(String tenantId);
    long countByTenantIdAndDoneTrue(String tenantId);
    long countByTenantIdAndDoneFalseAndOwnerIn(String tenantId, Collection<String> owners);
    long countByTenantIdAndDoneTrueAndOwnerIn(String tenantId, Collection<String> owners);
    long countByTenantIdAndDoneTrueAndCreatedAtBetween(String tenantId, LocalDateTime from, LocalDateTime to);
    long countByTenantIdAndDoneFalseAndCreatedAtBetween(String tenantId, LocalDateTime from, LocalDateTime to);
    long countByTenantIdAndDoneTrueAndOwnerInAndCreatedAtBetween(String tenantId, Collection<String> owners, LocalDateTime from, LocalDateTime to);
    long countByTenantIdAndDoneFalseAndOwnerInAndCreatedAtBetween(String tenantId, Collection<String> owners, LocalDateTime from, LocalDateTime to);
    long countByTenantIdAndDoneFalseAndUpdatedAtBetween(String tenantId, LocalDateTime from, LocalDateTime to);
    long countByTenantIdAndDoneFalseAndOwnerInAndUpdatedAtBetween(String tenantId, Collection<String> owners, LocalDateTime from, LocalDateTime to);
    java.util.Optional<TaskItem> findByIdAndTenantId(String id, String tenantId);
    boolean existsByIdAndTenantId(String id, String tenantId);
}
