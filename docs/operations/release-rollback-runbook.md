# Release / Rollback Runbook

## Trigger Conditions
- `test:full` missing `API_SMOKE_TEST_OK`
- Error rate spike on protected `/api/**` routes
- Cross-tenant authorization regression (`403` semantics broken)
- Login/session continuity regression

## Release Checklist
1. Run `npm run lint`
2. Run `npm run build`
3. Run `npm run test:e2e`
4. Run `npm run test:backend`
5. Run `npm run test:full`
6. Run `npm run preflight:prod`

## Rollback Steps
1. Identify target stable commit hash.
2. Redeploy backend + frontend artifacts from stable commit.
3. Re-apply stable environment config set.
4. Validate `/api/health/ready` is `UP`.
5. Run E2E smoke and API smoke.
6. Announce rollback completion with requestId/error window summary.

## Ownership
- Release driver: Tech Lead / DevOps
- Validation: QA
- Incident communication: On-duty owner
