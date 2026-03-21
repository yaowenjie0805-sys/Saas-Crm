# 备份/恢复手册 | Backup / Restore Runbook

---

## 适用范围 | Scope

| 中文 | English |
|------|---------|
| CRM 环境的 MySQL 备份和恢复验证手册 | Runbook for MySQL backup and restore verification for CRM environments. |

---

## 前置条件 | Preconditions

| 中文 | English |
|------|---------|
| 数据库主机和凭证访问权限 | Access to DB host and credentials. |
| 足够的磁盘空间存放转储文件 | Enough disk space for dump files. |
| 恢复演练已获得维护窗口批准 | Maintenance window approved for restore drills. |

---

## 备份 | Backup

| 步骤 | 中文 | English |
|------|------|---------|
| 1 | 运行备份脚本: `powershell -ExecutionPolicy Bypass -File scripts/db-backup.ps1` | Run backup script: `powershell -ExecutionPolicy Bypass -File scripts/db-backup.ps1` |
| 2 | 验证备份产物存在且非空 | Verify backup artifact exists and is non-empty. |
| 3 | 记录产物路径、大小和时间戳 | Record artifact path, size, and timestamp. |

---

## 恢复演练 | Restore Drill

| 步骤 | 中文 | English |
|------|------|---------|
| 1 | 准备隔离的目标数据库实例 | Prepare isolated target DB instance. |
| 2 | 运行恢复脚本: `powershell -ExecutionPolicy Bypass -File scripts/db-restore.ps1` | Run restore script: `powershell -ExecutionPolicy Bypass -File scripts/db-restore.ps1` |
| 3 | 运行冒烟验证: `npm run test:api` | Run smoke validation: `npm run test:api` |
| 4 | 验证关键表行数和最近记录 | Verify key tables row counts and recent records. |

---

## 成功标准 | Success Criteria

| 中文 | English |
|------|---------|
| 恢复完成且无 SQL 错误 | Restore completes without SQL errors. |
| API 冒烟测试通过 | API smoke test passes. |
| 租户和核心 CRM 数据可查询 | Tenant and core CRM data are queryable. |

---

## 回滚 | Rollback

| 中文 | English |
|------|---------|
| 如恢复演练失败，丢弃目标数据库并使用之前的已知良好备份重试 | If restore drill fails, discard target DB and retry with previous known-good backup. |
| 如连续两次尝试失败，升级至值班人员 | Escalate to on-call if two consecutive attempts fail. |

---

## 证据归档 | Evidence

| 中文 | English |
|------|---------|
| 备份命令输出 | Backup command output |
| 恢复命令输出 | Restore command output |
| API 冒烟结果 | API smoke result |
| 操作员 + 审核人签字 | Operator + reviewer sign-off |
