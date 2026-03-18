# QA Report - Commercial Ops Hardening

## Gate Results (2026-03-17)
- `npm run lint` PASS
- `npm run build` PASS
- `npm run test:e2e` PASS
- `npm run test:backend` PASS (94/94)
- `npm run test:full` PASS (`API_SMOKE_TEST_OK`)
- `npm run perf:baseline` PASS
- `npm run perf:gate` PASS
- `npm run sre:daily-check` PASS
- `npm run security:scan` PASS
- `npm run staging:verify` PASS
- `npm run drill:backup-restore` PASS
- `npm run drill:rollback` PASS
- `npm run preflight:prod` PASS (secure env injected, MQ check skipped by config)

## Verification Notes
- New SLO snapshot endpoint works and is covered by integration test:
  `/api/v1/ops/slo-snapshot`.
- `slo-snapshot` now exposes `performanceWindow` with request count, P95/P99, 5xx ratio, and slow ratio.
- Structured request metrics are aggregated for SLO evaluation and surfaced in daily checks.
- Performance baseline evidence is generated under `logs/perf/` and gated by `perf-gate-latest.json`.
- Daily SRE report is generated under `logs/sre/` and includes pass/fail verdict with reasons.
- Security evidence is generated under `logs/security/` (audit + secret scan + SBOM).
- Staging verification evidence is generated under `logs/staging/` and includes rollback recommendation.
- Drill reports are generated under `logs/drills/` for backup/restore and rollback.
- Preflight now verifies release snapshot + perf gate + SRE verdict + latest drill evidence.
- Mainline CI uploads operations evidence artifacts (`logs/release`, `logs/sre`, `logs/perf`, `logs/preflight`).

## Conclusion
This round reaches the targeted operational-closure baseline:
daily evaluable, evidence-backed release gate, executable drills, and rollback traceability.

## Staging Deployment Closure (Current Round)
- Added real staging deployment command (`npm run staging:deploy`) based on SSH + Docker Compose.
- Added staging rollback command (`npm run staging:rollback`) with previous-version restore logic.
- `staging-release` workflow now runs real deploy (replacing placeholder), keeps manual trigger, and uploads artifact as `staging-evidence-<run_number>`.
- Staging release summary now includes operator/approval metadata and deployment evidence links.

## SLO Alerting Closure (Current Round)
- `/api/v1/ops/slo-snapshot` now includes `errorBudget`, `alertsLevel/alertsDetailed`, and `oncall`.
- Added `npm run sre:alert-check` for alert verdict generation and release recommendation evidence.
- `preflight:prod` now enforces alert verdict, error budget, and oncall coverage checks.
- Mainline/staging CI now includes `sre:alert-check` in mandatory gate sequence.
