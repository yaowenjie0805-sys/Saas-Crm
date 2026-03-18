# Error Budget Policy

## Budget Windows
- Daily budget: `2%` API error-rate equivalent (`ops.error-budget.daily-max`).
- Weekly budget: `5%` cumulative equivalent (`ops.error-budget.weekly-max`).

## Release Decision Rules
- Block release if `alertsLevel=P1`.
- Block release if daily or weekly error budget is exceeded.
- Block release if current oncall primary or escalation chain is missing.
- Allow release with caution when only `P2/P3` advisory alerts exist.

## Gate Source of Truth
- `GET /api/v1/ops/slo-snapshot`:
  - `errorBudget.daily/weekly`
  - `alertsLevel` and `alertsDetailed`
  - `oncall`
- `logs/sre/alerts-latest.json` as auditable snapshot for preflight.

## Recovery Expectations
- After mitigation or rollback, rerun:
  - `npm run sre:daily-check`
  - `npm run sre:alert-check`
  - `npm run preflight:prod`
