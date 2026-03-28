-- ============================================================
-- FILE: 15_hrm_v3.sql
-- MO TA: Tao cac bang cho Module Quan ly Nhan su (HRM) V3
--        (Employees, AttendanceHistory, MonthlyPayroll)
-- DBMS:  SQL Server
-- ============================================================

USE QuanLyCuaHangThuoc;
GO

-- Xoa bang cu (V2) neu ton tai de tao lai tu dau
IF OBJECT_ID('HR_MonthlyPayroll', 'U') IS NOT NULL DROP TABLE HR_MonthlyPayroll;
IF OBJECT_ID('HR_AttendanceHistory', 'U') IS NOT NULL DROP TABLE HR_AttendanceHistory;
IF OBJECT_ID('HR_Employees', 'U') IS NOT NULL DROP TABLE HR_Employees;

IF OBJECT_ID('BangLuong', 'U') IS NOT NULL DROP TABLE BangLuong;
IF OBJECT_ID('ThuongPhat', 'U') IS NOT NULL DROP TABLE ThuongPhat;
IF OBJECT_ID('ChamCong', 'U') IS NOT NULL DROP TABLE ChamCong;
IF OBJECT_ID('LichLamViec', 'U') IS NOT NULL DROP TABLE LichLamViec;
IF OBJECT_ID('ThongTinNhanSu', 'U') IS NOT NULL DROP TABLE ThongTinNhanSu;
GO

-- 1. Bảng Employees (Mở rộng từ NguoiDung để giữ khóa ngoại an toàn)
CREATE TABLE HR_Employees (
    EmpID INT PRIMARY KEY, -- Trỏ về NguoiDung.MaND
    Age INT NOT NULL DEFAULT 20,
    HourlyRate DECIMAL(18,0) NOT NULL DEFAULT 20000,
    IsActive BIT NOT NULL DEFAULT 1,
    
    CONSTRAINT FK_HREmp_NguoiDung FOREIGN KEY (EmpID) REFERENCES NguoiDung(MaND)
);
GO

-- 2. Bảng AttendanceHistory
CREATE TABLE HR_AttendanceHistory (
    RecordID INT IDENTITY(1,1) PRIMARY KEY,
    EmpID INT NOT NULL,
    ClockIn DATETIME NOT NULL,
    ClockOut DATETIME NULL,
    TotalHours DECIMAL(8,2) NULL,
    DailyEarned DECIMAL(18,0) NULL, -- Thu nhập trong ngày = TotalHours * HourlyRate
    
    CONSTRAINT FK_HRAtt_HREmp FOREIGN KEY (EmpID) REFERENCES HR_Employees(EmpID)
);
GO

-- 3. Bảng MonthlyPayroll
CREATE TABLE HR_MonthlyPayroll (
    PayrollID INT IDENTITY(1,1) PRIMARY KEY,
    [Month] INT NOT NULL,
    [Year] INT NOT NULL,
    EmpID INT NOT NULL,
    TotalSalary DECIMAL(18,0) NOT NULL DEFAULT 0,
    IsPaid BIT NOT NULL DEFAULT 0,
    PaidDate DATETIME NULL,

    CONSTRAINT FK_HRPay_HREmp FOREIGN KEY (EmpID) REFERENCES HR_Employees(EmpID),
    CONSTRAINT UQ_HRPay_MonthYear_Emp UNIQUE ([Month], [Year], EmpID)
);
GO

-- ============================================================
-- PHẦN SEED DATA (Chuẩn N'')
-- ============================================================

-- BƯỚC 1: Insert HR_Employees (Dựa trên MaND có sẵn: 1(Admin), 3(NV01), 4(NV02))
INSERT INTO HR_Employees (EmpID, Age, HourlyRate, IsActive) VALUES 
(1, 35, 50000, 1),
(3, 24, 25000, 1),
(4, 22, 22000, 1);
GO

-- BƯỚC 2: AttendanceHistory Mẫu
DECLARE @Today DATE = GETDATE();
DECLARE @Yesterday DATE = DATEADD(DAY, -1, @Today);
DECLARE @BeforeYest DATE = DATEADD(DAY, -2, @Today);

-- NV01 làm hôm kia 8 tiếng (Từ 08:00 đến 16:00, lương 200,000)
INSERT INTO HR_AttendanceHistory (EmpID, ClockIn, ClockOut, TotalHours, DailyEarned) 
VALUES (3, DATEADD(HOUR, 8, CAST(@BeforeYest AS DATETIME)), DATEADD(HOUR, 16, CAST(@BeforeYest AS DATETIME)), 8.0, 200000);

-- NV01 làm hôm qua 9.5 tiếng (Từ 07:00 đến 16:30, lương 237,500)
INSERT INTO HR_AttendanceHistory (EmpID, ClockIn, ClockOut, TotalHours, DailyEarned) 
VALUES (3, DATEADD(HOUR, 7, CAST(@Yesterday AS DATETIME)), DATEADD(MINUTE, 30, DATEADD(HOUR, 16, CAST(@Yesterday AS DATETIME))), 9.5, 237500);

-- NV02 làm hôm qua 5 tiếng (Từ 13:00 đến 18:00, lương 110,000)
INSERT INTO HR_AttendanceHistory (EmpID, ClockIn, ClockOut, TotalHours, DailyEarned) 
VALUES (4, DATEADD(HOUR, 13, CAST(@Yesterday AS DATETIME)), DATEADD(HOUR, 18, CAST(@Yesterday AS DATETIME)), 5.0, 110000);

-- NV01 đang làm hôm nay (Chưa ra ca)
INSERT INTO HR_AttendanceHistory (EmpID, ClockIn, ClockOut, TotalHours, DailyEarned) 
VALUES (3, DATEADD(HOUR, 7, CAST(@Today AS DATETIME)), NULL, NULL, NULL);
GO

PRINT N'Hoàn tất tạo module HRM V3: HR_Employees, HR_AttendanceHistory, HR_MonthlyPayroll!';
GO
