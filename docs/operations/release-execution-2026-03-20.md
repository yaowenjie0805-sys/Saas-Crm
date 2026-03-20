# Release Execution Checklist (2026-03-20)

## Scope
- Goal: ship performance improvements without API contract changes.
- Focus:
  - Frontend chunk/i18n split + bundle gate stability.
  - Backend dashboard/reports read-path optimization and cache hit improvements.
  - DB online index rollout by batches A/B/C/D (`V10` + `V11`).

## Preflight (must be green before DB DDL)
1. `npm run lint`
2. `npm run build:frontend`
3. `npm run perf:bundle:report`
4. `npm run perf:bundle:gate`
5. `npm run build:backend`
6. `npm run test:backend`
7. `npm run perf:sql:explain`
8. `npm run perf:cache:headers`
9. `npm run perf:baseline`
10. `npm run perf:gate`
11. `npm run release:snapshot`

## Current Evidence (generated on 2026-03-20)
- Perf gate: `logs/perf/perf-gate-latest.json` (`pass=true`)
- SQL explain compare: `logs/perf/sql-explain-compare-latest.json`
- Cache headers: `CACHE_HEADERS_OK 5`
- Release snapshot: `logs/release/release-snapshot-2026-03-20T07-04-33-248Z.json`

## DB Rollout Strategy
- Window: low traffic only.
- Rule: one index change per step, verify after each step.
- If lock wait / error rate exceeds threshold: stop immediately and rollback current batch only.

## Batch A (Customer/Pipeline base indexes, V10)
Execute in order:
1. `CREATE INDEX idx_customers_tenant_status_updated ON customers(tenant_id, status, updated_at);`
2. `CREATE INDEX idx_customers_tenant_owner_created ON customers(tenant_id, owner, created_at);`
3. `CREATE INDEX idx_opportunities_tenant_stage_updated ON opportunities(tenant_id, stage, updated_at);`
4. `CREATE INDEX idx_opportunities_tenant_owner_created ON opportunities(tenant_id, owner, created_at);`
5. `CREATE INDEX idx_tasks_tenant_done_updated ON tasks(tenant_id, done, updated_at);`
6. `CREATE INDEX idx_tasks_tenant_owner_created ON tasks(tenant_id, owner, created_at);`

Validation after Batch A:
1. `npm run perf:sql:explain`
2. Check `tasks_list_by_done_updated` no regression in `query_cost/rows_examined_per_scan`.
3. `npm run perf:gate`

## Batch B (Follow-up/Contract/Payment, V10)
Execute in order:
1. `CREATE INDEX idx_followups_tenant_customer_updated ON follow_ups(tenant_id, customer_id, updated_at);`
2. `CREATE INDEX idx_followups_tenant_author_updated ON follow_ups(tenant_id, author, updated_at);`
3. `CREATE INDEX idx_followups_tenant_author_created ON follow_ups(tenant_id, author, created_at);`
4. `CREATE INDEX idx_payments_tenant_status_updated ON payments(tenant_id, status, updated_at);`
5. `CREATE INDEX idx_payments_tenant_owner_created ON payments(tenant_id, owner, created_at);`
6. `CREATE INDEX idx_contracts_tenant_status_updated ON contracts(tenant_id, status, updated_at);`
7. `CREATE INDEX idx_contracts_tenant_owner_created ON contracts(tenant_id, owner, created_at);`

Validation after Batch B:
1. `npm run perf:sql:explain`
2. `npm run perf:gate`

## Batch C (Commerce + Import jobs, V10)
Execute in order:
1. `CREATE INDEX idx_quotes_tenant_opp_status_owner_updated ON quotes(tenant_id, opportunity_id, status, owner, updated_at);`
2. `CREATE INDEX idx_orders_tenant_opp_status_owner_updated ON order_records(tenant_id, opportunity_id, status, owner, updated_at);`
3. `CREATE INDEX idx_lead_import_jobs_tenant_status_created ON lead_import_jobs(tenant_id, status, created_at);`

Validation after Batch C:
1. `npm run perf:sql:explain`
2. `npm run perf:gate`

## Batch D (Tasks query-path indexes, V11)
Execute in order:
1. `DROP INDEX idx_tasks_tenant_done_updated ON tasks;`
2. `DROP INDEX idx_tasks_tenant_owner_created ON tasks;`
3. `CREATE INDEX idx_tasks_tenant_done_updated_id ON tasks(tenant_id, done, updated_at, id);`
4. `CREATE INDEX idx_tasks_tenant_owner_done_updated_id ON tasks(tenant_id, owner, done, updated_at, id);`

Validation after Batch D (blocking):
1. `npm run perf:sql:explain`
2. Confirm:
   - `tasks_list_by_done_updated` remains improved vs baseline.
   - `tasks_list_by_owner_done_updated` is stable (allow small distribution-driven fluctuation).
3. `npm run perf:gate` (must pass with existing thresholds, no relaxation)
4. `npm run perf:cache:headers`
5. `npm run release:snapshot`

## Stop / Rollback Conditions
- Stop conditions (any one):
  - API error rate > 2% (from `docs/operations/perf-baseline.md`)
  - Global p95 or route p95 exceeds gate threshold
  - Sustained DB lock waits/timeouts during DDL
- Rollback:
  - Rollback current batch only.
  - Keep previously validated batches unchanged.
  - Resume next low-traffic window in smaller steps.

## Final Sign-off
1. Attach:
   - `logs/perf/perf-gate-latest.json`
   - `logs/perf/sql-explain-compare-latest.json`
   - latest `logs/release/release-snapshot-*.json`
2. Confirm no API schema/field semantic changes.
3. Mark rollout completed in change record.
