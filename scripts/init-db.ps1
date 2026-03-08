param(
  [string]$DbUser = "root",
  [string]$DbPassword = "root",
  [string]$DbName = "crm_local"
)

$ErrorActionPreference = "Stop"

$sql = "CREATE DATABASE IF NOT EXISTS $DbName CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
Write-Host "[db:init] Creating database '$DbName' with user '$DbUser'"

mysql -u$DbUser -p$DbPassword -e $sql

if ($LASTEXITCODE -ne 0) {
  throw "[db:init] mysql command failed"
}

Write-Host "[db:init] Done"
