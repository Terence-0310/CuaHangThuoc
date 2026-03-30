-- ============================================================
-- FILE: 10_add_online_status.sql
-- MO TA: Thêm cột DangOnline để theo dõi trạng thái online
--        real-time (giống Facebook) cho bảng NguoiDung
-- ============================================================

USE QuanLyCuaHangThuoc;
GO

-- Thêm cột DangOnline (BIT, mặc định 0 = Offline)
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('NguoiDung') AND name = 'DangOnline'
)
BEGIN
    ALTER TABLE NguoiDung ADD DangOnline BIT NOT NULL DEFAULT 0;
    PRINT N'✓ Đã thêm cột DangOnline vào bảng NguoiDung';
END
ELSE
BEGIN
    PRINT N'✓ Cột DangOnline đã tồn tại';
END
GO

-- Reset tất cả user về Offline khi chạy script này
UPDATE NguoiDung SET DangOnline = 0;
PRINT N'✓ Đã reset tất cả user về trạng thái Offline';
GO

-- Verify
SELECT MaND, TenDangNhap, HoTen, VaiTro, TrangThai, DangOnline 
FROM NguoiDung;
GO
