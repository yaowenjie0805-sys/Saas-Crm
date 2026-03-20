$ErrorActionPreference = "Stop"

Set-Location (Join-Path $PSScriptRoot "..")

$backendDir = Join-Path (Get-Location) "backend"
$weirdBuildDir = Join-Path $backendDir '${project.build.directory}'
if (Test-Path $weirdBuildDir) {
  Write-Host "[warn] found invalid build output folder: $weirdBuildDir"
  Remove-Item -Recurse -Force $weirdBuildDir
  Write-Host "[ok] removed invalid build output folder"
}

$portOwner = netstat -ano | Select-String ":8080" | Select-String "LISTENING" | ForEach-Object {
  ($_ -split "\s+")[-1]
} | Select-Object -First 1

if ($portOwner) {
  Write-Host "[warn] port 8080 is occupied by PID=$portOwner, trying to stop..."
  taskkill /PID $portOwner /F | Out-Null
  Start-Sleep -Milliseconds 500
  Write-Host "[ok] port 8080 released"
}

Write-Host "[step] mvn clean compile"
node scripts/run-maven.mjs -f apps/api/pom.xml clean compile -DskipTests

Write-Host "[step] run backend with dev profile"
node scripts/run-maven.mjs -f apps/api/pom.xml spring-boot:run "-Dspring-boot.run.profiles=dev"

