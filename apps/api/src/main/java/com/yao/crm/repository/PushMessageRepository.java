package com.yao.crm.repository;

import com.yao.crm.entity.PushMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PushMessageRepository extends JpaRepository<PushMessage, String> {

    List<PushMessage> findByUserIdOrderByCreatedAtDesc(String tenantId, String userId, Pageable pageable);

    @Query("SELECT p FROM PushMessage p WHERE p.tenantId = :tenantId AND p.userId = :userId AND p.status = 'UNREAD' ORDER BY p.createdAt DESC")
    List<PushMessage> findUnreadByUserId(@Param("tenantId") String tenantId, @Param("userId") String userId);

    @Query("SELECT COUNT(p) FROM PushMessage p WHERE p.tenantId = :tenantId AND p.userId = :userId AND p.status = 'UNREAD'")
    long countUnreadByUserId(@Param("tenantId") String tenantId, @Param("userId") String userId);

    @Query("SELECT p FROM PushMessage p WHERE p.tenantId = :tenantId AND p.status = 'FAILED'")
    List<PushMessage> findFailedByTenantId(@Param("tenantId") String tenantId);

    List<PushMessage> findByRelatedTypeAndRelatedId(String relatedType, String relatedId);

    @Query("SELECT p FROM PushMessage p WHERE p.tenantId = :tenantId AND p.userId = :userId ORDER BY p.createdAt DESC")
    List<PushMessage> findRecentByUserId(@Param("tenantId") String tenantId, @Param("userId") String userId, Pageable pageable);
}
