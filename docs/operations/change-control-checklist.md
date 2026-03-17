# Change Control Checklist

## Before Release
1. Link change to ticket/feature and owner.
2. Confirm impacted API/routes and rollback path.
3. Complete gate: `lint/build/test:e2e/test:backend/test:full/preflight:prod`.
4. Confirm runbook references updated if behavior changed.
5. Prepare release snapshot (`npm run release:snapshot`).

## Release Window
1. Notify on-duty and stakeholder channel.
2. Deploy backend + frontend artifacts.
3. Validate `/api/health/ready`.
4. Run smoke path (login, dashboard, customers, quotes).
5. Record requestId samples for traceability.

## After Release
1. Observe SLO indicators for 30 minutes.
2. Check audit export metrics and error logs.
3. Mark release completed or trigger rollback.
