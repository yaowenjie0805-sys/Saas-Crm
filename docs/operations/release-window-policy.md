# Release Window Policy

## Window Rules
- Staging release: anytime with evidence bundle.
- Production release: only in approved change windows.
- Freeze periods: holidays and critical incident windows.

## Mandatory Inputs
- Latest `staging-release-latest.json` is PASS.
- Latest `preflight-latest.json` is PASS.
- Latest perf/sre/security evidence files exist and pass.

## Rollback Policy
- Immediate rollback when:
  - `readiness` drops to DOWN/DEGRADED and does not recover within 10 minutes.
  - Error rate exceeds SLO threshold for 5 minutes.
  - P95 latency exceeds warning threshold for 10 minutes.
- Rollback owner: on-call engineer + release owner.
