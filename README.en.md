# Aster CRM

> A full-stack CRM monorepo for SMB teams, with built-in multi-tenancy, approval flows, reporting, and operational tooling.

<p align="center">
  <strong>English</strong> | <a href="./README.zh-CN.md">简体中文</a>
</p>

## Overview

Aster CRM covers the full business cycle from lead intake to payment collection, including:

- Customers, contacts, leads, opportunities, quotes, contracts, orders, and payments
- Multi-tenant isolation with RBAC
- Configurable approval workflows and audit logs
- Dashboards and export pipelines (CSV/Excel)
- Unified frontend/backend engineering workflow in one repository

## Repository Layout

- `apps/web`: React + Vite frontend
- `apps/api`: Spring Boot backend
- `docs`: architecture, operations, standards, and runbooks
- `scripts`: local dev, validation, and deployment scripts

See also: [PROJECT_STRUCTURE.md](./docs/PROJECT_STRUCTURE.md)

## Prerequisites

- Node.js `>= 18`
- JDK `>= 17`
- MySQL `>= 8.0`
- Redis (recommended)
- RabbitMQ (recommended)

## Quick Start

1. Install dependencies

```bash
npm install
```

2. Configure env files

```bash
cp .env.example .env
cp .env.backend.local.example .env.backend.local
```

3. Initialize database

```bash
npm run db:init
```

4. Start backend

```bash
npm run dev:backend
```

5. Start frontend (in another terminal)

```bash
npm run dev
```

## Common Commands

```bash
# Lint
npm run lint

# Frontend unit tests
npm run test:frontend

# Backend tests
npm run test:backend

# E2E tests
npm run test:e2e

# Build frontend/backend
npm run build
npm run build:backend
```

## Local Endpoints

- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Default username: `admin`
- Default password: `admin123`

Before production deployment, replace defaults (especially `AUTH_TOKEN_SECRET` and bootstrap credentials).

## Docs Entry

- [docs/README.md](./docs/README.md): documentation index
- [DEVELOPMENT_CONVENTIONS.md](./docs/DEVELOPMENT_CONVENTIONS.md): development standards
- [PROJECT_TROUBLESHOOTING.md](./docs/PROJECT_TROUBLESHOOTING.md): troubleshooting guide
- [operations/staging-deploy-runbook.md](./docs/operations/staging-deploy-runbook.md): deployment runbook

## License

Licensed under [AGPL-3.0](./LICENSE).
