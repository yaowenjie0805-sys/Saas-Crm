# Release Strategy

## Artifact Strategy
- Frontend artifact: `dist/assets/*`.
- Backend artifact: `backend/target/crm-backend-1.0.0.jar`.
- Snapshot mapping: `npm run release:snapshot` writes commit -> artifact -> config summary.

## Versioning
- Use semantic commit and tag per release candidate.
- Keep rollback target as last stable tag with matching snapshot file.

## Rollback Strategy
- Trigger: SLO breach, auth regression, tenant isolation regression, or failed readiness.
- Rollback to last stable artifact pair (frontend + backend) and restore config snapshot.
- Validate with `/api/health/ready` + smoke path.

## Evidence
- Keep release checklist, snapshot JSON, and post-release verification logs for audit.
