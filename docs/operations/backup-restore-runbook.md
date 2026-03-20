# 澶囦唤/鎭㈠鎵嬪唽 | Backup / Restore Runbook

鏈枃妗ｆ彁渚?MySQL 鏁版嵁搴撳浠藉拰鎭㈠婕旂粌鐨勫彲閲嶅鏈€灏忔祦绋嬨€? 
This document provides a repeatable minimum process for MySQL backup and restore drills.

---

## 澶囦唤鎿嶄綔 | Backup

### 鍛戒护 | Command

```powershell
powershell -ExecutionPolicy Bypass -File scripts/db-backup.ps1 -OutDir backups
```

### 鍙傛暟璇存槑 | Parameter Description

| 鍙傛暟 | 鎻忚堪 | Parameter | Description |
|------|------|-----------|-------------|
| `-OutDir` | 澶囦唤鏂囦欢杈撳嚭鐩綍 | `-OutDir` | Backup file output directory |

---

## 鎭㈠鎿嶄綔 | Restore

### 鍛戒护 | Command

```powershell
powershell -ExecutionPolicy Bypass -File scripts/db-restore.ps1 -BackupFile backups/<your-file>.sql
```

### 鍙傛暟璇存槑 | Parameter Description

| 鍙傛暟 | 鎻忚堪 | Parameter | Description |
|------|------|-----------|-------------|
| `-BackupFile` | 瑕佹仮澶嶇殑澶囦唤鏂囦欢璺緞 | `-BackupFile` | Backup file path to restore |

---

## 婕旂粌妫€鏌ユ竻鍗?| Drill Checklist

| 姝ラ | 鎿嶄綔 | Step | Action |
|------|------|------|--------|
| 1 | 鍦ㄦ紨缁冪幆澧冧腑鍒涘缓澶囦唤 | 1 | Create backup in drill environment |
| 2 | 鎻掑叆鎴栨洿鏂颁竴涓凡鐭ユ爣璁拌褰?| 2 | Insert or update a known marker record |
| 3 | 浠庡浠芥仮澶?| 3 | Restore from backup |
| 4 | 楠岃瘉鏍囪璁板綍宸插洖婊氬苟杩愯 `npm run test:e2e` 鍐掔儫娴嬭瘯 | 4 | Verify marker rollback and run `npm run test:e2e` smoke |
| 5 | 璁板綍婕旂粌鏃堕棿銆佹搷浣滀汉鍜岀粨鏋?| 5 | Record drill time, operator, and result |

---

## 娉ㄦ剰浜嬮」 | Notes

### 鍑瘉閰嶇疆 | Credentials Configuration

鍑瘉鏉ヨ嚜浠ヤ笅鐜鍙橀噺 | Credentials come from the following environment variables:

| 鍙橀噺 | 鎻忚堪 | Variable | Description |
|------|------|----------|-------------|
| `DB_HOST` | 鏁版嵁搴撲富鏈?| `DB_HOST` | Database host |
| `DB_PORT` | 鏁版嵁搴撶鍙?| `DB_PORT` | Database port |
| `DB_NAME` | 鏁版嵁搴撳悕绉?| `DB_NAME` | Database name |
| `DB_USER` | 鏁版嵁搴撶敤鎴峰悕 | `DB_USER` | Database username |
| `DB_PASSWORD` | 鏁版嵁搴撳瘑鐮?| `DB_PASSWORD` | Database password |

### 榛樿鍊?| Defaults

濡傛灉鐜鍙橀噺鏈缃紝灏嗕娇鐢ㄦ湰鍦板紑鍙戦粯璁ゅ€笺€? 
If environment variables are absent, local dev values are used as defaults.

---

## 婕旂粌璁板綍妯℃澘 | Drill Record Template

```markdown
## 澶囦唤鎭㈠婕旂粌璁板綍 | Backup/Restore Drill Record

- 鏃ユ湡 | Date: YYYY-MM-DD
- 鎿嶄綔浜?| Operator: [濮撳悕 | Name]
- 鐜 | Environment: [dev/staging]
- 澶囦唤鏂囦欢 | Backup File: [鏂囦欢鍚?| Filename]
- 鎭㈠缁撴灉 | Restore Result: [鎴愬姛/澶辫触 | Success/Failure]
- 楠岃瘉缁撴灉 | Verification Result: [閫氳繃/澶辫触 | Pass/Fail]
- 澶囨敞 | Notes: [浠讳綍闂鎴栬瀵?| Any issues or observations]
```
