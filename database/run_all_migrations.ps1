$ErrorActionPreference = "Continue"
$server = "localhost"
$user = "sa"
$pass = "123456"
$targetDb = "QuanLyCuaHangThuoc"
$dir = "d:\eproject\eProject-StoreBanThuoc\database"

# Ensure UTF-8 output encoding for PS itself just in case
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

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
    "34_schedule_snapshot_shift_config.sql"
)

$total = $phase1.Count + $phase2.Count + 1
$i = 0

# Run Phase 1
foreach ($f in $phase1) {
    $i++
    Write-Host "[$i/$total] $f" -ForegroundColor Cyan
    if ($f -eq "01_create_database.sql") {
        # file 01 drops and creates DB, so it must connect to master
        sqlcmd -S $server -d master -U $user -P $pass -f 65001 -i "$dir\$f" -I 2>&1 | Out-Null
    } else {
        # other files must connect directly to QuanLyCuaHangThuoc because they might lack the USE statement
        sqlcmd -S $server -d $targetDb -U $user -P $pass -f 65001 -i "$dir\$f" -I 2>&1 | Out-Null
    }
}

# HRM cleanup
$i++
Write-Host "[$i/$total] DROP old HR tables" -ForegroundColor Yellow
$phaseHRM_cleanup | sqlcmd -S $server -d $targetDb -U $user -P $pass -f 65001 -I 2>&1 | Out-Null

# Run Phase 2
foreach ($f in $phase2) {
    $i++
    Write-Host "[$i/$total] $f" -ForegroundColor Cyan
    sqlcmd -S $server -d $targetDb -U $user -P $pass -f 65001 -i "$dir\$f" -I 2>&1 | Out-Null
}

Write-Host "`n=== ALL $total MIGRATIONS COMPLETE ===" -ForegroundColor Green
