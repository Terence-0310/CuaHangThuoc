-- =================================================================
-- 22_hrm_phase3_attendances.sql
-- HRM Giai đoạn 3: Máy Chấm Công (HR_Attendances)
-- =================================================================
USE QuanLyCuaHangThuoc;
GO

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'HR_Attendances')
BEGIN
    CREATE TABLE HR_Attendances (
        AttendanceID INT IDENTITY(1,1) PRIMARY KEY,
        EmpID        INT NOT NULL,
        ScheduleID   INT NOT NULL,  -- Liên kết tới lịch gốc (có thể là của người khác bị làm thay)
        ClockIn      DATETIME NOT NULL DEFAULT GETDATE(),
        ClockOut     DATETIME NULL,
        LateReason   NVARCHAR(255) NULL,
        TotalHours   DECIMAL(18,2) NOT NULL DEFAULT 0,
        DailyEarned  DECIMAL(18,2) NOT NULL DEFAULT 0,
        CreatedAt    DATETIME NOT NULL DEFAULT GETDATE(),

        CONSTRAINT FK_Att_Employee FOREIGN KEY (EmpID) REFERENCES HR_Employees(EmpID),
        CONSTRAINT FK_Att_Schedule FOREIGN KEY (ScheduleID) REFERENCES HR_Schedules(ScheduleID)
    );
    PRINT N'Đã tạo bảng HR_Attendances.';
END
GO
