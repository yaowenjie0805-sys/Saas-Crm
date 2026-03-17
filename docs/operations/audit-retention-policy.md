# Audit Retention Policy

## Scope
Applies to `/api/audit-logs/**` and audit export jobs.

## Baseline Policy
- Retention period: 180 days online.
- Export cadence: weekly export of previous 7 days.
- High-priority events: `TENANT_FORBIDDEN`, auth failures, export failures.
- Sensitive fields: redact secrets/tokens/passwords in `details`.

## Export Failure Handling
1. Check `/api/audit-logs/export-metrics` for queue/failure trend.
2. Retry failed job via `/api/audit-logs/export-jobs/{jobId}/retry`.
3. If repeated failures continue, open incident with requestId + tenantId context.

## Compliance Replay
- Use `requestId + tenantId + username + action` for timeline reconstruction.
- Keep exported CSV artifacts in restricted access storage.
