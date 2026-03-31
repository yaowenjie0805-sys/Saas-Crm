I'm using the writing-plans skill to create the implementation plan.

# CollaborationService Pagination Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace in-memory filtering/pagination for `CollaborationService#getComments`, `getMentions`, and `searchComments` with database-backed queries while keeping their signatures & response shapes unchanged.

**Architecture:** Repository methods will expose paged finders for top-level comments, replies, mentions, and keyword searches; the service will build `PageRequest`s and hydrate reply maps from batched reply queries. Tests will assert the new repository usage and page metadata.

**Tech Stack:** Java 17, Spring Data JPA, Mockito, JUnit 5, Maven.

---

### Task 1: Update CommentRepository with paged helpers

**Files:**
- Modify: `apps/api/src/main/java/com/yao/crm/repository/CommentRepository.java`
- Test: None (repositories are validated indirectly via service tests below)

- [ ] **Step 1: Add the new repository method declarations**

```java
Page<Comment> findByTenantIdAndEntityTypeAndEntityIdAndParentCommentIdIsNull(
        String tenantId,
        String entityType,
        String entityId,
        Pageable pageable);

List<Comment> findByParentCommentIdInOrderByCreatedAtDesc(List<String> parentIds);

Page<Comment> findByTenantIdAndMentionsContaining(String tenantId, String mention, Pageable pageable);

Page<Comment> findByTenantIdAndContentIgnoreCaseContaining(String tenantId, String keyword, Pageable pageable);
```

- [ ] **Step 2: (Optional) Ensure the interface compiles**

Run: `./mvnw -pl apps/api -Dtest=CollaborationServiceTest test`

Expected: `BUILD SUCCESS` for this step is not necessary yet; we will rely on the full service test run later.

- [ ] **Step 3: Commit**

```bash
git add apps/api/src/main/java/com/yao/crm/repository/CommentRepository.java
git commit -m "feat: add paged comment queries"
```

### Task 2: Replace getComments logic with DB pagination and reply batching

**Files:**
- Modify: `apps/api/src/main/java/com/yao/crm/service/CollaborationService.java`
- Test: `apps/api/src/test/java/com/yao/crm/service/CollaborationServiceTest.java`

- [ ] **Step 1: Write failing test that expects paged top-level calls and reply hydration**

```java
@Test
void shouldPageTopLevelCommentsAndLoadReplies() {
    Pageable findPageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "createdAt"));
    Comment topA = new Comment(); topA.setId("top-1");
    Comment topB = new Comment(); topB.setId("top-2");

    when(commentRepository.findByTenantIdAndEntityTypeAndEntityIdAndParentCommentIdIsNull(
            eq("tenant-1"), eq("customer"), eq("cust-1"), eq(findPageable)))
            .thenReturn(new PageImpl<>(List.of(topA, topB), findPageable, 5));
    when(commentRepository.countTopLevelComments("tenant-1", "customer", "cust-1")).thenReturn(5L);

    List<Comment> replies = List.of(new Comment(), new Comment());
    when(commentRepository.findByParentCommentIdInOrderByCreatedAtDesc(List.of("top-1", "top-2")))
            .thenReturn(replies);

    CollaborationService.CommentListResult result = service.getComments("tenant-1", "customer", "cust-1", 1, 2, true);

    assertEquals(5, result.getTotal());
    assertEquals(2, result.getComments().size());
    verify(commentRepository).findByTenantIdAndEntityTypeAndEntityIdAndParentCommentIdIsNull(
            eq("tenant-1"), eq("customer"), eq("cust-1"), eq(findPageable));
    verify(commentRepository).findByParentCommentIdInOrderByCreatedAtDesc(List.of("top-1", "top-2"));
}
```

- [ ] **Step 2: Run the new test to ensure it fails (since the service still uses the old logic)**

Run: `./mvnw -pl apps/api -Dtest=CollaborationServiceTest#shouldPageTopLevelCommentsAndLoadReplies test`

Expected: FAIL with `Wanted but not invoked` because the old logic does not use the new repository methods.

- [ ] **Step 3: Implement service changes using the paged repository methods and reply batching**

```java
Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
Page<Comment> page = commentRepository.findByTenantIdAndEntityTypeAndEntityIdAndParentCommentIdIsNull(
        tenantId, entityType, entityId, pageable);
List<Comment> pagedComments = page.getContent();
if (includeReplies && !pagedComments.isEmpty()) {
    List<String> parentIds = pagedComments.stream().map(Comment::getId).collect(Collectors.toList());
    List<Comment> replies = commentRepository.findByParentCommentIdInOrderByCreatedAtDesc(parentIds);
    Map<String, List<Comment>> repliesByParent = replies.stream()
            .collect(Collectors.groupingBy(Comment::getParentCommentId, LinkedHashMap::new, Collectors.toList()));
    pagedComments.forEach(c -> c.setReplies(repliesByParent.getOrDefault(c.getId(), Collections.emptyList())));
}
CommentListResult result = new CommentListResult();
result.setComments(pagedComments);
result.setTotal((int) commentRepository.countTopLevelComments(tenantId, entityType, entityId));
result.setPage(page);
result.setSize(size);
result.setTotalPages(page.getTotalPages());
return result;
```

- [ ] **Step 4: Run the test again and expect it to pass**

Run: `./mvnw -pl apps/api -Dtest=CollaborationServiceTest#shouldPageTopLevelCommentsAndLoadReplies test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add apps/api/src/main/java/com/yao/crm/service/CollaborationService.java apps/api/src/test/java/com/yao/crm/service/CollaborationServiceTest.java
git commit -m "feat: use paged queries for comments"
```

### Task 3: Switch getMentions and searchComments to paged repository queries

**Files:**
- Modify: `apps/api/src/main/java/com/yao/crm/service/CollaborationService.java`
- Modify: `apps/api/src/test/java/com/yao/crm/service/CollaborationServiceTest.java`

- [ ] **Step 1: Add failing tests for mentions/search to assert repository pagination**

```java
@Test
void shouldPageMentionsViaRepository() {
    Pageable pageable = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "createdAt"));
    List<Comment> mentions = List.of(new Comment());
    when(commentRepository.findByTenantIdAndMentionsContaining(eq("tenant-1"), eq("user-1"), eq(pageable)))
            .thenReturn(new PageImpl<>(mentions, pageable, 1));

    List<Comment> result = service.getMentions("tenant-1", "user-1", 0, 3);

    assertEquals(mentions, result);
    verify(commentRepository).findByTenantIdAndMentionsContaining("tenant-1", "user-1", pageable);
}

@Test
void shouldPageSearchResultsViaRepository() {
    Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
    List<Comment> matches = List.of(new Comment());
    when(commentRepository.findByTenantIdAndContentIgnoreCaseContaining(
            eq("tenant-1"), eq("keyword"), eq(pageable)))
            .thenReturn(new PageImpl<>(matches, pageable, 1));

    List<Comment> result = service.searchComments("tenant-1", "keyword", 0, 5);

    assertEquals(matches, result);
}
```

- [ ] **Step 2: Run tests to see them fail (mentions/search still use in-memory lists)**

Run: `./mvnw -pl apps/api -Dtest=CollaborationServiceTest#shouldPageMentionsViaRepository,CollaborationServiceTest#shouldPageSearchResultsViaRepository test`

Expected: FAIL because the service still queries everything.

- [ ] **Step 3: Update `getMentions` and `searchComments` to use the paged repository queries**

```java
Pageable mentionsPageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
return commentRepository.findByTenantIdAndMentionsContaining(tenantId, userId, mentionsPageable)
        .getContent();

Pageable searchPageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
return commentRepository.findByTenantIdAndContentIgnoreCaseContaining(tenantId, keyword, searchPageable)
        .getContent();
```

- [ ] **Step 4: Rerun the two tests and expect PASS**

Run: `./mvnw -pl apps/api -Dtest=CollaborationServiceTest#shouldPageMentionsViaRepository,CollaborationServiceTest#shouldPageSearchResultsViaRepository test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add apps/api/src/main/java/com/yao/crm/service/CollaborationService.java \
        apps/api/src/test/java/com/yao/crm/service/CollaborationServiceTest.java
git commit -m "feat: paginate mentions and search"
```

### Task 4: Final verification

**Files:**
- None (this is a validation step)

- [ ] **Step 1: Run the suite of service tests covering all new logic**

Run: `./mvnw -pl apps/api -Dtest=CollaborationServiceTest test`

Expected: PASS (confirms the new tests plus existing ones still succeed)

- [ ] **Step 2: Commit if any remaining changes emerge (unlikely)**

```bash
# If additional files were updated during verification:
git add <changed files>
git commit -m "fix: adjust after tests"
```

---

Plan complete and saved to `docs/superpowers/plans/2026-03-31-collaboration-service-optimizations-plan.md`. Two execution options:

1. Subagent-Driven (recommended) - dispatch a subagent per task with superpowers:subagent-driven-development.
2. Inline Execution - continue work in this session via superpowers:executing-plans.

I will proceed with Inline Execution unless you prefer otherwise.
