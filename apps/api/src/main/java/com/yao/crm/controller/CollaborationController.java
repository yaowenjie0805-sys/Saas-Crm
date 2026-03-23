package com.yao.crm.controller;

import com.yao.crm.entity.Comment;
import com.yao.crm.entity.Team;
import com.yao.crm.service.CollaborationService;
import com.yao.crm.service.ApprovalDelegationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 协作控制器 - 提供评论、提及、团队协作等功能
 */
@RestController
@RequestMapping("/api/v2/collaboration")
public class CollaborationController {

    private final CollaborationService collaborationService;
    private final ApprovalDelegationService delegationService;

    public CollaborationController(
            CollaborationService collaborationService,
            ApprovalDelegationService delegationService) {
        this.collaborationService = collaborationService;
        this.delegationService = delegationService;
    }

    // ========== 评论 API ==========

    /**
     * 添加评论
     */
    @PostMapping("/comments")
    public ResponseEntity<?> addComment(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestBody AddCommentRequest request) {

        Comment comment = collaborationService.addComment(
                tenantId,
                request.entityType,
                request.entityId,
                request.authorId,
                request.authorName,
                request.content,
                request.parentCommentId,
                request.metadata
        );

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("comment", comment);
        return ResponseEntity.ok(result);
    }

    /**
     * 回复评论
     */
    @PostMapping("/comments/{commentId}/reply")
    public ResponseEntity<?> replyToComment(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String commentId,
            @RequestBody ReplyCommentRequest request) {

        Comment reply = collaborationService.replyToComment(
                tenantId,
                commentId,
                request.authorId,
                request.authorName,
                request.content
        );

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("comment", reply);
        return ResponseEntity.ok(result);
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable String commentId,
            @RequestParam String userId) {

        boolean deleted = collaborationService.deleteComment(commentId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", deleted);
        return ResponseEntity.ok(result);
    }

    /**
     * 点赞/取消点赞评论
     */
    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<?> likeComment(
            @PathVariable String commentId,
            @RequestParam String userId) {

        boolean isLiked = collaborationService.likeComment(commentId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("isLiked", isLiked);
        return ResponseEntity.ok(result);
    }

    /**
     * 编辑评论
     */
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<?> editComment(
            @PathVariable String commentId,
            @RequestBody EditCommentRequest request) {

        Comment comment = collaborationService.editComment(
                commentId,
                request.userId,
                request.newContent
        );

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("comment", comment);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取实体的评论列表
     */
    @GetMapping("/entities/{entityType}/{entityId}/comments")
    public ResponseEntity<?> getComments(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String entityType,
            @PathVariable String entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "true") boolean includeReplies) {

        CollaborationService.CommentListResult result = collaborationService.getComments(
                tenantId, entityType, entityId, page, size, includeReplies
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("comments", result.getComments());
        response.put("total", result.getTotal());
        response.put("page", result.getPage());
        response.put("size", result.getSize());
        response.put("totalPages", result.getTotalPages());
        return ResponseEntity.ok(response);
    }

    /**
     * 获取@提及我的评论
     */
    @GetMapping("/mentions")
    public ResponseEntity<?> getMentions(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<Comment> mentions = collaborationService.getMentions(tenantId, userId, page, size);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("mentions", mentions);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取我参与的讨论
     */
    @GetMapping("/discussions")
    public ResponseEntity<?> getMyDiscussions(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam String userId,
            @RequestParam(defaultValue = "20") int limit) {

        List<CollaborationService.DiscussionSummary> discussions =
                collaborationService.getMyDiscussions(tenantId, userId, limit);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("discussions", discussions);
        return ResponseEntity.ok(result);
    }

    /**
     * 搜索评论
     */
    @GetMapping("/comments/search")
    public ResponseEntity<?> searchComments(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<Comment> results = collaborationService.searchComments(tenantId, keyword, page, size);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("results", results);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取评论统计
     */
    @GetMapping("/entities/{entityType}/{entityId}/comments/stats")
    public ResponseEntity<?> getCommentStats(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String entityType,
            @PathVariable String entityId) {

        CollaborationService.CommentStatistics stats =
                collaborationService.getStatistics(tenantId, entityType, entityId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("stats", stats);
        return ResponseEntity.ok(result);
    }

    // ========== 团队 API ==========

    /**
     * 创建团队
     */
    @PostMapping("/teams")
    public ResponseEntity<?> createTeam(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestBody CreateTeamRequest request) {

        Team team = collaborationService.createTeam(
                tenantId,
                request.name,
                request.description,
                request.leaderId,
                request.memberIds
        );

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("team", team);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取团队列表
     */
    @GetMapping("/teams")
    public ResponseEntity<?> getTeams(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) String userId) {

        List<Team> teams;
        if (userId != null && !userId.isEmpty()) {
            teams = collaborationService.getUserTeams(tenantId, userId);
        } else {
            // 返回所有团队（需要实现）
            teams = new ArrayList<>();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("teams", teams);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取团队详情
     */
    @GetMapping("/teams/{teamId}")
    public ResponseEntity<?> getTeam(@PathVariable String teamId) {
        // 需要实现 TeamRepository.findById
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("teamId", teamId);
        return ResponseEntity.ok(result);
    }

    /**
     * 添加团队成员
     */
    @PostMapping("/teams/{teamId}/members")
    public ResponseEntity<?> addTeamMember(
            @PathVariable String teamId,
            @RequestBody AddMemberRequest request) {

        Team team = collaborationService.addTeamMember(teamId, request.userId, request.role);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("team", team);
        return ResponseEntity.ok(result);
    }

    /**
     * 移除团队成员
     */
    @DeleteMapping("/teams/{teamId}/members/{userId}")
    public ResponseEntity<?> removeTeamMember(
            @PathVariable String teamId,
            @PathVariable String userId) {

        Team team = collaborationService.removeTeamMember(teamId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("team", team);
        return ResponseEntity.ok(result);
    }

    // ========== 审批委托 API ==========

    /**
     * 委托审批任务
     */
    @PostMapping("/approval/tasks/{taskId}/delegate")
    public ResponseEntity<?> delegateTask(
            @PathVariable String taskId,
            @RequestBody DelegateRequest request) {

        ApprovalDelegationService.DelegationResult result = delegationService.delegateTask(
                taskId,
                request.fromUserId,
                request.toUserId,
                request.reason
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("result", result);
        return ResponseEntity.ok(response);
    }

    /**
     * 加签审批
     */
    @PostMapping("/approval/tasks/{taskId}/add-sign")
    public ResponseEntity<?> addSign(
            @PathVariable String taskId,
            @RequestBody AddSignRequest request) {

        ApprovalDelegationService.AddSignResult result = delegationService.addSign(
                taskId,
                request.approverId,
                request.addSignUserId,
                request.reason,
                request.type
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("result", result);
        return ResponseEntity.ok(response);
    }

    /**
     * 转交审批任务
     */
    @PostMapping("/approval/tasks/{taskId}/transfer")
    public ResponseEntity<?> transferTask(
            @PathVariable String taskId,
            @RequestBody TransferRequest request) {

        ApprovalDelegationService.TransferResult result = delegationService.transferTask(
                taskId,
                request.fromUserId,
                request.toUserId,
                request.reason
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("result", result);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取委托历史
     */
    @GetMapping("/approval/tasks/{taskId}/delegations")
    public ResponseEntity<?> getDelegationHistory(@PathVariable String taskId) {
        List<ApprovalDelegationService.DelegationRecord> history =
                delegationService.getDelegationHistory(taskId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("history", history);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取转交历史
     */
    @GetMapping("/approval/tasks/{taskId}/transfers")
    public ResponseEntity<?> getTransferHistory(@PathVariable String taskId) {
        List<ApprovalDelegationService.TransferRecord> history =
                delegationService.getTransferHistory(taskId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("history", history);
        return ResponseEntity.ok(result);
    }

    /**
     * 撤回委托
     */
    @PostMapping("/approval/delegations/{delegationId}/recall")
    public ResponseEntity<?> recallDelegation(
            @PathVariable String delegationId,
            @RequestParam String userId) {

        boolean recalled = delegationService.recallDelegation(delegationId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", recalled);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取可委托的用户列表
     */
    @GetMapping("/approval/tasks/delegatable-users")
    public ResponseEntity<?> getDelegatableUsers(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam String currentUserId) {

        List<Map<String, String>> users = delegationService.getDelegatableUsers(tenantId, currentUserId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("users", users);
        return ResponseEntity.ok(result);
    }

    // ========== Request DTOs ==========

    public static class AddCommentRequest {
        public String entityType;
        public String entityId;
        public String authorId;
        public String authorName;
        public String content;
        public String parentCommentId;
        public Map<String, Object> metadata;
    }

    public static class ReplyCommentRequest {
        public String authorId;
        public String authorName;
        public String content;
    }

    public static class EditCommentRequest {
        public String userId;
        public String newContent;
    }

    public static class CreateTeamRequest {
        public String name;
        public String description;
        public String leaderId;
        public List<String> memberIds;
    }

    public static class AddMemberRequest {
        public String userId;
        public String role = "MEMBER";
    }

    public static class DelegateRequest {
        public String fromUserId;
        public String toUserId;
        public String reason;
    }

    public static class AddSignRequest {
        public String approverId;
        public String addSignUserId;
        public String reason;
        public String type = "AFTER"; // BEFORE 或 AFTER
    }

    public static class TransferRequest {
        public String fromUserId;
        public String toUserId;
        public String reason;
    }
}
