# 性能基线 (P0) | Performance Baseline (P0)

---

## 目标 | Goal

| 中文 | English |
|------|---------|
| 为关键 CRM 路由提供可重复的基线检查: | Provide repeatable baseline checks for key CRM routes: |
| - `/api/dashboard` | - `/api/dashboard` |
| - `/api/customers/search` | - `/api/customers/search` |
| - `/api/v1/reports/overview` | - `/api/v1/reports/overview` |
| 将基线结果转化为可执行的发布门禁 | Turn baseline results into an enforceable release gate. |

---

## 负载模型 | Load Model

| 概念 | 中文 | English |
|------|------|---------|
| 预热 | 小并发爬坡，用于缓存/JIT 稳定 | Warm-up: small concurrency ramp for cache/JIT stabilization. |
| 稳态 | 固定中等并发，代表正常业务负载 | Steady-state: fixed medium concurrency to represent normal business load. |
| 短峰值 | 固定高并发突发，用于弹性探测 | Short peak: fixed higher concurrency burst for resilience probe. |
| 模式 | `perf:smoke` 用于快速本地验证 | Modes: `perf:smoke` for fast local verification. |
| 模式 | `perf:baseline` 用于发布证据和门禁检查 | `perf:baseline` for release evidence and gate checks. |

---

## 阈值 | Thresholds

| 中文 | English |
|------|---------|
| **来源对齐**: | **Source alignment**: |
| 错误率阈值与 `docs/operations/sre-slo-baseline.md` 对齐 (`api_error ratio >= 2%` 触发告警) | Error-rate threshold aligns with `docs/operations/sre-slo-baseline.md` (`api_error ratio >= 2%` triggers page). |
| 路由延迟目标是本仓库的性能基线默认值 | Route latency targets are performance baseline defaults for this repo. |

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

---

## 操作 | Operations

| 中文 | English |
|------|---------|
| **基线证据文件**: | **Baseline evidence files**: |
| - `logs/perf/perf-baseline-*.json` | - `logs/perf/perf-baseline-*.json` |
| - `logs/perf/perf-baseline-*.txt` | - `logs/perf/perf-baseline-*.txt` |
| - `logs/perf/perf-gate-latest.json` | - `logs/perf/perf-gate-latest.json` |
| 当基线证据缺失或门禁判决失败时，发布预检必须失败 | Release preflight must fail when baseline evidence is missing or gate verdict is failed. |
