# Environment Matrix

## Baseline
| Env | Purpose | Data Source | Seed Policy | Auth Cookie | SSO Mode |
|---|---|---|---|---|---|
| dev | feature development | local MySQL | enabled | secure=false | mock/oidc |
| staging | release validation | managed MySQL clone | disabled | secure=true | oidc |
| prod | live traffic | managed MySQL | disabled | secure=true | oidc |

## Secrets & Config Rules
- Never commit secrets in repo.
- Required per env: `AUTH_TOKEN_SECRET`, `DB_URL`, `DB_USER`, `DB_PASSWORD`.
- Prod forbidden defaults: `crm-secret-change-me`, `000000`, `mock`, `admin123`.
- `SECURITY_CORS_ALLOWED_ORIGINS` must be environment-specific allowlist.

## Promotion Rule
1. dev -> staging requires gate green + change checklist complete.
2. staging -> prod requires rollback drill evidence and release snapshot.
