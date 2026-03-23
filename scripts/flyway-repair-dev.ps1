param(
  [string]$DbUser = "root",
  [string]$DbPassword = "root",
  [string]$DbName = "crm_local",
  [string]$DbHost = "127.0.0.1",
  [int]$DbPort = 3306
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path $PSScriptRoot -Parent
Set-Location $repoRoot

$backupDir = Join-Path $repoRoot "backups"
if (!(Test-Path $backupDir)) {
  New-Item -ItemType Directory -Path $backupDir | Out-Null
}
$timestamp = Get-Date -Format "yyyy-MM-dd_HHmmss"
$backupFile = Join-Path $backupDir ("{0}_backup_{1}.sql" -f $DbName, $timestamp)

Write-Host "[flyway:repair] Checking MySQL connectivity $DbHost`:$DbPort ..."
mysql --protocol=TCP --host=$DbHost --port=$DbPort --user=$DbUser --password=$DbPassword -e "SELECT 1;" | Out-Null
if ($LASTEXITCODE -ne 0) {
  throw "[flyway:repair] Cannot connect to MySQL."
}

Write-Host "[flyway:repair] Creating backup: $backupFile"
mysqldump --protocol=TCP --host=$DbHost --port=$DbPort --user=$DbUser --password=$DbPassword --databases $DbName --single-transaction --routines --triggers --events > $backupFile
if ($LASTEXITCODE -ne 0) {
  throw "[flyway:repair] mysqldump failed."
}

$mvnCommonArgs = @(
  "-Dmaven.repo.local=C:/Users/Yao/Desktop/crm/.m2repo",
  "-gs", "C:/Users/Yao/Desktop/crm/.mvn/global-settings.xml",
  "-f", "C:/Users/Yao/Desktop/crm/apps/api/pom.xml"
)

Write-Host "[flyway:repair] Syncing resources to target/classes ..."
& mvn @mvnCommonArgs "resources:resources"
if ($LASTEXITCODE -ne 0) {
  throw "[flyway:repair] resources sync failed."
}

$flywayArgs = @(
  "-Dflyway.url=jdbc:mysql://$DbHost`:$DbPort/$DbName",
  "-Dflyway.user=$DbUser",
  "-Dflyway.password=$DbPassword",
  "-Dflyway.locations=classpath:db/migration"
)

Write-Host "[flyway:repair] Running Flyway repair ..."
& mvn @mvnCommonArgs "org.flywaydb:flyway-maven-plugin:9.22.3:repair" @flywayArgs
if ($LASTEXITCODE -ne 0) {
  throw "[flyway:repair] Flyway repair failed."
}

Write-Host "[flyway:repair] Running Flyway migrate ..."
& mvn @mvnCommonArgs "org.flywaydb:flyway-maven-plugin:9.22.3:migrate" @flywayArgs
if ($LASTEXITCODE -ne 0) {
  throw "[flyway:repair] Flyway migrate failed."
}

$failedCount = mysql --protocol=TCP --host=$DbHost --port=$DbPort --user=$DbUser --password=$DbPassword -D $DbName -N -B -e "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0;"
if ($LASTEXITCODE -ne 0) {
  throw "[flyway:repair] Query flyway_schema_history failed."
}

$latestVersion = mysql --protocol=TCP --host=$DbHost --port=$DbPort --user=$DbUser --password=$DbPassword -D $DbName -N -B -e "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1;"
if ($LASTEXITCODE -ne 0) {
  throw "[flyway:repair] Query latest flyway version failed."
}

Write-Host "[flyway:repair] Done."
Write-Host "[flyway:repair] Backup: $backupFile"
Write-Host "[flyway:repair] Failed rows in flyway_schema_history: $failedCount"
Write-Host "[flyway:repair] Latest version: $latestVersion"
