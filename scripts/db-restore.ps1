param(
  [Parameter(Mandatory = $true)]
  [string]$BackupFile
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $BackupFile)) {
  throw "Backup file not found: $BackupFile"
}

$hostName = if ($env:DB_HOST) { $env:DB_HOST } else { "127.0.0.1" }
$port = if ($env:DB_PORT) { $env:DB_PORT } else { "3306" }
$dbName = if ($env:DB_NAME) { $env:DB_NAME } else { "crm_local" }
$dbUser = if ($env:DB_USER) { $env:DB_USER } else { "root" }
$dbPassword = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "root" }

Write-Host "[restore] loading $BackupFile into $dbName"
Get-Content $BackupFile | & mysql -h $hostName -P $port -u $dbUser "-p$dbPassword" $dbName
Write-Host "[restore] done"
