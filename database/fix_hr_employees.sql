USE QuanLyCuaHangThuoc;
GO

-- Step 0: Drop ALL foreign key constraints on HR tables
DECLARE @sql NVARCHAR(MAX) = N'';
SELECT @sql = @sql + 'ALTER TABLE [' + OBJECT_NAME(parent_object_id) + '] DROP CONSTRAINT [' + name + '];' + CHAR(13)
FROM sys.foreign_keys 
WHERE OBJECT_NAME(parent_object_id) LIKE 'HR_%';
IF LEN(@sql) > 0 EXEC sp_executesql @sql;
GO

PRINT N'FK constraints dropped';
GO

-- Step 1: Drop all HR tables
DROP TABLE IF EXISTS HR_Payroll;
DROP TABLE IF EXISTS HR_AttendanceHistory;
DROP TABLE IF EXISTS HR_Attendances;
DROP TABLE IF EXISTS HR_Schedules;
DROP TABLE IF EXISTS HR_Employees;
DROP TABLE IF EXISTS HR_Shifts;
DROP TABLE IF EXISTS HR_Config;
GO

PRINT N'All HR tables dropped';
GO

-- Step 2: Recreate
CREATE TABLE HR_Shifts (
    ShiftID INT IDENTITY(1,1) PRIMARY KEY,
    ShiftName NVARCHAR(50) NOT NULL,
    DefaultStartTime TIME NULL,
    DefaultEndTime TIME NULL
);

CREATE TABLE HR_Employees (
    EmpID INT IDENTITY(1,1) PRIMARY KEY,
    FullName NVARCHAR(100) NOT NULL,
    PinCode VARCHAR(10) NOT NULL,
    Phone VARCHAR(20) NULL,
    HourlyRate DECIMAL(18,2) NOT NULL DEFAULT 25000,
    OvertimeRate DECIMAL(18,2) NULL DEFAULT 37500,
    HireDate DATE NULL,
    ResignDate DATE NULL,
    [Status] NVARCHAR(20) NOT NULL DEFAULT N'Dang lam',
    CreatedAt DATETIME NOT NULL DEFAULT GETDATE(),
    MaND INT NULL,
    CONSTRAINT UQ_HR_Employees_PinCode UNIQUE (PinCode)
);

CREATE TABLE HR_Schedules (
    ScheduleID INT IDENTITY(1,1) PRIMARY KEY,
    EmpID INT NOT NULL REFERENCES HR_Employees(EmpID),
    ShiftID INT NOT NULL REFERENCES HR_Shifts(ShiftID),
    WorkDate DATE NOT NULL,
    ActualStart TIME NULL,
    ActualEnd TIME NULL,
    ShiftDefaultStart TIME NULL,
    ShiftDefaultEnd TIME NULL
);

CREATE TABLE HR_Attendances (
    AttendanceID INT IDENTITY(1,1) PRIMARY KEY,
    EmpID INT NOT NULL REFERENCES HR_Employees(EmpID),
    ScheduleID INT NULL REFERENCES HR_Schedules(ScheduleID),
    ClockIn DATETIME NULL,
    ClockOut DATETIME NULL,
    TotalHours DECIMAL(5,2) NULL,
    DailyEarned DECIMAL(18,2) NULL,
    LateReason NVARCHAR(500) NULL,
    SnapshotRate DECIMAL(18,2) NULL,
    SnapshotOTRate DECIMAL(18,2) NULL,
    OvertimeHours DECIMAL(5,2) NULL DEFAULT 0,
    OvertimeReason NVARCHAR(500) NULL,
    ShiftDefaultStart TIME NULL,
    ShiftDefaultEnd TIME NULL
);

CREATE TABLE HR_AttendanceHistory (
    RecordID INT IDENTITY(1,1) PRIMARY KEY,
    AttendanceID INT NOT NULL REFERENCES HR_Attendances(AttendanceID),
    ModifiedBy INT NOT NULL REFERENCES NguoiDung(MaND),
    ModifiedAt DATETIME NOT NULL DEFAULT GETDATE(),
    FieldChanged NVARCHAR(50),
    OldValue NVARCHAR(200),
    NewValue NVARCHAR(200)
);

CREATE TABLE HR_Payroll (
    PayrollID INT IDENTITY(1,1) PRIMARY KEY,
    EmpID INT NOT NULL REFERENCES HR_Employees(EmpID),
    PayMonth INT NOT NULL,
    PayYear INT NOT NULL,
    TotalHours DECIMAL(10,2) DEFAULT 0,
    TotalEarned DECIMAL(18,2) DEFAULT 0,
    PaidAt DATETIME NULL,
    [Status] NVARCHAR(20) DEFAULT N'Cho thanh toan',
    CONSTRAINT UQ_Payroll_EmpMonth UNIQUE (EmpID, PayMonth, PayYear)
);

CREATE TABLE HR_Config (
    ConfigID INT IDENTITY(1,1) PRIMARY KEY,
    DefaultWeeklyLeave INT NOT NULL DEFAULT 1,
    DefaultAnnualLeave INT NOT NULL DEFAULT 12
);
GO

PRINT N'All HR tables recreated';
GO

-- Step 3: Seed
INSERT INTO HR_Config (DefaultWeeklyLeave, DefaultAnnualLeave) VALUES (1, 12);

INSERT INTO HR_Shifts (ShiftName, DefaultStartTime, DefaultEndTime) VALUES
(N'Ca Sang', '06:00', '14:00'),
(N'Ca Chieu', '14:00', '22:00'),
(N'Nghi Phep Tuan', '00:00', '00:00'),
(N'Nghi Phep Nam', '00:00', '00:00');

INSERT INTO HR_Employees (FullName, PinCode, Phone, HourlyRate, OvertimeRate, HireDate, [Status], MaND)
SELECT nd.HoTen, 
       RIGHT('0000' + CAST(ROW_NUMBER() OVER (ORDER BY nd.MaND) AS VARCHAR), 4),
       '',
       CASE WHEN nd.VaiTro = N'Admin' THEN 50000 ELSE 25000 END,
       CASE WHEN nd.VaiTro = N'Admin' THEN 75000 ELSE 37500 END,
       '2024-01-01',
       N'Dang lam',
       nd.MaND
FROM NguoiDung nd
WHERE nd.TrangThai = 1;
GO

PRINT N'=== HR Module rebuilt successfully! ===';
SELECT EmpID, FullName, PinCode, MaND FROM HR_Employees;
GO
