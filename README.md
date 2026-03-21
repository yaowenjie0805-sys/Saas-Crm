# CRM

<p align="center">
  <strong>Enterprise-grade Customer Relationship Management System</strong>
</p>

<p align="center">
  <strong>企业级客户关系管理系统</strong>
</p>

<p align="center">
  <a href="#-features">Features</a> •
  <a href="#-tech-stack">Tech Stack</a> •
  <a href="#-quick-start">Quick Start</a> •
  <a href="#-api-reference">API</a> •
  <a href="#-documentation">Docs</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Frontend-React%2019.2-61DAFB?logo=react" alt="Frontend">
  <img src="https://img.shields.io/badge/Backend-Spring%20Boot%202.7-6DB33F?logo=springboot" alt="Backend">
  <img src="https://img.shields.io/badge/Database-MySQL%208-4479A1?logo=mysql" alt="Database">
  <img src="https://img.shields.io/badge/JDK-8-orange" alt="JDK">
  <img src="https://img.shields.io/badge/Node.js-18+-339933?logo=node.js" alt="Node.js">
</p>

---

## 📖 Overview | 项目简介

A modern, full-stack CRM system with multi-tenant support, approval workflows, and comprehensive audit trails.

一个现代化的全栈 CRM 系统，支持多租户、审批流程和完整的审计追踪。

| Feature | Description |
|---------|-------------|
| 🏢 **Multi-tenant** | Complete data isolation between tenants 租户间数据完全隔离 |
| ✅ **Approval Workflow** | Template-based approval with conditions and transfers 模板化审批，支持条件和转交 |
| 📋 **Audit Trail** | Full operation logging with export and metrics 全操作日志，支持导出和指标 |
| 🔐 **Role-based Access** | ADMIN / MANAGER / ANALYST / SALES hierarchy 角色分级权限控制 |
| 🌐 **i18n Ready** | Chinese and English support 中英文双语支持 |
| 📊 **Observability** | Health checks, SLO monitoring, Ops diagnostics 健康检查、SLO监控、运维诊断 |

---

## 🚀 Features | 功能特性

<details>
<summary><strong>Core Modules | 核心模块</strong></summary>

| Module | 中文 | APIs |
|--------|------|------|
| Tenant Management | 租户管理 | `/api/v1/tenants` |
| Approval Center | 审批中心 | `/api/v1/approval/*` |
| Audit Logs | 审计日志 | `/api/audit-logs` |
| Customer Management | 客户管理 | `/api/customers` |
| Sales Pipeline | 销售管道 | `/api/opportunities`, `/api/tasks` |
| Commerce | 商务中心 | `/api/v1/commerce/*` |
| Reports | 报表分析 | `/api/v1/reports/*` |

</details>

<details>
<summary><strong>Enterprise Features | 企业级特性</strong></summary>

- **Authentication**: JWT + Session cookie, MFA, OIDC, Invitation flow
- **Security**: Rate limiting, CORS, Tenant isolation, Input validation
- **Performance**: Redis cache, Connection pooling, Query optimization
- **Operations**: Health probes, SLO monitoring, Backup/Restore runbooks

</details>

---

## 🛠 Tech Stack | 技术栈

| Layer | Technology | Version |
|-------|------------|---------|
| **Frontend** | React + Vite + React Router + Zustand | 19.2 / 7.3 / 7.9 / 5.0 |
| **Backend** | Spring Boot + Spring Data JPA | 2.7 (JDK 8) |
| **Database** | MySQL + Flyway migrations | 8+ |
| **Cache** | Redis | - |
| **Message Queue** | RabbitMQ | - |
| **Testing** | Playwright (E2E) + JUnit (Backend) | - |

---

## ⚡ Quick Start | 快速开始

### Prerequisites | 环境要求

| Requirement | Version |
|-------------|---------|
| JDK | 8 |
| Maven | 3.9+ |
| Node.js | 18+ |
| MySQL | 8+ |

### 1. Clone & Install | 克隆并安装

```bash
git clone <repository-url>
cd crm
npm install
```

### 2. Initialize Database | 初始化数据库

```bash
# Windows
npm run db:init

# Linux/macOS
bash scripts/init-db.sh root root crm_local
```

This creates the database, runs migrations, and seeds demo data including:
- `tenant_cn_demo` (CN/CNY/Asia-Shanghai/STRICT)
- `tenant_global_demo` (GLOBAL/USD/UTC/STAGE_GATE)

### 3. Start Services | 启动服务

```bash
# Terminal 1: Backend (port 8080)
npm run dev:backend

# Terminal 2: Frontend (port 5173)
npm run dev
```

### 4. Access | 访问地址

| Service | URL |
|---------|-----|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080/api |
| Health Check | http://localhost:8080/api/health |

### Demo Accounts | 演示账号

| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | ADMIN |
| manager | manager123 | MANAGER |
| sales | sales123 | SALES |
| analyst | analyst123 | ANALYST |

---

## 📁 Project Structure | 项目结构

```
crm/
├── apps/
│   ├── api/                 # Spring Boot backend
│   │   ├── src/main/java/com/yao/crm/
│   │   │   ├── controller/  # REST controllers (34)
│   │   │   ├── service/     # Business logic (24)
│   │   │   ├── entity/      # JPA entities (31)
│   │   │   ├── repository/  # Data access (33)
│   │   │   ├── dto/        # Request/Response DTOs
│   │   │   ├── config/      # Spring configs (12)
│   │   │   └── security/    # Auth & permissions (17)
│   │   └── src/main/resources/
│   │       └── db/migration/ # Flyway migrations
│   └── web/                 # React frontend
│       └── src/crm/
│           ├── components/  # UI components (74)
│           ├── hooks/      # Custom hooks (123)
│           ├── store/       # Zustand state
│           └── i18n/       # Translations
├── docs/
│   └── operations/          # SRE runbooks (18)
├── scripts/                # DevOps scripts (34)
├── infra/                  # Docker/Deploy configs
└── logs/                  # Local logs (gitignored)
```

---

## 📚 API Reference | API 参考

### Legacy APIs (`/api/**`)

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/auth/login` | POST | Public | User login |
| `/api/health` | GET | Public | Health check |
| `/api/customers` | GET/POST | Auth | Customer CRUD |
| `/api/opportunities` | GET/POST | Auth | Sales opportunities |
| `/api/audit-logs` | GET | ADMIN/MANAGER | Audit logs |

### Enterprise APIs (`/api/v1/**`)

All v1 APIs require:
- `Authorization: Bearer <token>`
- `X-Tenant-Id: <tenantId>`

| Module | Endpoints |
|--------|-----------|
| Tenants | `GET/POST/PATCH /api/v1/tenants` |
| Auth | `POST /api/v1/auth/login`, `/mfa/verify`, `/invitations/accept` |
| Approval | `/api/v1/approval/templates`, `/instances`, `/tasks` |
| Commerce | `/api/v1/commerce/quotes`, `/contracts`, `/orders`, `/payments` |
| Reports | `/api/v1/reports/overview`, `/export-jobs` |
| Ops | `/api/v1/ops/health`, `/slo-snapshot` |

### Error Format | 错误格式

```json
{
  "code": "tenant_not_found",
  "message": "...",
  "requestId": "abc-123",
  "details": {}
}
```

---

## 🧪 Testing | 测试

```bash
# E2E tests (Playwright)
npm run test:e2e

# Backend tests (JUnit)
npm run test:backend

# API smoke test
npm run test:api

# Full test suite (DB + E2E + API)
npm run test:full
```

---

## 🔧 Useful Commands | 常用命令

| Command | Description | 说明 |
|---------|-------------|------|
| `npm run dev` | Start frontend | 启动前端 |
| `npm run dev:backend` | Start backend | 启动后端 |
| `npm run build` | Build frontend | 构建前端 |
| `npm run lint` | Lint frontend | 代码检查 |
| `npm run db:init` | Initialize database | 初始化数据库 |
| `npm run test:full` | Full test suite | 完整测试 |
| `npm run perf:baseline` | Performance baseline | 性能基线 |
| `npm run staging:verify` | Staging verification | 预发验证 |
| `npm run security:scan` | Security scan | 安全扫描 |

---

## 📖 Documentation | 文档

| Document | Description |
|----------|-------------|
| [Project Structure](docs/PROJECT_STRUCTURE.md) | Directory layout |
| [Command Reference](docs/operations/command-reference.md) | All npm scripts |
| [Environment Matrix](docs/operations/environment-matrix.md) | Env configurations |
| [Release Strategy](docs/operations/release-strategy.md) | Deployment guide |
| [SRE SLO Baseline](docs/operations/sre-slo-baseline.md) | Reliability targets |
| [Backup/Restore](docs/operations/backup-restore-runbook.md) | Database runbook |

All operations docs support **Chinese/English bilingual** format.

---

## 🤝 Contributing | 贡献

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Before submitting PR, ensure:
```bash
npm run lint && npm run build && npm run test:full
```

---

## 📄 License | 许可证

This project is licensed under the MIT License.

---

<p align="center">
  Made with ❤️ by the CRM Team
</p>
