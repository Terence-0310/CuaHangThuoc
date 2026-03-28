-- =================================================================
-- 21_hrm_phase2_schedules.sql
-- HRM Giai đoạn 2: Bảng Xếp Ca (HR_Schedules)
-- =================================================================
USE QuanLyCuaHangThuoc;
GO

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'HR_Schedules')
BEGIN
    CREATE TABLE HR_Schedules (
        ScheduleID   INT IDENTITY(1,1) PRIMARY KEY,
        EmpID        INT NOT NULL,
        ShiftID      INT NOT NULL,
        WorkDate     DATE NOT NULL,
        ActualStart  TIME NOT NULL,  -- Giờ vào thực tế (có thể override)
        ActualEnd    TIME NOT NULL,  -- Giờ ra thực tế (có thể override)
        CreatedAt    DATETIME NOT NULL DEFAULT GETDATE(),

        CONSTRAINT FK_Schedule_Employee FOREIGN KEY (EmpID) REFERENCES HR_Employees(EmpID),
        CONSTRAINT FK_Schedule_Shift    FOREIGN KEY (ShiftID) REFERENCES HR_Shifts(ShiftID),
        CONSTRAINT UQ_Schedule_EmpDate  UNIQUE (EmpID, WorkDate)  -- Chống trùng: 1 NV 1 ngày chỉ 1 lịch
    );
    PRINT N'Đã tạo bảng HR_Schedules.';
END
GO

PRINT N'=== HRM Phase 2 (Schedules) hoàn tất! ===';
GO
