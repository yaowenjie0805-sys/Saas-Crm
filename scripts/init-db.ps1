param(
  [string]$DbUser = "root",
  [string]$DbPassword = "root",
  [string]$DbName = "crm_local",
  [string]$DbHost = "127.0.0.1",
  [int]$DbPort = 3306
)

$ErrorActionPreference = "Stop"

Write-Host "[db:init] Checking MySQL connectivity $DbHost`:$DbPort ..."
mysql --protocol=TCP -h$DbHost -P$DbPort -u$DbUser -p$DbPassword -e "SELECT 1;" | Out-Null
if ($LASTEXITCODE -ne 0) {
  throw "[db:init] Cannot connect to MySQL. Please start MySQL and verify credentials."
}

$sql = "CREATE DATABASE IF NOT EXISTS $DbName CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
Write-Host "[db:init] Creating database '$DbName' with user '$DbUser'"
mysql --protocol=TCP -h$DbHost -P$DbPort -u$DbUser -p$DbPassword -e $sql
if ($LASTEXITCODE -ne 0) {
  throw "[db:init] create database failed"
}

$env:DB_URL = "jdbc:mysql://$DbHost`:$DbPort/$DbName?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$env:DB_USER = $DbUser
$env:DB_PASSWORD = $DbPassword
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:LEAD_IMPORT_LISTENER_ENABLED = "false"
$env:LEAD_IMPORT_MQ_DECLARE_ENABLED = "false"
$env:LEAD_IMPORT_MQ_PUBLISH_ENABLED = "false"

Write-Host "[db:init] Running Flyway migrations + seed data ..."
mvn -f backend/pom.xml -DskipTests spring-boot:run "-Dspring-boot.run.arguments=--spring.main.web-application-type=none --app.init-only=true"
if ($LASTEXITCODE -ne 0) {
  throw "[db:init] backend init failed"
}

Write-Host "[db:init] Done (database + migration + seed)."
