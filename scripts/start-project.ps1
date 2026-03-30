$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$dbScript = Join-Path $projectRoot "database\run_all_migrations.ps1"

# SQL Server container settings
$containerName = "eproject-sql"
$saPassword = "Admin@123456"
$dbServer = "localhost"
$dbUser = "sa"
$dbName = "QuanLyCuaHangThuoc"

Write-Host "==> Step 1/4: Ensure Docker is running..."
docker info 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Docker daemon is not running. Open Docker Desktop first, then run this script again."
}

Write-Host "==> Step 2/4: Ensure SQL Server container exists/runs..."
$containerExists = docker ps -a --format "{{.Names}}" | Where-Object { $_ -eq $containerName }
if (-not $containerExists) {
    docker run -d --name $containerName `
        -e "ACCEPT_EULA=Y" `
        -e "MSSQL_SA_PASSWORD=$saPassword" `
        -p 1433:1433 `
        mcr.microsoft.com/mssql/server:2022-latest | Out-Null
} else {
    docker start $containerName | Out-Null
}

Write-Host "==> Step 3/4: Wait for SQL Server ready..."
$ready = $false
for ($i = 0; $i -lt 30; $i++) {
    sqlcmd -S $dbServer -U $dbUser -P $saPassword -Q "SELECT 1" -b > $null 2>&1
    if ($LASTEXITCODE -eq 0) {
        $ready = $true
        break
    }
    Start-Sleep -Seconds 2
}
if (-not $ready) {
    throw "SQL Server is not ready on localhost:1433."
}

Write-Host "==> Step 4/4: Run migrations and launch app..."
powershell -ExecutionPolicy Bypass -File $dbScript -Server $dbServer -User $dbUser -Pass $saPassword -TargetDb $dbName
if ($LASTEXITCODE -ne 0) {
    throw "Database migration failed."
}

Set-Location $projectRoot
mvn exec:java "-Dexec.mainClass=App"
