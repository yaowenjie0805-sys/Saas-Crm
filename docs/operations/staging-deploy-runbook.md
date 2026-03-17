# Staging Deploy Runbook (Docker Compose)

## Prerequisites
- Staging host is reachable by SSH.
- Docker Engine and Docker Compose are installed on the staging host.
- GitHub staging environment secrets are configured:
  - `STAGING_HOST`
  - `STAGING_USER`
  - `STAGING_SSH_PORT`
  - `STAGING_BASE_DIR`
  - `STAGING_BASE_URL`
- Backend database and required middleware are reachable from staging.

## Deploy Flow
1. Build artifacts on CI (`npm run build`, backend package).
2. Generate release snapshot: `npm run release:snapshot`.
3. Deploy to staging: `npm run staging:deploy -- --artifactVersion <commit_sha>`.
4. Run staging gate: `npm run staging:verify`.
5. Generate release summary: `npm run staging:release:summary`.
6. Run preflight gate: `npm run preflight:prod`.

## Rollback Trigger
- Any failure in `staging:deploy`, `staging:verify`, or `preflight:prod`.
- Health endpoints are not `UP` after deployment.
- Gate evidence indicates `rollbackRecommendation=rollback_recommended`.

## Rollback Flow
1. Execute rollback: `npm run staging:rollback`.
2. Wait for health endpoints:
   - `/api/health/live`
   - `/api/health/ready`
   - `/api/health/deps`
3. Re-run validation:
   - `npm run staging:verify`
   - `npm run staging:release:summary`
4. Archive evidence under `logs/staging/` and update incident ticket.

## Evidence Checklist
- `logs/release/release-snapshot-*.json`
- `logs/staging/staging-deploy-*.json`
- `logs/staging/staging-verify-*.json`
- `logs/staging/staging-release-*.json`
- `logs/preflight/preflight-latest.json`
