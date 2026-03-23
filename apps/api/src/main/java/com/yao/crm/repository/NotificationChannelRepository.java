package com.yao.crm.repository;

import com.yao.crm.entity.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 通知渠道配置Repository
 */
@Repository
public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, String> {

    /**
     * 根据租户ID查询
     */
    List<NotificationChannel> findByTenantId(String tenantId);

    /**
     * 根据租户ID和类型查询
     */
    List<NotificationChannel> findByTenantIdAndChannelType(String tenantId, String channelType);

    /**
     * 获取租户启用的渠道
     */
    List<NotificationChannel> findByTenantIdAndEnabledTrue(String tenantId);

    /**
     * 获取默认渠道
     */
    Optional<NotificationChannel> findByTenantIdAndChannelTypeAndIsDefaultTrue(String tenantId, String channelType);

    /**
     * 获取最高优先级的渠道
     */
    @Query("SELECT c FROM NotificationChannel c WHERE c.tenantId = :tenantId " +
           "AND c.channelType = :channelType AND c.enabled = true " +
           "ORDER BY c.priority DESC")
    List<NotificationChannel> findTopByTenantAndChannelType(@Param("tenantId") String tenantId,
                                                              @Param("channelType") String channelType);

    /**
     * 检查渠道是否存在
     */
    boolean existsByTenantIdAndChannelTypeAndChannelName(String tenantId, String channelType, String channelName);

    /**
     * 检查租户是否有启用的渠道
     */
    @Query("SELECT COUNT(c) > 0 FROM NotificationChannel c WHERE c.tenantId = :tenantId AND c.enabled = true")
    boolean hasEnabledChannel(@Param("tenantId") String tenantId);
}
