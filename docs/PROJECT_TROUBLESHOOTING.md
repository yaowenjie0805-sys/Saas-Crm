# 项目故障定位手册（Troubleshooting Playbook）

本文聚焦本仓库最常见的本地开发故障，按“现象 -> 快速判断 -> 修复动作”组织。

## 1. 后端启动失败：Flyway checksum mismatch / failed migration

### 现象

- 启动日志报 `FlywayValidateException`
- 提示 `Migration checksum mismatch` 或 `Detected failed migration`

### 快速判断

- 本地库已执行过历史版本脚本，脚本内容后来被修改
- `flyway_schema_history` 有 `success=0`

### 修复动作

```bash
npm run db:flyway:repair
```

脚本会自动备份并执行 repair + migrate。

关键脚本：

- `scripts/flyway-repair-dev.ps1`
- `scripts/flyway-repair-dev.sh`

---

## 2. 后端启动正常但接口 401/403

### 现象

- 接口返回未授权或无权限
- 前端页面空白或只显示部分数据

### 快速判断

- 请求头是否有 `Authorization`
- 请求头是否有 `X-Tenant-Id`
- 当前角色是否满足接口所需权限

### 修复动作

1. 重新登录获取 token
2. 确认 localStorage 中 `tenantId` 不为空
3. 对照控制器鉴权逻辑（ADMIN/MANAGER 等）

关键文件：

- `apps/api/src/main/java/com/yao/crm/controller/BaseApiController.java`
- `apps/web/src/crm/hooks/useApi.js`

---

## 3. 前端可访问但后端 API 全红

### 现象

- 前端页面打开但数据接口全部失败
- 控制台显示 `Failed to fetch` / 网络错误

### 快速判断

- 后端是否监听 `8080`
- CORS 是否允许当前前端地址

### 修复动作

1. 启动后端：`npm run dev:backend` 或 `npm run dev:backend:stable`
2. 检查 `application.properties` 的 `security.cors.allowed-origins`
3. 访问 `http://localhost:8080/api/health`

---

## 4. 飞书发送失败（App 模式）

### 现象

- 通知任务失败
- 连通性脚本里 `FEISHU_APP` 为 false

### 快速判断

- `INTEGRATION_FEISHU_APP_ID/APP_SECRET` 是否已配置
- `INTEGRATION_FEISHU_RECEIVE_ID` 是否为空
- 应用权限 scope 是否满足消息发送

### 修复动作

```bash
powershell -ExecutionPolicy Bypass -File scripts/test-webhooks.ps1
```

配置建议（`.env.backend.local`）：

```env
INTEGRATION_FEISHU_APP_ID=cli_xxx
INTEGRATION_FEISHU_APP_SECRET=xxx
INTEGRATION_FEISHU_RECEIVE_ID=oc_xxx
INTEGRATION_FEISHU_RECEIVE_ID_TYPE=chat_id
```

关键文件：

- `apps/api/src/main/java/com/yao/crm/service/IntegrationWebhookService.java`
- `scripts/test-webhooks.ps1`

---

## 5. 企业微信/钉钉发送失败

### 现象

- 通知作业一直重试或失败
- 日志出现 webhook 非 2xx

### 快速判断

- webhook URL 是否正确
- 钉钉是否配置签名密钥

### 修复动作

配置 `.env.backend.local`：

```env
INTEGRATION_WECOM_WEBHOOK_URL=
INTEGRATION_DINGTALK_WEBHOOK_URL=
INTEGRATION_DINGTALK_SECRET=
```

执行 `scripts/test-webhooks.ps1` 逐项验证。

---

## 6. 本地配置不生效

### 现象

- 改了环境变量文件但运行结果没变

### 快速判断

- 是否通过 `npm run dev:backend` 或 `npm run dev:backend:stable` 启动
- `.env.backend.local` 文件是否在仓库根目录

### 修复动作

1. 使用项目脚本启动（会自动加载 `.env.backend.local`）
2. 重启后端进程

关键文件：

- `scripts/run-maven.mjs`

---

## 7. 导入任务异常（文件上传后失败）

### 现象

- 导入任务创建后很快失败
- 失败行导出异常

### 快速判断

- 文件大小/行数是否超过限制
- MQ 相关开关是否符合当前环境

### 修复动作

- 检查 `lead.import.*` 配置
- 本地纯调试可关闭 MQ 发布/监听

关键文件：

- `apps/api/src/main/java/com/yao/crm/service/LeadImportService.java`
- `apps/api/src/main/resources/application*.properties`

---

## 8. Redis 相关日志噪音很多

### 现象

- 启动日志大量提示 `Could not safely identify store assignment`

### 快速判断

- 是信息提示，不一定是故障

### 修复动作

- 若功能不受影响可先忽略
- 需要收敛时再做 repository 扫描/配置治理

---

## 9. 前端 build 告警/打包分块异常

### 现象

- 构建出现 chunk 相关告警

### 快速判断

- 查看 `apps/web/vite.config.js` 中 `manualChunks` 策略

### 修复动作

- 把跨路由共享依赖下沉到公共 chunk
- 保持路由懒加载行为不变

---

## 10. CI 或本地测试偶发失败

### 现象

- `test:backend` 或 `test:e2e` 间歇失败

### 快速判断

- 依赖服务是否稳定（MySQL/Redis/RabbitMQ）
- 端口占用、环境变量污染

### 修复动作

1. 先跑后端单测：`npm run test:backend`
2. 再跑前端构建：`npm run build`
3. 最后跑端到端：`npm run test:full`

---

## 附：建议排查顺序

1. 先看健康检查：`/api/health`
2. 再看配置是否加载（run-maven 输出）
3. 再看业务日志 + 审计日志
4. 最后再看数据库状态（任务表 / flyway 表）
