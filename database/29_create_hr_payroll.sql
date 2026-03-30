USE QuanLyCuaHangThuoc;
GO

-- ================================================================
-- Tạo bảng HR_Payroll — Lịch sử trả lương tháng
-- ================================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'HR_Payroll')
BEGIN
    CREATE TABLE HR_Payroll (
        PayrollID       INT IDENTITY(1,1) PRIMARY KEY,
        EmpID           INT NOT NULL,
        Thang           INT NOT NULL,
        Nam             INT NOT NULL,
        TongGio         DECIMAL(18,2) NOT NULL DEFAULT 0,
        TongTien        DECIMAL(18,0) NOT NULL DEFAULT 0,
        TrangThai       NVARCHAR(50) NOT NULL DEFAULT N'Đã thanh toán',
        NgayThanhToan   DATETIME NOT NULL DEFAULT GETDATE(),

        CONSTRAINT FK_Payroll_Employee FOREIGN KEY (EmpID) REFERENCES HR_Employees(EmpID),
        CONSTRAINT UQ_Payroll_EmpMonthYear UNIQUE (EmpID, Thang, Nam)
    );

    PRINT N'=== Tạo bảng HR_Payroll thành công. ===';
END
ELSE
    PRINT N'=== Bảng HR_Payroll đã tồn tại, bỏ qua. ===';
GO
