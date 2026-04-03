# Aster CRM

A lightweight CRM monorepo (`web + api`) with multi-tenant support, approvals, reporting, and integrations.

## Quick Start

### Requirements
- `JDK 17+`
- `Node.js 18+`
- `MySQL 8+`

### Steps
1. Copy env templates:
   - `.env.example` -> `.env`
   - `.env.backend.local.example` -> `.env.backend.local`
2. Fill required values:
   - `DB_URL`, `DB_USER`, `DB_PASSWORD`
   - `AUTH_TOKEN_SECRET` (recommended >= 32 chars)
   - `APP_SEED_TENANT_ID` (see Seed Tenant section below)
   - `AUTH_BOOTSTRAP_DEFAULT_PASSWORD` (dev default: `admin123`)
   - `VITE_DEFAULT_TENANT` (default tenant shown in login UI)
3. Start backend: `npm run dev:backend`
4. Install and start frontend:
   - `npm install`
   - `npm run dev`

### URLs
- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`

## Seed Tenant

- When `APP_SEED_ENABLED=true`, `APP_SEED_TENANT_ID` must be non-empty.
- Recommended setup:
  - `dev`: `APP_SEED_ENABLED=true`, `APP_SEED_TENANT_ID=tenant_default`, `APP_SEED_DEMO_ENABLED=true`, `AUTH_BOOTSTRAP_DEFAULT_PASSWORD=admin123`
  - `test`: `APP_SEED_ENABLED=true`, `APP_SEED_TENANT_ID=tenant_test`
  - `prod`: `APP_SEED_ENABLED=false`; keep `APP_SEED_TENANT_ID` empty unless seeding, and avoid weak/default bootstrap passwords

Common failure symptom: login 500 or `seed_tenant_id_required` when seed is enabled but tenant id is blank.

## Documentation

- Docs index (Chinese-first): [`docs/README.md`](./docs/README.md)
- Project structure: [`docs/PROJECT_STRUCTURE.md`](./docs/PROJECT_STRUCTURE.md)
- Project flow map: [`docs/PROJECT_FLOW_MAP.md`](./docs/PROJECT_FLOW_MAP.md)
- Development conventions: [`docs/DEVELOPMENT_CONVENTIONS.md`](./docs/DEVELOPMENT_CONVENTIONS.md)

## Notes

- Redis repository warnings are informational if Redis repositories are not in use.
- AI features require `AI_OPENAI_API_KEY` or `AI_ANTHROPIC_API_KEY` when enabled.
