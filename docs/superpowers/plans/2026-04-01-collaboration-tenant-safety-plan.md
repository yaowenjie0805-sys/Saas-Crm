I'm using the writing-plans skill to create the implementation plan.

# Collaboration Tenant Safety Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ensure comment mutations and team membership diffs stay tenant-scoped by threading `X-Tenant-Id` from the controller through the service into tenant-aware repository calls.

**Architecture:** Controllers demand the tenant header and forward the normalized value to `CollaborationService`, which uses tenant-filtered repository methods so the database never returns records that belong to other tenants.

**Tech Stack:** Java 17, Spring Boot, Spring Data JPA, Mockito, JUnit 5, Maven

---

### Task 1: Add tenant-aware comment repository usage

**Files:**
- Modify: `apps/api/src/main/java/com/yao/crm/repository/CommentRepository.java`
- Modify: `apps/api/src/main/java/com/yao/crm/service/CollaborationService.java`
- Modify: `apps/api/src/test/java/com/yao/crm/service/CollaborationServiceTest.java`

- [ ] **Step 1: Update the unit tests to call the future `findByIdAndTenantId` signature**
```java
when(commentRepository.findByIdAndTenantId("comment-1", "tenant-1"))
        .thenReturn(Optional.of(comment));
boolean result = service.deleteComment("tenant-1", "comment-1", "user-1");
```
Mirror this pattern in the `likeComment`, `editComment`, and `replyToComment` branches so the tests exhibit the desired tenant parameter.

- [ ] **Step 2: Run the targeted failing test**
Run: `mvn -f apps/api/pom.xml -Dtest=CollaborationServiceTest#shouldDeleteComment_whenUserIsAuthor test`
Expected: FAIL because `findByIdAndTenantId` does not exist yet and the service still calls the old signature.

- [ ] **Step 3: Implement the missing method and switch the service off of `findById`**
```java
Optional<Comment> findByIdAndTenantId(String id, String tenantId);
```
and
```java
Comment comment = commentRepository.findByIdAndTenantId(commentId, tenantId)
        .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
```
Apply this change in `replyToComment`, `deleteComment`, `likeComment`, and `editComment` so the comments loaded belong to the tenant in context.

- [ ] **Step 4: Re-run the same unit test**
Run: `mvn -f apps/api/pom.xml -Dtest=CollaborationServiceTest#shouldDeleteComment_whenUserIsAuthor test`
Expected: PASS because the repository method now exists and the service signature matches.

- [ ] **Step 5: Commit the repo/service/test trio**
```bash
git add apps/api/src/main/java/com/yao/crm/repository/CommentRepository.java \
        apps/api/src/main/java/com/yao/crm/service/CollaborationService.java \
        apps/api/src/test/java/com/yao/crm/service/CollaborationServiceTest.java
git commit -m "feat: tenant-scope collaboration comments"
```

### Task 2: Tenant-guard team membership changes

**Files:**
- Modify: `apps/api/src/main/java/com/yao/crm/service/CollaborationService.java`
- Modify: `apps/api/src/test/java/com/yao/crm/service/CollaborationServiceTest.java`

- [ ] **Step 1: Update tests to reflect the new tenant-first signatures**
```java
when(teamRepository.findByIdAndTenantId("team-1", "tenant-1"))
        .thenReturn(Optional.of(team));
Team result = service.addTeamMember("tenant-1", "team-1", "user-3", "member");
```
Repeat for `removeTeamMember` and add expectations on `findByIdAndTenantId` so tests fail until code changes.

- [ ] **Step 2: Run the relevant test**
Run: `mvn -f apps/api/pom.xml -Dtest=CollaborationServiceTest#shouldAddMember_whenAddTeamMember test`
Expected: FAIL because the service still calls `findById` and the signature lacks a tenant argument.

- [ ] **Step 3: Update `CollaborationService` to accept `tenantId` for team membership mutations and use the tenant-aware finder**
```java
public Team addTeamMember(String tenantId, String teamId, String userId, String role)
```
```java
Team team = teamRepository.findByIdAndTenantId(teamId, tenantId)
        .orElseThrow(() -> new IllegalArgumentException("Team not found"));
```
Do the same for `removeTeamMember` and keep the member manipulation logic intact.

- [ ] **Step 4: Re-run the failing test**
Run: `mvn -f apps/api/pom.xml -Dtest=CollaborationServiceTest#shouldAddMember_whenAddTeamMember test`
Expected: PASS because the service now matches the new signatures.

- [ ] **Step 5: Commit the service/test updates**
```bash
git add apps/api/src/main/java/com/yao/crm/service/CollaborationService.java \
        apps/api/src/test/java/com/yao/crm/service/CollaborationServiceTest.java
git commit -m "fix: tenant guard team membership"
```

### Task 3: Propagate the tenant header through the controller

**Files:**
- Modify: `apps/api/src/main/java/com/yao/crm/controller/CollaborationController.java`

- [ ] **Step 1: Require `X-Tenant-Id` for comment delete/like/edit endpoints and pass the value into the service**
```java
@DeleteMapping("/comments/{commentId}")
public ResponseEntity<?> deleteComment(
        HttpServletRequest httpRequest,
        @RequestHeader("X-Tenant-Id") String tenantId,
        @PathVariable String commentId,
        @RequestParam(required = false) String userId) {
    String normalizedTenantId = normalizeRequiredValue(tenantId);
    ...
    boolean deleted = collaborationService.deleteComment(normalizedTenantId, normalizedCommentId, currentUser);
```
Apply similar `@RequestHeader` additions to `likeComment` and `editComment` and forward `tenantId` into those service methods.

- [ ] **Step 2: Require the header on team member add/remove endpoints and forward the tenant**
```java
@PostMapping("/teams/{teamId}/members")
public ResponseEntity<?> addTeamMember(
        @RequestHeader("X-Tenant-Id") String tenantId,
        @PathVariable String teamId,
        @RequestBody AddMemberRequest request) {
    ...
    Team team = collaborationService.addTeamMember(normalizedTenantId, normalizedTeamId, normalizedUserId, request.role);
```
Mirror for the delete endpoint.

- [ ] **Step 3: Quickly compile and sanity-check the controller changes**
Run: `mvn -f apps/api/pom.xml -DskipTests --also-make compile`
Expected: PASS (controller still compiles with the new parameters).

- [ ] **Step 4: Re-run `CollaborationServiceTest` suite to ensure nothing regresses**
Run: `mvn -f apps/api/pom.xml -Dtest=CollaborationServiceTest test`
Expected: PASS.

- [ ] **Step 5: Commit the controller adjustments**
```bash
git add apps/api/src/main/java/com/yao/crm/controller/CollaborationController.java
git commit -m "feat: tenant-safe collaboration controller"
```

### Task 4: Final verification and reporting

**Files:** Not applicable beyond existing touched files.

- [ ] **Step 1: Run the requested integration test command**
Run: `mvn -f apps/api/pom.xml -Dtest=CollaborationServiceTest test`
Expected: PASS; capture and report the output.

- [ ] **Step 2: Stage all modified files together to prepare the last commit**
```bash
git add apps/api/src/main/java/com/yao/crm/controller/CollaborationController.java \
        apps/api/src/main/java/com/yao/crm/service/CollaborationService.java \
        apps/api/src/main/java/com/yao/crm/repository/CommentRepository.java \
        apps/api/src/test/java/com/yao/crm/service/CollaborationServiceTest.java
```

- [ ] **Step 3: Make a concluding commit if any of the prior commits were deferred**
```bash
git commit -m "chore: tenant-scope collaboration workflows"
```

- [ ] **Step 4: Summarize the test results and spec alignment**
Document that service methods now accept tenants, that the controller forwards the header, and that the `CollaborationServiceTest` suite passes, satisfying the spec requirements.

- [ ] **Step 5: Ask for execution preference**
Plan complete and saved to `docs/superpowers/plans/2026-04-01-collaboration-tenant-safety-plan.md`. Two execution options:
1. Subagent-Driven (recommended) - new subagent per task + review.
2. Inline Execution - run tasks in this session with executing-plans skill.
Which approach should I take?
