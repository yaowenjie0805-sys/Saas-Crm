# CI Actions Catalog

This document tracks reusable local GitHub Actions under `.github/actions` and where they are used.

## Active Actions

| Action | Purpose | Key Inputs | Key Outputs | Used By |
| --- | --- | --- | --- | --- |
| `detect-changed-scope-with-summary` | Detect frontend/backend/security scope changes and write summary | `workflow_file`, `security_job_name` | `frontend`, `backend`, `security` | `pr-gate.yml`, `main-gate.yml` |
| `mark-job-start` | Store UTC start timestamp in `GITHUB_ENV` for later timing summary | `start_env_var` | none | `pr-gate.yml`, `main-gate.yml` |
| `job-timing-summary` | Append elapsed minutes for a job to `GITHUB_STEP_SUMMARY` | `job_label`, `start_env_var` | none | `pr-gate.yml`, `main-gate.yml` |
| `prepare-frontend-node` | Run standard frontend environment bootstrap (`checkout + setup-node + npm ci`) | `node_version` (optional) | none | `pr-gate.yml`, `main-gate.yml` |
| `prepare-backend-java` | Run standard backend bootstrap (`checkout + setup-java + maven cache`) | `java_version` (optional) | none | `pr-gate.yml`, `main-gate.yml` |
| `run-lint-quality-gates` | Run shared lint/static/runtime/pages checks | none | none | `pr-gate.yml`, `main-gate.yml` |
| `security-scan-summary` | Print condensed security scan metrics | `report_path` (optional) | none | `pr-gate.yml`, `main-gate.yml` |
| `run-release-prep-checks` | Run release snapshot + production preflight checks | none | none | `pr-gate.yml`, `main-gate.yml` |
| `upload-artifact` | Shared wrapper for artifact upload defaults | `artifact_name`, `artifact_path`, `if_no_files_found` | none | `pr-gate.yml`, `main-gate.yml` |

## Ownership Notes

- Keep action names behavior-oriented (`run-*`, `mark-*`, `*-summary`) to make workflow YAML readable.
- Prefer adding optional inputs over cloning similar actions.
- If an action is no longer referenced by any workflow, remove its directory and update this catalog in the same PR.

## Last Cleanup

- Removed deprecated empty action directories:
  - `.github/actions/detect-changed-scope`
  - `.github/actions/changed-scope-summary`
