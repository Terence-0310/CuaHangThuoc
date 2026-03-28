-- ============================================================
-- FILE: 18_hrm_v6_schedules.sql
-- MO TA: Nâng cấp HRM V6 — Xếp ca theo tuần + Leave Quota
-- DBMS:  SQL Server
-- ============================================================
USE QuanLyCuaHangThuoc;
GO

-- 1. Thêm cột Leave Quota vào HR_Employees
IF COL_LENGTH('HR_Employees', 'WeeklyLeaveQuota') IS NULL
BEGIN
    ALTER TABLE HR_Employees ADD WeeklyLeaveQuota INT NOT NULL DEFAULT 1;
END
GO

IF COL_LENGTH('HR_Employees', 'YearlyLeaveQuota') IS NULL
BEGIN
    ALTER TABLE HR_Employees ADD YearlyLeaveQuota INT NOT NULL DEFAULT 12;
END
GO

-- 2. Tạo bảng Schedules (Xếp ca theo ngày)
IF OBJECT_ID('HR_Schedules', 'U') IS NULL
BEGIN
    CREATE TABLE HR_Schedules (
        ScheduleID   INT IDENTITY(1,1) PRIMARY KEY,
        EmpID        INT NOT NULL,
        WorkDate     DATE NOT NULL,
        ShiftID      INT NOT NULL,
        CustomStartTime TIME NULL,  -- NULL = lấy giờ mặc định từ HR_Shifts
        CustomEndTime   TIME NULL,  -- NULL = lấy giờ mặc định từ HR_Shifts
        CreatedAt    DATETIME NOT NULL DEFAULT GETDATE(),

        CONSTRAINT FK_Schedule_Emp   FOREIGN KEY (EmpID)   REFERENCES HR_Employees(EmpID),
        CONSTRAINT FK_Schedule_Shift FOREIGN KEY (ShiftID) REFERENCES HR_Shifts(ShiftID),
        CONSTRAINT UQ_Schedule_EmpDate UNIQUE (EmpID, WorkDate)
    );
END
GO

-- 3. Seed data: Tạo lịch mẫu cho tuần này
DECLARE @Today DATE = GETDATE();
DECLARE @DayOfWeek INT = DATEPART(WEEKDAY, @Today);
DECLARE @Monday DATE = DATEADD(DAY, 2 - @DayOfWeek, @Today); -- Thứ Hai tuần này

-- NV EmpID=1 (Admin): Ca Sáng cả tuần (ShiftID=1)
DECLARE @i INT = 0;
WHILE @i < 6  -- Thứ 2 đến Thứ 7
BEGIN
    IF NOT EXISTS (SELECT 1 FROM HR_Schedules WHERE EmpID = 1 AND WorkDate = DATEADD(DAY, @i, @Monday))
    BEGIN
        INSERT INTO HR_Schedules (EmpID, WorkDate, ShiftID) VALUES (1, DATEADD(DAY, @i, @Monday), 1);
    END
    SET @i = @i + 1;
END

-- NV EmpID=3 (NV01): Ca Sáng cả tuần
SET @i = 0;
WHILE @i < 6
BEGIN
    IF NOT EXISTS (SELECT 1 FROM HR_Schedules WHERE EmpID = 3 AND WorkDate = DATEADD(DAY, @i, @Monday))
    BEGIN
        INSERT INTO HR_Schedules (EmpID, WorkDate, ShiftID) VALUES (3, DATEADD(DAY, @i, @Monday), 1);
    END
    SET @i = @i + 1;
END

-- NV EmpID=4 (NV02): Ca Chiều cả tuần (ShiftID=2)
SET @i = 0;
WHILE @i < 6
BEGIN
    IF NOT EXISTS (SELECT 1 FROM HR_Schedules WHERE EmpID = 4 AND WorkDate = DATEADD(DAY, @i, @Monday))
    BEGIN
        INSERT INTO HR_Schedules (EmpID, WorkDate, ShiftID) VALUES (4, DATEADD(DAY, @i, @Monday), 2);
    END
    SET @i = @i + 1;
END
GO

PRINT N'Hoàn tất cập nhật HRM V6: HR_Schedules + Leave Quota!';
GO
