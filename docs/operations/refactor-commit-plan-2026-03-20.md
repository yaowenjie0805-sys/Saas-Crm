# Refactor Commit Plan (apps + packages)

## Scope
- Structural migration to `apps/api` and `apps/web`.
- Workspace command switch at root `package.json`.
- Script/path contract updates (`apps/api/*`, `apps/web/*`).
- E2E + mobile responsiveness + tenants layout assertions.
- Noise governance (`verify-*.png` cleanup, ignore rules, IDE explorer cleanup).

## Suggested Commit Batches

1. `chore(repo): migrate to apps-packages layout`
- `apps/api/**` (rename/move + path-safe edits)
- `apps/web/**` (rename/move + path-safe edits)
- `packages/.gitkeep`
- `package.json`
- `package-lock.json`
- `README.md`
- `docs/PROJECT_STRUCTURE.md`

2. `chore(ci): switch scripts and release paths to new structure`
- `scripts/**/*.mjs`
- `scripts/*.ps1`
- `scripts/*.sh`
- `infra/staging/docker-compose.yml`
- `.github/workflows/staging-release.yml`

3. `test(e2e): add mobile and tenants layout interaction guards`
- `apps/web/tests/e2e/mobile-responsive.spec.js`
- `apps/web/tests/e2e/mobile-viewport-matrix.spec.js`
- `apps/web/tests/e2e/tenants-layout.spec.js`
- `apps/web/tests/e2e/helpers/auth.js`
- `apps/web/scripts/run-playwright-mobile-e2e.mjs`

4. `docs(ops): align release and gate language with new commands`
- `docs/operations/change-control-checklist.md`
- `docs/operations/change-request-template.md`
- `docs/operations/release-strategy.md`
- other touched `docs/operations/*` files updated for path/contract consistency

5. `chore(cleanup): ignore transient artifacts and reduce explorer noise`
- `.gitignore`
- `.vscode/settings.json`
- root `verify-*.png` deletions

## Validation Snapshot
- `npm run lint` pass
- `npm run build` pass
- `npm run test:e2e` pass
- `npm run test:e2e:mobile` pass
- `npm run ci:frontend` pass
- `npm run ci:backend` pass
- `npm run ci:separated` pass

## Notes
- Current working tree includes pre-existing unrelated deletions under `.boss/` and `.arts/`; keep out of refactor commits unless intentionally part of this release.
- Large move set is expected; prefer `git add -A apps` for rename detection, then stage docs/scripts in separate commits for clearer review.
