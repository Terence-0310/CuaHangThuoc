-- 16_hrm_v4_updates.sql
USE QuanLyCuaHangThuoc;
GO

-- 1. Cap nhat bang HR_Employees
IF COL_LENGTH('HR_Employees', 'OvertimeRate') IS NULL
BEGIN
    ALTER TABLE HR_Employees ADD OvertimeRate DECIMAL(18,0) NOT NULL DEFAULT 30000;
END
GO

-- 2. Cap nhat bang HR_AttendanceHistory
IF COL_LENGTH('HR_AttendanceHistory', 'IsLate') IS NULL
BEGIN
    ALTER TABLE HR_AttendanceHistory ADD IsLate BIT NOT NULL DEFAULT 0;
END
GO

IF COL_LENGTH('HR_AttendanceHistory', 'RegularHours') IS NULL
BEGIN
    ALTER TABLE HR_AttendanceHistory ADD RegularHours DECIMAL(8,2) NULL;
END
GO

IF COL_LENGTH('HR_AttendanceHistory', 'OvertimeHours') IS NULL
BEGIN
    ALTER TABLE HR_AttendanceHistory ADD OvertimeHours DECIMAL(8,2) NULL;
END
GO

PRINT N'Hoàn tất cập nhật cấu trúc HRM lên phiên bản có Lương Tăng Ca và Phạt Đi Trễ!';
GO
