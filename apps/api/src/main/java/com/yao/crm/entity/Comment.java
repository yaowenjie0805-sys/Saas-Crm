package com.yao.crm.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 璇勮瀹炰綋 - 鍥㈤槦鍗忎綔鍔熻兘
 */
@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 40)
    private String entityType;

    @Column(nullable = false, length = 64)
    private String entityId;

    @Column(nullable = false, length = 80)
    private String author;

    @Column(length = 120)
    private String authorName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 64)
    private String parentId;

    @Column(columnDefinition = "TEXT")
    private String mentions;

    @Column(columnDefinition = "TEXT")
    private String mentionsUsers;

    @Column
    private Integer likeCount;

    @Column(length = 64)
    private String authorId;

    @Column
    private Integer replyCount;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(columnDefinition = "TEXT")
    private String likedUsers;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column
    private LocalDateTime editedAt;

    @Column(length = 64)
    private String parentCommentId;

    @Column(nullable = false)
    private Boolean isDeleted;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 鐬椂瀛楁锛屼笉瀛樺偍
    @Transient
    private java.util.List<Comment> replies;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (id == null) id = java.util.UUID.randomUUID().toString();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (likeCount == null) likeCount = 0;
        if (isDeleted == null) isDeleted = false;
        if (tenantId == null || tenantId.trim().isEmpty()) throw new IllegalStateException("tenant_id_required");
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getMentions() { return mentions; }
    public void setMentions(String mentions) { this.mentions = mentions; }

    public String getMentionsUsers() { return mentionsUsers; }
    public void setMentionsUsers(String mentionsUsers) { this.mentionsUsers = mentionsUsers; }

    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public Integer getReplyCount() { return replyCount; }
    public void setReplyCount(Integer replyCount) { this.replyCount = replyCount; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getLikedUsers() { return likedUsers; }
    public void setLikedUsers(String likedUsers) { this.likedUsers = likedUsers; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public LocalDateTime getEditedAt() { return editedAt; }
    public void setEditedAt(LocalDateTime editedAt) { this.editedAt = editedAt; }

    public String getParentCommentId() { return parentCommentId; }
    public void setParentCommentId(String parentCommentId) { this.parentCommentId = parentCommentId; }

    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<Comment> getReplies() { return replies; }
    public void setReplies(List<Comment> replies) { this.replies = replies; }
}
