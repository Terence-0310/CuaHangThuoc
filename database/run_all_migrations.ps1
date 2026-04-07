# Chay tat ca migration SQL Server — duong dan theo thu muc chua script (khong hard-code may khac).
# Bien moi truong (tuy chon): MEPHAR_SQL_SERVER, MEPHAR_SQL_USER, MEPHAR_SQL_PASSWORD
# Vi du instance: $env:MEPHAR_SQL_SERVER = "localhost\SQLEXPRESS"

$ErrorActionPreference = "Stop"
$server = if ($env:MEPHAR_SQL_SERVER) { $env:MEPHAR_SQL_SERVER } else { "localhost" }
$user   = if ($env:MEPHAR_SQL_USER)   { $env:MEPHAR_SQL_USER }   else { "sa" }
$pass   = if ($env:MEPHAR_SQL_PASSWORD) { $env:MEPHAR_SQL_PASSWORD } else { "123456" }
$targetDb = "QuanLyCuaHangThuoc"
$dir = $PSScriptRoot

if (-not (Get-Command sqlcmd -ErrorAction SilentlyContinue)) {
    Write-Host "LOI: Khong tim thay sqlcmd. Cai SQL Server Command Line Tools hoac SQL Server." -ForegroundColor Red
    exit 1
}

# Ensure UTF-8 output encoding for PS itself just in case
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

function Invoke-SqlFile {
    param(
        [string]$Database,
        [string]$FilePath
    )
    $out = & sqlcmd -S $server -d $Database -U $user -P $pass -f 65001 -i $FilePath -I 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host $out
        throw "sqlcmd that bai (exit $LASTEXITCODE): $FilePath"
    }
}

# Phase 1: Base schema + core data
$phase1 = @(
    "01_create_database.sql",
    "02_seed_data.sql",
    "03_stored_procedures.sql",
    "05_alter_khachhang.sql",
    "05_migration_financial_fix.sql",
    "06_edge_case_fixes.sql",
    "07_alter_hoadon_trangthai.sql",
    "07_unique_lot_constraint.sql",
    "08_phieunhap_giasi_fix.sql",
    "09_nha_cung_cap.sql",
    "09_seed_25_products.sql",
    "10_add_online_status.sql",
    "10_index_sort_columns.sql",
    "11_tra_hang_huy_hang.sql",
    "12_return_order.sql",
    "12_system_logs.sql"
)

# Phase 2: HRM — skip old files 13-19, start from 20 (clean schema)
$phaseHRM_cleanup = @"
USE QuanLyCuaHangThuoc;
IF OBJECT_ID('HR_Payroll','U') IS NOT NULL DROP TABLE HR_Payroll;
IF OBJECT_ID('HR_AttendanceHistory','U') IS NOT NULL DROP TABLE HR_AttendanceHistory;
IF OBJECT_ID('HR_Attendances','U') IS NOT NULL DROP TABLE HR_Attendances;
IF OBJECT_ID('HR_Schedules','U') IS NOT NULL DROP TABLE HR_Schedules;
IF OBJECT_ID('HR_Employees','U') IS NOT NULL DROP TABLE HR_Employees;
IF OBJECT_ID('HR_Shifts','U') IS NOT NULL DROP TABLE HR_Shifts;
IF OBJECT_ID('HR_Config','U') IS NOT NULL DROP TABLE HR_Config;
PRINT N'HR tables dropped for clean rebuild';
"@

$phase2 = @(
    "20_hrm_phase1_employees_shifts.sql",
    "21_hrm_phase2_schedules.sql",
    "22_hrm_phase3_attendances.sql",
    "23_hrm_leave_quota.sql",
    "24_hrm_snapshot_rate.sql",
    "25_hrm_link_user_account.sql",
    "26_hrm_weekly_leave_quota.sql",
    "27_hrm_employee_overhaul.sql",
    "28_seed_hrm_3years.sql",
    "29_create_hr_payroll.sql",
    "30_seed_150_batches.sql",
    "31_allow_null_clockin.sql",
    "32_overtime_columns.sql",
    "33_fix_overtime_baseline.sql",
    "33_seed_leave_shifts.sql",
    "34_nullable_schedule_times.sql",
    "34_schedule_snapshot_shift_config.sql",
    "35_fix_hrm_mand_links.sql",
    "36_seed_test_employees_and_products.sql",
    "37_seed_existing_employees_for_testing.sql",
    "38_hrm_24x7_three_shifts.sql",
    "39_seed_next_7_days_three_shifts.sql",
    "40_seed_next_30_days_three_shifts.sql",
    "41_seed_payroll_demo_from_attendance.sql"
)

$total = $phase1.Count + $phase2.Count + 1
$i = 0

try {

# Run Phase 1
foreach ($f in $phase1) {
    $i++
    Write-Host "[$i/$total] $f" -ForegroundColor Cyan
    $full = Join-Path $dir $f
    if ($f -eq "01_create_database.sql") {
        $out = & sqlcmd -S $server -d master -U $user -P $pass -f 65001 -i $full -I 2>&1
        if ($LASTEXITCODE -ne 0) { Write-Host $out; throw "sqlcmd that bai: $f" }
    } else {
        Invoke-SqlFile -Database $targetDb -FilePath $full
    }
}

# HRM cleanup
$i++
Write-Host "[$i/$total] DROP old HR tables" -ForegroundColor Yellow
$out = $phaseHRM_cleanup | & sqlcmd -S $server -d $targetDb -U $user -P $pass -f 65001 -I 2>&1
if ($LASTEXITCODE -ne 0) { Write-Host $out; throw "sqlcmd that bai: HRM cleanup" }

# Run Phase 2
foreach ($f in $phase2) {
    $i++
    Write-Host "[$i/$total] $f" -ForegroundColor Cyan
    Invoke-SqlFile -Database $targetDb -FilePath (Join-Path $dir $f)
}

Write-Host "`n=== ALL $total MIGRATIONS COMPLETE ===" -ForegroundColor Green

} catch {
    Write-Host "`nLOI MIGRATION: $($_.Exception.Message)" -ForegroundColor Red
    throw
}
