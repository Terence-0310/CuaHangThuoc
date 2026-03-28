-- =================================================================
-- 20_hrm_phase1_employees_shifts.sql
-- HRM Giai đoạn 1: Nhân viên & Ca làm việc
-- =================================================================
USE QuanLyCuaHangThuoc;
GO

-- =================================================================
-- 1. BẢNG CA LÀM VIỆC (HR_Shifts)
-- =================================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'HR_Shifts')
BEGIN
    CREATE TABLE HR_Shifts (
        ShiftID     INT IDENTITY(1,1) PRIMARY KEY,
        ShiftName   NVARCHAR(50)  NOT NULL,
        DefaultStartTime TIME NOT NULL,
        DefaultEndTime   TIME NOT NULL
    );
    PRINT N'Đã tạo bảng HR_Shifts.';
END
GO

-- =================================================================
-- 2. BẢNG NHÂN VIÊN (HR_Employees)
--    - PinCode: VARCHAR (giữ số 0 đầu), UNIQUE
--    - Soft Delete qua cột Status
-- =================================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'HR_Employees')
BEGIN
    CREATE TABLE HR_Employees (
        EmpID       INT IDENTITY(1,1) PRIMARY KEY,
        FullName    NVARCHAR(100)   NOT NULL,
        PinCode     VARCHAR(10)     NOT NULL,
        HourlyRate  DECIMAL(18,2)   NOT NULL DEFAULT 0,
        OvertimeRate DECIMAL(18,2)  NOT NULL DEFAULT 0,
        WeeklyLeaveQuota INT       NOT NULL DEFAULT 1,
        YearlyLeaveQuota INT       NOT NULL DEFAULT 12,
        Status      NVARCHAR(20)    NOT NULL DEFAULT N'Đang làm',
        CreatedAt   DATETIME        NOT NULL DEFAULT GETDATE(),

        CONSTRAINT UQ_HR_Employees_PinCode UNIQUE (PinCode)
    );
    PRINT N'Đã tạo bảng HR_Employees.';
END
ELSE
BEGIN
    -- Thêm cột PinCode nếu chưa có
    IF COL_LENGTH('HR_Employees', 'PinCode') IS NULL
    BEGIN
        ALTER TABLE HR_Employees ADD PinCode VARCHAR(10) NULL;
        -- Gán PIN tạm cho dòng cũ (nếu có)
        UPDATE HR_Employees SET PinCode = RIGHT('000' + CAST(EmpID AS VARCHAR), 4) WHERE PinCode IS NULL;
        ALTER TABLE HR_Employees ALTER COLUMN PinCode VARCHAR(10) NOT NULL;
        IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'UQ_HR_Employees_PinCode')
            ALTER TABLE HR_Employees ADD CONSTRAINT UQ_HR_Employees_PinCode UNIQUE (PinCode);
        PRINT N'Đã thêm cột PinCode (VARCHAR, UNIQUE).';
    END

    -- Thêm cột FullName nếu chưa có
    IF COL_LENGTH('HR_Employees', 'FullName') IS NULL
    BEGIN
        ALTER TABLE HR_Employees ADD FullName NVARCHAR(100) NULL;
        UPDATE HR_Employees SET FullName = N'NV-' + CAST(EmpID AS NVARCHAR) WHERE FullName IS NULL;
        ALTER TABLE HR_Employees ALTER COLUMN FullName NVARCHAR(100) NOT NULL;
        PRINT N'Đã thêm cột FullName.';
    END

    -- Thêm cột Status nếu chưa có
    IF COL_LENGTH('HR_Employees', 'Status') IS NULL
    BEGIN
        ALTER TABLE HR_Employees ADD Status NVARCHAR(20) NOT NULL DEFAULT N'Đang làm';
        PRINT N'Đã thêm cột Status.';
    END

    -- Thêm cột HourlyRate nếu chưa có
    IF COL_LENGTH('HR_Employees', 'HourlyRate') IS NULL
    BEGIN
        ALTER TABLE HR_Employees ADD HourlyRate DECIMAL(18,2) NOT NULL DEFAULT 0;
        PRINT N'Đã thêm cột HourlyRate.';
    END

    -- Thêm cột OvertimeRate nếu chưa có
    IF COL_LENGTH('HR_Employees', 'OvertimeRate') IS NULL
    BEGIN
        ALTER TABLE HR_Employees ADD OvertimeRate DECIMAL(18,2) NOT NULL DEFAULT 0;
        PRINT N'Đã thêm cột OvertimeRate.';
    END

    -- Thêm cột WeeklyLeaveQuota nếu chưa có
    IF COL_LENGTH('HR_Employees', 'WeeklyLeaveQuota') IS NULL
    BEGIN
        ALTER TABLE HR_Employees ADD WeeklyLeaveQuota INT NOT NULL DEFAULT 1;
        PRINT N'Đã thêm cột WeeklyLeaveQuota.';
    END

    -- Thêm cột YearlyLeaveQuota nếu chưa có
    IF COL_LENGTH('HR_Employees', 'YearlyLeaveQuota') IS NULL
    BEGIN
        ALTER TABLE HR_Employees ADD YearlyLeaveQuota INT NOT NULL DEFAULT 12;
        PRINT N'Đã thêm cột YearlyLeaveQuota.';
    END

    -- Thêm cột CreatedAt nếu chưa có
    IF COL_LENGTH('HR_Employees', 'CreatedAt') IS NULL
    BEGIN
        ALTER TABLE HR_Employees ADD CreatedAt DATETIME NOT NULL DEFAULT GETDATE();
        PRINT N'Đã thêm cột CreatedAt.';
    END
END
GO

-- =================================================================
-- 3. SEED DATA: 2 Ca làm + 2 Nhân viên mẫu
-- =================================================================

-- Ca làm
IF NOT EXISTS (SELECT 1 FROM HR_Shifts)
BEGIN
    INSERT INTO HR_Shifts (ShiftName, DefaultStartTime, DefaultEndTime) VALUES
        (N'Ca Sáng',  '06:00', '14:00'),
        (N'Ca Chiều', '14:00', '22:00');
    PRINT N'Đã seed 2 ca làm (Sáng/Chiều).';
END
GO

-- Nhân viên mẫu
IF NOT EXISTS (SELECT 1 FROM HR_Employees)
BEGIN
    INSERT INTO HR_Employees (FullName, PinCode, HourlyRate, OvertimeRate, WeeklyLeaveQuota, YearlyLeaveQuota, Status) VALUES
        (N'Lê Văn Nhân Viên',   '0001', 25000, 37500, 1, 12, N'Đang làm'),
        (N'Phạm Thị Bán Hàng',  '0002', 20000, 30000, 1, 12, N'Đang làm');
    PRINT N'Đã seed 2 nhân viên mẫu.';
END
GO

PRINT N'=== HRM Phase 1 hoàn tất! ===';
GO
