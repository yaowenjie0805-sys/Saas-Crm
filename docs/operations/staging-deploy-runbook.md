# 棰勫彂鐜閮ㄧ讲鎵嬪唽 | Staging Deploy Runbook (Docker Compose)

鏈枃妗ｆ弿杩板浣曞皢搴旂敤閮ㄧ讲鍒伴鍙戠幆澧冦€? 
This document describes how to deploy the application to the staging environment.

---

## 鍓嶇疆鏉′欢 | Prerequisites

- 棰勫彂鏈嶅姟鍣ㄥ彲閫氳繃 SSH 璁块棶 | Staging host is reachable by SSH
- 棰勫彂鏈嶅姟鍣ㄥ凡瀹夎 Docker Engine 鍜?Docker Compose | Docker Engine and Docker Compose are installed on the staging host
- GitHub 棰勫彂鐜瀵嗛挜宸查厤缃?| GitHub staging environment secrets are configured:
  - `STAGING_HOST` - 鏈嶅姟鍣ㄥ湴鍧€ | Server hostname
  - `STAGING_USER` - SSH 鐢ㄦ埛鍚?| SSH username
  - `STAGING_SSH_PORT` - SSH 绔彛 | SSH port
  - `STAGING_BASE_DIR` - 閮ㄧ讲鐩綍 | Deployment directory
  - `STAGING_BASE_URL` - 鏈嶅姟 URL | Service URL
- 鍚庣鏁版嵁搴撳拰涓棿浠跺彲浠庨鍙戠幆澧冭闂?| Backend database and required middleware are reachable from staging

---

## 閮ㄧ讲娴佺▼ | Deploy Flow

1. **鏋勫缓浜х墿** | Build artifacts on CI
   ```bash
   npm run build
   npm run build:backend
   ```

2. **鐢熸垚鍙戝竷蹇収** | Generate release snapshot
   ```bash
   npm run release:snapshot
   ```

3. **閮ㄧ讲鍒伴鍙?* | Deploy to staging
   ```bash
   npm run staging:deploy -- --artifactVersion <commit_sha>
   ```

4. **杩愯棰勫彂楠岃瘉** | Run staging gate
   ```bash
   npm run staging:verify
   ```

5. **鐢熸垚鍙戝竷鎽樿** | Generate release summary
   ```bash
   npm run staging:release:summary
   ```

6. **杩愯棰勬闂ㄧ** | Run preflight gate
   ```bash
   npm run preflight:prod
   ```

---

## 鍥炴粴瑙﹀彂鏉′欢 | Rollback Trigger

婊¤冻浠ヤ笅浠讳竴鏉′欢鏃惰Е鍙戝洖婊?| Rollback is triggered when any of the following conditions are met:

- `staging:deploy`銆乣staging:verify` 鎴?`preflight:prod` 鎵ц澶辫触 | Any failure in `staging:deploy`, `staging:verify`, or `preflight:prod`
- 閮ㄧ讲鍚庡仴搴锋鏌ョ鐐圭姸鎬侀潪 `UP` | Health endpoints are not `UP` after deployment
- 闂ㄧ璇佹嵁鏄剧ず `rollbackRecommendation=rollback_recommended` | Gate evidence indicates `rollbackRecommendation=rollback_recommended`

---

## 鍥炴粴娴佺▼ | Rollback Flow

1. **鎵ц鍥炴粴** | Execute rollback
   ```bash
   npm run staging:rollback
   ```

2. **绛夊緟鍋ュ悍妫€鏌?* | Wait for health endpoints
   - `/api/health/live` - 瀛樻椿鎺㈤拡 | Liveness probe
   - `/api/health/ready` - 灏辩华鎺㈤拡 | Readiness probe
   - `/api/health/deps` - 渚濊禆妫€鏌?| Dependencies check

3. **閲嶆柊楠岃瘉** | Re-run validation
   ```bash
   npm run staging:verify
   npm run staging:release:summary
   ```

4. **褰掓。璇佹嵁** | Archive evidence
   - 灏嗚瘉鎹綊妗ｅ埌 `logs/staging/` | Archive evidence under `logs/staging/`
   - 鏇存柊浜嬫晠宸ュ崟 | Update incident ticket

---

## 璇佹嵁娓呭崟 | Evidence Checklist

| 鏂囦欢 | 鎻忚堪 | File | Description |
|------|------|------|-------------|
| `logs/release/release-snapshot-*.json` | 鍙戝竷蹇収 | `logs/release/release-snapshot-*.json` | Release snapshot |
| `logs/staging/staging-deploy-*.json` | 閮ㄧ讲璁板綍 | `logs/staging/staging-deploy-*.json` | Deploy record |
| `logs/staging/staging-verify-*.json` | 楠岃瘉璁板綍 | `logs/staging/staging-verify-*.json` | Verify record |
| `logs/staging/staging-release-*.json` | 鍙戝竷鎽樿 | `logs/staging/staging-release-*.json` | Release summary |
| `logs/preflight/preflight-latest.json` | 棰勬缁撴灉 | `logs/preflight/preflight-latest.json` | Preflight result |
