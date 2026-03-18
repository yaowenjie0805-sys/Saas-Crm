# Oncall Escalation Runbook

## Alert Levels
- `P1`: customer-impacting outage or release-blocking fault. Immediate response required.
- `P2`: significant degradation with no complete outage. Response in current shift.
- `P3`: warning-level trend or threshold drift. Track and resolve within 24h.

## Escalation Path
1. Primary oncall acknowledges alert and starts incident record.
2. If no mitigation in 15 minutes (`P1`) or 60 minutes (`P2`), escalate to SRE Lead.
3. If unresolved after second SLA window, escalate to Engineering Manager.
4. For cross-tenant/safety incidents, include Security owner immediately.

## Required Evidence
- `requestId`, `tenantId`, impacted route, and first seen timestamp.
- `logs/sre/alerts-latest.json` with level/reasons/recommendation.
- rollback decision and validation result (`staging:verify` / health checks).

## Shift Handover Checklist
- active alerts and owners
- mitigation status and next checkpoint
- rollback state and pending release decisions
