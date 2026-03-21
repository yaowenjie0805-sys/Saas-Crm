# 发布窗口策略 | Release Window Policy

---

## 窗口规则 | Window Rules

| 中文 | English |
|------|---------|
| **预发发布**: 随时进行，需附带证据包 | **Staging release**: anytime with evidence bundle. |
| **生产发布**: 仅在批准的变更窗口内进行 | **Production release**: only in approved change windows. |
| **冻结期**: 节假日及重大事故窗口期 | **Freeze periods**: holidays and critical incident windows. |

---

## 必需输入 | Mandatory Inputs

| 中文 | English |
|------|---------|
| 最新的 `staging-release-latest.json` 为 PASS | Latest `staging-release-latest.json` is PASS. |
| 最新的 `preflight-latest.json` 为 PASS | Latest `preflight-latest.json` is PASS. |
| 最新的性能/SRE/安全证据文件存在且通过 | Latest perf/sre/security evidence files exist and pass. |

---

## 回滚策略 | Rollback Policy

| 条件 | 中文 | English |
|------|------|---------|
| 立即回滚当 | `readiness` 降至 DOWN/DEGRADED 且 10 分钟内未恢复 | Immediate rollback when `readiness` drops to DOWN/DEGRADED and does not recover within 10 minutes. |
| 立即回滚当 | 错误率超过 SLO 阈值持续 5 分钟 | Error rate exceeds SLO threshold for 5 minutes. |
| 立即回滚当 | P95 延迟超过警告阈值持续 10 分钟 | P95 latency exceeds warning threshold for 10 minutes. |

| 中文 | English |
|------|---------|
| **回滚负责人**: 值班工程师 + 发布负责人 | **Rollback owner**: on-call engineer + release owner. |
