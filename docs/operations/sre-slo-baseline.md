# SRE SLO Baseline

## Objectives
- Availability SLO: 99.9% monthly for authenticated `/api/**`.
- Error budget: 0.1% monthly.
- API latency SLO: P95 < 800ms for core routes (`/api/dashboard`, `/api/customers/search`, `/api/v1/reports/overview`).
- Auth continuity SLO: login success rate >= 99.5% (excluding invalid credentials).

## Alert Thresholds
- `api_error` ratio >= 2% for 5 minutes: page on-call.
- P95 latency >= 1500ms for 10 minutes: warning.
- `TENANT_FORBIDDEN` spike >= 3x baseline: security review.
- Audit export failed ratio >= 10% in 30 minutes: owner escalation.

## Operational Signals
- Use structured logs from `api_request/api_error/api_slow`.
- Use `/api/health/ready` as deployment gate.
- Use `/api/audit-logs/export-metrics` for queue/failure/retry trend.
