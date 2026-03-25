-- =============================================
-- Thêm cột GioiTinh cho bảng KhachHang
-- Chạy file này 1 lần trên SQL Server
-- =============================================
USE CuaHangThuoc;
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE Name = 'GioiTinh' AND Object_ID = OBJECT_ID('KhachHang'))
BEGIN
    ALTER TABLE KhachHang ADD GioiTinh NVARCHAR(10) NULL DEFAULT N'Khác';
END
GO

PRINT N'✅ Đã thêm cột GioiTinh cho KhachHang thành công!';
GO
