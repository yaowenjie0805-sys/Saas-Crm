# Backup / Restore Runbook

## Purpose
Provide a repeatable minimum process for MySQL backup and restore drills.

## Backup
```powershell
powershell -ExecutionPolicy Bypass -File scripts/db-backup.ps1 -OutDir backups
```

## Restore
```powershell
powershell -ExecutionPolicy Bypass -File scripts/db-restore.ps1 -BackupFile backups/<your-file>.sql
```

## Drill Checklist
1. Create backup in drill environment.
2. Insert or update a known marker record.
3. Restore from backup.
4. Verify marker rollback and run `npm run test:e2e` smoke.
5. Record drill time, operator, and result.

## Notes
- Credentials come from `DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD` env vars.
- Defaults are local dev values if env vars are absent.
