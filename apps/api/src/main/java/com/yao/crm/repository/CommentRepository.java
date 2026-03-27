package com.yao.crm.repository;

import com.yao.crm.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 评论Repository
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {

    /**
     * 根据租户ID查询所有评论
     */
    List<Comment> findByTenantId(String tenantId);

    /**
     * 根据租户ID分页查询
     */
    Page<Comment> findByTenantId(String tenantId, Pageable pageable);

    /**
     * 根据实体查询评论
     */
    List<Comment> findByTenantIdAndEntityTypeAndEntityId(String tenantId, String entityType, String entityId);

    /**
     * 根据实体分页查询
     */
    Page<Comment> findByTenantIdAndEntityTypeAndEntityId(
            String tenantId, String entityType, String entityId, Pageable pageable);

    /**
     * 查询顶级评论（无父评论）
     */
    List<Comment> findByTenantIdAndEntityTypeAndEntityIdAndParentCommentIdIsNull(
            String tenantId, String entityType, String entityId);

    /**
     * 查询回复
     */
    List<Comment> findByTenantIdAndEntityTypeAndEntityIdAndParentCommentId(
            String tenantId, String entityType, String entityId, String parentCommentId);

    /**
     * 根据作者ID查询评论
     */
    List<Comment> findByTenantIdAndAuthorId(String tenantId, String authorId);

    /**
     * 根据作者ID分页查询
     */
    Page<Comment> findByTenantIdAndAuthorId(String tenantId, String authorId, Pageable pageable);

    /**
     * 根据实体类型查询
     */
    List<Comment> findByTenantIdAndEntityType(String tenantId, String entityType);

    /**
     * 根据实体类型分页查询
     */
    Page<Comment> findByTenantIdAndEntityType(String tenantId, String entityType, Pageable pageable);

    /**
     * 搜索评论内容
     */
    @Query("SELECT c FROM Comment c WHERE c.tenantId = :tenantId AND c.content LIKE %:keyword%")
    List<Comment> searchByContent(@Param("tenantId") String tenantId, @Param("keyword") String keyword);

    /**
     * 统计实体的评论数
     */
    long countByTenantIdAndEntityTypeAndEntityId(String tenantId, String entityType, String entityId);

    /**
     * 统计顶级评论数
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.tenantId = :tenantId AND c.entityType = :entityType " +
           "AND c.entityId = :entityId AND c.parentCommentId IS NULL")
    long countTopLevelComments(@Param("tenantId") String tenantId,
                               @Param("entityType") String entityType,
                               @Param("entityId") String entityId);

    /**
     * 统计回复数
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.tenantId = :tenantId AND c.entityType = :entityType " +
           "AND c.entityId = :entityId AND c.parentCommentId IS NOT NULL")
    long countReplies(@Param("tenantId") String tenantId,
                      @Param("entityType") String entityType,
                      @Param("entityId") String entityId);

    /**
     * 统计总点赞数
     */
    @Query("SELECT COALESCE(SUM(c.likeCount), 0) FROM Comment c WHERE c.tenantId = :tenantId " +
           "AND c.entityType = :entityType AND c.entityId = :entityId")
    long sumLikes(@Param("tenantId") String tenantId,
                  @Param("entityType") String entityType,
                  @Param("entityId") String entityId);

    /**
     * 获取参与讨论的用户数
     */
    @Query("SELECT COUNT(DISTINCT c.authorId) FROM Comment c WHERE c.tenantId = :tenantId " +
           "AND c.entityType = :entityType AND c.entityId = :entityId")
    long countUniqueParticipants(@Param("tenantId") String tenantId,
                                 @Param("entityType") String entityType,
                                 @Param("entityId") String entityId);

    /**
     * 查询最近评论
     */
    @Query("SELECT c FROM Comment c WHERE c.tenantId = :tenantId AND c.entityType = :entityType " +
           "AND c.entityId = :entityId ORDER BY c.createdAt DESC")
    List<Comment> findRecentComments(@Param("tenantId") String tenantId,
                                     @Param("entityType") String entityType,
                                     @Param("entityId") String entityId,
                                     Pageable pageable);

    /**
     * 根据ID列表批量查询
     */
    List<Comment> findByIdIn(List<String> ids);

    /**
     * 检查评论是否存在
     */
    boolean existsByIdAndAuthorId(String id, String authorId);

    /**
     * 删除实体所有评论
     */
    void deleteByTenantIdAndEntityTypeAndEntityId(String tenantId, String entityType, String entityId);

    /**
     * 原子递增父评论的回复计数
     */
    @Modifying
    @Query("UPDATE Comment c SET c.replyCount = c.replyCount + 1 WHERE c.id = :parentId")
    void incrementReplyCount(@Param("parentId") String parentId);

    /**
     * 原子递减父评论的回复计数
     */
    @Modifying
    @Query("UPDATE Comment c SET c.replyCount = CASE WHEN c.replyCount > 0 THEN c.replyCount - 1 ELSE 0 END WHERE c.id = :parentId")
    void decrementReplyCount(@Param("parentId") String parentId);
}
