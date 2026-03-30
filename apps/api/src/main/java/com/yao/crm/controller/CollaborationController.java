package com.yao.crm.controller;

import com.yao.crm.entity.Comment;
import com.yao.crm.entity.Team;
import com.yao.crm.service.CollaborationService;
import com.yao.crm.service.ApprovalDelegationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 闁告绻嬬紞鏃堝箳瑜嶉崺妤呭闯?- 闁圭粯鍔掔欢鐢垫嫚閸曨噮鍟堥柕鍡曠劍瑜颁線宕ｆ繛搴撳亾娴ｅ憡绀嬮梻鍐枎瀹曟鎷呭鍛惣闁告梻鍠曢崗?
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

    // ========== 閻犲洤瀚?API ==========

    /**
     * 婵烇綀顕ф慨鐐垫嫚閸曨噮鍟?
     */
    @PostMapping("/comments")
    public ResponseEntity<?> addComment(
            HttpServletRequest httpRequest,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestBody AddCommentRequest request) {
        String normalizedTenantId = normalizeRequiredValue(tenantId);
        if (normalizedTenantId == null) {
            return ResponseEntity.badRequest().build();
        }

        String normalizedEntityType = normalizeRequiredValue(request.entityType);
        String normalizedEntityId = normalizeRequiredValue(request.entityId);
        if (normalizedEntityType == null || normalizedEntityId == null) {
            return ResponseEntity.badRequest().build();
        }

        String currentUser = resolveCurrentUser(httpRequest, request.authorId);
        if (currentUser == null) {
            return ResponseEntity.badRequest().build();
        }

        Comment comment = collaborationService.addComment(
                normalizedTenantId,
                normalizedEntityType,
                normalizedEntityId,
                currentUser,
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
     * 闁搞儳鍋涢ˇ鑼嫚閸曨噮鍟?
     */
    @PostMapping("/comments/{commentId}/reply")
    public ResponseEntity<?> replyToComment(
            HttpServletRequest httpRequest,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String commentId,
            @RequestBody ReplyCommentRequest replyRequest) {
        String normalizedTenantId = normalizeRequiredValue(tenantId);
        if (normalizedTenantId == null) {
            return ResponseEntity.badRequest().build();
        }

        String normalizedCommentId = normalizeRequiredValue(commentId);
        if (normalizedCommentId == null) {
            return ResponseEntity.badRequest().build();
        }

        String currentUser = resolveCurrentUser(httpRequest, replyRequest.authorId);
        if (currentUser == null) {
            return ResponseEntity.badRequest().build();
        }

        Comment reply = collaborationService.replyToComment(
                normalizedTenantId,
                normalizedCommentId,
                currentUser,
                replyRequest.authorName,
                replyRequest.content
        );

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("comment", reply);
        return ResponseEntity.ok(result);
    }

    /**
     * 闁告帞濞€濞呭海鎷犻崟顕呭晥
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(
            HttpServletRequest httpRequest,
            @PathVariable String commentId,
            @RequestParam(required = false) String userId) {
        String normalizedCommentId = normalizeRequiredValue(commentId);
        if (normalizedCommentId == null) {
            return ResponseEntity.badRequest().build();
        }

        String currentUser = resolveCurrentUser(httpRequest, userId);
        if (currentUser == null) {
            return ResponseEntity.badRequest().build();
        }

        boolean deleted = collaborationService.deleteComment(normalizedCommentId, currentUser);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * 闁绘劗顢婄粋?闁告瑦鐗楃粔鐑芥倷绾懐顩甸悹鍥у椤?
     */
    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<?> likeComment(
            HttpServletRequest httpRequest,
            @PathVariable String commentId,
            @RequestParam(required = false) String userId) {
        String normalizedCommentId = normalizeRequiredValue(commentId);
        if (normalizedCommentId == null) {
            return ResponseEntity.badRequest().build();
        }

        String currentUser = resolveCurrentUser(httpRequest, userId);
        if (currentUser == null) {
            return ResponseEntity.badRequest().build();
        }

        boolean isLiked = collaborationService.likeComment(normalizedCommentId, currentUser);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("isLiked", isLiked);
        return ResponseEntity.ok(result);
    }

    /**
     * 缂傚倹鐗炵欢顐ゆ嫚閸曨噮鍟?
     */
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<?> editComment(
            HttpServletRequest httpRequest,
            @PathVariable String commentId,
            @RequestBody EditCommentRequest editRequest) {
        String normalizedCommentId = normalizeRequiredValue(commentId);
        if (normalizedCommentId == null) {
            return ResponseEntity.badRequest().build();
        }

        String currentUser = resolveCurrentUser(httpRequest, editRequest.userId);
        if (currentUser == null) {
            return ResponseEntity.badRequest().build();
        }

        Comment comment = collaborationService.editComment(
                normalizedCommentId,
                currentUser,
                editRequest.newContent
        );

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("comment", comment);
        return ResponseEntity.ok(result);
    }

    /**
     * 闁兼儳鍢茶ぐ鍥┾偓鍦仒缂嶅鎯冮崟顔炬閻犱礁鎼崹顏嗘偘?
     */
    @GetMapping("/entities/{entityType}/{entityId}/comments")
    public ResponseEntity<?> getComments(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String entityType,
            @PathVariable String entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "true") boolean includeReplies) {
        String normalizedTenantId = normalizeRequiredValue(tenantId);
        if (normalizedTenantId == null) {
            return ResponseEntity.badRequest().build();
        }

        String normalizedEntityType = normalizeRequiredValue(entityType);
        String normalizedEntityId = normalizeRequiredValue(entityId);
        if (normalizedEntityType == null || normalizedEntityId == null) {
            return ResponseEntity.badRequest().build();
        }

        CollaborationService.CommentListResult result = collaborationService.getComments(
                normalizedTenantId, normalizedEntityType, normalizedEntityId, page, size, includeReplies
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
     * 闁兼儳鍢茶ぐ鍢嗛柟缁樺姇瀵兘骞嬮幋鐘崇暠閻犲洤瀚?
     */
    @GetMapping("/mentions")
    public ResponseEntity<?> getMentions(
            HttpServletRequest httpRequest,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String normalizedTenantId = normalizeRequiredValue(tenantId);
        if (normalizedTenantId == null) {
            return ResponseEntity.badRequest().build();
        }

        String currentUser = resolveCurrentUser(httpRequest, userId);
        if (currentUser == null) {
            return ResponseEntity.badRequest().build();
        }

        List<Comment> mentions = collaborationService.getMentions(
                normalizedTenantId, currentUser, page, size);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("mentions", mentions);
        return ResponseEntity.ok(result);
    }

    /**
     * 闁兼儳鍢茶ぐ鍥箣閹存繂妫樺☉鎾虫捣濞堟垹鎷嬮妸顭戝晥
     */
    @GetMapping("/discussions")
    public ResponseEntity<?> getMyDiscussions(
            HttpServletRequest httpRequest,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam String userId,
            @RequestParam(defaultValue = "20") int limit) {
        String normalizedTenantId = normalizeRequiredValue(tenantId);
        if (normalizedTenantId == null) {
            return ResponseEntity.badRequest().build();
        }

        String currentUser = resolveCurrentUser(httpRequest, userId);
        if (currentUser == null) {
            return ResponseEntity.badRequest().build();
        }

        List<CollaborationService.DiscussionSummary> discussions =
                collaborationService.getMyDiscussions(
                        normalizedTenantId, currentUser, limit);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("discussions", discussions);
        return ResponseEntity.ok(result);
    }

    /**
     * 闁瑰吋绮庨崒銊ф嫚閸曨噮鍟?
     */
    @GetMapping("/comments/search")
    public ResponseEntity<?> searchComments(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String normalizedTenantId = normalizeRequiredValue(tenantId);
        if (normalizedTenantId == null) {
            return ResponseEntity.badRequest().build();
        }

        List<Comment> results = collaborationService.searchComments(normalizedTenantId, keyword, page, size);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("results", results);
        return ResponseEntity.ok(result);
    }

    /**
     * 闁兼儳鍢茶ぐ鍥╂嫚閸曨噮鍟堢紓浣哄枙椤?
     */
    @GetMapping("/entities/{entityType}/{entityId}/comments/stats")
    public ResponseEntity<?> getCommentStats(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String entityType,
            @PathVariable String entityId) {
        String normalizedTenantId = normalizeRequiredValue(tenantId);
        if (normalizedTenantId == null) {
            return ResponseEntity.badRequest().build();
        }

        String normalizedEntityType = normalizeRequiredValue(entityType);
        String normalizedEntityId = normalizeRequiredValue(entityId);
        if (normalizedEntityType == null || normalizedEntityId == null) {
            return ResponseEntity.badRequest().build();
        }

        CollaborationService.CommentStatistics stats =
                collaborationService.getStatistics(normalizedTenantId, normalizedEntityType, normalizedEntityId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("stats", stats);
        return ResponseEntity.ok(result);
    }

    // ========== 闁搞儯鍨藉Σ?API ==========

    /**
     * 闁告帗绋戠紓鎾诲炊閵忋倖袝
     */
    @PostMapping("/teams")
    public ResponseEntity<?> createTeam(
            HttpServletRequest httpRequest,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestBody CreateTeamRequest request) {
        String normalizedTenantId = normalizeRequiredValue(tenantId);
        if (normalizedTenantId == null) {
            return ResponseEntity.badRequest().build();
        }

        String currentUser = resolveCurrentUser(httpRequest, request.leaderId);
        if (currentUser == null) {
            return ResponseEntity.badRequest().build();
        }

        Team team = collaborationService.createTeam(
                normalizedTenantId,
                request.name,
                request.description,
                currentUser,
                request.memberIds
        );

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("team", team);
        return ResponseEntity.ok(result);
    }

    /**
     * 闁兼儳鍢茶ぐ鍥炊閵忋倖袝闁告帗顨夐妴?
     */
    @GetMapping("/teams")
    public ResponseEntity<?> getTeams(
            HttpServletRequest httpRequest,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) String userId) {
        String normalizedTenantId = normalizeRequiredValue(tenantId);
        if (normalizedTenantId == null) {
            return ResponseEntity.badRequest().build();
        }

        List<Team> teams;
        String currentUser = resolveCurrentUser(httpRequest, userId);
        if (currentUser != null) {
            teams = collaborationService.getUserTeams(normalizedTenantId, currentUser);
        } else {
            // 返回所有团队（空 userId 时不查询服务）
            teams = new ArrayList<>();
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("teams", teams);
        return ResponseEntity.ok(result);
    }
    @GetMapping("/teams/{teamId}")
    public ResponseEntity<?> getTeam(@PathVariable String teamId) {
        String normalizedTeamId = normalizeRequiredValue(teamId);
        if (normalizedTeamId == null) {
            return ResponseEntity.badRequest().build();
        }

        // 闂傚洠鍋撻悷鏇氱閻ゅ嫰鎮?TeamRepository.findById
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("teamId", normalizedTeamId);
        return ResponseEntity.ok(result);
    }
    /**
     * 婵烇綀顕ф慨鐐哄炊閵忋倖袝闁瑰瓨鍔曢幉?
     */
    @PostMapping("/teams/{teamId}/members")
    public ResponseEntity<?> addTeamMember(
            @PathVariable String teamId,
            @RequestBody AddMemberRequest request) {
        String normalizedTeamId = normalizeRequiredValue(teamId);
        if (normalizedTeamId == null) {
            return ResponseEntity.badRequest().build();
        }

        String normalizedUserId = normalizeRequiredValue(request.userId);
        if (normalizedUserId == null) {
            return ResponseEntity.badRequest().build();
        }

        Team team = collaborationService.addTeamMember(normalizedTeamId, normalizedUserId, request.role);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("team", team);
        return ResponseEntity.ok(result);
    }
    /**
     * 缂佸顭峰▍搴ㄥ炊閵忋倖袝闁瑰瓨鍔曢幉?
     */
    @DeleteMapping("/teams/{teamId}/members/{userId}")
    public ResponseEntity<?> removeTeamMember(
            @PathVariable String teamId,
            @PathVariable String userId) {
        String normalizedTeamId = normalizeRequiredValue(teamId);
        if (normalizedTeamId == null) {
            return ResponseEntity.badRequest().build();
        }

        String normalizedUserId = normalizeRequiredValue(userId);
        if (normalizedUserId == null) {
            return ResponseEntity.badRequest().build();
        }

        Team team = collaborationService.removeTeamMember(normalizedTeamId, normalizedUserId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("team", team);
        return ResponseEntity.ok(result);
    }
    // ========== 閻庡厜鍓濇竟鎺撴叏閺冣偓婢?API ==========

    /**
     * 濠殿喗姊规晶顓犫偓鍏夊墲婢规帗绂掔拠鎻掝潳
     */
    @PostMapping("/approval/tasks/{taskId}/delegate")
    public ResponseEntity<?> delegateTask(
            HttpServletRequest httpRequest,
            @PathVariable String taskId,
            @RequestBody DelegateRequest request) {
        String normalizedTaskId = normalizeRequiredValue(taskId);
        if (normalizedTaskId == null) {
            return ResponseEntity.badRequest().build();
        }

        String currentUser = resolveCurrentUser(httpRequest, request.fromUserId);
        if (currentUser == null) {
            return ResponseEntity.badRequest().build();
        }

        ApprovalDelegationService.DelegationResult result = delegationService.delegateTask(
                normalizedTaskId,
                currentUser,
                request.toUserId,
                request.reason
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("result", result);
        return ResponseEntity.ok(response);
    }

    /**
     * 闁告梻濮烽椋庘偓鍏夊墲婢?
     */
    @PostMapping("/approval/tasks/{taskId}/add-sign")
    public ResponseEntity<?> addSign(
            HttpServletRequest httpRequest,
            @PathVariable String taskId,
            @RequestBody AddSignRequest request) {
        String normalizedTaskId = normalizeRequiredValue(taskId);
        if (normalizedTaskId == null) {
            return ResponseEntity.badRequest().build();
        }

        String currentUser = resolveCurrentUser(httpRequest, request.approverId);
        if (currentUser == null) {
            return ResponseEntity.badRequest().build();
        }

        ApprovalDelegationService.AddSignResult result = delegationService.addSign(
                normalizedTaskId,
                currentUser,
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
     * 閺夌儐鍏涘锔锯偓鍏夊墲婢规帗绂掔拠鎻掝潳
     */
    @PostMapping("/approval/tasks/{taskId}/transfer")
    public ResponseEntity<?> transferTask(
            HttpServletRequest httpRequest,
            @PathVariable String taskId,
            @RequestBody TransferRequest request) {
        String normalizedTaskId = normalizeRequiredValue(taskId);
        if (normalizedTaskId == null) {
            return ResponseEntity.badRequest().build();
        }

        String currentUser = resolveCurrentUser(httpRequest, request.fromUserId);
        if (currentUser == null) {
            return ResponseEntity.badRequest().build();
        }

        ApprovalDelegationService.TransferResult result = delegationService.transferTask(
                normalizedTaskId,
                currentUser,
                request.toUserId,
                request.reason
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("result", result);
        return ResponseEntity.ok(response);
    }

    /**
     * 闁兼儳鍢茶ぐ鍥ㄦ叏閺冣偓婢ь參宕㈤崱妤€钑?
     */
    @GetMapping("/approval/tasks/{taskId}/delegations")
    public ResponseEntity<?> getDelegationHistory(@PathVariable String taskId) {
        String normalizedTaskId = normalizeRequiredValue(taskId);
        if (normalizedTaskId == null) {
            return ResponseEntity.badRequest().build();
        }

        List<ApprovalDelegationService.DelegationRecord> history =
                delegationService.getDelegationHistory(normalizedTaskId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("history", history);
        return ResponseEntity.ok(result);
    }

    /**
     * 闁兼儳鍢茶ぐ鍥ㄦ姜椤戞寧鍞夐柛妯烘瑜?
     */
    @GetMapping("/approval/tasks/{taskId}/transfers")
    public ResponseEntity<?> getTransferHistory(@PathVariable String taskId) {
        String normalizedTaskId = normalizeRequiredValue(taskId);
        if (normalizedTaskId == null) {
            return ResponseEntity.badRequest().build();
        }

        List<ApprovalDelegationService.TransferRecord> history =
                delegationService.getTransferHistory(normalizedTaskId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("history", history);
        return ResponseEntity.ok(result);
    }

    /**
     * 闁逛勘鍊曞ú鏍ㄦ叏閺冣偓婢?
     */
    @PostMapping("/approval/delegations/{delegationId}/recall")
    public ResponseEntity<?> recallDelegation(
            HttpServletRequest httpRequest,
            @PathVariable String delegationId,
            @RequestParam String userId) {
        String normalizedDelegationId = normalizeRequiredValue(delegationId);
        if (normalizedDelegationId == null) {
            return ResponseEntity.badRequest().build();
        }

        String currentUser = resolveCurrentUser(httpRequest, userId);
        if (currentUser == null) {
            return ResponseEntity.badRequest().build();
        }

        boolean recalled = delegationService.recallDelegation(normalizedDelegationId, currentUser);
        Map<String, Object> result = new HashMap<>();
        result.put("success", recalled);
        return ResponseEntity.ok(result);
    }

    /**
     * 闁兼儳鍢茶ぐ鍥矗椤栨凹娼ら柟鍨焽濞堟垿鎮介妸锕€鐓曢柛鎺擃殙閵?
     */
    @GetMapping("/approval/tasks/delegatable-users")
    public ResponseEntity<?> getDelegatableUsers(
            HttpServletRequest httpRequest,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam String currentUserId) {
        String normalizedTenantId = normalizeRequiredValue(tenantId);
        if (normalizedTenantId == null) {
            return ResponseEntity.badRequest().build();
        }

        String currentUser = resolveCurrentUser(httpRequest, currentUserId);
        if (currentUser == null) {
            return ResponseEntity.badRequest().build();
        }

        List<Map<String, String>> users = delegationService.getDelegatableUsers(
                normalizedTenantId, currentUser);
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
        public String type = "AFTER"; // BEFORE 闁?AFTER
    }

    public static class TransferRequest {
        public String fromUserId;
        public String toUserId;
        public String reason;
    }

    private String resolveCurrentUser(HttpServletRequest request, String legacyUserId) {
        Object authUsername = request == null ? null : request.getAttribute("authUsername");
        if (authUsername != null && !String.valueOf(authUsername).trim().isEmpty()) {
            return String.valueOf(authUsername).trim();
        }
        if (legacyUserId != null && !legacyUserId.trim().isEmpty()) {
            return legacyUserId.trim();
        }
        return null;
    }

    private String normalizeRequiredValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}


