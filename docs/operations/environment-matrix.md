# 鐜鐭╅樀 | Environment Matrix

鏈枃妗ｆ弿杩?CRM 绯荤粺鍚勭幆澧冪殑閰嶇疆鍜屾檵鍗囪鍒欍€? 
This document describes the configuration and promotion rules for each environment in the CRM system.

---

## 鐜鍩虹嚎 | Baseline

| 鐜 | 鐢ㄩ€?| 鏁版嵁婧?| 绉嶅瓙绛栫暐 | 璁よ瘉 Cookie | SSO 妯″紡 |
|------|------|--------|----------|-------------|----------|
| Env | Purpose | Data Source | Seed Policy | Auth Cookie | SSO Mode |
| dev | 鍔熻兘寮€鍙?| 鏈湴 MySQL | 鍚敤 | secure=false | mock/oidc |
| staging | 鍙戝竷楠岃瘉 | 鎵樼 MySQL 鍏嬮殕 | 绂佺敤 | secure=true | oidc |
| prod | 鐢熶骇娴侀噺 | 鎵樼 MySQL | 绂佺敤 | secure=true | oidc |

---

## 瀵嗛挜涓庨厤缃鍒?| Secrets & Config Rules

### 鍩烘湰鍘熷垯 | Basic Principles

- **姘镐笉鎻愪氦瀵嗛挜鍒颁唬鐮佷粨搴?* | Never commit secrets in repo
- **绂佹鐢熶骇鐜浣跨敤榛樿鍊?* | Prod forbidden defaults: `crm-secret-change-me`, `000000`, `mock`, `admin123`

### 蹇呴渶鐨勭幆澧冨彉閲?| Required Environment Variables

| 鍙橀噺 | 鎻忚堪 | Variable | Description |
|------|------|----------|-------------|
| `AUTH_TOKEN_SECRET` | 璁よ瘉浠ょ墝瀵嗛挜 | `AUTH_TOKEN_SECRET` | Auth token secret |
| `DB_URL` | 鏁版嵁搴撹繛鎺?URL | `DB_URL` | Database connection URL |
| `DB_USER` | 鏁版嵁搴撶敤鎴峰悕 | `DB_USER` | Database username |
| `DB_PASSWORD` | 鏁版嵁搴撳瘑鐮?| `DB_PASSWORD` | Database password |

### CORS 閰嶇疆 | CORS Configuration

- `SECURITY_CORS_ALLOWED_ORIGINS` 蹇呴』浣跨敤鐜鐗瑰畾鐨勭櫧鍚嶅崟
- `SECURITY_CORS_ALLOWED_ORIGINS` must be environment-specific allowlist

---

## 鏅嬪崌瑙勫垯 | Promotion Rules

### dev 鈫?staging

1. 闂ㄧ妫€鏌ュ叏閮ㄩ€氳繃 | Gate green
2. 鍙樻洿妫€鏌ユ竻鍗曞畬鎴?| Change checklist complete

### staging 鈫?prod

1. 鍥炴粴婕旂粌璇佹嵁 | Rollback drill evidence
2. 鍙戝竷蹇収 | Release snapshot
