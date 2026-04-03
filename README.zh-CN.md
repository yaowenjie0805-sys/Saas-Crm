# Aster CRM

> 轻量级企业级 CRM 系统 · 全栈单体仓库 · 开箱即用的多租户与审批引擎

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
  <a href="./README.en.md">English</a> | <strong>中文</strong>
</p>

---

## 📋 目录

- [🎯 为什么选择 Aster CRM](#为什么选择-aster-crm)
- [✨ 核心功能](#核心功能)
- [🚀 快速开始](#快速开始)
- [📦 安装选项](#安装选项)
- [🛠️ 技术栈](#技术栈)
- [📖 文档导航](#文档导航)
- [🧪 测试](#测试)
- [🐛 常见问题](#常见问题)
- [🤝 贡献指南](#贡献指南)
- [📄 许可证](#许可证)
- [🙏 致谢](#致谢)

---

## 🎯 为什么选择 Aster CRM

Aster CRM 是一个面向中小企业的开源全栈 CRM 系统，提供从销售线索到合同签订的完整业务闭环，同时具备灵活的多租户隔离和企业级权限治理能力。

- ✅ **全栈单体仓库**：前后端统一管理，一套命令完成开发、构建、测试全流程
- ✅ **多租户原生支持**：基于 `X-Tenant-Id` 请求头的强隔离架构，支持 SaaS 化部署
- ✅ **开箱即用的审批引擎**：内置可配置审批流程，支持多级审核，无需二次开发
- ✅ **AI 智能分析集成**：原生对接 OpenAI / Claude，提供销售预测与智能报表洞察
- ✅ **企业即时通讯集成**：支持企业微信、钉钉、飞书三大平台 Webhook 通知
- ✅ **完善的 CI/CD 体系**：GitHub Actions 多阶段流水线，内置性能门控与安全扫描

---

## ✨ 核心功能

### 销售管理
- 客户（Account）全生命周期管理与标签分类
- 销售管道（Pipeline）可视化，商机（Opportunity）阶段跟踪
- 联系人（Contact）关系图谱与沟通历史记录
- 合同与报价单生成、版本管理与电子签署

### 业务流程
- 订单管理（创建 → 履约 → 交付 → 结算）
- 支付管理与对账流水
- 多级审批流程引擎（可配置节点、条件分支）
- 任务管理与日程提醒

### 数据分析
- 实时仪表盘（ECharts 5 可视化）
- 自定义报表设计器（拖拽字段、条件过滤、聚合计算）
- 数据导入导出（Excel / CSV）
- AI 智能分析（销售趋势预测、异常检测）

### 权限治理
- 多租户数据隔离（Row-Level Security）
- RBAC 角色权限模型
- 字段级数据脱敏与安全控制
- 完整审计日志（操作者、时间、变更内容）

### 企业集成
- 企业微信 / 钉钉 / 飞书 Webhook 通知
- OpenAI / Claude AI 接口对接
- 标准 REST API + Swagger 文档
- Excel 批量导入导出

### 功能状态

| 模块 | 状态 |
|------|------|
| 客户 / 联系人 / 商机管理 | ✅ Stable |
| 合同 / 报价单 | ✅ Stable |
| 订单 / 支付管理 | ✅ Stable |
| 审批流程引擎 | ✅ Stable |
| RBAC 权限 + 多租户隔离 | ✅ Stable |
| 仪表盘 + 基础报表 | ✅ Stable |
| 审计日志 | ✅ Stable |
| 自定义报表设计器 | 🚧 Beta |
| AI 智能分析 | 🚧 Beta |
| 企业微信 / 钉钉 / 飞书集成 | 🚧 Beta |
| 移动端适配 | 📋 Planned |
| 工作流可视化编辑器 | 📋 Planned |

---

## 🚀 快速开始

### 环境要求

| 依赖 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 后端运行时 |
| Node.js | 18+ | 前端构建与运行 |
| MySQL | 8.0+ | 主数据库（必需） |
| Redis | 6+ | 本地缓存（推荐） |
| RabbitMQ | 3.11+ | 消息队列（推荐） |

### 5 分钟启动

**第一步：克隆仓库**

```bash
git clone https://github.com/your-repo/aster-crm.git
cd aster-crm
```

**第二步：配置环境变量**

```bash
cp .env.example .env
cp .env.backend.local.example .env.backend.local
```

编辑 `.env`，填写数据库连接信息：

```env
DB_URL=jdbc:mysql://localhost:3306/aster_crm?useUnicode=true&characterEncoding=utf8
DB_USER=root
DB_PASSWORD=your_password
```

编辑 `.env.backend.local`，修改 Token 密钥（**生产环境必须修改**）：

```env
AUTH_TOKEN_SECRET=your-secret-key-at-least-32-chars
```

**第三步：初始化数据库**

```bash
npm run db:init
```

**第四步：启动后端**

```bash
npm run dev:backend
```

**第五步：安装依赖并启动前端**

```bash
npm install
npm run dev
```

### 默认登录信息

| 项目 | 值 |
|------|-----|
| 访问地址 | http://localhost:5173 |
| 用户名 | `admin` |
| 密码 | `admin123` |
| API 地址 | http://localhost:8080/api |
| Swagger | http://localhost:8080/swagger-ui.html |

> ⚠️ `admin123` 仅用于本地开发。**生产环境请通过 `AUTH_BOOTSTRAP_DEFAULT_PASSWORD` 设置强密码。**

---

## 📦 安装选项

### 🐳 Docker Compose 一键部署（推荐）

**生产环境：**

```bash
cp .env.production.example .env.production
# 编辑 .env.production 填写生产配置
docker compose --env-file .env.production -f infra/production/docker-compose.yml up -d
```

**预发布（Staging）环境：**

```bash
cd infra/staging
cp staging.env.example staging.env
# 编辑 staging.env
docker compose -f docker-compose.yml --env-file staging.env up -d
```

### 💻 本地开发环境

参考 [🚀 快速开始](#快速开始) 章节，逐步完成本地启动。

开发常用命令：

```bash
npm run dev:backend      # 启动后端（热重载）
npm run dev              # 启动前端（Vite HMR）
npm run lint             # ESLint 代码检查
npm run test:frontend    # 前端单元测试
npm run test:e2e         # E2E 端到端测试
```

### 🏭 生产环境手动部署

```bash
# 构建后端 JAR
npm run build:backend

# 构建前端静态文件
npm run build

# 后端 JAR 位于 apps/api/target/
# 前端产物位于 apps/web/dist/
```

详细部署流程请参考 [docs/operations/staging-deploy-runbook.md](./docs/operations/staging-deploy-runbook.md)。

---

## 🛠️ 技术栈

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| React | 19 | UI 框架 |
| Vite | 7 | 构建工具 |
| Ant Design | 5 | 组件库 |
| Tailwind CSS | 4 | 原子化 CSS |
| Zustand | 5 | 状态管理 |
| ECharts | 5 | 数据可视化 |
| Vitest | latest | 单元测试 |
| Playwright | latest | E2E 测试 |

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 2.7 | 应用框架 |
| JDK | 17 | 运行时 |
| Spring Data JPA | - | ORM 数据访问 |
| Spring Security | - | 认证与授权 |
| Flyway | 9 | 数据库版本管理 |
| MySQL | 8+ | 主数据库 |
| Redis / Caffeine | - | 本地 + 分布式缓存 |
| RabbitMQ | - | 异步消息队列 |

### 基础设施

| 技术 | 用途 |
|------|------|
| Docker Compose | 容器化部署 |
| Nginx | 反向代理 / 静态文件服务 |
| GitHub Actions | CI/CD 流水线 |

完整架构依赖图请参考 [docs/ARCH_RUNTIME_DEPENDENCY_MAP.md](./docs/ARCH_RUNTIME_DEPENDENCY_MAP.md)。

---

## 📖 文档导航

| 文档 | 说明 |
|------|------|
| [docs/PROJECT_SPECIFICATION.md](./docs/PROJECT_SPECIFICATION.md) | 项目规范与设计原则 |
| [docs/PROJECT_STRUCTURE.md](./docs/PROJECT_STRUCTURE.md) | 目录结构说明 |
| [docs/DEVELOPMENT_CONVENTIONS.md](./docs/DEVELOPMENT_CONVENTIONS.md) | 开发约定与编码规范 |
| [docs/api-documentation.md](./docs/api-documentation.md) | API 接口文档 |
| [docs/ARCH_RUNTIME_DEPENDENCY_MAP.md](./docs/ARCH_RUNTIME_DEPENDENCY_MAP.md) | 架构运行时依赖图 |
| [docs/PROJECT_TROUBLESHOOTING.md](./docs/PROJECT_TROUBLESHOOTING.md) | 常见问题排查手册 |
| [docs/operations/environment-matrix.md](./docs/operations/environment-matrix.md) | 多环境配置矩阵 |
| [docs/operations/release-strategy.md](./docs/operations/release-strategy.md) | 发布策略与流程 |

---

## 🧪 测试

Aster CRM 采用三层测试策略，确保核心业务逻辑的可靠性：

```bash
# 前端单元测试（Vitest）
npm run test:frontend

# 后端单元测试（JUnit 5）
npm run test:backend

# E2E 端到端测试（Playwright）
npm run test:e2e

# 查看前端测试覆盖率报告
# 报告生成于 apps/web/coverage/
```

| 测试类型 | 工具 | 覆盖范围 |
|----------|------|----------|
| 前端单元测试 | Vitest | 组件、工具函数、状态管理 |
| 后端单元测试 | JUnit 5 + Mockito | Service 层、Repository 层 |
| E2E 测试 | Playwright | 核心用户流程（登录、CRUD、审批） |

---

## 🐛 常见问题

### 启动后登录返回 500 / `seed_tenant_id_required`

**原因：** `APP_SEED_ENABLED=true` 但 `APP_SEED_TENANT_ID` 为空。

**解决：** 在 `.env.backend.local` 中添加：

```env
APP_SEED_ENABLED=true
APP_SEED_TENANT_ID=tenant_default
APP_SEED_DEMO_ENABLED=true
```

### 登录始终失败 / Token 验证错误

**原因：** `AUTH_TOKEN_SECRET` 使用了示例默认值或长度不足 32 位。

**解决：** 在 `.env.backend.local` 中设置足够强度的密钥：

```env
AUTH_TOKEN_SECRET=my-super-secret-key-at-least-32-chars
```

### 后端启动时出现 Redis 相关 WARNING

**原因：** 未启用 Redis Repository，属于正常信息日志。

**解决：** 本地开发可安全忽略。若需完整 Redis 支持，请配置 `REDIS_HOST` 和 `REDIS_PORT`。

### AI 功能无响应 / 报错

**原因：** 未配置 AI 服务 API Key。

**解决：** 在 `.env.backend.local` 中配置对应服务密钥：

```env
AI_OPENAI_API_KEY=sk-...
# 或
AI_ANTHROPIC_API_KEY=sk-ant-...
```

更多排查步骤请参考 [docs/PROJECT_TROUBLESHOOTING.md](./docs/PROJECT_TROUBLESHOOTING.md)。

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 贡献流程

```
1. Fork 本仓库
2. 基于 main 创建特性分支：git checkout -b feat/your-feature
3. 提交变更（遵循 Conventional Commits）：git commit -m "feat: add awesome feature"
4. 推送分支：git push origin feat/your-feature
5. 提交 Pull Request，等待 CI 通过后请求 Review
```

### 开发规范

- 代码风格请遵循 [开发约定文档](./docs/DEVELOPMENT_CONVENTIONS.md)
- 新功能需附带单元测试，覆盖核心分支
- PR 描述需说明变更动机与影响范围
- 寻找 `good first issue` 标签的 Issue 入手贡献

### 提交消息格式

```
feat:     新功能
fix:      Bug 修复
docs:     文档更新
refactor: 代码重构（不含功能变更）
test:     测试相关
chore:    构建/CI/依赖等杂项
```

---

## 📄 许可证

本项目基于 **[AGPL-3.0](./LICENSE)** 许可证开源。

- ✅ 允许个人学习、研究与非商业使用
- ✅ 修改后的代码必须以相同许可证开源
- ⚠️ 商业使用请确保遵守 AGPL-3.0 的网络服务条款，或联系作者获取商业授权

---

## 🙏 致谢

感谢以下优秀开源项目为 Aster CRM 提供的基础支持：

- [Spring Boot](https://spring.io/projects/spring-boot) - 后端应用框架
- [React](https://react.dev/) - 前端 UI 框架
- [Ant Design](https://ant.design/) - 企业级 UI 组件库
- [Vite](https://vitejs.dev/) - 极速前端构建工具
- [Flyway](https://flywaydb.org/) - 数据库版本迁移管理
- [ECharts](https://echarts.apache.org/) - 开源可视化图表库
- [Playwright](https://playwright.dev/) - 可靠的端到端测试框架

---

<p align="center">
  如果这个项目对你有帮助，请给我们一个 ⭐ Star！
</p>

---

## AI 功能入口与回归测试（2026-04 更新）

- 前端入口：顶部操作栏 `AI功能` 按钮（`data-testid="topbar-ai-shortcut"`）
- 跳转行为：点击后自动切到 Dashboard，并定位到 `AI Follow-up Summary` 区块
- 核心实现：
  - `apps/web/src/crm/components/layout/TopBar.jsx`
  - `apps/web/src/crm/components/MainContent.jsx`
  - `apps/web/src/crm/components/pages/dashboard/AiFollowUpSummarySection.jsx`

推荐回归命令：

```bash
# 单测：TopBar AI 入口按钮
npm run test --workspace apps/web -- --run src/crm/__tests__/TopBar.aiShortcut.test.jsx

# 单测：MainContent 跳转链路（AI 按钮 -> Dashboard -> AI 面板）
npm run test --workspace apps/web -- --run src/crm/__tests__/MainContent.aiShortcutBridge.test.jsx

# E2E：真实浏览器链路 smoke
npm run test:e2e:runner --workspace apps/web -- tests/e2e/topbar-ai-shortcut.spec.js
```
