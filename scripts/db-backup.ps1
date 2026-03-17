param(
  [string]$OutDir = "backups"
)

$ErrorActionPreference = "Stop"

$hostName = if ($env:DB_HOST) { $env:DB_HOST } else { "127.0.0.1" }
$port = if ($env:DB_PORT) { $env:DB_PORT } else { "3306" }
$dbName = if ($env:DB_NAME) { $env:DB_NAME } else { "crm_local" }
$dbUser = if ($env:DB_USER) { $env:DB_USER } else { "root" }
$dbPassword = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "root" }

if (-not (Test-Path $OutDir)) {
  New-Item -Path $OutDir -ItemType Directory | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$file = Join-Path $OutDir "crm-backup-$dbName-$timestamp.sql"

Write-Host "[backup] dumping $dbName to $file"
& mysqldump --single-transaction --routines --events --triggers -h $hostName -P $port -u $dbUser "-p$dbPassword" $dbName | Out-File -Encoding utf8 $file

if (-not (Test-Path $file)) {
  throw "Backup failed: dump file not generated."
}

Write-Host "[backup] done: $file"
