-- ============================================================
-- FILE: 12_system_logs.sql
-- MO TA: Tạo bảng SystemLogs cho audit trail (chống gian lận)
-- ============================================================

USE QuanLyCuaHangThuoc;
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'SystemLogs')
BEGIN
    CREATE TABLE SystemLogs (
        MaLog     INT IDENTITY(1,1) PRIMARY KEY,
        MaND      INT NULL,
        TenUser   NVARCHAR(100) NOT NULL,
        HanhDong  NVARCHAR(50)  NOT NULL,
        DoiTuong  NVARCHAR(200) NOT NULL,
        ChiTiet   NVARCHAR(500) NULL,
        ThoiGian  DATETIME NOT NULL DEFAULT GETDATE(),

        CONSTRAINT FK_SystemLogs_NguoiDung FOREIGN KEY (MaND) 
            REFERENCES NguoiDung(MaND)
    );
    PRINT N'✅ Tạo bảng SystemLogs';
END
ELSE
    PRINT N'✓ Bảng SystemLogs đã tồn tại';
GO

-- Index cho truy vấn audit
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_SystemLogs_ThoiGian')
    CREATE NONCLUSTERED INDEX IX_SystemLogs_ThoiGian ON SystemLogs (ThoiGian DESC);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_SystemLogs_HanhDong')
    CREATE NONCLUSTERED INDEX IX_SystemLogs_HanhDong ON SystemLogs (HanhDong);
GO

PRINT N'✅ Hoàn tất tạo bảng SystemLogs';
GO
