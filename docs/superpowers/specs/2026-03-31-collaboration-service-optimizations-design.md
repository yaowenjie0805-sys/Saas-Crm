# CollaborationService Pagination Optimization Design

## Objective
- replace the current in-memory pagination/filtering in `CollaborationService#getComments`, `#getMentions`, and `#searchComments` with database-driven filters so the service can scale without loading everything.
- keep public method signatures/response structures untouched and preserve the `createdAt` descending sort order.

## Context
- `CommentRepository` already exposes several derived finder methods, but the service currently fetches entire comment sets and paginates in Java, which uses excessive memory and makes pagination expensive for large discussions.
- `Comment#mentions` is stored as a JSON string, so mention filtering must still rely on SQL `LIKE` rather than normalized tables.
- Maintain existing reply population logic but back it with batched DB reads instead of iterating over a single in-memory list.

## Constraints
- Signatures/DTOs for `getComments`, `getMentions`, and `searchComments` must remain unchanged.
- Sorting must remain `createdAt` descending for both top-level comments and replies (where applicable).
- Repository changes must be limited to the two allowed files and tests updated in the specified test file.

## Proposed Solution
1. `getComments`
   - Introduce a paged repository method that returns only top-level comments sorted by `createdAt` descending (`parentCommentId` is `NULL`).
   - Fetch replies by calling a repository method that batches `findByParentCommentIdIn(...)` with a `createdAt`-based sort.
   - Use the repository-count helper to populate `total` and `totalPages` using the page metadata.
   - Keep the `includeReplies` switch by attaching replies when requested.
2. `getMentions`
   - Add a repository method that filters by tenant and `mentions` column using `LIKE`, and paginate via `Pageable` with `createdAt` descending.
   - Return the page content directly.
3. `searchComments`
   - Replace the in-memory filter with a repository method that performs a `LOWER(content)` `LIKE` search and returns a `Page<Comment>` sorted by `createdAt` descending.
   - Return the page content to the caller.

## Repository Changes
- Add derived query method `Page<Comment> findByTenantIdAndEntityTypeAndEntityIdAndParentCommentIdIsNull(..., Pageable)` to fetch top-level comments.
- Add derived query method `List<Comment> findByParentCommentIdInOrderByCreatedAtDesc(List<String> parentIds)` to batch fetch replies.
- Add derived query `Page<Comment> findByTenantIdAndMentionsContaining(..., Pageable)` for mentions.
- Replace the existing `searchByContent` list-based method with a pageable query (using either derived query or `@Query`) so pagination occurs in the database.

## Service Changes
- Create `Pageable` instances using `PageRequest.of(page, size, Sort.by("createdAt").descending())` wherever DB pagination is needed.
- Replace list-based filtering with repository pages, but continue to build `CommentListResult` in the same shape and populate replies on demand.
- Ensure `includeReplies` still attaches replies via new repository method.

## Testing Plan
- Extend `CollaborationServiceTest` with three tests:
  1. `getComments` should invoke the paged `findByTenantIdAndEntityTypeAndEntityIdAndParentCommentIdIsNull` and reply fetch method, and should return the repository-provided comments and page metadata.
  2. `getMentions` should call the new pageable repository method and return its page content.
  3. `searchComments` should call the pageable search method and return its page.
- Use `PageImpl` and `PageRequest` in tests and verify repository interactions via Mockito `verify`.

## Next Steps
1. Commit this design doc so we have a recorded agreement on the approach.
2. Invoke [`writing-plans`](skills/writing-plans) to translate this design into an implementation plan.
3. After the plan is approved, implement the repository/service/test changes and run `CollaborationServiceTest`.
