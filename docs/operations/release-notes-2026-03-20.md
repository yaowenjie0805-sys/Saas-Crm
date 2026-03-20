# Release Notes (2026-03-20)

## Summary
- This release focuses on performance hardening without API contract changes.
- Scope includes frontend load-path optimization, backend read-path optimization, cache consistency hardening, and DB index rollout readiness.

## User-Facing Impact
- No API schema changes.
- No business field semantic changes.
- Faster and more stable dashboard/reports/tasks read experience under load.

## Backend Changes
- Dashboard read-path:
  - List payload reduced from top20 to top8 for opportunities/tasks/customers.
  - Cache key de-fragmentation to improve hit rate across users in same role scope.
- Reports overview/funnel:
  - Cache key de-fragmentation based on filter dimensions.
  - Single-sided date filters (`from` only / `to` only) now use normalized date fast-path to avoid fallback full-load behavior.
- Cache headers:
  - `X-CRM-Cache*` behavior validated for key read endpoints.

## Frontend Changes
- i18n load-path split expanded:
  - Added `opportunity-workbench` namespace and page binding for `dashboard/customers/pipeline`.
- Bundle gate hardening:
  - Required chunk presence checks now include i18n critical chunks (core + key namespaces).

## Database Changes
- Rollout plan finalized for online index batches A/B/C/D:
  - `V10` high-priority composite indexes.
  - `V11` tasks query-path indexes:
    - `idx_tasks_tenant_done_updated_id`
    - `idx_tasks_tenant_owner_done_updated_id`

## Validation Results
- Frontend:
  - `npm run lint` PASS
  - `npm run build:frontend` PASS
  - `npm run perf:bundle:report` PASS
  - `npm run perf:bundle:gate` PASS
- Backend:
  - `npm run build:backend` PASS
  - `npm run test:backend` PASS (`112` tests, `0` failures)
- Perf/DB:
  - `npm run perf:sql:explain` PASS
  - `npm run perf:cache:headers` PASS (`CACHE_HEADERS_OK 5`)
  - `npm run perf:gate` PASS
  - `npm run release:snapshot` PASS

## Key Metrics (Latest)
- Perf gate:
  - global `rps=227.56`
  - global `p95=6ms`, `p99=21ms`
  - dashboard `p95/p99=6/7ms`
  - reports `p95/p99=5/7ms`
- SQL explain:
  - `tasks_list_by_done_updated`: rows `32 -> 27`, cost `6.9 -> 3.45`
  - `tasks_list_by_owner_done_updated`: rows `1`, cost `0.35` (V11 branch path)

## Evidence Artifacts
- `logs/perf/perf-gate-latest.json`
- `logs/perf/sql-explain-compare-latest.json`
- `logs/perf/bundle-gate-latest.json`
- `logs/release/release-snapshot-2026-03-20T07-11-08-701Z.json`

## Rollback Notes
- If rollout-time lock wait/error thresholds are exceeded:
  - rollback current DB batch only;
  - keep previously validated batches unchanged;
  - resume in next low-traffic window with smaller step granularity.
