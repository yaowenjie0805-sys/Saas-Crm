# Commit Checklist (2026-03-20)

## Recommended Commit Split

## Commit 1: Frontend performance (i18n/chunk gate)
Suggested message:
- `perf(frontend): split opportunity-workbench i18n namespace and harden chunk presence gate`

Suggested files:
- `src/crm/i18n.js`
- `src/crm/i18n/common/en.js`
- `src/crm/i18n/common/zh.js`
- `src/crm/i18n/namespaces/opportunity-workbench.js`
- `vite.config.js`
- `docs/operations/perf-bundle-budget.json`

Command template:
```bash
git add src/crm/i18n.js src/crm/i18n/common/en.js src/crm/i18n/common/zh.js src/crm/i18n/namespaces/opportunity-workbench.js vite.config.js docs/operations/perf-bundle-budget.json
git commit -m "perf(frontend): split opportunity-workbench i18n namespace and harden chunk presence gate"
```

## Commit 2: Backend performance (dashboard/reports read path + cache key)
Suggested message:
- `perf(backend): reduce dashboard topN and normalize reports/dashboard cache keys`

Suggested files:
- `backend/src/main/java/com/yao/crm/service/DashboardService.java`
- `backend/src/main/java/com/yao/crm/service/ReportService.java`
- `backend/src/main/java/com/yao/crm/repository/CustomerRepository.java`
- `backend/src/main/java/com/yao/crm/repository/OpportunityRepository.java`
- `backend/src/main/java/com/yao/crm/repository/TaskRepository.java`
- `backend/src/test/java/com/yao/crm/service/DashboardServiceTest.java`
- `backend/src/test/java/com/yao/crm/service/ReportServiceTest.java`

Command template:
```bash
git add backend/src/main/java/com/yao/crm/service/DashboardService.java backend/src/main/java/com/yao/crm/service/ReportService.java backend/src/main/java/com/yao/crm/repository/CustomerRepository.java backend/src/main/java/com/yao/crm/repository/OpportunityRepository.java backend/src/main/java/com/yao/crm/repository/TaskRepository.java backend/src/test/java/com/yao/crm/service/DashboardServiceTest.java backend/src/test/java/com/yao/crm/service/ReportServiceTest.java
git commit -m "perf(backend): reduce dashboard topN and normalize reports/dashboard cache keys"
```

## Commit 3: Release/operations docs
Suggested message:
- `docs(release): add 2026-03-20 runbook, release notes, and rollout execution templates`

Suggested files:
- `docs/operations/index-rollout-online.md`
- `docs/operations/release-execution-2026-03-20.md`
- `docs/operations/release-notes-2026-03-20.md`
- `docs/operations/index-rollout-execution-record-2026-03-20.md`
- `docs/operations/release-runbook-2026-03-20.md`
- `docs/operations/commit-checklist-2026-03-20.md`

Command template:
```bash
git add docs/operations/index-rollout-online.md docs/operations/release-execution-2026-03-20.md docs/operations/release-notes-2026-03-20.md docs/operations/index-rollout-execution-record-2026-03-20.md docs/operations/release-runbook-2026-03-20.md docs/operations/commit-checklist-2026-03-20.md
git commit -m "docs(release): add 2026-03-20 runbook, release notes, and rollout execution templates"
```

## Safety Notes
- Do not use `git add .` in this repo state (workspace is very dirty).
- Stage only file lists above to avoid mixing unrelated historical changes.
