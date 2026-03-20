# Release Runbook (2026-03-20, Single Page)

## 1) Scope & Guardrails
- No API contract changes.
- No business field semantic changes.
- Performance-focused release only:
  - frontend bundle/load-path optimization
  - backend dashboard/reports read-path + cache improvements
  - DB online index rollout (`V10` + `V11`)

## 2) Preflight (all must pass)
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

Latest verified evidence:
- `logs/perf/perf-gate-latest.json`
- `logs/perf/sql-explain-compare-latest.json`
- `logs/perf/bundle-gate-latest.json`
- `logs/release/release-snapshot-2026-03-20T07-11-08-701Z.json`

## 3) Current Acceptance Status (2026-03-20)
- Perf gate: PASS
  - `rps=227.56`, `global p95/p99=6/21ms`
  - `dashboard p95/p99=6/7ms`
  - `reports p95/p99=5/7ms`
- SQL explain: PASS
  - `tasks_list_by_done_updated`: rows `32 -> 27`, cost `6.9 -> 3.45`
  - `tasks_list_by_owner_done_updated`: rows `1`, cost `0.35`
- Cache headers: PASS (`CACHE_HEADERS_OK 5`)
- Backend tests: PASS (`112` tests, `0` failures)
- Bundle gate: PASS

## 4) DB Online Rollout (A/B/C/D)
- Rule: low-traffic window, one index step at a time, validate after each batch.
- Stop immediately if lock wait/error threshold breached.

### Batch A (V10 customers/opportunities/tasks)
1. `CREATE INDEX idx_customers_tenant_status_updated ON customers(tenant_id, status, updated_at);`
2. `CREATE INDEX idx_customers_tenant_owner_created ON customers(tenant_id, owner, created_at);`
3. `CREATE INDEX idx_opportunities_tenant_stage_updated ON opportunities(tenant_id, stage, updated_at);`
4. `CREATE INDEX idx_opportunities_tenant_owner_created ON opportunities(tenant_id, owner, created_at);`
5. `CREATE INDEX idx_tasks_tenant_done_updated ON tasks(tenant_id, done, updated_at);`
6. `CREATE INDEX idx_tasks_tenant_owner_created ON tasks(tenant_id, owner, created_at);`

### Batch B (V10 follow_ups/contracts/payments)
1. `CREATE INDEX idx_followups_tenant_customer_updated ON follow_ups(tenant_id, customer_id, updated_at);`
2. `CREATE INDEX idx_followups_tenant_author_updated ON follow_ups(tenant_id, author, updated_at);`
3. `CREATE INDEX idx_followups_tenant_author_created ON follow_ups(tenant_id, author, created_at);`
4. `CREATE INDEX idx_contracts_tenant_status_updated ON contracts(tenant_id, status, updated_at);`
5. `CREATE INDEX idx_contracts_tenant_owner_created ON contracts(tenant_id, owner, created_at);`
6. `CREATE INDEX idx_payments_tenant_status_updated ON payments(tenant_id, status, updated_at);`
7. `CREATE INDEX idx_payments_tenant_owner_created ON payments(tenant_id, owner, created_at);`

### Batch C (V10 quotes/order_records/lead_import_jobs)
1. `CREATE INDEX idx_quotes_tenant_opp_status_owner_updated ON quotes(tenant_id, opportunity_id, status, owner, updated_at);`
2. `CREATE INDEX idx_orders_tenant_opp_status_owner_updated ON order_records(tenant_id, opportunity_id, status, owner, updated_at);`
3. `CREATE INDEX idx_lead_import_jobs_tenant_status_created ON lead_import_jobs(tenant_id, status, created_at);`

### Batch D (V11 tasks query path)
1. `DROP INDEX idx_tasks_tenant_done_updated ON tasks;`
2. `DROP INDEX idx_tasks_tenant_owner_created ON tasks;`
3. `CREATE INDEX idx_tasks_tenant_done_updated_id ON tasks(tenant_id, done, updated_at, id);`
4. `CREATE INDEX idx_tasks_tenant_owner_done_updated_id ON tasks(tenant_id, owner, done, updated_at, id);`

## 5) Post-Batch Validation (every batch)
1. `npm run perf:sql:explain`
2. `npm run perf:gate`
3. Check DB lock wait/write latency telemetry.

For Batch D additionally:
1. `npm run perf:cache:headers`
2. `npm run release:snapshot`

## 6) Stop / Rollback
- Stop if any condition persists:
  - API error-rate > 2%
  - p95 breaches gate thresholds
  - sustained DB lock wait timeout/spike
- Rollback policy:
  - rollback current batch only
  - keep already validated batches
  - continue next low-traffic window with smaller steps

## 7) Final Sign-off
1. Attach:
   - `logs/perf/perf-gate-latest.json`
   - `logs/perf/sql-explain-compare-latest.json`
   - `logs/perf/bundle-gate-latest.json`
   - latest `logs/release/release-snapshot-*.json`
2. Confirm API/semantic compatibility unchanged.
3. Approver marks `GO`.
