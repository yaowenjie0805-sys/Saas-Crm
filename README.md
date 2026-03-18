# CRM (Frontend/Backend Separated)

A separated CRM system:
- Frontend: React + Vite
- Backend: Spring Boot 2.7 (JDK 8)
- Database: MySQL 8

## 1. Environment
- JDK: 8 (current project built with JDK 8)
- Maven: 3.9+
- Node.js: 18+
- MySQL: 8+

## 2. Database
Backend uses local MySQL credentials:
- user: `root`
- password: `root`
- db: `crm_local`

Database create command:
```bash
mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS crm_local CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

Windows one-click init (create DB + Flyway migrate + seed data):
```bash
npm run db:init
```

`db:init` now also applies dual-market demo seed (`scripts/seed-dual-market-demo.sql`), including:
- `tenant_cn_demo` (`CN/CNY/Asia-Shanghai/STRICT`)
- `tenant_global_demo` (`GLOBAL/USD/UTC/STAGE_GATE`)

Linux/macOS:
```bash
bash scripts/init-db.sh root root crm_local
```

## 3. Run Backend (Java)
```bash
mvn -f backend/pom.xml spring-boot:run
```
Windows recommended startup (auto cleanup invalid build folder + release 8080 + run):
```bash
powershell -ExecutionPolicy Bypass -File scripts/start-backend.ps1
```
Backend URL:
- `http://localhost:8080`
- Health: `http://localhost:8080/api/health`
- Liveness: `http://localhost:8080/api/health/live`
- Readiness: `http://localhost:8080/api/health/ready`
- Dependencies: `http://localhost:8080/api/health/deps`

## 4. Run Frontend (React)
Install deps:
```bash
npm install
```
Run:
```bash
npm run dev
```
Frontend URL:
- `http://localhost:5173`

Frontend calls backend via:
- `VITE_API_BASE_URL` (default: `http://localhost:8080/api`)
- browser session cookie (`HttpOnly`) with `credentials: include`

## 5. Build
Frontend:
```bash
npm run build
```
Backend:
```bash
mvn -f backend/pom.xml clean package -DskipTests
```

API smoke test:
```bash
npm run test:api
```

Browser-backed frontend E2E:
```bash
npm run test:e2e
```

Performance quick smoke:
```bash
npm run perf:smoke
```

Performance baseline + gate:
```bash
npm run perf:baseline
npm run perf:gate
```

Staging verification and supply-chain scan:
```bash
npm run staging:verify
npm run security:scan
```

Staging deploy and rollback:
```bash
npm run staging:deploy -- --artifactVersion <commit_sha>
npm run staging:rollback
```

Backend automated tests:
```bash
npm run test:backend
```

One command from DB creation to browser E2E + API smoke test:
```bash
npm run test:full
```

Production preflight gate:
```bash
npm run preflight:prod
```

SRE daily check:
```bash
npm run sre:daily-check
```

SRE alert gate:
```bash
npm run sre:alert-check
```

Drill commands:
```bash
npm run drill:backup-restore
npm run drill:rollback
```

Release snapshot mapping:
```bash
npm run release:snapshot
```

## 6. API
- `POST /api/auth/login` (public)
- `GET /api/auth/session` (requires valid session)
- `GET /api/health`
- `GET /api/health/live`
- `GET /api/health/ready`
- `GET /api/health/deps`
- `GET /api/dashboard`
- `GET /api/reports/overview` (ADMIN/MANAGER/ANALYST)
- `GET /api/audit-logs` (ADMIN/MANAGER)
- `GET /api/audit-logs/search` (ADMIN/MANAGER/ANALYST, pagination/filter/sort, supports `username`/`role`/`action` + `from`/`to`)
- `GET /api/audit-logs/export` (ADMIN/MANAGER/ANALYST, CSV export, supports `username`/`role`/`action` + `from`/`to`)
- `GET /api/audit-logs/export-metrics` (ADMIN/MANAGER/ANALYST, export queue/failure/retry/latency snapshot)
- `GET /api/customers`
- `GET /api/customers/search` (with pagination/filter)
- `POST /api/customers`
- `PATCH /api/customers/{id}`
- `DELETE /api/customers/{id}`
- `GET /api/tasks`
- `GET /api/tasks/search` (with pagination/filter/sort)
- `POST /api/tasks`
- `PATCH /api/tasks/{id}`
- `GET /api/follow-ups/search` (with pagination/filter/sort)
- `POST /api/follow-ups`
- `PATCH /api/follow-ups/{id}`
- `DELETE /api/follow-ups/{id}`
- `GET /api/opportunities`
- `GET /api/opportunities/search` (with pagination/filter)
- `POST /api/opportunities`
- `PATCH /api/opportunities/{id}`

### v1 Enterprise APIs (`/api/v1/**`)
- All protected `v1` APIs require:
  - `Authorization: Bearer <token>`
  - `X-Tenant-Id: <tenantId>` (must match token claim)
- All `/api/v1/**` error responses are normalized:
  - `{ "code": "...", "message": "...", "requestId": "...", "details": {} }`
  - `code` uses lower snake_case.
- Legacy `/api/**` is in compatibility transition mode:
  - Keeps `message`
  - Adds `code/requestId/details`
  - Uses legacy-style upper-case code values (for backward compatibility)

- Tenant / Auth:
  - `GET /api/v1/tenants`
  - `POST /api/v1/tenants`
  - `PATCH /api/v1/tenants/{id}`
  - `POST /api/v1/auth/login`
  - `GET /api/v1/auth/session`
  - `POST /api/v1/auth/mfa/verify`
  - `POST /api/v1/auth/invitations/accept`
  - `POST /api/v1/auth/oidc/callback`
  - Success example (`POST /api/v1/auth/invitations/accept`, HTTP 201):
    - `{ "code": "invitation_accepted", "message": "...", "requestId": "...", "details": {}, "tenantId": "...", "username": "...", "displayName": "..." }`
  - Success example (`POST /api/v1/approval/templates`, HTTP 201):
    - `{ "code": "approval_template_created", "message": "...", "requestId": "...", "details": {}, "id": "...", "tenantId": "...", "bizType": "...", "status": "PUBLISHED" }`
  - Success example (`GET /api/v1/integrations/notifications/jobs`, HTTP 200):
    - `{ "code": "notification_jobs_listed", "message": "...", "requestId": "...", "details": {}, "items": [], "page": 1, "size": 20, "totalPages": 1, "total": 0 }`

- User Admin / Invitation:
  - `GET /api/v1/admin/users`
  - `PATCH /api/v1/admin/users/{id}`
  - `POST /api/v1/admin/users/invite`

- Approval:
  - `POST /api/v1/approval/templates`
  - `GET /api/v1/approval/templates`
  - `PATCH /api/v1/approval/templates/{id}`
  - `POST /api/v1/approval/instances/{bizType}/{bizId}/submit`
  - `GET /api/v1/approval/instances`
  - `GET /api/v1/approval/instances/{id}`
  - `GET /api/v1/approval/tasks`
  - `GET /api/v1/approval/stats`
  - `POST /api/v1/approval/tasks/{taskId}/approve`
  - `POST /api/v1/approval/tasks/{taskId}/reject`
  - `POST /api/v1/approval/tasks/{taskId}/transfer`

- Automation / Integrations:
  - `POST /api/v1/automation/rules`
  - `POST /api/v1/integrations/webhooks/wecom`
  - `POST /api/v1/integrations/webhooks/dingtalk`

- Reports:
  - `GET /api/v1/reports/overview`
  - `POST /api/v1/reports/export-jobs`
- `GET /api/v1/reports/export-jobs`
- `GET /api/v1/reports/export-jobs/{jobId}`
- `POST /api/v1/reports/export-jobs/{jobId}/retry`
- `GET /api/v1/reports/export-jobs/{jobId}/download`
- `GET /api/v1/ops/slo-snapshot`
- `GET /api/v1/ops/slo-snapshot`

## 7. Notes
- IDE startup quick troubleshooting:
  - Maven Reload/Reimport project in IDEA.
  - Ensure runtime classpath contains both `flyway-core` and `flyway-mysql`.
  - Verify dependency tree includes MySQL extension:
    - `mvn -f backend/pom.xml dependency:tree | findstr flyway`
  - If you ever see `backend/${project.build.directory}`, delete it and rebuild:
    - `mvn -f backend/pom.xml clean compile -DskipTests`
  - If IDEA still throws `Unsupported Database: MySQL 8.0`, run once by Maven to sync runtime:
    - `mvn -f backend/pom.xml spring-boot:run`
  - If `8080` is in use, stop old process:
    - `netstat -ano | findstr :8080`
    - `taskkill /PID <pid> /F`
- Backend schema is versioned by Flyway migrations (`backend/src/main/resources/db/migration/V*.sql`).
- `dev/test/prod` are split by Spring profiles; `test/prod` do not allow schema drift by Hibernate DDL.
- Backend startup includes Flyway state guard and will fail fast on invalid migration states.
- Backend startup also performs datasource precheck and fails with readable error if DB is unreachable.
- Backend auto seeds initial data when tables are empty (`APP_SEED_ENABLED`, `APP_SEED_DEMO_ENABLED`).
- Backend also seeds users (dev/test seed by default):
  - `admin / admin123` (role: `ADMIN`)
  - `manager / manager123` (role: `MANAGER`)
  - `sales / sales123` (role: `SALES`)
  - `analyst / analyst123` (role: `ANALYST`)
- All `/api/**` endpoints accept either `HttpOnly` session cookie or `Authorization: Bearer <token>` (transition mode).
- Frontend no longer persists bearer token in `localStorage`.
- All `/api/**` endpoints require authenticated session except:
  - `/api/health`
  - `/api/auth/login`
- Permission rule:
  - `DELETE /api/customers/{id}` is `ADMIN` or `MANAGER`.
  - `PATCH /api/opportunities/{id}` for `amount` is `ADMIN` or `MANAGER`.
- Audit log:
  - Every login/create/update/delete operation is persisted in `audit_logs`.
- API error i18n:
  - Default response messages are English.
  - Send `Accept-Language: zh` to receive Chinese error messages.
- `/api/v1/**` error examples:
  - `401`: `{ "code": "unauthorized", "message": "...", "requestId": "...", "details": {} }`
  - `403`: `{ "code": "forbidden", "message": "...", "requestId": "...", "details": {} }`
  - `404`: `{ "code": "tenant_not_found", "message": "...", "requestId": "...", "details": {} }`
  - `409`: `{ "code": "username_exists", "message": "...", "requestId": "...", "details": {} }`
- Legacy `/api/**` compatibility error example:
  - `403`: `{ "message": "...", "code": "FORBIDDEN", "requestId": "...", "details": {} }`
- Robustness baseline:
  - Production profile includes security fail-fast checks for weak defaults (`AUTH_TOKEN_SECRET`, `SECURITY_MFA_STATIC_CODE`, `SECURITY_SSO_MODE`, `AUTH_BOOTSTRAP_DEFAULT_PASSWORD`).
  - Global exception handling returns normalized JSON error bodies.
  - Rate limiting is enabled on `/api/**` (except `OPTIONS`) and returns localized 429 errors.
  - Endpoint-specific rate limits are applied for login/approval/batch-retry/export paths.
  - Notification batch operations are protected by `integration.notifications.batch-max-size` (default `100`).
  - Backend has datasource pool/timeouts and request timeouts configured.
  - Ops diagnostics endpoints:
    - `GET /api/v1/ops/health`
    - `GET /api/v1/ops/metrics/summary`
  - Release/ops runbooks:
    - `docs/operations/backup-restore-runbook.md`
    - `docs/operations/release-rollback-runbook.md`
    - `docs/operations/audit-retention-policy.md`
  - `docs/operations/release-strategy.md`
  - `docs/operations/sre-slo-baseline.md`
  - `docs/operations/perf-baseline.md`
  - `docs/operations/error-budget-policy.md`
  - `docs/operations/oncall-escalation-runbook.md`
  - `docs/operations/weekly-oncall-alert-review-template.md`
  - `docs/operations/change-control-checklist.md`
  - `docs/operations/incident-postmortem-template.md`
  - `docs/operations/staging-deploy-runbook.md`
  - `logs/security/security-scan-latest.json`
  - `logs/staging/staging-release-latest.json`
  - `logs/sre/daily-*.json` (daily operational verdict)
  - `logs/drills/backup-restore-*.json`, `logs/drills/rollback-*.json` (drill evidence)
    - `docs/operations/environment-matrix.md`
  - CI workflows:
    - `.github/workflows/pr-gate.yml`
    - `.github/workflows/main-gate.yml`
  - Frontend API requests use timeout + retry for GET requests.
  - Frontend includes a role permission matrix visualization.
  - Backend request validation is enabled (`spring-boot-starter-validation` + DTO + `@Valid`).
  - Task/Follow-up/Opportunity create APIs now use DTO validation with normalized validation error responses.


## 8. Controller architecture
- Controllers are split by domain (auth/health/dashboard/reports/customers/tasks/follow-ups/opportunities/audit).
- Common controller helpers are centralized in `backend/src/main/java/com/yao/crm/controller/BaseApiController.java`.
- Dashboard/report aggregation logic is moved to service layer (DashboardService, ReportService).
