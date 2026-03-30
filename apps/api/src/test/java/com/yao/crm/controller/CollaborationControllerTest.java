package com.yao.crm.controller;

import com.yao.crm.entity.Comment;
import com.yao.crm.entity.Team;
import com.yao.crm.service.ApprovalDelegationService;
import com.yao.crm.service.CollaborationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollaborationControllerTest {

    @Mock
    private CollaborationService collaborationService;

    @Mock
    private ApprovalDelegationService delegationService;

    private CollaborationController controller;

    @BeforeEach
    void setUp() {
        controller = new CollaborationController(collaborationService, delegationService);
    }

    @Test
    void addCommentShouldPreferAuthUsernameOverBodyAuthorId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        CollaborationController.AddCommentRequest body = new CollaborationController.AddCommentRequest();
        body.entityType = "customer";
        body.entityId = "cust-1";
        body.authorId = "legacy-author";
        body.authorName = "Alice";
        body.content = "hello world";

        Comment saved = new Comment();
        saved.setId("comment-1");
        when(collaborationService.addComment(
                eq("tenant-1"),
                eq("customer"),
                eq("cust-1"),
                eq("alice"),
                eq("Alice"),
                eq("hello world"),
                isNull(),
                isNull()
        )).thenReturn(saved);

        ResponseEntity<?> response = controller.addComment(request, "tenant-1", body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(collaborationService).addComment(
                eq("tenant-1"),
                eq("customer"),
                eq("cust-1"),
                eq("alice"),
                eq("Alice"),
                eq("hello world"),
                isNull(),
                isNull()
        );
    }

    @Test
    void addCommentShouldTrimTenantIdBeforePassingToService() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        CollaborationController.AddCommentRequest body = new CollaborationController.AddCommentRequest();
        body.entityType = "customer";
        body.entityId = "cust-1";
        body.authorName = "Alice";
        body.content = "hello world";

        Comment saved = new Comment();
        saved.setId("comment-1");
        when(collaborationService.addComment(
                eq("tenant-1"),
                eq("customer"),
                eq("cust-1"),
                eq("alice"),
                eq("Alice"),
                eq("hello world"),
                isNull(),
                isNull()
        )).thenReturn(saved);

        ResponseEntity<?> response = controller.addComment(request, "  tenant-1  ", body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(collaborationService).addComment(
                eq("tenant-1"),
                eq("customer"),
                eq("cust-1"),
                eq("alice"),
                eq("Alice"),
                eq("hello world"),
                isNull(),
                isNull()
        );
    }

    @Test
    void addCommentShouldReturnBadRequestWhenUserContextIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        CollaborationController.AddCommentRequest body = new CollaborationController.AddCommentRequest();
        body.entityType = "customer";
        body.entityId = "cust-1";
        body.content = "hello world";

        ResponseEntity<?> response = controller.addComment(request, "tenant-1", body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(collaborationService);
    }

    @Test
    void addCommentShouldReturnBadRequestWhenTenantIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        CollaborationController.AddCommentRequest body = new CollaborationController.AddCommentRequest();
        body.entityType = "customer";
        body.entityId = "cust-1";
        body.authorId = "legacy-author";
        body.authorName = "Alice";
        body.content = "hello world";

        ResponseEntity<?> response = controller.addComment(request, "   ", body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(collaborationService);
        verifyNoInteractions(delegationService);
    }

    @Test
    void replyToCommentShouldReturnBadRequestWhenCommentIdIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        CollaborationController.ReplyCommentRequest body = new CollaborationController.ReplyCommentRequest();
        body.authorId = "legacy-author";
        body.authorName = "Alice";
        body.content = "reply";

        ResponseEntity<?> response = controller.replyToComment(request, "tenant-1", "   ", body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(collaborationService);
        verifyNoInteractions(delegationService);
    }

    @Test
    void deleteCommentShouldReturnNoContentAndPreferAuthUsernameOverLegacyUserId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        when(collaborationService.deleteComment("comment-1", "alice")).thenReturn(true);

        ResponseEntity<?> response = controller.deleteComment(request, "comment-1", "legacy-user");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(collaborationService).deleteComment("comment-1", "alice");
    }

    @Test
    void deleteCommentShouldReturnNotFoundWhenDeleteFails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        when(collaborationService.deleteComment("comment-1", "alice")).thenReturn(false);

        ResponseEntity<?> response = controller.deleteComment(request, "comment-1", "legacy-user");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(collaborationService).deleteComment("comment-1", "alice");
    }

    @Test
    void deleteCommentShouldReturnBadRequestWhenUserContextIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<?> response = controller.deleteComment(request, "comment-1", null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(collaborationService);
    }

    @Test
    void editCommentShouldPreferAuthUsernameOverBodyUserId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        CollaborationController.EditCommentRequest body = new CollaborationController.EditCommentRequest();
        body.userId = "legacy-user";
        body.newContent = "updated";

        Comment updated = new Comment();
        updated.setId("comment-1");
        updated.setContent("updated");
        when(collaborationService.editComment("comment-1", "alice", "updated")).thenReturn(updated);

        ResponseEntity<?> response = controller.editComment(request, "comment-1", body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(collaborationService).editComment("comment-1", "alice", "updated");
    }

    @Test
    void getMentionsShouldPreferAuthUsernameOverQueryUserId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");
        when(collaborationService.getMentions("tenant-1", "alice", 0, 20)).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = controller.getMentions(request, " tenant-1 ", "legacy-user", 0, 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(collaborationService).getMentions("tenant-1", "alice", 0, 20);
    }

    @Test
    void getMentionsShouldReturnBadRequestWhenTenantIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        ResponseEntity<?> response = controller.getMentions(request, "   ", "legacy-user", 0, 20);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(collaborationService);
        verifyNoInteractions(delegationService);
    }

    @Test
    void getCommentsShouldReturnBadRequestWhenEntityTypeIsBlank() {
        ResponseEntity<?> response = controller.getComments("tenant-1", "  ", "cust-1", 0, 20, true);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(collaborationService);
        verifyNoInteractions(delegationService);
    }

    @Test
    void getMyDiscussionsShouldPreferAuthUsernameOverQueryUserId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");
        when(collaborationService.getMyDiscussions("tenant-1", "alice", 20)).thenReturn(new ArrayList<>());

        ResponseEntity<?> response = controller.getMyDiscussions(request, "tenant-1", "legacy-user", 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(collaborationService).getMyDiscussions("tenant-1", "alice", 20);
    }

    @Test
    void getMyDiscussionsShouldReturnBadRequestWhenTenantIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        ResponseEntity<?> response = controller.getMyDiscussions(request, "", "legacy-user", 20);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(collaborationService);
        verifyNoInteractions(delegationService);
    }

    @Test
    void getTeamsShouldPreferAuthUsernameOverQueryUserId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        Team team = new Team();
        team.setId("team-1");
        when(collaborationService.getUserTeams("tenant-1", "alice")).thenReturn(Collections.singletonList(team));

        ResponseEntity<?> response = controller.getTeams(request, "tenant-1", "legacy-user");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(collaborationService).getUserTeams("tenant-1", "alice");
    }

    @Test
    void getTeamsShouldFallbackToQueryUserIdWhenAuthUsernameMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        when(collaborationService.getUserTeams("tenant-1", "legacy-user")).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = controller.getTeams(request, "tenant-1", "legacy-user");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(collaborationService).getUserTeams("tenant-1", "legacy-user");
    }

    @Test
    void getTeamsShouldReturnBadRequestWhenTenantIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        ResponseEntity<?> response = controller.getTeams(request, "", "legacy-user");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(collaborationService);
        verifyNoInteractions(delegationService);
    }

    @Test
    void getTeamShouldReturnBadRequestWhenTeamIdIsBlank() {
        ResponseEntity<?> response = controller.getTeam("   ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(collaborationService);
        verifyNoInteractions(delegationService);
    }

    @Test
    void addTeamMemberShouldReturnBadRequestWhenTeamIdIsBlank() {
        CollaborationController.AddMemberRequest body = new CollaborationController.AddMemberRequest();
        body.userId = "bob";
        body.role = "MEMBER";

        ResponseEntity<?> response = controller.addTeamMember("   ", body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(collaborationService);
        verifyNoInteractions(delegationService);
    }

    @Test
    void removeTeamMemberShouldReturnBadRequestWhenTeamIdIsBlank() {
        ResponseEntity<?> response = controller.removeTeamMember(" ", "user-1");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(collaborationService);
        verifyNoInteractions(delegationService);
    }

    @Test
    void removeTeamMemberShouldReturnBadRequestWhenUserIdIsBlank() {
        ResponseEntity<?> response = controller.removeTeamMember("team-1", "   ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(collaborationService);
        verifyNoInteractions(delegationService);
    }

    @Test
    void getDelegationHistoryShouldReturnBadRequestWhenTaskIdIsBlank() {
        ResponseEntity<?> response = controller.getDelegationHistory("   ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(collaborationService);
        verifyNoInteractions(delegationService);
    }

    @Test
    void getTransferHistoryShouldReturnBadRequestWhenTaskIdIsBlank() {
        ResponseEntity<?> response = controller.getTransferHistory("\t");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(collaborationService);
        verifyNoInteractions(delegationService);
    }

    @Test
    void createTeamShouldPreferAuthUsernameOverBodyLeaderId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        CollaborationController.CreateTeamRequest body = new CollaborationController.CreateTeamRequest();
        body.name = "Ops Team";
        body.description = "Operations";
        body.leaderId = "legacy-leader";
        body.memberIds = Arrays.asList("alice", "bob");

        Team team = new Team();
        team.setId("team-1");
        when(collaborationService.createTeam("tenant-1", "Ops Team", "Operations", "alice", body.memberIds))
                .thenReturn(team);

        ResponseEntity<?> response = controller.createTeam(request, "tenant-1", body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(collaborationService).createTeam("tenant-1", "Ops Team", "Operations", "alice", body.memberIds);
    }

    @Test
    void createTeamShouldTrimTenantIdBeforePassingToService() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        CollaborationController.CreateTeamRequest body = new CollaborationController.CreateTeamRequest();
        body.name = "Ops Team";
        body.description = "Operations";
        body.memberIds = Arrays.asList("alice", "bob");

        Team team = new Team();
        team.setId("team-1");
        when(collaborationService.createTeam("tenant-1", "Ops Team", "Operations", "alice", body.memberIds))
                .thenReturn(team);

        ResponseEntity<?> response = controller.createTeam(request, " tenant-1 ", body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(collaborationService).createTeam("tenant-1", "Ops Team", "Operations", "alice", body.memberIds);
    }

    @Test
    void createTeamShouldReturnBadRequestWhenTenantIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        CollaborationController.CreateTeamRequest body = new CollaborationController.CreateTeamRequest();
        body.name = "Ops Team";
        body.description = "Operations";
        body.leaderId = "legacy-leader";
        body.memberIds = Arrays.asList("alice", "bob");

        ResponseEntity<?> response = controller.createTeam(request, " ", body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(collaborationService);
        verifyNoInteractions(delegationService);
    }

    @Test
    void delegateTaskShouldPreferAuthUsernameOverBodyFromUserId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        CollaborationController.DelegateRequest body = new CollaborationController.DelegateRequest();
        body.fromUserId = "legacy-from";
        body.toUserId = "bob";
        body.reason = "handoff";

        ApprovalDelegationService.DelegationResult result = new ApprovalDelegationService.DelegationResult();
        result.setSuccess(true);
        when(delegationService.delegateTask("task-1", "alice", "bob", "handoff")).thenReturn(result);

        ResponseEntity<?> response = controller.delegateTask(request, "task-1", body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(delegationService).delegateTask("task-1", "alice", "bob", "handoff");
    }

    @Test
    void addSignShouldPreferAuthUsernameOverBodyApproverId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        CollaborationController.AddSignRequest body = new CollaborationController.AddSignRequest();
        body.approverId = "legacy-approver";
        body.addSignUserId = "bob";
        body.reason = "need review";
        body.type = "AFTER";

        ApprovalDelegationService.AddSignResult result = new ApprovalDelegationService.AddSignResult();
        result.setSuccess(true);
        when(delegationService.addSign("task-1", "alice", "bob", "need review", "AFTER")).thenReturn(result);

        ResponseEntity<?> response = controller.addSign(request, "task-1", body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(delegationService).addSign("task-1", "alice", "bob", "need review", "AFTER");
    }

    @Test
    void transferTaskShouldPreferAuthUsernameOverBodyFromUserId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        CollaborationController.TransferRequest body = new CollaborationController.TransferRequest();
        body.fromUserId = "legacy-from";
        body.toUserId = "bob";
        body.reason = "handoff";

        ApprovalDelegationService.TransferResult result = new ApprovalDelegationService.TransferResult();
        result.setSuccess(true);
        when(delegationService.transferTask("task-1", "alice", "bob", "handoff")).thenReturn(result);

        ResponseEntity<?> response = controller.transferTask(request, "task-1", body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(delegationService).transferTask("task-1", "alice", "bob", "handoff");
    }

    @Test
    void delegateTaskShouldReturnBadRequestWhenTaskIdIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        CollaborationController.DelegateRequest body = new CollaborationController.DelegateRequest();
        body.fromUserId = "legacy-from";
        body.toUserId = "bob";
        body.reason = "handoff";

        ResponseEntity<?> response = controller.delegateTask(request, " ", body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(collaborationService);
        verifyNoInteractions(delegationService);
    }

    @Test
    void addSignShouldReturnBadRequestWhenTaskIdIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        CollaborationController.AddSignRequest body = new CollaborationController.AddSignRequest();
        body.approverId = "legacy-approver";
        body.addSignUserId = "bob";
        body.reason = "need review";
        body.type = "AFTER";

        ResponseEntity<?> response = controller.addSign(request, "   ", body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(collaborationService);
        verifyNoInteractions(delegationService);
    }

    @Test
    void transferTaskShouldReturnBadRequestWhenTaskIdIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        CollaborationController.TransferRequest body = new CollaborationController.TransferRequest();
        body.fromUserId = "legacy-from";
        body.toUserId = "bob";
        body.reason = "handoff";

        ResponseEntity<?> response = controller.transferTask(request, "\t", body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(collaborationService);
        verifyNoInteractions(delegationService);
    }

    @Test
    void recallDelegationShouldPreferAuthUsernameOverLegacyUserId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");
        when(delegationService.recallDelegation("delegation-1", "alice")).thenReturn(true);

        ResponseEntity<?> response = controller.recallDelegation(request, "delegation-1", "legacy-user");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(delegationService).recallDelegation("delegation-1", "alice");
    }

    @Test
    void recallDelegationShouldReturnBadRequestWhenDelegationIdIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        ResponseEntity<?> response = controller.recallDelegation(request, "  ", "legacy-user");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(collaborationService);
        verifyNoInteractions(delegationService);
    }

    @Test
    void getDelegatableUsersShouldPreferAuthUsernameOverLegacyCurrentUserId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");
        when(delegationService.getDelegatableUsers("tenant-1", "alice")).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = controller.getDelegatableUsers(request, "tenant-1", "legacy-user");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(delegationService).getDelegatableUsers("tenant-1", "alice");
    }

    @Test
    void getDelegatableUsersShouldTrimTenantIdBeforePassingToService() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");
        when(delegationService.getDelegatableUsers("tenant-1", "alice")).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = controller.getDelegatableUsers(request, "  tenant-1  ", "legacy-user");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(delegationService).getDelegatableUsers("tenant-1", "alice");
    }
}

