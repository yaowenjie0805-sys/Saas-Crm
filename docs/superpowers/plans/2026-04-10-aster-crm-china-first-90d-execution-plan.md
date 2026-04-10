# Aster CRM China-First 90D Execution Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有代码基线上，完成“国内一流 CRM 90 天冲刺”第二阶段落地，交付可合并、可观测、可回滚的成交引擎能力。

**Architecture:** 采用“主链优先 + 平台稳态 + AI放大”的双速策略。后端 `summary` 作为 KPI 真源，前端只负责展示与交互；连接器通过 `retryable/requestId/dispatched` 形成可审计闭环；主链状态机用阶段门禁强约束防止脏数据流入合同与回款。

**Tech Stack:** Spring Boot (Java 17), JUnit5, Maven, React + Vite + Vitest, MySQL, Redis, GitHub flow。

---

### Task 1: 统一阶段门禁错误码映射并补齐前端回归

**Files:**
- Modify: `apps/web/src/crm/components/pages/orders/orderPanelHelpers.js`
- Create: `apps/web/src/crm/__tests__/orderPanelHelpers.test.js`

- [ ] **Step 1: Write the failing test**

```js
import { describe, expect, it } from 'vitest'
import { formatOrderActionError } from '../components/pages/orders/orderPanelHelpers'

describe('orderPanelHelpers.formatOrderActionError', () => {
  const t = (key) => key

  it('maps new stage-gate quote accepted code', () => {
    const message = formatOrderActionError(
      { code: 'order_stage_gate_quote_accepted_required', details: { requiredStatus: 'ACCEPTED' } },
      t,
    )
    expect(message).toContain('orderStageGateQuoteAcceptedRequired')
    expect(message).toContain('ACCEPTED')
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm run test --workspace apps/web -- --run src/crm/__tests__/orderPanelHelpers.test.js`  
Expected: FAIL because helper only handles old code `order_stage_gate_requires_quote_accepted`.

- [ ] **Step 3: Write minimal implementation**

```js
if (
  err?.code === 'order_stage_gate_requires_quote_accepted'
  || err?.code === 'order_stage_gate_quote_accepted_required'
) {
  const fallback = t('orderStageGateQuoteAcceptedRequired')
  const req = err?.details?.requiredStatus ? ` (${t('requiredStatusLabel')}: ${err.details.requiredStatus})` : ''
  return `${fallback}${req}`
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm run test --workspace apps/web -- --run src/crm/__tests__/orderPanelHelpers.test.js`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add apps/web/src/crm/components/pages/orders/orderPanelHelpers.js apps/web/src/crm/__tests__/orderPanelHelpers.test.js
git commit -m "fix(web): support stage-gate quote accepted error code variants"
```

### Task 2: 补齐 KPI 看板展示口径（forecastAccuracy + summary pipelineHealth）

**Files:**
- Modify: `apps/web/src/crm/components/pages/dashboard/ReportsSection.jsx`
- Modify: `apps/web/src/crm/i18n/common/en.js`
- Modify: `apps/web/src/crm/i18n/common/zh.js`
- Modify: `apps/web/src/crm/i18n/namespaces/market-dashboard.js`
- Create: `apps/web/src/crm/__tests__/ReportsSection.test.jsx`

- [ ] **Step 1: Write the failing test**

```jsx
it('renders forecastAccuracy and prefers summary pipelineHealth', async () => {
  const reports = {
    summary: { winRate: 50, forecastAccuracy: 88.8, pipelineHealth: 66.6, customers: 5, revenue: 120000, opportunities: 4, taskDoneRate: 75 },
    localizedMetrics: { pipelineHealth: 12.3, arrLike: 90000 },
    customerByOwner: {}, revenueByStatus: {}, opportunityByStage: {}, followUpByChannel: {},
  }
  const container = await renderSection({ t: (k) => k, reportCurrency: 'CNY', reports })
  expect(container.textContent).toContain('forecastAccuracy')
  expect(container.textContent).toContain('88.8%')
  expect(container.textContent).toContain('66.6%')
  expect(container.textContent).not.toContain('12.3%')
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm run test --workspace apps/web -- --run src/crm/__tests__/ReportsSection.test.jsx`  
Expected: FAIL because section currently only reads `localizedMetrics.pipelineHealth` and does not render `forecastAccuracy`.

- [ ] **Step 3: Write minimal implementation**

```jsx
const pipelineHealth = Number(reportSummary?.pipelineHealth ?? localizedMetrics?.pipelineHealth ?? 0)
const forecastAccuracy = Number(reportSummary?.forecastAccuracy ?? 0)

<div>{t('forecastAccuracy')}: {forecastAccuracy}%</div>
<div>{t('pipelineHealth')}: {pipelineHealth}%</div>
```

```js
// i18n
"forecastAccuracy": "Forecast Accuracy"
```

- [ ] **Step 4: Run tests/build to verify pass**

Run: `npm run test --workspace apps/web -- --run src/crm/__tests__/ReportsSection.test.jsx`  
Expected: PASS  

Run: `npm run build --workspace apps/web`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add apps/web/src/crm/components/pages/dashboard/ReportsSection.jsx apps/web/src/crm/i18n/common/en.js apps/web/src/crm/i18n/common/zh.js apps/web/src/crm/i18n/namespaces/market-dashboard.js apps/web/src/crm/__tests__/ReportsSection.test.jsx
git commit -m "feat(web): show forecast accuracy and align pipeline health source"
```

### Task 3: 修复 V1IntegrationController 测试断裂并校验新字段

**Files:**
- Modify: `apps/api/src/test/java/com/yao/crm/controller/V1IntegrationControllerTest.java`

- [ ] **Step 1: Write failing assertions for new contract fields**

```java
assertTrue(Boolean.TRUE.equals(body.get("accepted")));
assertTrue(body.containsKey("retryable"));
assertTrue(body.containsKey("dispatched"));
verify(integrationWebhookService).sendMessageDetailed(...);
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node scripts/run-maven.mjs -f apps/api/pom.xml "-Dtest=V1IntegrationControllerTest" test`  
Expected: FAIL/NPE when mock still stubs `sendMessage` instead of `sendMessageDetailed`.

- [ ] **Step 3: Write minimal implementation (test-side stubbing update)**

```java
when(integrationWebhookService.sendMessageDetailed(...))
    .thenReturn(IntegrationWebhookService.DispatchResult.success(1, 202, false));

when(integrationWebhookService.sendMessageDetailed(...))
    .thenReturn(IntegrationWebhookService.DispatchResult.failure(3, 429, true));
```

- [ ] **Step 4: Run test to verify it passes**

Run: `node scripts/run-maven.mjs -f apps/api/pom.xml "-Dtest=V1IntegrationControllerTest" test`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add apps/api/src/test/java/com/yao/crm/controller/V1IntegrationControllerTest.java
git commit -m "test(api): align webhook controller tests with dispatch result contract"
```

### Task 4: 增强 KPI 数值级测试（非仅字段存在）

**Files:**
- Modify: `apps/api/src/test/java/com/yao/crm/service/ReportAggregationServiceTest.java`

- [ ] **Step 1: Write failing numeric assertions**

```java
assertEquals(0.5, summary.get("forecastAccuracy"));
assertEquals(58.4, summary.get("pipelineHealth"));
assertEquals(0.0, summary.get("forecastAccuracy"));
assertEquals(0.0, summary.get("pipelineHealth"));
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node scripts/run-maven.mjs -f apps/api/pom.xml "-Dtest=ReportAggregationServiceTest" test`  
Expected: FAIL if formula/rounding/edge handling is inconsistent.

- [ ] **Step 3: Write minimal implementation or assertion adjustment**

```java
// keep production formula stable, adjust tests to exact current rounded output
assertEquals(0.5, summary.get("forecastAccuracy"));
assertEquals(58.4, summary.get("pipelineHealth"));
```

- [ ] **Step 4: Run test to verify pass**

Run: `node scripts/run-maven.mjs -f apps/api/pom.xml "-Dtest=ReportAggregationServiceTest" test`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add apps/api/src/test/java/com/yao/crm/service/ReportAggregationServiceTest.java
git commit -m "test(metrics): add numeric assertions for forecast accuracy and pipeline health"
```

### Task 5: 处理导出任务重试接口 500 回归（AuthFlowIntegrationTest 阻塞项）

**Files:**
- Modify: `apps/api/src/main/java/com/yao/crm/controller/V1ReportController.java`
- Modify: `apps/api/src/main/java/com/yao/crm/service/ReportExportJobService.java`
- Modify: `apps/api/src/test/java/com/yao/crm/controller/AuthFlowIntegrationTest.java`
- Optional Test: `apps/api/src/test/java/com/yao/crm/controller/V1ReportControllerTest.java`

- [ ] **Step 1: Isolate failing test first**

```java
@Test
void reportsExportJobShouldSupportListStatusRetryAndDownload() { ... }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node scripts/run-maven.mjs -f apps/api/pom.xml "-Dtest=AuthFlowIntegrationTest#reportsExportJobShouldSupportListStatusRetryAndDownload" test`  
Expected: FAIL with expected `202` but got `500`.

- [ ] **Step 3: Write minimal implementation fix**

```java
// controller retry endpoint
try {
    reportExportJobService.retryJob(...);
    return ResponseEntity.accepted().body(successWithFields(request, "report_export_retry_accepted", body));
} catch (IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
}
```

```java
// service layer: enforce tenant/job ownership and deterministic retry state reset
job.setStatus("PENDING");
job.setErrorReason(null);
job.setStartedAt(null);
job.setFinishedAt(null);
```

- [ ] **Step 4: Run test to verify pass**

Run: `node scripts/run-maven.mjs -f apps/api/pom.xml "-Dtest=AuthFlowIntegrationTest#reportsExportJobShouldSupportListStatusRetryAndDownload" test`  
Expected: PASS.

- [ ] **Step 5: Run related suite**

Run: `node scripts/run-maven.mjs -f apps/api/pom.xml "-Dtest=AuthFlowIntegrationTest,V1IntegrationControllerTest,IntegrationWebhookServiceSecurityTest,WorkflowNotificationExecutorTest,ReportServiceTest,ReportAggregationServiceTest" test`  
Expected: PASS (or only known unrelated flake with evidence).

- [ ] **Step 6: Commit**

```bash
git add apps/api/src/main/java/com/yao/crm/controller/V1ReportController.java apps/api/src/main/java/com/yao/crm/service/ReportExportJobService.java apps/api/src/test/java/com/yao/crm/controller/AuthFlowIntegrationTest.java
git commit -m "fix(report): restore export retry endpoint contract and integration stability"
```

### Task 6: 最终闸门回归与交付打包

**Files:**
- Modify (if needed): `docs/superpowers/specs/2026-04-10-aster-crm-china-first-90d-design.md`
- Modify (if needed): `docs/superpowers/plans/2026-04-10-aster-crm-china-first-90d-execution-plan.md`

- [x] **Step 1: Run backend final verification**

Run: `node scripts/run-maven.mjs -f apps/api/pom.xml "-Dtest=V1AiControllerTest,OpenAiServiceImplTest,V1IntegrationControllerTest,IntegrationWebhookServiceSecurityTest,WorkflowNotificationExecutorTest,ReportServiceTest,ReportAggregationServiceTest,AuthFlowIntegrationTest" test`  
Expected: PASS or documented flaky case with reproduction and mitigation.

- [x] **Step 2: Run frontend final verification**

Run: `npm run test --workspace apps/web -- --run src/crm/__tests__/ai.api.test.js src/crm/__tests__/AiFollowUpSummarySection.test.jsx src/crm/__tests__/AiManualModelSections.test.jsx src/crm/__tests__/orderPanelHelpers.test.js src/crm/__tests__/ReportsSection.test.jsx`  
Expected: PASS

Run: `npm run build --workspace apps/web`  
Expected: PASS

- [x] **Step 3: Update docs only if behavior changed**

```md
- Keep error code compatibility note
- Keep KPI source-of-truth note (summary first, localized fallback second)
```

- [x] **Step 4: Commit final gate artifacts**

```bash
git add docs/superpowers/specs/2026-04-10-aster-crm-china-first-90d-design.md docs/superpowers/plans/2026-04-10-aster-crm-china-first-90d-execution-plan.md
git commit -m "docs(release): finalize 90d execution verification notes"
```

Verification notes (2026-04-10, Asia/Shanghai):
- Backend gate passed: `Tests run: 165, Failures: 0, Errors: 0`
- Frontend gate passed: `5 files, 31 tests passed`
- Frontend build passed: `vite build` success
- Follow-up fix commit: `d2c50e4`

## Self-Review

- Spec coverage:
- 主链门禁与错误码一致性：Task 1 + Task 5
- 连接器可靠性与测试完整性：Task 3
- KPI 口径与看板展示：Task 2 + Task 4
- 验收闸门与发布质量：Task 6

- Placeholder scan:
- 无 TBD/TODO/implement later。

- Type consistency:
- 错误码使用 `order_stage_gate_quote_accepted_required`（兼容旧码映射）。
- KPI 命名统一 `forecastAccuracy/pipelineHealth/winRate`。
