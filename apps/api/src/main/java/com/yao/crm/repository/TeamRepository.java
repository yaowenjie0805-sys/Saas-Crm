package com.yao.crm.repository;

import com.yao.crm.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 团队Repository
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, String> {

    /**
     * 根据租户ID查询团队列表
     */
    List<Team> findByTenantId(String tenantId);

    /**
     * 根据租户ID分页查询
     */
    Page<Team> findByTenantId(String tenantId, Pageable pageable);

    /**
     * 根据团队名称模糊查询
     */
    List<Team> findByTenantIdAndNameContaining(String tenantId, String name);

    /**
     * 根据负责人查询团队
     */
    List<Team> findByTenantIdAndLeaderId(String tenantId, String leaderId);

    /**
     * 根据成员ID查询团队
     */
    @Query("SELECT t FROM Team t WHERE t.tenantId = :tenantId AND t.memberIds LIKE %:userId%")
    List<Team> findByTenantIdAndMemberId(@Param("tenantId") String tenantId, @Param("userId") String userId);

    /**
     * 根据租户ID和团队ID查询
     */
    Optional<Team> findByIdAndTenantId(String id, String tenantId);

    /**
     * 检查团队名称是否存在
     */
    boolean existsByTenantIdAndName(String tenantId, String name);

    /**
     * 统计团队成员数量
     */
    @Query("SELECT COUNT(t) FROM Team t WHERE t.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") String tenantId);
}
