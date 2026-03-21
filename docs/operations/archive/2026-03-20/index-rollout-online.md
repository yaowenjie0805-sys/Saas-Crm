# Online Index Rollout (High Priority + V11 Tasks)

## Goal
- Roll out `V10__high_priority_performance_indexes.sql` and `V11__tasks_query_path_indexes.sql` with low lock impact.
- Validate each batch by `EXPLAIN FORMAT=JSON` and latency checks before moving on.

## Pre-Run Checklist
1. Generate and archive baseline:
   - `npm run perf:sql:explain`
   - Save `logs/perf/sql-explain-latest.json` as `sql-explain-pre-rollout.json`.
2. Confirm cache + API health before DDL:
   - `npm run perf:cache:headers`
   - Ensure no active incident for API error-rate and DB lock wait.
3. Confirm release window is low-traffic.

## Batch Execution Sheet
### Batch A (Customer Pipeline)
- Tables: `customers`, `opportunities`, `tasks`
- SQL in this batch:
  - `idx_customers_tenant_status_updated`
  - `idx_customers_tenant_owner_created`
  - `idx_opportunities_tenant_stage_updated`
  - `idx_opportunities_tenant_owner_created`
  - `idx_tasks_tenant_done_updated`
  - `idx_tasks_tenant_owner_created`

### Batch B (Follow-up / Contract / Payment)
- Tables: `follow_ups`, `contracts`, `payments`
- SQL in this batch:
  - `idx_followups_tenant_customer_updated`
  - `idx_followups_tenant_author_updated`
  - `idx_followups_tenant_author_created`
  - `idx_contracts_tenant_status_updated`
  - `idx_contracts_tenant_owner_created`
  - `idx_payments_tenant_status_updated`
  - `idx_payments_tenant_owner_created`

### Batch C (Commerce + Import Jobs)
- Tables: `quotes`, `order_records`, `lead_import_jobs`
- SQL in this batch:
  - `idx_quotes_tenant_opp_status_owner_updated`
  - `idx_orders_tenant_opp_status_owner_updated`
  - `idx_lead_import_jobs_tenant_status_created`

### Batch D (V11 Tasks Query Path, low-traffic only)
- Tables: `tasks`
- SQL in this batch:
  - `DROP INDEX idx_tasks_tenant_done_updated ON tasks`
  - `DROP INDEX idx_tasks_tenant_owner_created ON tasks`
  - `idx_tasks_tenant_done_updated_id`
  - `idx_tasks_tenant_owner_done_updated_id`

## Online DDL Rule
- Prefer online DDL semantics in production runbooks:
  - `ALTER TABLE ... ADD INDEX ... ALGORITHM=INPLACE, LOCK=NONE`
- Apply one index at a time inside each batch (for Batch D, run each drop/create as single step).
- After each index, verify lock wait and write latency before next index.

## Validation After Every Batch
1. Run:
   - `npm run perf:sql:explain`
2. Compare latest against pre-rollout baseline:
   - `query_cost`
   - `rows_examined_per_scan`
3. Required result:
   - No regression on critical queries.
   - API p95 and DB lock wait stay within threshold.

## Release Gate Chain (Reproducible)
1. `npm run perf:sql:explain`
2. `npm run perf:cache:headers`
3. `npm run perf:baseline`
4. `npm run perf:gate`
5. `npm run release:snapshot`

## Latest Validation Snapshot (2026-03-20)
- `perf:gate`: PASS
  - global rps: `221.54`
  - global p95/p99: `6ms / 9ms`
  - dashboard p95/p99: `5ms / 8ms`
  - reports p95/p99: `5ms / 8ms`
  - source: `logs/perf/perf-gate-latest.json`
- `perf:cache:headers`: PASS (`CACHE_HEADERS_OK 5`)
- `perf:sql:explain`: PASS
  - `tasks_list_by_done_updated`: rows examined `32 -> 27`, query cost `6.9 -> 3.45`
  - `tasks_list_by_owner_done_updated`: rows examined `0 -> 1`, query cost `0 -> 0.35` (new V11 branch query)
  - source: `logs/perf/sql-explain-compare-latest.json`
- release snapshot generated:
  - `logs/release/release-snapshot-2026-03-20T07-04-33-248Z.json`

## Pause / Rollback Conditions
- Pause immediately when any condition holds for 5+ minutes:
  - API error-rate > 1.5x baseline.
  - API p95 > 1.3x baseline.
  - DB lock wait timeout or sustained lock wait spike.
- Rollback policy:
  - Stop current batch.
  - Revert only current batch DDL.
  - Resume in smaller unit (single index) in next low-traffic window.

## Tasks Full-Scan Strategy (Default + Fallback)
- Current observation: `tasks_list_by_done_updated` may still choose full scan on small dataset.
- Default strategy:
  - Execute V11 tasks indexes and switch tasks read path to index-friendly predicates:
    - `tenant_id + done + updated_at DESC`
    - `tenant_id + owner + done + updated_at DESC`
  - Validate on staging with realistic row volume (>= 100k tasks).
  - Record optimizer plan and compare with/without index hint for evidence.
- Fallback strategy:
  - If optimizer still prefers full scan and latency is acceptable, keep V11 indexes and continue monitoring.
  - If lock wait or error-rate crosses threshold during Batch D, rollback only Batch D and keep V10 indexes.
