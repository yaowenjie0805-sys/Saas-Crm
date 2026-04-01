package com.yao.crm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.entity.Comment;
import com.yao.crm.entity.Team;
import com.yao.crm.repository.CommentRepository;
import com.yao.crm.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 协作服务 - 支持评论、提及、@提及、点赞等功能
 */
@Service
public class CollaborationService {

    private static final Logger log = LoggerFactory.getLogger(CollaborationService.class);

    // @提及正则表达式
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9_]+)");
    // #标签# 正则表达式
    private static final Pattern TAG_PATTERN = Pattern.compile("#([^#]+)#");

    private final CommentRepository commentRepository;
    private final TeamRepository teamRepository;
    private final ObjectMapper objectMapper;
    private final NotificationDispatchService notificationService;

    public CollaborationService(
            CommentRepository commentRepository,
            TeamRepository teamRepository,
            ObjectMapper objectMapper,
            NotificationDispatchService notificationService) {
        this.commentRepository = commentRepository;
        this.teamRepository = teamRepository;
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    /**
     * 添加评论
     */
    @Transactional(timeout = 30)
    public Comment addComment(String tenantId, String entityType, String entityId,
                                String authorId, String authorName, String content,
                                String parentCommentId, Map<String, Object> metadata) {
        // 解析提及
        String resolvedEntityType = entityType;
        String resolvedEntityId = entityId;
        if (parentCommentId != null) {
            Comment parentComment = commentRepository.findByIdAndTenantId(parentCommentId, tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent comment not found"));
            resolvedEntityType = parentComment.getEntityType();
            resolvedEntityId = parentComment.getEntityId();
        }

        List<String> mentions = extractMentions(content);
        List<String> tags = extractTags(content);

        // 创建评论
        Comment comment = new Comment();
        comment.setId(UUID.randomUUID().toString());
        comment.setTenantId(tenantId);
        comment.setEntityType(resolvedEntityType);
        comment.setEntityId(resolvedEntityId);
        comment.setAuthorId(authorId);
        comment.setAuthorName(authorName);
        comment.setContent(content);
        comment.setParentCommentId(parentCommentId);
        try {
            comment.setMentions(objectMapper.writeValueAsString(mentions));
            comment.setTags(objectMapper.writeValueAsString(tags));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize mentions or tags", e);
        }
        comment.setLikeCount(0);
        comment.setReplyCount(0);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());

        // 处理元数据
        if (metadata != null) {
            try {
                comment.setMetadata(objectMapper.writeValueAsString(metadata));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize metadata", e);
            }
        }

        // 保存评论
        comment = commentRepository.save(comment);

        // 更新父评论的回复数（原子操作避免竞态条件）
        if (parentCommentId != null) {
            commentRepository.incrementReplyCount(parentCommentId, tenantId);
        }

        // 发送提及通知
        sendMentionNotifications(comment, mentions);

        // 发送评论通知给实体负责人
        sendCommentNotifications(comment);

        log.info("Comment added: {} for {} {}", comment.getId(), resolvedEntityType, resolvedEntityId);

        return comment;
    }

    /**
     * 回复评论
     */
    @Transactional(timeout = 30)
    public Comment replyToComment(String tenantId, String parentCommentId,
                                  String authorId, String authorName, String content) {
        Comment parentComment = commentRepository.findByIdAndTenantId(parentCommentId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Parent comment not found"));

        return addComment(
                tenantId,
                parentComment.getEntityType(),
                parentComment.getEntityId(),
                authorId,
                authorName,
                content,
                parentCommentId,
                null
        );
    }

    /**
     * 删除评论
     */
    @Transactional(timeout = 30)
    public boolean deleteComment(String tenantId, String commentId, String userId) {
        Comment comment = commentRepository.findByIdAndTenantId(commentId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        // 只能删除自己的评论
        if (!comment.getAuthorId().equals(userId)) {
            throw new IllegalStateException("Cannot delete other user's comment");
        }

        // 更新父评论的回复数（原子操作避免竞态条件）
        if (comment.getParentCommentId() != null) {
            commentRepository.decrementReplyCount(comment.getParentCommentId(), tenantId);
        }

        commentRepository.delete(comment);
        log.info("Comment deleted: {}", commentId);

        return true;
    }

    /**
     * 点赞评论
     */
    @Transactional(timeout = 30)
    public boolean likeComment(String tenantId, String commentId, String userId) {
        Comment comment = commentRepository.findByIdAndTenantId(commentId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        // 解析现有点赞
        Set<String> likedUsers = parseLikedUsers(comment.getLikedUsers());
        if (likedUsers.contains(userId)) {
            // 取消点赞
            likedUsers.remove(userId);
            comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
        } else {
            // 添加点赞
            likedUsers.add(userId);
            comment.setLikeCount(comment.getLikeCount() + 1);

            // 发送点赞通知
            sendLikeNotification(comment, userId);
        }

        comment.setLikedUsers(String.join(",", likedUsers));
        commentRepository.save(comment);

        return likedUsers.contains(userId);
    }

    /**
     * 编辑评论
     */
    @Transactional(timeout = 30)
    public Comment editComment(String tenantId, String commentId, String userId, String newContent) {
        Comment comment = commentRepository.findByIdAndTenantId(commentId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (!comment.getAuthorId().equals(userId)) {
            throw new IllegalStateException("Cannot edit other user's comment");
        }

        // 解析新的提及
        List<String> newMentions = extractMentions(newContent);
        List<String> newTags = extractTags(newContent);

        // 更新内容
        String oldContent = comment.getContent();
        comment.setContent(newContent);
        comment.setEditedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());

        try {
            comment.setMentions(objectMapper.writeValueAsString(newMentions));
            comment.setTags(objectMapper.writeValueAsString(newTags));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize mentions/tags", e);
        }

        comment = commentRepository.save(comment);

        // 通知新增的提及
        List<String> oldMentions = extractMentions(oldContent);
        List<String> addedMentions = newMentions.stream()
                .filter(m -> !oldMentions.contains(m))
                .collect(Collectors.toList());
        if (!addedMentions.isEmpty()) {
            sendMentionNotifications(comment, addedMentions);
        }

        return comment;
    }

    /**
     * 获取实体的评论列表
     */
    /**
     * 鑾峰彇瀹炰綋鐨勮瘎璁哄垪琛?
     */
    public CommentListResult getComments(String tenantId, String entityType, String entityId,
                                        int page, int size, boolean includeReplies) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> topLevelPage = commentRepository
                .findByTenantIdAndEntityTypeAndEntityIdAndParentCommentIdIsNull(
                        tenantId, entityType, entityId, pageable);
        List<Comment> pagedComments = topLevelPage.getContent();

        if (includeReplies && !pagedComments.isEmpty()) {
            List<String> parentIds = pagedComments.stream()
                    .map(Comment::getId)
                    .collect(Collectors.toList());
            List<Comment> replies = commentRepository
                    .findByTenantIdAndEntityTypeAndEntityIdAndParentCommentIdInOrderByCreatedAtDesc(
                            tenantId, entityType, entityId, parentIds);
            Map<String, List<Comment>> repliesByParent = replies.stream()
                    .collect(Collectors.groupingBy(Comment::getParentCommentId, LinkedHashMap::new, Collectors.toList()));

            for (Comment topLevel : pagedComments) {
                topLevel.setReplies(repliesByParent.getOrDefault(topLevel.getId(), Collections.emptyList()));
            }
        }

        CommentListResult result = new CommentListResult();
        long totalTopLevel = commentRepository.countTopLevelComments(tenantId, entityType, entityId);
        result.setComments(pagedComments);
        result.setTotal((int) Math.min(totalTopLevel, Integer.MAX_VALUE));
        result.setPage(page);
        result.setSize(size);
        result.setTotalPages(topLevelPage.getTotalPages());

        return result;
    }

    /**
     * 鑾峰彇@鎻愬強鎴戠殑璇勮
     */
    public List<Comment> getMentions(String tenantId, String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return commentRepository.findByTenantIdAndMentionsContaining(tenantId, userId, pageable)
                .getContent();
    }

    public List<DiscussionSummary> getMyDiscussions(String tenantId, String userId, int limit) {
        List<Comment> myComments = commentRepository.findByTenantIdAndAuthorId(tenantId, userId);

        // 按实体分组
        Map<String, List<Comment>> entityComments = myComments.stream()
                .collect(Collectors.groupingBy(c -> c.getEntityType() + ":" + c.getEntityId()));

        List<DiscussionSummary> discussions = new ArrayList<>();

        for (Map.Entry<String, List<Comment>> entry : entityComments.entrySet()) {
            String[] parts = entry.getKey().split(":");
            String entityType = parts[0];
            String entityId = parts[1];

            List<Comment> comments = entry.getValue();
            Comment latest = comments.stream()
                    .max(Comparator.comparing(Comment::getCreatedAt))
                    .orElse(null);

            DiscussionSummary summary = new DiscussionSummary();
            summary.setEntityType(entityType);
            summary.setEntityId(entityId);
            summary.setCommentCount(comments.size());
            summary.setLatestCommentAt(latest != null ? latest.getCreatedAt() : null);
            summary.setLatestCommentAuthor(latest != null ? latest.getAuthorName() : null);
            summary.setLatestCommentPreview(latest != null ?
                    truncateContent(latest.getContent(), 100) : null);

            discussions.add(summary);
        }

        // 按最新评论时间排序
        discussions.sort((a, b) ->
                (b.getLatestCommentAt() != null ? b.getLatestCommentAt() : LocalDateTime.MIN)
                        .compareTo(a.getLatestCommentAt() != null ? a.getLatestCommentAt() : LocalDateTime.MIN));

        return discussions.subList(0, Math.min(limit, discussions.size()));
    }

    /**
     * 搜索评论
     */
    /**
     * 鎼滅储璇勮鍐呭
     */
    public List<Comment> searchComments(String tenantId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return commentRepository.findByTenantIdAndContentIgnoreCaseContaining(tenantId, keyword, pageable)
                .getContent();
    }

    public CommentStatistics getStatistics(String tenantId, String entityType, String entityId) {
        List<Comment> comments = commentRepository.findByTenantIdAndEntityTypeAndEntityId(
                tenantId, entityType, entityId);

        CommentStatistics stats = new CommentStatistics();
        stats.setTotalComments(comments.size());
        stats.setTopLevelComments((int) comments.stream()
                .filter(c -> c.getParentCommentId() == null).count());
        stats.setReplies((int) comments.stream()
                .filter(c -> c.getParentCommentId() != null).count());
        stats.setTotalLikes(comments.stream()
                .mapToInt(Comment::getLikeCount).sum());
        stats.setUniqueParticipants((int) comments.stream()
                .map(Comment::getAuthorId).distinct().count());

        return stats;
    }

    // ========== 团队协作功能 ==========

    /**
     * 创建团队
     */
    @Transactional(timeout = 30)
    public Team createTeam(String tenantId, String name, String description, String leaderId, List<String> memberIds) {
        Team team = new Team();
        team.setId(UUID.randomUUID().toString());
        team.setTenantId(tenantId);
        team.setName(name);
        team.setDescription(description);
        team.setLeaderId(leaderId);
        team.setMemberIds(String.join(",", memberIds));
        team.setMemberCount(memberIds.size());
        team.setCreatedAt(LocalDateTime.now());
        team.setUpdatedAt(LocalDateTime.now());

        return teamRepository.save(team);
    }

    /**
     * 添加团队成员
     */
    @Transactional(timeout = 30)
    public Team addTeamMember(String tenantId, String teamId, String userId, String role) {
        Team team = teamRepository.findByIdAndTenantId(teamId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        Set<String> members = new HashSet<>(Set.of(team.getMemberIds().split(",")));
        members.add(userId);

        team.setMemberIds(String.join(",", members));
        team.setMemberCount(members.size());
        team.setUpdatedAt(LocalDateTime.now());

        return teamRepository.save(team);
    }

    /**
     * 移除团队成员
     */
    @Transactional(timeout = 30)
    public Team removeTeamMember(String tenantId, String teamId, String userId) {
        Team team = teamRepository.findByIdAndTenantId(teamId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        Set<String> members = new HashSet<>(Set.of(team.getMemberIds().split(",")));
        members.remove(userId);

        team.setMemberIds(String.join(",", members));
        team.setMemberCount(members.size());
        team.setUpdatedAt(LocalDateTime.now());

        return teamRepository.save(team);
    }

    /**
     * 获取用户的团队列表
     */
    public List<Team> getUserTeams(String tenantId, String userId) {
        return teamRepository.findByTenantId(tenantId).stream()
                .filter(t -> t.getMemberIds() != null &&
                        Arrays.asList(t.getMemberIds().split(",")).contains(userId))
                .collect(Collectors.toList());
    }

    // ========== 私有方法 ==========

    /**
     * 提取@提及
     */
    private List<String> extractMentions(String content) {
        List<String> mentions = new ArrayList<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            mentions.add(matcher.group(1));
        }
        return mentions;
    }

    /**
     * 提取#标签#
     */
    private List<String> extractTags(String content) {
        List<String> tags = new ArrayList<>();
        Matcher matcher = TAG_PATTERN.matcher(content);
        while (matcher.find()) {
            tags.add(matcher.group(1));
        }
        return tags;
    }

    /**
     * 解析点赞用户列表
     */
    private Set<String> parseLikedUsers(String likedUsers) {
        if (likedUsers == null || likedUsers.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(likedUsers.split(",")));
    }

    /**
     * 截断内容
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }

    /**
     * 发送提及通知
     */
    private void sendMentionNotifications(Comment comment, List<String> mentions) {
        for (String mentionedUser : mentions) {
            try {
                Map<String, Object> notificationData = new HashMap<>();
                notificationData.put("type", "MENTION");
                notificationData.put("commentId", comment.getId());
                notificationData.put("entityType", comment.getEntityType());
                notificationData.put("entityId", comment.getEntityId());
                notificationData.put("title", "有人在评论中提到了你");
                notificationData.put("content", comment.getAuthorName() + " 在评论中@了你");
                notificationData.put("preview", truncateContent(comment.getContent(), 50));

                notificationService.sendNotification(mentionedUser, "IN_APP", notificationData);
                notificationService.sendNotification(mentionedUser, "WECHAT_WORK", notificationData);
            } catch (Exception e) {
                log.error("Failed to send mention notification to {}", mentionedUser, e);
            }
        }
    }

    /**
     * 发送评论通知
     */
    private void sendCommentNotifications(Comment comment) {
        // 通知实体负责人（这里应该查询实体的负责人）
        // 暂时跳过，具体实现需要结合业务逻辑
    }

    /**
     * 发送点赞通知
     */
    private void sendLikeNotification(Comment comment, String likerId) {
        if (comment.getAuthorId().equals(likerId)) {
            return; // 不通知自己
        }

        try {
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("type", "LIKE");
            notificationData.put("commentId", comment.getId());
            notificationData.put("title", "有人点赞了你的评论");
            notificationData.put("content", "有人点赞了你的评论：" + truncateContent(comment.getContent(), 30));

            notificationService.sendNotification(comment.getAuthorId(), "IN_APP", notificationData);
        } catch (Exception e) {
            log.error("Failed to send like notification", e);
        }
    }

    // ========== 结果类 ==========

    public static class CommentListResult {
        private List<Comment> comments;
        private int total;
        private int page;
        private int size;
        private int totalPages;

        // Getters and Setters
        public List<Comment> getComments() { return comments; }
        public void setComments(List<Comment> comments) { this.comments = comments; }
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    }

    public static class DiscussionSummary {
        private String entityType;
        private String entityId;
        private int commentCount;
        private LocalDateTime latestCommentAt;
        private String latestCommentAuthor;
        private String latestCommentPreview;

        // Getters and Setters
        public String getEntityType() { return entityType; }
        public void setEntityType(String entityType) { this.entityType = entityType; }
        public String getEntityId() { return entityId; }
        public void setEntityId(String entityId) { this.entityId = entityId; }
        public int getCommentCount() { return commentCount; }
        public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
        public LocalDateTime getLatestCommentAt() { return latestCommentAt; }
        public void setLatestCommentAt(LocalDateTime latestCommentAt) { this.latestCommentAt = latestCommentAt; }
        public String getLatestCommentAuthor() { return latestCommentAuthor; }
        public void setLatestCommentAuthor(String latestCommentAuthor) { this.latestCommentAuthor = latestCommentAuthor; }
        public String getLatestCommentPreview() { return latestCommentPreview; }
        public void setLatestCommentPreview(String latestCommentPreview) { this.latestCommentPreview = latestCommentPreview; }
    }

    public static class CommentStatistics {
        private int totalComments;
        private int topLevelComments;
        private int replies;
        private int totalLikes;
        private int uniqueParticipants;

        // Getters and Setters
        public int getTotalComments() { return totalComments; }
        public void setTotalComments(int totalComments) { this.totalComments = totalComments; }
        public int getTopLevelComments() { return topLevelComments; }
        public void setTopLevelComments(int topLevelComments) { this.topLevelComments = topLevelComments; }
        public int getReplies() { return replies; }
        public void setReplies(int replies) { this.replies = replies; }
        public int getTotalLikes() { return totalLikes; }
        public void setTotalLikes(int totalLikes) { this.totalLikes = totalLikes; }
        public int getUniqueParticipants() { return uniqueParticipants; }
        public void setUniqueParticipants(int uniqueParticipants) { this.uniqueParticipants = uniqueParticipants; }
    }
}
