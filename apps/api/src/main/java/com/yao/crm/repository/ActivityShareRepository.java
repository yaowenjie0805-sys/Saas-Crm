package com.yao.crm.repository;

import com.yao.crm.entity.ActivityShare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 活动分享Repository
 */
@Repository
public interface ActivityShareRepository extends JpaRepository<ActivityShare, String> {

    /**
     * 根据租户ID查询
     */
    List<ActivityShare> findByTenantId(String tenantId);

    /**
     * 根据租户ID分页查询
     */
    Page<ActivityShare> findByTenantId(String tenantId, Pageable pageable);

    /**
     * 根据分享目标查询
     */
    List<ActivityShare> findByTenantIdAndShareTypeAndShareTarget(
            String tenantId, String shareType, String shareTarget);

    /**
     * 根据实体查询分享记录
     */
    List<ActivityShare> findByTenantIdAndEntityTypeAndEntityId(
            String tenantId, String entityType, String entityId);

    /**
     * 根据分享人查询
     */
    List<ActivityShare> findByTenantIdAndSharedBy(String tenantId, String sharedBy);

    /**
     * 查询最近的分享记录
     */
    @Query("SELECT a FROM ActivityShare a WHERE a.tenantId = :tenantId ORDER BY a.createdAt DESC")
    List<ActivityShare> findRecentByTenantId(@Param("tenantId") String tenantId, Pageable pageable);

    /**
     * 统计分享次数
     */
    long countByTenantIdAndEntityTypeAndEntityId(String tenantId, String entityType, String entityId);

    /**
     * 删除实体所有分享记录
     */
    void deleteByTenantIdAndEntityTypeAndEntityId(String tenantId, String entityType, String entityId);
}
