# SRE SLO 鍩虹嚎 | SRE SLO Baseline

鏈枃妗ｅ畾涔?CRM 绯荤粺鐨勬湇鍔℃按骞崇洰鏍囷紙SLO锛夊拰鍛婅闃堝€笺€? 
This document defines the Service Level Objectives (SLO) and alert thresholds for the CRM system.

---

## 鏈嶅姟姘村钩鐩爣 | Service Level Objectives

| 鎸囨爣 | 鐩爣 | Metric | Target |
|------|------|--------|--------|
| 鍙敤鎬?SLO | 99.9% 鏈堝害锛堣璇佺敤鎴?`/api/**`锛?| Availability SLO | 99.9% monthly for authenticated `/api/**` |
| 閿欒棰勭畻 | 0.1% 鏈堝害 | Error budget | 0.1% monthly |
| API 寤惰繜 SLO | P95 < 800ms锛堟牳蹇冭矾鐢憋級 | API latency SLO | P95 < 800ms for core routes |
| 璁よ瘉杩炵画鎬?SLO | 鐧诲綍鎴愬姛鐜?>= 99.5%锛堟帓闄ゆ棤鏁堝嚟璇侊級 | Auth continuity SLO | Login success rate >= 99.5% (excluding invalid credentials) |

### 鏍稿績璺敱 | Core Routes

- `/api/dashboard` - 浠〃鐩?| Dashboard
- `/api/customers/search` - 瀹㈡埛鎼滅储 | Customer search
- `/api/v1/reports/overview` - 鎶ヨ〃姒傝 | Reports overview

---

## 鍛婅闃堝€?| Alert Thresholds

| 鍛婅鏉′欢 | 闃堝€?| 鍝嶅簲鍔ㄤ綔 | Alert Condition | Threshold | Action |
|----------|------|----------|-----------------|-----------|--------|
| API 閿欒鐜?| >= 2% 鎸佺画 5 鍒嗛挓 | 鍊肩彮鍝嶅簲 | `api_error` ratio | >= 2% for 5 minutes | Page on-call |
| P95 寤惰繜 | >= 1500ms 鎸佺画 10 鍒嗛挓 | 璀﹀憡 | P95 latency | >= 1500ms for 10 minutes | Warning |
| 绉熸埛绂佹閿欒 | >= 3x 鍩虹嚎宄板€?| 瀹夊叏瀹℃煡 | `TENANT_FORBIDDEN` spike | >= 3x baseline | Security review |
| 瀹¤瀵煎嚭澶辫触 | >= 10% 鎸佺画 30 鍒嗛挓 | 璐熻矗浜哄崌绾?| Audit export failed ratio | >= 10% in 30 minutes | Owner escalation |

---
## 杩愮淮淇″彿 | Operational Signals

### 缁撴瀯鍖栨棩蹇?| Structured Logs

| 鏃ュ織绫诲瀷 | 鎻忚堪 | Log Type | Description |
|----------|------|----------|-------------|
| `api_request` | API 璇锋眰鏃ュ織 | `api_request` | API request logs |
| `api_error` | API 閿欒鏃ュ織 | `api_error` | API error logs |
| `api_slow` | API 鎱㈣姹傛棩蹇?| `api_slow` | API slow request logs |

### 鍋ュ悍妫€鏌ョ鐐?| Health Endpoints

| 绔偣 | 鐢ㄩ€?| Endpoint | Purpose |
|------|------|----------|---------|
| `/api/health/ready` | 閮ㄧ讲闂ㄧ | `/api/health/ready` | Deployment gate |
| `/api/health/live` | 瀛樻椿鎺㈤拡 | `/api/health/live` | Liveness probe |
| `/api/health/deps` | 渚濊禆妫€鏌?| `/api/health/deps` | Dependencies check |

### 瀹¤瀵煎嚭鎸囨爣 | Audit Export Metrics

浣跨敤 `/api/audit-logs/export-metrics` 鐩戞帶浠ヤ笅瓒嬪娍锛?
Use `/api/audit-logs/export-metrics` to monitor the following trends:

- 闃熷垪娣卞害 | Queue depth
- 澶辫触鐜?| Failure rate
- 閲嶈瘯瓒嬪娍 | Retry trend
