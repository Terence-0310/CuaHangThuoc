-- =================================================================
-- 27_hrm_employee_overhaul.sql
-- HRM: Đại tu Employee + Tách Phép Tuần / Phép Năm + Config Table
-- =================================================================
USE QuanLyCuaHangThuoc;
GO

-- 1. Thêm cột mới cho HR_Employees
IF COL_LENGTH('HR_Employees', 'Phone') IS NULL
    ALTER TABLE HR_Employees ADD Phone VARCHAR(20) NULL;
GO

IF COL_LENGTH('HR_Employees', 'HireDate') IS NULL
    ALTER TABLE HR_Employees ADD HireDate DATE NULL;
GO

IF COL_LENGTH('HR_Employees', 'ResignDate') IS NULL
    ALTER TABLE HR_Employees ADD ResignDate DATE NULL;
GO

-- Backfill HireDate = CreatedAt cho dữ liệu cũ
UPDATE HR_Employees SET HireDate = CAST(CreatedAt AS DATE) WHERE HireDate IS NULL;
GO

-- 2. Xóa cột quota cũ (chuyển sang config toàn cục)
IF COL_LENGTH('HR_Employees', 'WeeklyLeaveQuota') IS NOT NULL
    ALTER TABLE HR_Employees DROP COLUMN WeeklyLeaveQuota;
GO

IF COL_LENGTH('HR_Employees', 'AnnualLeaveQuota') IS NOT NULL
    ALTER TABLE HR_Employees DROP COLUMN AnnualLeaveQuota;
GO

-- 3. Tạo bảng Config toàn cục
IF OBJECT_ID('HR_Config', 'U') IS NULL
BEGIN
    CREATE TABLE HR_Config (
        ConfigID INT IDENTITY(1,1) PRIMARY KEY,
        DefaultWeeklyLeave INT NOT NULL DEFAULT 1,   -- Số ngày nghỉ/tuần
        DefaultAnnualLeave INT NOT NULL DEFAULT 12    -- Số ngày phép năm cơ bản
    );
    INSERT INTO HR_Config (DefaultWeeklyLeave, DefaultAnnualLeave) VALUES (1, 12);
    PRINT N'Tạo bảng HR_Config + seed mặc định (1 ngày/tuần, 12 ngày/năm)';
END
GO

-- 4. Tách ca nghỉ: Đổi "Nghỉ Phép" → "Nghỉ Phép Tuần" + Thêm "Nghỉ Phép Năm"
UPDATE HR_Shifts SET ShiftName = N'Nghỉ Phép Tuần' WHERE ShiftName = N'Nghỉ Phép';
GO

IF NOT EXISTS (SELECT 1 FROM HR_Shifts WHERE ShiftName = N'Nghỉ Phép Năm')
BEGIN
    INSERT INTO HR_Shifts (ShiftName, DefaultStartTime, DefaultEndTime)
    VALUES (N'Nghỉ Phép Năm', '00:00', '00:00');
    PRINT N'Thêm ca Nghỉ Phép Năm';
END
GO

-- 5. Verify
SELECT * FROM HR_Employees;
SELECT * FROM HR_Shifts;
SELECT * FROM HR_Config;
GO

PRINT N'=== Migration 27 (Employee Overhaul) hoàn tất! ===';
GO
