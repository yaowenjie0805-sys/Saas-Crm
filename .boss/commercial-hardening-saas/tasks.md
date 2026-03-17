# 任务拆解（一次性收口）

## 阶段 A：视觉商用化（不改逻辑）
- 调整 Topbar 结构语义分组（搜索 / 次级动作 / 主动作）。
- 调整 Sidebar 底部退出区为低干扰样式。
- 调整 Tasks 语义类与标题标记，提升可扫读性。
- 统一亮色商务样式覆盖并压制赛博 glow。

## 阶段 B：安全与隔离收口
- 复核 Cookie 会话主路径与登出失效逻辑。
- 复核 legacy tenant 隔离查询路径与 `403` 语义。
- 复核 prod fail-fast 与 seed/cors 环境开关。

## 阶段 C：验证与交付
- 运行全量门禁：`lint/build/e2e/backend/full`。
- 产出 qa-report。
- 全量提交（排除本地缓存目录）。
