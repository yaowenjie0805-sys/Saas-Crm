# Aster CRM China-First Phase 1 (90 Days) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有仓库基础上，交付“成交引擎 V1”：销售主链闭环 + AI 辅助能力标准化 + 国产生态可扩展底座，并可量化验证赢单率/效率/合规改进。

**Architecture:** 采用双速推进：主线以 `V1Lead/V1Quote/V1Order/Workflow` 为核心打穿线索到回款，副线并行建设 AI 模型网关统一契约与连接器可靠投递。通过统一错误模型、事件驱动与指标采集，形成可观测、可灰度、可回滚的演进路径。

**Tech Stack:** Spring Boot (Java 17), MySQL, Redis, React + Vite, Vitest, JUnit, Maven。

---

### Task 1: AI 能力统一契约（模型 ID / Base URL / API Key 全链路）

**Files:**
- Modify: `apps/api/src/main/java/com/yao/crm/controller/V1AiController.java`
- Modify: `apps/api/src/main/java/com/yao/crm/service/AiContentGenerationService.java`
- Modify: `apps/api/src/main/java/com/yao/crm/service/AiSalesForecastService.java`
- Modify: `apps/api/src/main/java/com/yao/crm/service/impl/OpenAiServiceImpl.java`
- Modify: `apps/web/src/crm/api/ai.js`
- Test: `apps/api/src/test/java/com/yao/crm/controller/V1AiControllerTest.java`
- Test: `apps/api/src/test/java/com/yao/crm/service/impl/OpenAiServiceImplTest.java`
- Test: `apps/web/src/crm/__tests__/ai.api.test.js`

- [ ] **Step 1: Write the failing tests**
```java
@Test void commentReplyShouldPassCustomConnectionWhenProvided() {}
@Test void marketingEmailShouldPassCustomConnectionWhenProvided() {}
@Test void salesAdviceShouldPassCustomConnectionWhenProvided() {}
```

- [ ] **Step 2: Run test to verify it fails**
Run: `node scripts/run-maven.mjs -f apps/api/pom.xml "-Dtest=V1AiControllerTest,OpenAiServiceImplTest" test`
Expected: FAIL with signature/assertion mismatch around model/baseUrl/apiKey forwarding.

- [ ] **Step 3: Write minimal implementation**
```java
return aiContentGenerationService.generateCommentReply(
  originalComment,
  context,
  isBlank(model) ? null : model,
  isBlank(baseUrl) ? null : baseUrl,
  isBlank(apiKey) ? null : apiKey
);
```

- [ ] **Step 4: Add frontend API payload assertion**
```js
expect(apiMock).toHaveBeenCalledWith('/v1/ai/salesAdvice', {
  method: 'POST',
  body: JSON.stringify({ opportunityName: 'Acme Expansion', stage: 'Proposal', customerName: 'Acme', lastActivity: 'Demo completed', model: 'qwen-plus', baseUrl: 'http://localhost:11434/v1', apiKey: 'sk-local' }),
}, 'token-advice', 'en')
```

- [ ] **Step 5: Run tests to verify pass**
Run: `node scripts/run-maven.mjs -f apps/api/pom.xml "-Dtest=V1AiControllerTest,OpenAiServiceImplTest" test`
Expected: PASS
Run: `npm run test --workspace apps/web -- --run src/crm/__tests__/ai.api.test.js`
Expected: PASS

- [ ] **Step 6: Commit**
```bash
git add apps/api/src/main/java/com/yao/crm/controller/V1AiController.java apps/api/src/main/java/com/yao/crm/service/AiContentGenerationService.java apps/api/src/main/java/com/yao/crm/service/AiSalesForecastService.java apps/api/src/main/java/com/yao/crm/service/impl/OpenAiServiceImpl.java apps/api/src/test/java/com/yao/crm/controller/V1AiControllerTest.java apps/api/src/test/java/com/yao/crm/service/impl/OpenAiServiceImplTest.java apps/web/src/crm/api/ai.js apps/web/src/crm/__tests__/ai.api.test.js
git commit -m "feat(ai): unify custom model/baseUrl/apiKey contract"
```

### Task 2: AI 前端三入口落地（评论回复/营销邮件/销售建议）

**Files:**
- Create: `apps/web/src/crm/components/pages/dashboard/AiCommentReplySection.jsx`
- Create: `apps/web/src/crm/components/pages/dashboard/AiMarketingEmailSection.jsx`
- Create: `apps/web/src/crm/components/pages/dashboard/AiSalesAdviceSection.jsx`
- Modify: `apps/web/src/crm/components/pages/dashboard/AiFollowUpSummarySection.jsx`
- Modify: `apps/web/src/crm/components/pages/dashboard/sections.js`
- Modify: `apps/web/src/crm/components/pages/DashboardPanel.jsx`
- Modify: `apps/web/src/crm/i18n/common/en.js`
- Modify: `apps/web/src/crm/i18n/common/zh.js`
- Test: `apps/web/src/crm/__tests__/AiFollowUpSummarySection.test.jsx`

- [ ] **Step 1: Write failing test**
```js
expect(generateFollowUpSummaryMock).toHaveBeenCalledWith({ customerName: '', channel: '', interactionDetails: 'Customer asked for updated pricing.', model: '', token: 'token-gen', lang: 'en' })
```

- [ ] **Step 2: Run test to verify it fails**
Run: `npm run test --workspace apps/web -- --run src/crm/__tests__/AiFollowUpSummarySection.test.jsx`
Expected: FAIL when model still defaults or persists unexpectedly.

- [ ] **Step 3: Write minimal implementation**
```jsx
<AiFollowUpSummarySection t={t} apiContext={apiContext} />
<AiCommentReplySection t={t} apiContext={apiContext} />
<AiMarketingEmailSection t={t} apiContext={apiContext} />
<AiSalesAdviceSection t={t} apiContext={apiContext} />
```

- [ ] **Step 4: Ensure model input defaults empty**
```jsx
setSelectedModel('')
```

- [ ] **Step 5: Run tests/build to verify pass**
Run: `npm run test --workspace apps/web -- --run src/crm/__tests__/ai.api.test.js src/crm/__tests__/AiFollowUpSummarySection.test.jsx`
Expected: PASS
Run: `npm run build --workspace apps/web`
Expected: PASS

- [ ] **Step 6: Commit**
```bash
git add apps/web/src/crm/components/pages/dashboard/AiCommentReplySection.jsx apps/web/src/crm/components/pages/dashboard/AiMarketingEmailSection.jsx apps/web/src/crm/components/pages/dashboard/AiSalesAdviceSection.jsx apps/web/src/crm/components/pages/dashboard/AiFollowUpSummarySection.jsx apps/web/src/crm/components/pages/dashboard/sections.js apps/web/src/crm/components/pages/DashboardPanel.jsx apps/web/src/crm/i18n/common/en.js apps/web/src/crm/i18n/common/zh.js apps/web/src/crm/__tests__/AiFollowUpSummarySection.test.jsx
git commit -m "feat(web): add ai tool panels with manual model id input"
```

### Task 3: 成交主链阶段门禁与审批一致性加固（报价->订单->合同）

**Files:**
- Modify: `apps/api/src/main/java/com/yao/crm/controller/V1QuoteController.java`
- Modify: `apps/api/src/main/java/com/yao/crm/controller/V1OrderController.java`
- Modify: `apps/api/src/main/java/com/yao/crm/controller/ContractController.java`
- Modify: `apps/api/src/main/resources/i18n/messages_zh.properties`
- Modify: `apps/api/src/main/resources/i18n/messages_en.properties`
- Test: `apps/api/src/test/java/com/yao/crm/controller/WorkflowControllerTest.java`
- Test: `apps/api/src/test/java/com/yao/crm/controller/AuthFlowIntegrationTest.java`

- [ ] **Step 1: Write failing integration test**
```java
@Test
void orderShouldRejectConfirmWhenQuoteNotAcceptedInStageGateMode() {}
```

- [ ] **Step 2: Run test to verify it fails**
Run: `node scripts/run-maven.mjs -f apps/api/pom.xml "-Dtest=WorkflowControllerTest,AuthFlowIntegrationTest" test`
Expected: FAIL due missing or inconsistent transition check.

- [ ] **Step 3: Write minimal implementation**
```java
if (stageGateEnabled && !"ACCEPTED".equals(quoteStatus)) {
  return badRequest(request, "order_stage_gate_quote_accepted_required");
}
```

- [ ] **Step 4: Add i18n error keys**
```properties
order_stage_gate_quote_accepted_required=Stage gate requires linked quote to be ACCEPTED before order confirmation
```

- [ ] **Step 5: Run tests to verify pass**
Run: `node scripts/run-maven.mjs -f apps/api/pom.xml "-Dtest=WorkflowControllerTest,AuthFlowIntegrationTest" test`
Expected: PASS

- [ ] **Step 6: Commit**
```bash
git add apps/api/src/main/java/com/yao/crm/controller/V1QuoteController.java apps/api/src/main/java/com/yao/crm/controller/V1OrderController.java apps/api/src/main/java/com/yao/crm/controller/ContractController.java apps/api/src/main/resources/i18n/messages_zh.properties apps/api/src/main/resources/i18n/messages_en.properties apps/api/src/test/java/com/yao/crm/controller/WorkflowControllerTest.java apps/api/src/test/java/com/yao/crm/controller/AuthFlowIntegrationTest.java
git commit -m "feat(core): harden stage-gate transition controls"
```

### Task 4: 自动化与连接器可靠性（事件->外部回执）

**Files:**
- Modify: `apps/api/src/main/java/com/yao/crm/service/IntegrationWebhookService.java`
- Modify: `apps/api/src/main/java/com/yao/crm/controller/V1IntegrationController.java`
- Modify: `apps/api/src/main/java/com/yao/crm/service/NotificationDispatchService.java`
- Test: `apps/api/src/test/java/com/yao/crm/service/IntegrationWebhookServiceSecurityTest.java`
- Test: `apps/api/src/test/java/com/yao/crm/service/WorkflowNotificationExecutorTest.java`

- [ ] **Step 1: Write failing tests**
```java
@Test
void shouldRetryAndEmitAuditWhenWebhookReturns5xx() {}
```

- [ ] **Step 2: Run test to verify it fails**
Run: `node scripts/run-maven.mjs -f apps/api/pom.xml "-Dtest=IntegrationWebhookServiceSecurityTest,WorkflowNotificationExecutorTest" test`
Expected: FAIL with missing retry/audit assertions.

- [ ] **Step 3: Write minimal implementation**
```java
boolean retryable = status >= 500 || status == 429;
audit.put("requestId", requestId);
audit.put("retryable", retryable);
```

- [ ] **Step 4: Run tests to verify pass**
Run: `node scripts/run-maven.mjs -f apps/api/pom.xml "-Dtest=IntegrationWebhookServiceSecurityTest,WorkflowNotificationExecutorTest" test`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add apps/api/src/main/java/com/yao/crm/service/IntegrationWebhookService.java apps/api/src/main/java/com/yao/crm/controller/V1IntegrationController.java apps/api/src/main/java/com/yao/crm/service/NotificationDispatchService.java apps/api/src/test/java/com/yao/crm/service/IntegrationWebhookServiceSecurityTest.java apps/api/src/test/java/com/yao/crm/service/WorkflowNotificationExecutorTest.java
git commit -m "feat(integration): add retryable delivery and auditable callbacks"
```

### Task 5: KPI 可观测性与90天验收看板

**Files:**
- Modify: `apps/api/src/main/java/com/yao/crm/service/ApiRequestMetricsService.java`
- Modify: `apps/api/src/main/java/com/yao/crm/controller/V1SalesInsightController.java`
- Modify: `apps/api/src/main/java/com/yao/crm/service/ReportAggregationService.java`
- Modify: `apps/web/src/crm/components/pages/DashboardPanel.jsx`
- Modify: `apps/web/src/crm/components/pages/ReportDesignerPanel.jsx`
- Test: `apps/api/src/test/java/com/yao/crm/service/ReportServiceTest.java`

- [ ] **Step 1: Write failing test**
```java
assertThat(result).containsKeys("winRate", "forecastAccuracy", "pipelineHealth");
```

- [ ] **Step 2: Run test to verify it fails**
Run: `node scripts/run-maven.mjs -f apps/api/pom.xml "-Dtest=ReportServiceTest" test`
Expected: FAIL with missing metric keys.

- [ ] **Step 3: Write minimal implementation**
```java
out.put("winRate", metrics.getWinRate());
out.put("forecastAccuracy", metrics.getForecastAccuracy());
out.put("pipelineHealth", metrics.getPipelineHealth());
```

- [ ] **Step 4: Run tests/build to verify pass**
Run: `node scripts/run-maven.mjs -f apps/api/pom.xml "-Dtest=ReportServiceTest" test`
Expected: PASS
Run: `npm run build --workspace apps/web`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add apps/api/src/main/java/com/yao/crm/service/ApiRequestMetricsService.java apps/api/src/main/java/com/yao/crm/controller/V1SalesInsightController.java apps/api/src/main/java/com/yao/crm/service/ReportAggregationService.java apps/web/src/crm/components/pages/DashboardPanel.jsx apps/web/src/crm/components/pages/ReportDesignerPanel.jsx apps/api/src/test/java/com/yao/crm/service/ReportServiceTest.java
git commit -m "feat(metrics): ship phase1 win-rate efficiency compliance kpis"
```

## Self-Review

- Spec coverage complete for Phase 1：
  - 销售主链：Task 3
  - AI 自动化：Task 1 + Task 2
  - 生态可靠性：Task 4
  - 指标验收：Task 5
- Placeholder scan: 无 TBD/TODO。
- Consistency: `model/baseUrl/apiKey` 与 `requestId` 在前后端统一。

## Notes

- 本计划严格对应：`docs/superpowers/specs/2026-04-10-aster-crm-china-first-design.md`。
- Phase 2/3 将在 Phase 1 验收后拆分独立计划文件。
