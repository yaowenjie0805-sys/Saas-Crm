# Collaboration Tenant Safety Design

Summary
1. Ensure comment delete/like/edit/reply flows plus team membership changes are scoped to the tenant that owns the target records.
2. Require `X-Tenant-Id` for the affected controller endpoints so the service can rely on tenant-aware repository queries.

Context
1. `CollaborationService` currently loads comments/teams by plain ID, which allows cross-tenant operations if a caller guesses an ID.
2. `CommentRepository` already exposes tenant-scoped finder variants but lacks `findByIdAndTenantId`.
3. `TeamRepository` already exposes `findByIdAndTenantId`, yet the service still uses the non-scoped `findById`.
4. The controller already requires `X-Tenant-Id` for the majority of comment APIs, but delete/like/edit and team member manipulation still omit it.

Assumptions
1. The new tenant safety guarantees should default to the strictest mode; callers must provide `X-Tenant-Id` before the change ships.
2. We can treat any missing tenant header for the affected endpoints as a bad request rather than attempting a fallback path.
3. Existing clients will be updated together with this change or will tolerate the new requirement because they already send the header for related endpoints.

Requirements
1. Add tenant filtering for `replyToComment`, `deleteComment`, `likeComment`, `editComment`, `addTeamMember`, and `removeTeamMember`.
2. Ensure `CollaborationController` reads `X-Tenant-Id` for the affected endpoints and forwards the normalized value to the service.
3. Keep backward-compatible signatures where reasonable while making the tenant-safe overload the default path.
4. Update `CollaborationServiceTest` so the new service signatures and repository interactions are verified.
5. Run `mvn -f apps/api/pom.xml -Dtest=CollaborationServiceTest test` after implementing and explain the outcome.

Approaches
1. Retrofit the controller/service to accept the tenant header but continue using `findById`; add an extra validation step in the service that compares the tenant on the loaded entity before mutating it. This minimizes repository changes but duplicates tenant checks.
2. Introduce `findByIdAndTenantId` on the `CommentRepository` and have the affected service methods take the tenant ID as an explicit parameter. This keeps tenant enforcement inside the repository while keeping the call site clear and simple. Use the existing `TeamRepository.findByIdAndTenantId` for team updates. This is the recommended approach.
3. Capture the tenant ID behind the scenes (e.g., via a `TenantContext` thread-local) so repository calls can read the current tenant implicitly. This requires more infrastructure and is riskier for a quick fix.

Implementation Notes
1. Service: add `findByIdAndTenantId` usage in `replyToComment`, `deleteComment`, `likeComment`, and `editComment`. Update the method signatures for delete/like/edit and the team member helpers to accept a `tenantId` parameter. Ensure that reply uses the tenant when loading the parent comment.
2. Controller: require `X-Tenant-Id` on `DELETE /comments/{commentId}`, `POST /comments/{commentId}/like`, `PUT /comments/{commentId}`, `POST /teams/{teamId}/members`, and `DELETE /teams/{teamId}/members/{userId}`. Normalize the header and pass it into the service.
3. Repository: add `Optional<Comment> findByIdAndTenantId(String id, String tenantId)` to `CommentRepository` and adjust the `TeamRepository` usage to match the existing tenant-aware finder.
4. Tests: update `CollaborationServiceTest` to mock the new repository calls and to verify that tenant-aware signatures are used when adding/removing members and deleting/liking/editing comments.
5. Keep existing helper logic, notifications, and reply count adjustments unchanged aside from the tenant guard.

Testing
1. `mvn -f apps/api/pom.xml -Dtest=CollaborationServiceTest test` as requested to cover the updated service logic.
2. Reuse the existing unit test coverage for comment pagination and mentions because they already accept tenant IDs.

Risks
1. Clients that still omit `X-Tenant-Id` for the newly strict endpoints will start receiving 400 responses; double-check release coordination.
2. Repository methods must stay in sync with service parameters; forgetting to update a call site could lead to a compile error, which the tests will catch.

Next Steps
1. Confirm that the tenant header requirement described above matches your expectation.
2. Once approved, I will invoke the `writing-plans` skill to outline the implementation steps and then make the repository changes and run the requested test.
