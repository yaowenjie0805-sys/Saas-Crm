# Performance Baseline (P0)

## Goal
- Provide repeatable baseline checks for key CRM routes:
  - `/api/dashboard`
  - `/api/customers/search`
  - `/api/v1/reports/overview`
- Turn baseline results into an enforceable release gate.

## Load Model
- Warm-up: small concurrency ramp for cache/JIT stabilization.
- Steady-state: fixed medium concurrency to represent normal business load.
- Short peak: fixed higher concurrency burst for resilience probe.
- Modes:
  - `perf:smoke` for fast local verification.
  - `perf:baseline` for release evidence and gate checks.

## Thresholds
- Source alignment:
  - Error-rate threshold aligns with `docs/operations/sre-slo-baseline.md` (`api_error ratio >= 2%` triggers page).
  - Route latency targets are performance baseline defaults for this repo.

<!-- perf-thresholds-json-begin -->
```json
{
  "global": {
    "minRps": 5,
    "maxP95Ms": 1500,
    "maxP99Ms": 2500,
    "maxErrorRate": 0.02,
    "maxTimeouts": 5
  },
  "routes": {
    "dashboard": { "maxP95Ms": 1200, "maxP99Ms": 2200 },
    "customers": { "maxP95Ms": 1300, "maxP99Ms": 2300 },
    "reports": { "maxP95Ms": 1500, "maxP99Ms": 2600 }
  }
}
```
<!-- perf-thresholds-json-end -->

## Operations
- Baseline evidence files:
  - `logs/perf/perf-baseline-*.json`
  - `logs/perf/perf-baseline-*.txt`
  - `logs/perf/perf-gate-latest.json`
- Release preflight must fail when baseline evidence is missing or gate verdict is failed.
