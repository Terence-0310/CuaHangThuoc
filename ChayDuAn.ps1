# Chay du an Swing nhanh va on dinh.
# Mac dinh: BO QUA migration de tranh fail khi may khong co sqlcmd/SQL local.
# Can cap nhat DB thi bat MEPHAR_RUN_DB_MIGRATION = "1".
# Yeu cau: SQL Server dang chay, tai khoan sa / mat khau trung cau hinh DB,
#          JDK 17+, Maven.
#
# Bat migration:      $env:MEPHAR_RUN_DB_MIGRATION = "1"
# Instance khac:     $env:MEPHAR_SQL_SERVER = "localhost\SQLEXPRESS"
#                    + dat MEPHAR_DB_URL trong session neu can (xem application.properties)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot

function Test-Command($Name) {
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

function Use-IfBlank([string]$value, [string]$fallback) {
    if ([string]::IsNullOrWhiteSpace($value)) { return $fallback }
    return $value
}

if (-not (Test-Command "mvn")) {
    Write-Host "LOI: Khong tim thay Maven (mvn). Them Maven vao PATH." -ForegroundColor Red
    exit 1
}
if (-not (Test-Command "java")) {
    Write-Host "LOI: Khong tim thay Java. Cai JDK 17+ va them vao PATH." -ForegroundColor Red
    exit 1
}

# Khoi tao bien moi truong DB de tranh login user rong khi properties de trong db.user/db.password.
# Uu tien cao nhat: DB_URL / MEPHAR_DB_URL. Neu khong co, tu tao URL tu Server Name.
$dbName = Use-IfBlank $env:DB_NAME (Use-IfBlank $env:MEPHAR_DB_NAME "QuanLyCuaHangThuoc")
$dbServer = Use-IfBlank $env:DB_SERVER (Use-IfBlank $env:MEPHAR_SQL_SERVER "localhost")
$dbPort = Use-IfBlank $env:DB_PORT (Use-IfBlank $env:MEPHAR_DB_PORT "1433")

if ($dbServer -like "*\*") {
    # Dang named instance, vi du: localhost\SQLEXPRESS
    $instanceName = $dbServer.Split("\", 2)[1]
    $hostName = $dbServer.Split("\", 2)[0]
    $autoDbUrl = "jdbc:sqlserver://$hostName;instanceName=$instanceName;databaseName=$dbName;encrypt=false;trustServerCertificate=true;characterEncoding=UTF-8;sendStringParametersAsUnicode=true;useUnicode=true;"
} else {
    # Dang host:port, vi du localhost:1433
    $autoDbUrl = "jdbc:sqlserver://$dbServer`:$dbPort;databaseName=$dbName;encrypt=false;trustServerCertificate=true;characterEncoding=UTF-8;sendStringParametersAsUnicode=true;useUnicode=true;"
}

$env:DB_URL = Use-IfBlank $env:DB_URL (Use-IfBlank $env:MEPHAR_DB_URL $autoDbUrl)
$env:DB_USER = Use-IfBlank $env:DB_USER (Use-IfBlank $env:MEPHAR_DB_USER "sa")
$env:DB_PASSWORD = Use-IfBlank $env:DB_PASSWORD (Use-IfBlank $env:MEPHAR_DB_PASSWORD "123456")
$env:DB_DRIVER = Use-IfBlank $env:DB_DRIVER (Use-IfBlank $env:MEPHAR_DB_DRIVER "com.microsoft.sqlserver.jdbc.SQLServerDriver")
Write-Host ">>> DB config active: USER=$($env:DB_USER), SERVER=$dbServer, DB=$dbName" -ForegroundColor DarkGray
Write-Host ">>> DB_URL full: $($env:DB_URL)" -ForegroundColor DarkGray
Write-Host ">>> DB_DRIVER: $($env:DB_DRIVER)" -ForegroundColor DarkGray

if ($env:MEPHAR_RUN_DB_MIGRATION -eq "1") {
    if (-not (Test-Command "sqlcmd")) {
        Write-Host "LOI: Khong tim thay sqlcmd. Cai SQL Server hoac Command Line Tools." -ForegroundColor Red
        exit 1
    }
    Write-Host ">>> Dang chay migration database..." -ForegroundColor Cyan
    try {
        & "$root\database\run_all_migrations.ps1"
    } catch {
        Write-Host $_.Exception.Message -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host ">>> Chay nhanh: bo qua migration (dat MEPHAR_RUN_DB_MIGRATION=1 neu can)" -ForegroundColor Yellow
}

Set-Location $root
Write-Host ">>> Dang bien dich va mo ung dung..." -ForegroundColor Cyan
Write-Host "    Dang nhap mau: admin / admin123  |  nhan vien: nhanvien01 / nv123" -ForegroundColor DarkGray
mvn compile exec:java "-Dexec.mainClass=App"
if ($LASTEXITCODE -ne 0) {
    Write-Host "LOI: Maven exec that bai." -ForegroundColor Red
    exit $LASTEXITCODE
}
