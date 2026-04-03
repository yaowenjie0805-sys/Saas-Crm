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
- `apps/web/src/crm/hooks/api/`（按领域拆分，优先核对对应的 `useXxxApi`）

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

## 8.1 Caffeine 本地缓存问题

### 现象

- 缓存数据不一致
- 缓存未生效导致数据库压力增大

### 快速判断

- `CacheService` 是否正确配置 Caffeine
- 缓存键是否包含租户隔离前缀
- 缓存过期时间是否合理

### 修复动作

1. 检查 `apps/api/src/main/java/com/yao/crm/service/CacheService.java`
2. 确认 Caffeine 配置：`maximumSize`、`expireAfterWrite`
3. 验证缓存键格式：`tenantId:entity:id`

关键配置：
```properties
cache.caffeine.maximum-size=1000
cache.caffeine.expire-after-write=300s
```

---

## 9. 业务异常与 ErrorCode 排查

### 现象

- 接口返回 4xx/5xx 错误码
- 错误响应包含 `code` 字段

### 快速判断

| ErrorCode | 含义 | 常见原因 |
|-----------|------|----------|
| `ENTITY_NOT_FOUND` | 实体不存在 | ID 错误、数据已删除 |
| `DUPLICATE_ENTITY` | 实体重复 | 唯一约束冲突 |
| `INVALID_STATE` | 状态非法 | 状态机转换不满足条件 |
| `PERMISSION_DENIED` | 权限不足 | 角色权限不足、租户隔离 |
| `VALIDATION_ERROR` | 参数校验失败 | 字段格式错误、必填项为空 |
| `EXTERNAL_SERVICE_ERROR` | 外部服务调用失败 | 第三方 API 超时、网络问题 |

### 修复动作

1. 查看响应 `code` 字段定位错误类型
2. 查看 `message` 字段获取详细信息
3. 对照 `exception/ErrorCode.java` 确认错误码定义

关键文件：
- `apps/api/src/main/java/com/yao/crm/exception/ErrorCode.java`
- `apps/api/src/main/java/com/yao/crm/exception/BusinessException.java`

---

## 10. 参数校验失败（400 Bad Request）

### 现象

- 接口返回 400 错误
- 响应包含字段级错误信息

### 快速判断

- 请求体是否满足 DTO 定义的 JSR-303 注解约束
- 是否缺少必填字段

### 修复动作

1. 检查 Controller 方法参数是否有 `@Valid` 注解
2. 检查 DTO 字段注解：
   - `@NotNull`：不能为 null
   - `@NotBlank`：字符串非空
   - `@Size(min, max)`：长度范围
   - `@Pattern(regexp)`：正则匹配
   - `@Email`：邮箱格式
   - `@Min`/`@Max`：数值范围

错误响应示例：
```json
{
  "code": "VALIDATION_ERROR",
  "message": "参数校验失败",
  "errors": [
    {"field": "name", "message": "不能为空"},
    {"field": "email", "message": "邮箱格式不正确"}
  ]
}
```

关键文件：
- `apps/api/src/main/java/com/yao/crm/dto/request/` — 请求 DTO 定义

---

## 11. Swagger UI 无法访问

### 现象

- 访问 `/swagger-ui.html` 返回 404
- 访问 `/api-docs` 无响应

### 快速判断

- `springdoc-openapi` 依赖是否已添加
- `OpenApiConfig` 配置是否正确
- Security 配置是否放行了 swagger 相关路径

### 修复动作

1. 确认依赖已添加到 `pom.xml`：
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-ui</artifactId>
    <version>1.7.0</version>
</dependency>
```

2. 检查 Security 配置是否放行：
```java
.antMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
```

3. 检查 `OpenApiConfig` 配置类

关键文件：
- `apps/api/src/main/java/com/yao/crm/config/OpenApiConfig.java`
- `apps/api/src/main/java/com/yao/crm/security/SecurityConfig.java`

---

## 12. 前端 build 告警/打包分块异常

### 现象

- 构建出现 chunk 相关告警

### 快速判断

- 查看 `apps/web/vite.config.js` 中 `manualChunks` 策略

### 修复动作

- 把跨路由共享依赖下沉到公共 chunk
- 保持路由懒加载行为不变

---

## 13. CI 或本地测试偶发失败

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
