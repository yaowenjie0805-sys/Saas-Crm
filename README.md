# CRM

企业级 CRM 单体仓库（前后端分目录），覆盖销售流程、审批流、协作、报表、导入导出、租户治理与审计运维。

## 1. 项目现状（你可以先看这个）

- 架构：`React + Vite`（前端）+ `Spring Boot 2.7`（后端，JDK 17）+ `MySQL + Flyway`。
- 运行模式：本地默认 `dev`，数据库默认 `crm_local`。
- 多租户：核心接口按 `X-Tenant-Id` 做租户隔离。
- 集成能力：支持企业微信/钉钉 webhook；飞书支持 webhook 和 App ID/App Secret 直连（tenant_access_token + 消息接口）。
- 迁移状态：当前 migration 版本到 `V20`。

## 2. 目录结构

```text
crm/
  apps/
    api/                      # Spring Boot 后端
      src/main/java/com/yao/crm/
        controller/           # REST 控制器
        service/              # 业务服务
        entity/               # JPA 实体
        repository/           # 数据访问
        config/               # Spring 配置
        security/             # 鉴权与安全相关
        exception/            # 业务异常体系（BusinessException、ErrorCode）
        enums/                # 常量枚举（状态码、类型枚举）
        event/                # 领域事件（事件发布与监听）
      src/main/resources/
        db/migration/         # Flyway 脚本 V1~V20
    web/                      # React 前端
      src/crm/components/     # 页面与业务组件
  scripts/                    # 启动、DB、修复、巡检脚本
  docs/                       # 运维与治理文档
```

## 3. 核心功能模块

### 3.1 业务模块

- 客户与联系人：客户、联系人、跟进、任务
- 销售流程：线索、商机、报价、订单、回款、合同
- 审批中心：模板、实例、任务、转审、催办、SLA
- 协作与权限：评论、活动共享、字段权限、团队
- 报表与分析：仪表盘、报表模板、导出任务
- 工作流：节点、执行、调度、通知
- 导入导出：批量导入、失败重试、导出作业

### 3.2 典型接口前缀

- 基础接口：`/api/**`
- 租户业务接口：`/api/v1/**`
- 配置/治理接口：`/api/v2/**`

常见控制器可在目录查看：
`apps/api/src/main/java/com/yao/crm/controller`

## 4. 技术栈

- 前端：React 19、Vite、React Router、Zustand
- 后端：Spring Boot 2.7、Spring Data JPA、Flyway、Redis、Caffeine（本地缓存）、RabbitMQ
- API 文档：springdoc-openapi（Swagger UI）
- 数据库：MySQL 8+
- 测试：JUnit（后端）、Playwright（前端 E2E）

## 5. 本地开发快速开始

### 5.1 环境要求 | Environment Requirements

| 工具 Tool | 版本 Version | 说明 Note |
|-----------|-------------|----------|
| JDK | 17+ | Java 开发环境 |
| Node.js | 18+ | 前端运行环境 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 6.0+ | 缓存（可选，默认使用本地缓存） |
| Maven | 3.6+ | Java 构建工具 |

### 5.2 一键启动指南 | Quick Start Guide

> 复制以下命令，按顺序执行即可启动项目

```bash
# 1. 克隆项目 | Clone the project
git clone https://github.com/yaowenjie0805-sys/Saas-Crm.git
cd Saas-Crm

# 2. 配置环境变量 | Configure environment
# Windows:
copy .env.example .env
copy .env.backend.local.example .env.backend.local
# Linux/Mac:
cp .env.example .env
cp .env.backend.local.example .env.backend.local
# 编辑 .env 和 .env.backend.local 填入你的数据库密码等配置

# 3. 初始化数据库 | Initialize database
# Windows:
npm run db:init
# Linux/Mac:
bash scripts/init-db.sh root root crm_local

# 4. 启动后端 | Start backend
npm run dev:backend
# 或者使用更稳定的脚本:
# npm run dev:backend:stable

# 5. 启动前端（另开一个终端）| Start frontend (new terminal)
npm install
npm run dev

# 6. 访问 | Access
# 前端 Frontend: http://localhost:5173
# 后端 API: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### 5.3 默认访问地址 | Default URLs

| 服务 Service | 地址 URL |
|-------------|---------|
| 前端 Frontend | `http://localhost:5173` |
| 后端 Backend | `http://localhost:8080` |
| 健康检查 Health Check | `http://localhost:8080/api/health` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/api-docs` |

## 6. 数据库迁移与修复

### 6.1 正常迁移

后端启动时会自动 Flyway migrate（`validate-on-migrate=true`）。

### 6.2 如果遇到 checksum mismatch / failed migration

使用仓库脚本做“先备份再修复”：

```bash
npm run db:flyway:repair
```

该脚本会：

1. 备份当前数据库到 `backups/`
2. 执行 `flyway repair`
3. 执行 `flyway migrate`
4. 输出最新版本与失败记录数

## 7. 集成配置（重点）

### 7.1 配置文件加载规则

后端命令会自动读取根目录 `.env.backend.local`（若存在）。

- 读取逻辑在：`scripts/run-maven.mjs`
- 模板文件：`.env.backend.local.example`

### 7.2 飞书（App 模式，推荐）

在 `.env.backend.local` 配置：

```env
INTEGRATION_WEBHOOK_PROVIDERS=WECOM,DINGTALK,FEISHU
INTEGRATION_FEISHU_APP_ID=cli_xxx
INTEGRATION_FEISHU_APP_SECRET=xxx
INTEGRATION_FEISHU_RECEIVE_ID=oc_xxx
INTEGRATION_FEISHU_RECEIVE_ID_TYPE=chat_id
INTEGRATION_FEISHU_BASE_URL=https://open.feishu.cn
```

说明：

- `RECEIVE_ID_TYPE` 支持：`chat_id` / `open_id` / `user_id` / `union_id`
- 飞书会优先走 App API 直连，未配置时才回退 webhook。

### 7.3 企业微信/钉钉 webhook（可选）

```env
INTEGRATION_WECOM_WEBHOOK_URL=
INTEGRATION_DINGTALK_WEBHOOK_URL=
INTEGRATION_DINGTALK_SECRET=
```

### 7.4 真机连通性测试

```bash
powershell -ExecutionPolicy Bypass -File scripts/test-webhooks.ps1
```

脚本会同时检查：

- WECOM webhook
- DINGTALK webhook
- FEISHU webhook
- FEISHU App 模式

## 8. 常用命令

```bash
# 前端构建
npm run build

# 后端测试
npm run test:backend

# 前端 E2E
npm run test:e2e

# 全链路本地验证
npm run test:full

# 环境校验
npm run validate:env
```

## 9. 常见问题 | FAQ

### 9.1 启动时报 Flyway 校验失败 | Flyway Checksum Mismatch

先执行：`npm run db:flyway:repair`。

### 9.2 数据库连接失败怎么办？| Database Connection Failed

检查以下几点：
1. MySQL 服务是否启动：`mysql -u root -p`
2. 数据库是否存在：`SHOW DATABASES LIKE 'crm_local';`
3. `.env` 文件中的 `DB_URL`、`DB_USER`、`DB_PASSWORD` 是否正确
4. MySQL 连接数是否已满：`SHOW STATUS LIKE 'Threads_connected';`

### 9.3 端口被占用怎么办？| Port Already in Use

Windows:
```powershell
# 查看 8080 端口占用
netstat -ano | findstr :8080
# 结束进程（PID 为上一步查到的进程ID）
taskkill /PID <进程ID> /F
```

Linux/Mac:
```bash
# 查看 8080 端口占用
lsof -i :8080
# 结束进程
kill -9 <PID>
```

### 9.4 Maven 下载慢怎么配置镜像？| Maven Mirror Configuration

编辑 Maven 配置文件（Windows: `~/.m2/settings.xml`，Linux/Mac: `~/.m2/settings.xml`）：

```xml
<mirrors>
  <mirror>
    <id>aliyun</id>
    <mirrorOf>central</mirrorOf>
    <name>Aliyun Maven Mirror</name>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

### 9.5 前端启动报错 Node 版本不对？| Node Version Mismatch

项目要求 Node.js 18+，推荐使用 nvm 管理多版本：

```bash
# 安装 nvm (Windows 用户下载 nvm-windows)
# Windows: https://github.com/coreybutler/nvm-windows/releases

# 安装并切换到 Node 18
nvm install 18
nvm use 18

# 验证版本
node -v
```

### 9.6 如何切换中英文？| How to Switch Language?

前端支持中英文切换：
1. 点击页面右上角用户头像
2. 选择"设置 / Settings"
3. 在"语言 / Language"下拉框中选择"中文"或"English"

### 9.7 后端起来了但飞书发不出去 | Feishu Integration Not Working

优先检查：

1. `.env.backend.local` 是否存在且键名正确
2. `INTEGRATION_FEISHU_RECEIVE_ID` 是否可用
3. 应用权限是否包含消息发送相关 scope

### 9.8 本地改了配置但不生效 | Config Changes Not Taking Effect

确认你是通过 `npm run dev:backend` 或 `npm run dev:backend:stable` 启动（两者都会走 `run-maven.mjs` 读取本地 env）。

## 10. 相关阅读

- 目录说明：`docs/PROJECT_STRUCTURE.md`
- 全景调用图：`docs/PROJECT_FLOW_MAP.md`
- 运维文档索引：`docs/README.md`
- 命令参考：`docs/operations/command-reference.md`
- 环境矩阵：`docs/operations/environment-matrix.md`

---

如果你要继续扩展这个项目，建议从这三条线并行推进：

1. 把三方集成配置做成租户后台可维护（而不是仅环境变量）
2. 给通知链路加失败告警与可视化重试面板
3. 完善“接口-页面-数据库”对照文档，降低接手成本

## 11. 二开快速入口

- 热点文件地图：`docs/DEVELOPMENT_HOTSPOTS.md`
- 全景调用图：`docs/PROJECT_FLOW_MAP.md`
- 故障定位手册：`docs/PROJECT_TROUBLESHOOTING.md`
- 功能模块矩阵：`docs/MODULE_CAPABILITY_MATRIX.md`
- 接口分组清单：`docs/API_ENDPOINT_CATALOG.md`
- Postman 联调包：`docs/postman/crm-api.postman_collection.json` + `docs/postman/crm-local.postman_environment.json`
