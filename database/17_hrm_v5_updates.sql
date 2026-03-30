-- 17_hrm_v5_updates.sql
USE QuanLyCuaHangThuoc;
GO

-- 1. Tao bang Ca Lam (Shifts)
IF OBJECT_ID('HR_Shifts', 'U') IS NULL
BEGIN
    CREATE TABLE HR_Shifts (
        ShiftID INT IDENTITY(1,1) PRIMARY KEY,
        ShiftName NVARCHAR(50) NOT NULL,
        StartTime TIME NOT NULL,
        EndTime TIME NOT NULL
    );
    -- Seed data
    INSERT INTO HR_Shifts (ShiftName, StartTime, EndTime) VALUES 
    (N'Ca Sáng', '06:00:00', '14:00:00'),
    (N'Ca Chiều', '14:00:00', '22:00:00');
END
GO

-- 2. Cap nhat AttendanceHistory them LateReason, IsExcused
IF COL_LENGTH('HR_AttendanceHistory', 'LateReason') IS NULL
BEGIN
    ALTER TABLE HR_AttendanceHistory ADD LateReason NVARCHAR(500) NULL;
END
GO
IF COL_LENGTH('HR_AttendanceHistory', 'IsExcused') IS NULL
BEGIN
    ALTER TABLE HR_AttendanceHistory ADD IsExcused BIT NOT NULL DEFAULT 0;
END
GO

-- 3. Tao bang LeaveRules
IF OBJECT_ID('HR_LeaveRules', 'U') IS NULL
BEGIN
    CREATE TABLE HR_LeaveRules (
        RuleID INT IDENTITY(1,1) PRIMARY KEY,
        MaxPerMonth INT NOT NULL DEFAULT 1,
        MaxPerYear INT NOT NULL DEFAULT 12
    );
    INSERT INTO HR_LeaveRules (MaxPerMonth, MaxPerYear) VALUES (1, 12);
END
GO

-- 4. Tao bang Leaves
IF OBJECT_ID('HR_Leaves', 'U') IS NULL
BEGIN
    CREATE TABLE HR_Leaves (
        LeaveID INT IDENTITY(1,1) PRIMARY KEY,
        EmpID INT NOT NULL,
        LeaveDate DATE NOT NULL,
        Reason NVARCHAR(500) NULL,
        IsExcused BIT NOT NULL DEFAULT 0,
        CONSTRAINT FK_Leaves_Emp FOREIGN KEY (EmpID) REFERENCES HR_Employees(EmpID)
    );
END
GO

PRINT N'Hoàn tất cập nhật cấu trúc HRM V5: Ca làm động, Xin phép, Nghỉ phép!';
GO
