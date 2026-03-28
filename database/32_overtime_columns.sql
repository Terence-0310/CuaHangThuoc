USE QuanLyCuaHangThuoc;
GO

-- Snapshot OvertimeRate at clock-in time
IF COL_LENGTH('HR_Attendances', 'SnapshotOTRate') IS NULL
    ALTER TABLE HR_Attendances ADD SnapshotOTRate DECIMAL(18,2) NULL;
GO

-- Shift's DefaultEndTime — to detect when overtime starts
IF COL_LENGTH('HR_Attendances', 'ShiftDefaultEnd') IS NULL
    ALTER TABLE HR_Attendances ADD ShiftDefaultEnd TIME NULL;
GO

-- Separated overtime hours for reporting
IF COL_LENGTH('HR_Attendances', 'OvertimeHours') IS NULL
    ALTER TABLE HR_Attendances ADD OvertimeHours DECIMAL(18,2) NOT NULL DEFAULT 0;
GO

PRINT N'OK: HR_Attendances overtime columns added';
GO
