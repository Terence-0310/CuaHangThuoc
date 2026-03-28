-- =================================================================
-- 23_hrm_leave_quota.sql
-- HRM: Thêm cột Quỹ Phép Cá Nhân (Monthly + Annual Leave Quota)
-- =================================================================
USE QuanLyCuaHangThuoc;
GO

-- 1. Đổi tên cột WeeklyLeaveQuota -> MonthlyLeaveQuota
IF COL_LENGTH('HR_Employees', 'WeeklyLeaveQuota') IS NOT NULL
   AND COL_LENGTH('HR_Employees', 'MonthlyLeaveQuota') IS NULL
BEGIN
    EXEC sp_rename 'HR_Employees.WeeklyLeaveQuota', 'MonthlyLeaveQuota', 'COLUMN';
    PRINT N'Đã đổi WeeklyLeaveQuota -> MonthlyLeaveQuota';
END
GO

-- 2. Đổi tên cột YearlyLeaveQuota -> AnnualLeaveQuota
IF COL_LENGTH('HR_Employees', 'YearlyLeaveQuota') IS NOT NULL
   AND COL_LENGTH('HR_Employees', 'AnnualLeaveQuota') IS NULL
BEGIN
    EXEC sp_rename 'HR_Employees.YearlyLeaveQuota', 'AnnualLeaveQuota', 'COLUMN';
    PRINT N'Đã đổi YearlyLeaveQuota -> AnnualLeaveQuota';
END
GO

-- 3. Thêm Ca "Nghỉ Phép" nếu chưa có
IF NOT EXISTS (SELECT 1 FROM HR_Shifts WHERE ShiftName = N'Nghỉ Phép')
BEGIN
    INSERT INTO HR_Shifts (ShiftName, DefaultStartTime, DefaultEndTime)
    VALUES (N'Nghỉ Phép', '00:00', '00:00');
    PRINT N'Đã thêm ca Nghỉ Phép';
END
GO

-- 4. Thêm Ca "Nghỉ Không Lương" nếu chưa có
IF NOT EXISTS (SELECT 1 FROM HR_Shifts WHERE ShiftName = N'Nghỉ Không Lương')
BEGIN
    INSERT INTO HR_Shifts (ShiftName, DefaultStartTime, DefaultEndTime)
    VALUES (N'Nghỉ Không Lương', '00:00', '00:00');
    PRINT N'Đã thêm ca Nghỉ Không Lương';
END
GO

PRINT N'=== Migration 23 (Leave Quota) hoàn tất! ===';
GO
