# Aster CRM

> Lightweight Enterprise CRM · Full-Stack Monorepo · Multi-Tenancy & Approval Engine Out of the Box

<p align="center">
  <img src="https://img.shields.io/badge/build-passing-brightgreen?style=flat-square" alt="Build Status" />
  <img src="https://img.shields.io/badge/coverage-85%25-green?style=flat-square" alt="Coverage" />
  <img src="https://img.shields.io/badge/license-AGPL--3.0-blue?style=flat-square" alt="License" />
  <img src="https://img.shields.io/badge/version-1.0.0-orange?style=flat-square" alt="Version" />
  <img src="https://img.shields.io/badge/JDK-17%2B-red?style=flat-square" alt="JDK" />
  <img src="https://img.shields.io/badge/Node.js-18%2B-339933?style=flat-square&logo=node.js&logoColor=white" alt="Node.js" />
  <img src="https://img.shields.io/badge/MySQL-8%2B-4479A1?style=flat-square&logo=mysql&logoColor=white" alt="MySQL" />
</p>

<p align="center">
  <strong>English</strong> | <a href="./README.zh-CN.md">中文</a>
</p>

---

## 📋 Table of Contents

- [🎯 Why Aster CRM](#why-aster-crm)
- [✨ Features](#features)
- [🚀 Quick Start](#quick-start)
- [📦 Installation](#installation)
- [🛠️ Tech Stack](#tech-stack)
- [📖 Documentation](#documentation)
- [🧪 Testing](#testing)
- [🐛 FAQ / Troubleshooting](#faq--troubleshooting)
- [🤝 Contributing](#contributing)
- [📄 License](#license)
- [🙏 Acknowledgments](#acknowledgments)

---

## 🎯 Why Aster CRM

Aster CRM is an open-source full-stack CRM system built for small and medium-sized enterprises. It covers the complete business cycle from sales leads to contract signing, with flexible multi-tenant isolation and enterprise-grade access governance.

- ✅ **Full-Stack Monorepo**: Frontend and backend managed together — one set of commands for development, build, and testing
- ✅ **Native Multi-Tenancy**: Strong isolation architecture based on `X-Tenant-Id` request headers, SaaS-ready deployment
- ✅ **Approval Engine Out of the Box**: Built-in configurable approval workflows with multi-level review, no custom development needed
- ✅ **AI-Powered Analytics**: Native integration with OpenAI / Claude for sales forecasting and intelligent report insights
- ✅ **Enterprise IM Integration**: Webhook notifications for WeCom, DingTalk, and Lark
- ✅ **Robust CI/CD Pipeline**: GitHub Actions multi-stage pipeline with built-in performance gates and security scanning

---

## ✨ Features

### Sales Management
- Full lifecycle management of Accounts with tagging and classification
- Visual sales Pipeline with Opportunity stage tracking
- Contact relationship mapping and communication history
- Contract & quote generation, version management, and e-signing

### Business Processes
- Order management (Create → Fulfill → Deliver → Settle)
- Payment management and reconciliation
- Multi-level approval workflow engine (configurable nodes and conditional branching)
- Task management and schedule reminders

### Data & Analytics
- Real-time dashboard (ECharts 5 visualizations)
- Custom report designer (drag-and-drop fields, conditional filters, aggregate calculations)
- Data import/export (Excel / CSV)
- AI-powered analytics (sales trend forecasting, anomaly detection)

### Access Governance
- Multi-tenant data isolation (Row-Level Security)
- RBAC role-based access control model
- Field-level data masking and security controls
- Full audit log (actor, timestamp, change details)

### Enterprise Integrations
- WeCom / DingTalk / Lark Webhook notifications
- OpenAI / Claude AI API integration
- Standard REST API + Swagger documentation
- Bulk Excel import/export

### Feature Status

| Module | Status |
|--------|--------|
| Account / Contact / Opportunity Management | ✅ Stable |
| Contract / Quote | ✅ Stable |
| Order / Payment Management | ✅ Stable |
| Approval Workflow Engine | ✅ Stable |
| RBAC + Multi-Tenant Isolation | ✅ Stable |
| Dashboard + Basic Reports | ✅ Stable |
| Audit Log | ✅ Stable |
| Custom Report Designer | 🚧 Beta |
| AI-Powered Analytics | 🚧 Beta |
| WeCom / DingTalk / Lark Integration | 🚧 Beta |
| Mobile Responsive | 📋 Planned |
| Visual Workflow Editor | 📋 Planned |

---

## 🚀 Quick Start

### Prerequisites

| Dependency | Version | Notes |
|------------|---------|-------|
| JDK | 17+ | Backend runtime |
| Node.js | 18+ | Frontend build & runtime |
| MySQL | 8.0+ | Primary database (required) |
| Redis | 6+ | Local cache (recommended) |
| RabbitMQ | 3.11+ | Message queue (recommended) |

### Up and Running in 5 Minutes

**Step 1: Clone the repository**

```bash
git clone https://github.com/your-repo/aster-crm.git
cd aster-crm
```

**Step 2: Configure environment variables**

```bash
cp .env.example .env
cp .env.backend.local.example .env.backend.local
```

Edit `.env` and fill in your database connection details:

```env
DB_URL=jdbc:mysql://localhost:3306/aster_crm?useUnicode=true&characterEncoding=utf8
DB_USER=root
DB_PASSWORD=your_password
```

Edit `.env.backend.local` and update the token secret (**required for production**):

```env
AUTH_TOKEN_SECRET=your-secret-key-at-least-32-chars
```

**Step 3: Initialize the database**

```bash
npm run db:init
```

**Step 4: Start the backend**

```bash
npm run dev:backend
```

**Step 5: Install dependencies and start the frontend**

```bash
npm install
npm run dev
```

### Default Login Credentials

| Item | Value |
|------|-------|
| URL | http://localhost:5173 |
| Username | `admin` |
| Password | `admin123` |
| API Base URL | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |

> ⚠️ `admin123` is for local development only. **Set a strong password via `AUTH_BOOTSTRAP_DEFAULT_PASSWORD` before deploying to production.**

---

## 📦 Installation

### 🐳 Docker Compose (Recommended)

**Production:**

```bash
cp .env.production.example .env.production
# Edit .env.production with your production settings
docker compose --env-file .env.production -f infra/production/docker-compose.yml up -d
```

**Staging:**

```bash
cd infra/staging
cp staging.env.example staging.env
# Edit staging.env
docker compose -f docker-compose.yml --env-file staging.env up -d
```

### 💻 Local Development

Follow the [🚀 Quick Start](#quick-start) section to get your local environment running.

Common development commands:

```bash
npm run dev:backend      # Start backend (hot reload)
npm run dev              # Start frontend (Vite HMR)
npm run lint             # ESLint code check
npm run test:frontend    # Frontend unit tests
npm run test:e2e         # E2E end-to-end tests
```

### 🏭 Manual Production Build

```bash
# Build backend JAR
npm run build:backend

# Build frontend static assets
npm run build

# Backend JAR output: apps/api/target/
# Frontend build output: apps/web/dist/
```

For the full deployment process, see [docs/operations/staging-deploy-runbook.md](./docs/operations/staging-deploy-runbook.md).

---

## 🛠️ Tech Stack

### Frontend

| Technology | Version | Purpose |
|------------|---------|---------|
| React | 19 | UI framework |
| Vite | 7 | Build tool |
| Ant Design | 5 | Component library |
| Tailwind CSS | 4 | Utility-first CSS |
| Zustand | 5 | State management |
| ECharts | 5 | Data visualization |
| Vitest | latest | Unit testing |
| Playwright | latest | E2E testing |

### Backend

| Technology | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 2.7 | Application framework |
| JDK | 17 | Runtime |
| Spring Data JPA | - | ORM / data access |
| Spring Security | - | Authentication & authorization |
| Flyway | 9 | Database migration management |
| MySQL | 8+ | Primary database |
| Redis / Caffeine | - | Local + distributed cache |
| RabbitMQ | - | Async message queue |

### Infrastructure

| Technology | Purpose |
|------------|---------|
| Docker Compose | Containerized deployment |
| Nginx | Reverse proxy / static file serving |
| GitHub Actions | CI/CD pipeline |

For the full architecture dependency map, see [docs/ARCH_RUNTIME_DEPENDENCY_MAP.md](./docs/ARCH_RUNTIME_DEPENDENCY_MAP.md).

---

## 📖 Documentation

| Document | Description |
|----------|-------------|
| [docs/PROJECT_SPECIFICATION.md](./docs/PROJECT_SPECIFICATION.md) | Project specification and design principles |
| [docs/PROJECT_STRUCTURE.md](./docs/PROJECT_STRUCTURE.md) | Directory structure overview |
| [docs/DEVELOPMENT_CONVENTIONS.md](./docs/DEVELOPMENT_CONVENTIONS.md) | Development conventions and coding standards |
| [docs/api-documentation.md](./docs/api-documentation.md) | API reference documentation |
| [docs/ARCH_RUNTIME_DEPENDENCY_MAP.md](./docs/ARCH_RUNTIME_DEPENDENCY_MAP.md) | Architecture runtime dependency map |
| [docs/PROJECT_TROUBLESHOOTING.md](./docs/PROJECT_TROUBLESHOOTING.md) | Troubleshooting guide |
| [docs/operations/environment-matrix.md](./docs/operations/environment-matrix.md) | Multi-environment configuration matrix |
| [docs/operations/release-strategy.md](./docs/operations/release-strategy.md) | Release strategy and process |

---

## 🧪 Testing

Aster CRM uses a three-layer testing strategy to ensure reliability of core business logic:

```bash
# Frontend unit tests (Vitest)
npm run test:frontend

# Backend unit tests (JUnit 5)
npm run test:backend

# E2E end-to-end tests (Playwright)
npm run test:e2e

# View frontend coverage report
# Report generated at apps/web/coverage/
```

| Test Type | Tool | Coverage Scope |
|-----------|------|----------------|
| Frontend Unit Tests | Vitest | Components, utility functions, state management |
| Backend Unit Tests | JUnit 5 + Mockito | Service layer, Repository layer |
| E2E Tests | Playwright | Core user flows (login, CRUD, approval) |

---

## 🐛 FAQ / Troubleshooting

### Login returns 500 / `seed_tenant_id_required` after startup

**Cause:** `APP_SEED_ENABLED=true` but `APP_SEED_TENANT_ID` is not set.

**Fix:** Add the following to `.env.backend.local`:

```env
APP_SEED_ENABLED=true
APP_SEED_TENANT_ID=tenant_default
APP_SEED_DEMO_ENABLED=true
```

### Login always fails / Token validation error

**Cause:** `AUTH_TOKEN_SECRET` is using the example default value or is shorter than 32 characters.

**Fix:** Set a sufficiently strong secret in `.env.backend.local`:

```env
AUTH_TOKEN_SECRET=my-super-secret-key-at-least-32-chars
```

### Redis-related WARNINGs on backend startup

**Cause:** Redis Repository is not enabled — this is a normal informational log.

**Fix:** Safe to ignore during local development. For full Redis support, configure `REDIS_HOST` and `REDIS_PORT`.

### AI features are unresponsive / throwing errors

**Cause:** AI service API key is not configured.

**Fix:** Add the corresponding service key to `.env.backend.local`:

```env
AI_OPENAI_API_KEY=sk-...
# or
AI_ANTHROPIC_API_KEY=sk-ant-...
```

For more troubleshooting steps, see [docs/PROJECT_TROUBLESHOOTING.md](./docs/PROJECT_TROUBLESHOOTING.md).

---

## 🤝 Contributing

Issues and Pull Requests are welcome!

### Contribution Workflow

```
1. Fork this repository
2. Create a feature branch from main: git checkout -b feat/your-feature
3. Commit your changes (follow Conventional Commits): git commit -m "feat: add awesome feature"
4. Push the branch: git push origin feat/your-feature
5. Open a Pull Request and request a review after CI passes
```

### Development Guidelines

- Follow the coding standards in the [Development Conventions](./docs/DEVELOPMENT_CONVENTIONS.md)
- New features must include unit tests covering core branches
- PR descriptions should explain the motivation and scope of changes
- Look for issues labeled `good first issue` to get started

### Commit Message Format

```
feat:     New feature
fix:      Bug fix
docs:     Documentation update
refactor: Code refactor (no functional change)
test:     Test-related changes
chore:    Build / CI / dependency maintenance
```

---

## 📄 License

This project is open-sourced under the **[AGPL-3.0](./LICENSE)** license.

- ✅ Personal learning, research, and non-commercial use permitted
- ✅ Modified code must be open-sourced under the same license
- ⚠️ For commercial use, ensure compliance with AGPL-3.0 network service terms, or contact the author for a commercial license

---

## 🙏 Acknowledgments

Thanks to the following outstanding open-source projects that power Aster CRM:

- [Spring Boot](https://spring.io/projects/spring-boot) - Backend application framework
- [React](https://react.dev/) - Frontend UI framework
- [Ant Design](https://ant.design/) - Enterprise-grade UI component library
- [Vite](https://vitejs.dev/) - Next-generation frontend build tool
- [Flyway](https://flywaydb.org/) - Database version migration management
- [ECharts](https://echarts.apache.org/) - Open-source data visualization library
- [Playwright](https://playwright.dev/) - Reliable end-to-end testing framework

---

<p align="center">
  If this project has been helpful to you, please give us a ⭐ Star!
</p>
