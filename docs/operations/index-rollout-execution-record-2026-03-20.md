# Index Rollout Execution Record (2026-03-20)

## Change Meta
- Change ID: `<fill>`
- Environment: `<fill>`
- Release Window (local time): `<fill>`
- Operator: `<fill>`
- Reviewer/Approver: `<fill>`

## Global Preflight Record
- [ ] `npm run perf:sql:explain`
- [ ] `npm run perf:cache:headers`
- [ ] `npm run perf:baseline`
- [ ] `npm run perf:gate`
- [ ] `npm run release:snapshot`

Preflight result: `<PASS/FAIL>`
Preflight notes: `<fill>`

Preflight evidence:
- `logs/perf/perf-gate-latest.json`
- `logs/perf/sql-explain-compare-latest.json`
- `logs/perf/bundle-gate-latest.json`
- `logs/release/release-snapshot-2026-03-20T07-11-08-701Z.json`

## Batch A (V10: customers/opportunities/tasks)
Start: `<time>`
End: `<time>`
Operator: `<name>`

Executed SQL:
1. `idx_customers_tenant_status_updated`
2. `idx_customers_tenant_owner_created`
3. `idx_opportunities_tenant_stage_updated`
4. `idx_opportunities_tenant_owner_created`
5. `idx_tasks_tenant_done_updated`
6. `idx_tasks_tenant_owner_created`

Post-batch checks:
- [ ] `npm run perf:sql:explain`
- [ ] `npm run perf:gate`
- [ ] Lock wait / write latency in threshold

Result: `<PASS/FAIL>`
Notes: `<fill>`
Evidence path(s): `<fill>`

## Batch B (V10: follow_ups/contracts/payments)
Start: `<time>`
End: `<time>`
Operator: `<name>`

Executed SQL:
1. `idx_followups_tenant_customer_updated`
2. `idx_followups_tenant_author_updated`
3. `idx_followups_tenant_author_created`
4. `idx_contracts_tenant_status_updated`
5. `idx_contracts_tenant_owner_created`
6. `idx_payments_tenant_status_updated`
7. `idx_payments_tenant_owner_created`

Post-batch checks:
- [ ] `npm run perf:sql:explain`
- [ ] `npm run perf:gate`
- [ ] Lock wait / write latency in threshold

Result: `<PASS/FAIL>`
Notes: `<fill>`
Evidence path(s): `<fill>`

## Batch C (V10: quotes/order_records/lead_import_jobs)
Start: `<time>`
End: `<time>`
Operator: `<name>`

Executed SQL:
1. `idx_quotes_tenant_opp_status_owner_updated`
2. `idx_orders_tenant_opp_status_owner_updated`
3. `idx_lead_import_jobs_tenant_status_created`

Post-batch checks:
- [ ] `npm run perf:sql:explain`
- [ ] `npm run perf:gate`
- [ ] Lock wait / write latency in threshold

Result: `<PASS/FAIL>`
Notes: `<fill>`
Evidence path(s): `<fill>`

## Batch D (V11: tasks query-path indexes)
Start: `<time>`
End: `<time>`
Operator: `<name>`

Executed SQL:
1. `DROP INDEX idx_tasks_tenant_done_updated ON tasks`
2. `DROP INDEX idx_tasks_tenant_owner_created ON tasks`
3. `CREATE INDEX idx_tasks_tenant_done_updated_id ON tasks(tenant_id, done, updated_at, id)`
4. `CREATE INDEX idx_tasks_tenant_owner_done_updated_id ON tasks(tenant_id, owner, done, updated_at, id)`

Post-batch checks (blocking):
- [ ] `npm run perf:sql:explain`
- [ ] `npm run perf:gate`
- [ ] `npm run perf:cache:headers`
- [ ] `npm run release:snapshot`
- [ ] `tasks_list_by_done_updated` no regression
- [ ] `tasks_list_by_owner_done_updated` stable

Result: `<PASS/FAIL>`
Notes: `<fill>`
Evidence path(s): `<fill>`

## Incident / Pause / Rollback Log
- Triggered pause: `<yes/no>`
- Trigger condition: `<fill>`
- Rolled back batch: `<none/A/B/C/D>`
- Rollback command/evidence: `<fill>`

## Final Sign-off
- API contract unchanged: `<yes/no>`
- Business field semantics unchanged: `<yes/no>`
- Final decision: `<GO/NO-GO>`
- Signed by operator: `<name/time>`
- Signed by reviewer: `<name/time>`
