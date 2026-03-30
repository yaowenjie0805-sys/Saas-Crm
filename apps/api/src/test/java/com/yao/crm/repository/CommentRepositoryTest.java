package com.yao.crm.repository;

import com.yao.crm.entity.Comment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void decrementReplyCount_shouldDecreaseWhenReplyCountIsPositive() {
        Comment parent = buildComment("parent-positive", 2);
        entityManager.persistAndFlush(parent);

        commentRepository.decrementReplyCount(parent.getId());
        entityManager.flush();
        entityManager.clear();

        Comment reloaded = commentRepository.findById(parent.getId()).orElseThrow(AssertionError::new);
        assertThat(reloaded.getReplyCount()).isEqualTo(1);
    }

    @Test
    void decrementReplyCount_shouldKeepZeroWhenReplyCountIsZero() {
        Comment parent = buildComment("parent-zero", 0);
        entityManager.persistAndFlush(parent);

        commentRepository.decrementReplyCount(parent.getId());
        entityManager.flush();
        entityManager.clear();

        Comment reloaded = commentRepository.findById(parent.getId()).orElseThrow(AssertionError::new);
        assertThat(reloaded.getReplyCount()).isEqualTo(0);
    }

    private Comment buildComment(String id, int replyCount) {
        LocalDateTime now = LocalDateTime.now();
        Comment comment = new Comment();
        comment.setId(id);
        comment.setTenantId("tenant-1");
        comment.setEntityType("customer");
        comment.setEntityId("entity-1");
        comment.setAuthor("author");
        comment.setAuthorId("author-1");
        comment.setContent("content");
        comment.setLikeCount(0);
        comment.setReplyCount(replyCount);
        comment.setIsDeleted(false);
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        return comment;
    }
}
