package com.yao.crm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.entity.Comment;
import com.yao.crm.entity.Team;
import com.yao.crm.repository.CommentRepository;
import com.yao.crm.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CollaborationService
 */
@ExtendWith(MockitoExtension.class)
class CollaborationServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private NotificationDispatchService notificationService;

    private ObjectMapper objectMapper;

    private CollaborationService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new CollaborationService(commentRepository, teamRepository, objectMapper, notificationService);
    }

    @Test
    @DisplayName("shouldCreateComment_whenAddComment")
    void shouldCreateComment_whenAddComment() {
        Comment savedComment = new Comment();
        savedComment.setId("comment-123");
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        Comment result = service.addComment(
                "tenant-1", "customer", "cust-1",
                "user-1", "John", "Test content",
                null, null
        );

        assertNotNull(result);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("shouldExtractMentions_whenContentContainsMentions")
    void shouldExtractMentions_whenContentContainsMentions() {
        Comment savedComment = new Comment();
        savedComment.setId("comment-123");
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        service.addComment(
                "tenant-1", "customer", "cust-1",
                "user-1", "John", "Hello @alice @bob",
                null, null
        );

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        Comment saved = captor.getValue();
        
        assertNotNull(saved.getMentions());
        assertTrue(saved.getMentions().contains("alice"));
        assertTrue(saved.getMentions().contains("bob"));
    }

    @Test
    @DisplayName("shouldUpdateParentReplyCount_whenReplyToComment")
    void shouldUpdateParentReplyCount_whenReplyToComment() {
        Comment parentComment = new Comment();
        parentComment.setId("parent-1");
        parentComment.setEntityType("customer");
        parentComment.setEntityId("cust-1");
        parentComment.setReplyCount(0);

        when(commentRepository.findById("parent-1")).thenReturn(Optional.of(parentComment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.replyToComment("tenant-1", "parent-1", "user-2", "Jane", "Reply content");

        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    @DisplayName("shouldThrowException_whenReplyToNonExistentComment")
    void shouldThrowException_whenReplyToNonExistentComment() {
        when(commentRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            service.replyToComment("tenant-1", "non-existent", "user-2", "Jane", "Reply");
        });
    }

    @Test
    @DisplayName("shouldDeleteComment_whenUserIsAuthor")
    void shouldDeleteComment_whenUserIsAuthor() {
        Comment comment = new Comment();
        comment.setId("comment-1");
        comment.setAuthorId("user-1");

        when(commentRepository.findById("comment-1")).thenReturn(Optional.of(comment));

        boolean result = service.deleteComment("comment-1", "user-1");

        assertTrue(result);
        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("shouldThrowException_whenDeleteOtherUserComment")
    void shouldThrowException_whenDeleteOtherUserComment() {
        Comment comment = new Comment();
        comment.setId("comment-1");
        comment.setAuthorId("user-1");

        when(commentRepository.findById("comment-1")).thenReturn(Optional.of(comment));

        assertThrows(IllegalStateException.class, () -> {
            service.deleteComment("comment-1", "user-2");
        });

        verify(commentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("shouldThrowException_whenDeleteNonExistentComment")
    void shouldThrowException_whenDeleteNonExistentComment() {
        when(commentRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            service.deleteComment("non-existent", "user-1");
        });
    }

    @Test
    @DisplayName("shouldAddLike_whenUserLikesComment")
    void shouldAddLike_whenUserLikesComment() {
        Comment comment = new Comment();
        comment.setId("comment-1");
        comment.setAuthorId("author-1");
        comment.setLikeCount(0);
        comment.setLikedUsers("");

        when(commentRepository.findById("comment-1")).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        boolean result = service.likeComment("comment-1", "user-1");

        assertTrue(result);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("shouldRemoveLike_whenUserLikesAgain")
    void shouldRemoveLike_whenUserLikesAgain() {
        Comment comment = new Comment();
        comment.setId("comment-1");
        comment.setAuthorId("author-1");
        comment.setLikeCount(1);
        comment.setLikedUsers("user-1");

        when(commentRepository.findById("comment-1")).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        boolean result = service.likeComment("comment-1", "user-1");

        assertFalse(result);
    }

    @Test
    @DisplayName("shouldEditComment_whenUserIsAuthor")
    void shouldEditComment_whenUserIsAuthor() {
        Comment comment = new Comment();
        comment.setId("comment-1");
        comment.setAuthorId("user-1");
        comment.setContent("Old content");

        when(commentRepository.findById("comment-1")).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        Comment result = service.editComment("comment-1", "user-1", "New content");

        assertEquals("New content", result.getContent());
        assertNotNull(result.getEditedAt());
    }

    @Test
    @DisplayName("shouldThrowException_whenEditOtherUserComment")
    void shouldThrowException_whenEditOtherUserComment() {
        Comment comment = new Comment();
        comment.setId("comment-1");
        comment.setAuthorId("user-1");

        when(commentRepository.findById("comment-1")).thenReturn(Optional.of(comment));

        assertThrows(IllegalStateException.class, () -> {
            service.editComment("comment-1", "user-2", "New content");
        });
    }

    @Test
    @DisplayName("shouldCreateTeam_whenCreateTeam")
    void shouldCreateTeam_whenCreateTeam() {
        Team savedTeam = new Team();
        savedTeam.setId("team-1");
        when(teamRepository.save(any(Team.class))).thenReturn(savedTeam);

        List<String> memberIds = Arrays.asList("user-1", "user-2");
        Team result = service.createTeam("tenant-1", "Sales Team", "Description", "leader-1", memberIds);

        assertNotNull(result);
        verify(teamRepository).save(any(Team.class));
    }

    @Test
    @DisplayName("shouldAddMember_whenAddTeamMember")
    void shouldAddMember_whenAddTeamMember() {
        Team team = new Team();
        team.setId("team-1");
        team.setMemberIds("user-1,user-2");
        team.setMemberCount(2);

        when(teamRepository.findById("team-1")).thenReturn(Optional.of(team));
        when(teamRepository.save(any(Team.class))).thenReturn(team);

        Team result = service.addTeamMember("team-1", "user-3", "member");

        assertEquals(3, result.getMemberCount());
        assertTrue(result.getMemberIds().contains("user-3"));
    }

    @Test
    @DisplayName("shouldRemoveMember_whenRemoveTeamMember")
    void shouldRemoveMember_whenRemoveTeamMember() {
        Team team = new Team();
        team.setId("team-1");
        team.setMemberIds("user-1,user-2,user-3");
        team.setMemberCount(3);

        when(teamRepository.findById("team-1")).thenReturn(Optional.of(team));
        when(teamRepository.save(any(Team.class))).thenReturn(team);

        Team result = service.removeTeamMember("team-1", "user-2");

        assertEquals(2, result.getMemberCount());
        assertFalse(result.getMemberIds().contains("user-2"));
    }

    @Test
    @DisplayName("shouldThrowException_whenAddMemberToNonExistentTeam")
    void shouldThrowException_whenAddMemberToNonExistentTeam() {
        when(teamRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            service.addTeamMember("non-existent", "user-1", "member");
        });
    }
}
