-- =================================================================
-- 26_hrm_weekly_leave_quota.sql
-- HRM: Đổi MonthlyLeaveQuota → WeeklyLeaveQuota cho rõ nghĩa
-- =================================================================
USE QuanLyCuaHangThuoc;
GO

-- Rename cột
IF COL_LENGTH('HR_Employees', 'MonthlyLeaveQuota') IS NOT NULL
BEGIN
    EXEC sp_rename 'HR_Employees.MonthlyLeaveQuota', 'WeeklyLeaveQuota', 'COLUMN';
    PRINT N'Đã đổi MonthlyLeaveQuota → WeeklyLeaveQuota';
END
GO

-- Set giá trị = 1 (1 ngày phép/tuần)
UPDATE HR_Employees SET WeeklyLeaveQuota = 1;
GO

-- Verify
SELECT EmpID, FullName, WeeklyLeaveQuota, AnnualLeaveQuota FROM HR_Employees;
GO

PRINT N'=== Migration 26 (Weekly Leave Quota) hoàn tất! ===';
GO
