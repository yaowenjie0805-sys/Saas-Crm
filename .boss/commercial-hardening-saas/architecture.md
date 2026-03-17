# 架构收口说明

## 摘要
采用“前端展示层统一 + 后端安全与租户隔离兜底”的双层收口方式。前端仅改结构语义类与 CSS；后端保持兼容期（Cookie 主路径 + Bearer 过渡）。

## 核心设计
- 会话层：`AuthInterceptor` 优先 Bearer，再读 Cookie；前端统一 `credentials: include`。
- 隔离层：legacy 控制器统一 `currentTenant(request)` 驱动查询/更新/删除，仓储接口使用 tenant-aware 方法。
- 安全守卫：`ProductionSecurityGuard` 在 `prod` 启动时检测弱 secret、mock sso、静态 mfa、弱默认密码并 fail-fast。
- 配置层：seed/demo 与 CORS 白名单全部环境化，`prod` 默认禁用 seed。

## 兼容策略
- 对外 API 路径不变。
- 错误体结构不变（包含 `code/message/requestId/details`）。
- Bearer 仅做一个版本周期过渡兼容。
