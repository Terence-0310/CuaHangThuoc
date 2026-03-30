-- ==================================================================
-- NUCLEAR WIPE: XÓA SẠCH DB - CHỈ GIỮ TÀI KHOẢN ADMIN
-- ==================================================================
USE QuanLyCuaHangThuoc;
GO
SET NOCOUNT ON;

-- === 1. TẮT TOÀN BỘ FOREIGN KEY ===
EXEC sp_MSforeachtable 'ALTER TABLE ? NOCHECK CONSTRAINT ALL';
PRINT N'[1/5] FK DISABLED.';

-- === 2. XÓA TRẮNG DỮ LIỆU (thứ tự con → cha) ===
BEGIN TRAN;

-- HRM
DELETE FROM HR_Attendances;
DELETE FROM HR_Schedules;
DELETE FROM HR_Employees;
DELETE FROM HR_Shifts;
DELETE FROM HR_Config;

-- Hóa đơn & bán hàng
DELETE FROM ChiTietHoaDon;
DELETE FROM HoaDon;
DELETE FROM TraHangKhach;

-- Nhập hàng & trả hàng
DELETE FROM ChiTietTraHang;
DELETE FROM PhieuTraHang;
DELETE FROM TraHangNCC;
DELETE FROM ChiTietHuyHang;
DELETE FROM PhieuHuyHang;
DELETE FROM HuyHang;
DELETE FROM ChiTietKiemKho;
DELETE FROM PhieuKiemKho;
DELETE FROM ChiTietHuyHang;
DELETE FROM LoHang;
DELETE FROM PhieuNhap;

-- Master data
DELETE FROM SanPham;
DELETE FROM NhaCungCap;
DELETE FROM KhachHang;
DELETE FROM SystemLogs;

-- User: CHỈ GIỮ ADMIN
DELETE FROM NguoiDung WHERE TenDangNhap != 'admin';

PRINT N'[2/5] ALL DATA WIPED. Admin preserved.';

-- === 3. RESET IDENTITY (chỉ bảng xóa trắng) ===
DBCC CHECKIDENT ('HR_Attendances', RESEED, 0);
DBCC CHECKIDENT ('HR_Schedules',   RESEED, 0);
DBCC CHECKIDENT ('HR_Employees',   RESEED, 0);
DBCC CHECKIDENT ('HR_Shifts',      RESEED, 0);
DBCC CHECKIDENT ('HR_Config',      RESEED, 0);
DBCC CHECKIDENT ('ChiTietHoaDon',  RESEED, 0);
DBCC CHECKIDENT ('HoaDon',         RESEED, 0);
DBCC CHECKIDENT ('LoHang',         RESEED, 0);
DBCC CHECKIDENT ('PhieuNhap',      RESEED, 0);
DBCC CHECKIDENT ('SanPham',        RESEED, 0);
DBCC CHECKIDENT ('NhaCungCap',     RESEED, 0);
DBCC CHECKIDENT ('KhachHang',      RESEED, 0);
DBCC CHECKIDENT ('SystemLogs',     RESEED, 0);

PRINT N'[3/5] IDENTITY RESEEDED.';

COMMIT TRAN;
PRINT N'[4/5] TRANSACTION COMMITTED.';

-- === 5. BẬT LẠI FOREIGN KEY ===
EXEC sp_MSforeachtable 'ALTER TABLE ? WITH CHECK CHECK CONSTRAINT ALL';
PRINT N'[5/5] FK RE-ENABLED.';

-- === VERIFY ===
SELECT N'NguoiDung' AS T, COUNT(*) AS N FROM NguoiDung UNION ALL
SELECT N'HR_Employees',   COUNT(*) FROM HR_Employees   UNION ALL
SELECT N'HR_Shifts',      COUNT(*) FROM HR_Shifts       UNION ALL
SELECT N'HR_Schedules',   COUNT(*) FROM HR_Schedules    UNION ALL
SELECT N'HR_Attendances',  COUNT(*) FROM HR_Attendances  UNION ALL
SELECT N'SanPham',         COUNT(*) FROM SanPham          UNION ALL
SELECT N'HoaDon',          COUNT(*) FROM HoaDon           UNION ALL
SELECT N'KhachHang',       COUNT(*) FROM KhachHang;

PRINT N'=== NUCLEAR WIPE COMPLETE. ONLY ADMIN SURVIVED. ===';
GO
