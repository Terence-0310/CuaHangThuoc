# Chay migration datamart + chuoi nha thuoc (42 datamart -> 42 chain -> 43 -> 44).
# Can file 42_demand_planning_datamart.sql truoc 42_chain (DimTime, Fact*, v.v.).
# Len DB QuanLyCuaHangThuoc da ton tai.
# Bien moi truong: MEPHAR_SQL_SERVER, MEPHAR_SQL_USER, MEPHAR_SQL_PASSWORD (giong run_all_migrations.ps1)

$ErrorActionPreference = "Stop"
$server = if ($env:MEPHAR_SQL_SERVER) { $env:MEPHAR_SQL_SERVER } else { "localhost" }
$user   = if ($env:MEPHAR_SQL_USER)   { $env:MEPHAR_SQL_USER }   else { "sa" }
$pass   = if ($env:MEPHAR_SQL_PASSWORD) { $env:MEPHAR_SQL_PASSWORD } else { "123456" }
$targetDb = "QuanLyCuaHangThuoc"
$dir = $PSScriptRoot

if (-not (Get-Command sqlcmd -ErrorAction SilentlyContinue)) {
    Write-Host "LOI: Khong tim thay sqlcmd." -ForegroundColor Red
    exit 1
}

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

function Invoke-SqlFile {
    param([string]$Database, [string]$FilePath)
    $out = & sqlcmd -S $server -d $Database -U $user -P $pass -f 65001 -i $FilePath -I -b 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host $out
        throw "sqlcmd that bai (exit $LASTEXITCODE): $FilePath"
    }
}

$files = @(
    "42_demand_planning_datamart.sql",
    "42_chain_pharmacy_schema.sql",
    "43_chain_pharmacy_etl_forecast.sql",
    "44_seed_large_operational_data.sql"
)

Write-Host "Chay chain pharmacy migrations len $targetDb @ $server ..." -ForegroundColor Cyan
foreach ($f in $files) {
    $full = Join-Path $dir $f
    if (-not (Test-Path $full)) {
        throw "Khong tim thay file: $full"
    }
    Write-Host "  -> $f" -ForegroundColor Yellow
    Invoke-SqlFile -Database $targetDb -FilePath $full
}
Write-Host "Hoan tat datamart + chain (42 datamart, 42 chain, 43, 44)." -ForegroundColor Green
