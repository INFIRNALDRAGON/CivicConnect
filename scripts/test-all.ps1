$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent $PSScriptRoot
$Container = 'civicconnect-test-db'
$Port = if ($env:TEST_DB_PORT) { $env:TEST_DB_PORT } else { '55432' }
$AdminPassword = 'TestAdminPass1'

$env:DB_HOST = 'localhost'
$env:DB_PORT = $Port
$env:DB_NAME = 'civicconnect'
$env:DB_OWNER_PASSWORD = 'TestOwnerPass1'
$env:DB_APP_PASSWORD = 'TestAppPass1'
$env:DB_READONLY_PASSWORD = 'TestReadPass1'
$env:APP_AUTH_SECRET = 'local-test-secret-that-is-only-used-by-the-test-script-123456'
$env:SPRING_PROFILES_ACTIVE = 'dev'

function Invoke-Checked {
  param([scriptblock]$Command)
  & $Command
  if ($LASTEXITCODE -ne 0) { throw "Command failed with exit code $LASTEXITCODE" }
}

function Remove-TestDatabase {
  docker rm -f $Container *> $null
  $global:LASTEXITCODE = 0
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  throw 'Docker is required for the database tests. Install Docker Desktop and start it.'
}
docker info *> $null
if ($LASTEXITCODE -ne 0) { throw 'Docker is installed but not running. Start Docker Desktop and try again.' }

try {
  Write-Host "==> Starting a throwaway PostgreSQL on port $Port (your own database is not touched)"
  Remove-TestDatabase
  Invoke-Checked { docker run -d --name $Container -e POSTGRES_DB=civicconnect -e POSTGRES_USER=ccadmin -e POSTGRES_PASSWORD=$AdminPassword -p "${Port}:5432" postgres:15 | Out-Null }

  for ($i = 0; $i -lt 40; $i++) {
    docker exec $Container pg_isready -U ccadmin -d civicconnect *> $null
    if ($LASTEXITCODE -eq 0) { break }
    Start-Sleep -Seconds 1
  }
  Start-Sleep -Seconds 2

  $sql = (Get-Content (Join-Path $Root 'db/00_roles.sql') -Raw) `
    -replace 'CHANGE_ME_owner', $env:DB_OWNER_PASSWORD `
    -replace 'CHANGE_ME_app', $env:DB_APP_PASSWORD `
    -replace 'CHANGE_ME_readonly', $env:DB_READONLY_PASSWORD
  $sql | docker exec -i $Container psql -v ON_ERROR_STOP=1 -q -U ccadmin -d civicconnect
  if ($LASTEXITCODE -ne 0) { throw 'Creating the database roles failed' }

  Write-Host '==> Backend: unit tests, architecture rules and API tests'
  Push-Location (Join-Path $Root 'backend')
  Invoke-Checked { & .\mvnw.cmd -B verify }
  Pop-Location

  Write-Host '==> Frontend: build, unit tests and browser tests'
  Push-Location (Join-Path $Root 'frontend')
  Invoke-Checked { npm ci }
  Invoke-Checked { npm run build }
  Invoke-Checked { npm test }
  Invoke-Checked { npx playwright install chromium }
  Invoke-Checked { npm run test:e2e }
  Pop-Location

  Write-Host ''
  Write-Host 'All tests passed.'
  Write-Host 'Coverage report: backend\target\site\jacoco\index.html'
}
finally {
  Remove-TestDatabase
}
