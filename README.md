# Aster CRM

A lightweight CRM monorepo (web + api) with multi-tenant support, approvals, reporting, and integrations.

## Quick Start

Requirements:
- JDK 17+
- Node.js 18+
- MySQL 8+

Steps:
1. Copy environment templates:
   - `.env.example` -> `.env`
   - `.env.backend.local.example` -> `.env.backend.local`
2. Fill required values:
   - `DB_URL`, `DB_USER`, `DB_PASSWORD`
   - `AUTH_TOKEN_SECRET` (>= 32 chars recommended)
   - `APP_SEED_TENANT_ID` (see Seed Tenant section)
   - `VITE_DEFAULT_TENANT` (login UI default)
3. Start backend:
   - `npm run dev:backend`
4. Start frontend:
   - `npm install`
   - `npm run dev`

URLs:
- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html

## Seed Tenant

- When `APP_SEED_ENABLED=true`, `APP_SEED_TENANT_ID` must be non-empty.
- Recommended defaults:
  - dev: `APP_SEED_ENABLED=true`, `APP_SEED_TENANT_ID=tenant_default`, `APP_SEED_DEMO_ENABLED=true`
  - test: `APP_SEED_ENABLED=true`, `APP_SEED_TENANT_ID=tenant_test`
  - prod: `APP_SEED_ENABLED=false`, keep `APP_SEED_TENANT_ID` empty unless running seed

Common failure symptom:
- login 500 or `seed_tenant_id_required` if seed is enabled but tenant id is blank.

## Notes

- Redis repository warnings are informational if you are not using Redis repositories.
- AI features require `AI_OPENAI_API_KEY` or `AI_ANTHROPIC_API_KEY` if enabled.