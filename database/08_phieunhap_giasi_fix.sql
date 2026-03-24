-- ============================================================
-- FILE: 08_phieunhap_giasi_fix.sql
-- MO TA:
--   🔴 1. GỠ UNIQUE(MaSP, SoLo) — cho phép nhập cùng lô nhiều lần
--   🟢 2. Thêm bảng PhieuNhap — quản lý dòng tiền chi ra
--   🟢 3. Thêm GiaBanSi vào SanPham — chống gian lận bán sỉ
-- ============================================================

USE QuanLyCuaHangThuoc;
GO

-- ============================================================
-- 🔴 FIX 1: GỠ UNIQUE(MaSP, SoLo)
-- ============================================================
IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = 'UQ_LoHang_MaSP_SoLo')
BEGIN
    ALTER TABLE LoHang DROP CONSTRAINT UQ_LoHang_MaSP_SoLo;
    PRINT N'✅ Đã gỡ UNIQUE(MaSP, SoLo)';
END
GO

-- ============================================================
-- 🟢 FIX 2: Tạo bảng PhieuNhap
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'PhieuNhap')
BEGIN
    CREATE TABLE PhieuNhap (
        MaPN      INT IDENTITY(1,1) PRIMARY KEY,
        MaND      INT           NOT NULL,          -- Người tạo phiếu nhập
        NgayNhap  DATETIME      NOT NULL DEFAULT GETDATE(),
        TongTien  DECIMAL(18,0) NOT NULL DEFAULT 0,-- Tổng chi cho NCC
        GhiChu    NVARCHAR(500) NULL,

        CONSTRAINT FK_PhieuNhap_NguoiDung FOREIGN KEY (MaND) REFERENCES NguoiDung(MaND)
    );

    CREATE NONCLUSTERED INDEX IX_PhieuNhap_NgayNhap ON PhieuNhap (NgayNhap DESC);
    PRINT N'✅ Tạo bảng PhieuNhap';
END
GO

-- Bước 2.1: Tạo PhieuNhap từ dữ liệu cũ (nhóm theo MaND + ngày)
INSERT INTO PhieuNhap (MaND, NgayNhap, TongTien, GhiChu)
SELECT
    ISNULL(MaND, (SELECT TOP 1 MaND FROM NguoiDung WHERE VaiTro = N'Admin')),
    CAST(NgayNhap AS DATE),
    SUM(GiaNhap * SoLuong),
    N'Phiếu nhập tự tạo từ migration'
FROM LoHang
GROUP BY MaND, CAST(NgayNhap AS DATE);
GO

PRINT N'✅ Tạo PhieuNhap từ dữ liệu cũ';
GO

-- Bước 2.2: Thêm MaPN vào LoHang
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('LoHang') AND name = 'MaPN')
BEGIN
    ALTER TABLE LoHang ADD MaPN INT NULL;
    PRINT N'✅ Thêm cột MaPN vào LoHang';
END
GO

-- Bước 2.3: Gán MaPN cho LoHang cũ (khớp theo MaND + ngày)
UPDATE lh
SET lh.MaPN = pn.MaPN
FROM LoHang lh
JOIN PhieuNhap pn ON
    ISNULL(lh.MaND, (SELECT TOP 1 MaND FROM NguoiDung WHERE VaiTro = N'Admin')) = pn.MaND
    AND CAST(lh.NgayNhap AS DATE) = CAST(pn.NgayNhap AS DATE)
WHERE lh.MaPN IS NULL;
GO

-- Bước 2.4: Thêm FK
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_LoHang_PhieuNhap')
BEGIN
    ALTER TABLE LoHang ADD CONSTRAINT FK_LoHang_PhieuNhap
        FOREIGN KEY (MaPN) REFERENCES PhieuNhap(MaPN);
    PRINT N'✅ FK LoHang → PhieuNhap';
END
GO

-- Bước 2.5: Xóa MaND khỏi LoHang (đã chuyển sang PhieuNhap)
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_LoHang_NguoiDung')
BEGIN
    ALTER TABLE LoHang DROP CONSTRAINT FK_LoHang_NguoiDung;
END
GO

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('LoHang') AND name = 'MaND')
BEGIN
    ALTER TABLE LoHang DROP COLUMN MaND;
    PRINT N'✅ Xóa MaND khỏi LoHang (đã dời sang PhieuNhap)';
END
GO

-- ============================================================
-- 🟢 FIX 3: Thêm GiaBanSi vào SanPham
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('SanPham') AND name = 'GiaBanSi')
BEGIN
    ALTER TABLE SanPham ADD GiaBanSi DECIMAL(18,0) NULL;
    PRINT N'✅ Thêm cột GiaBanSi vào SanPham';
END
GO

-- Backfill: Giá sỉ mặc định = 90% giá lẻ
UPDATE SanPham SET GiaBanSi = CAST(GiaBan * 0.9 AS DECIMAL(18,0)) WHERE GiaBanSi IS NULL;
PRINT N'✅ Backfill GiaBanSi = 90% GiaBan';
GO

-- ============================================================
-- Cập nhật View vw_TonKhoChiTiet (bỏ MaND, thêm MaPN+PhieuNhap)
-- ============================================================
CREATE OR ALTER VIEW vw_TonKhoChiTiet AS
SELECT
    sp.MaSP, sp.TenSP, sp.DonViTinh, lh.GiaNhap, sp.GiaBan,
    lh.MaLo, lh.SoLo, lh.HanSuDung, lh.SoLuong AS SoLuongConLai,
    lh.NgayNhap, lh.MaPN,
    pn.MaND AS MaNguoiNhap, nd.HoTen AS TenNguoiNhap,
    CASE WHEN lh.HanSuDung <= GETDATE() THEN N'ĐÃ HẾT HẠN'
         WHEN lh.HanSuDung <= DATEADD(MONTH, 3, GETDATE()) THEN N'SẮP HẾT HẠN'
         ELSE N'Còn hạn' END AS TrangThaiHSD
FROM LoHang lh
JOIN SanPham sp ON lh.MaSP = sp.MaSP
LEFT JOIN PhieuNhap pn ON lh.MaPN = pn.MaPN
LEFT JOIN NguoiDung nd ON pn.MaND = nd.MaND
WHERE lh.SoLuong > 0 AND sp.TrangThai = 1;
GO

PRINT N'✅ View vw_TonKhoChiTiet đã cập nhật';
GO

-- ============================================================
-- VERIFY
-- ============================================================
PRINT N'';
PRINT N'=== VERIFY ===';
SELECT 'PhieuNhap' AS Bang, COUNT(*) AS Rows FROM PhieuNhap;
SELECT 'LoHang.MaPN' AS Field, COUNT(*) AS Total,
    SUM(CASE WHEN MaPN IS NOT NULL THEN 1 ELSE 0 END) AS HasPN FROM LoHang;
SELECT 'SanPham.GiaBanSi' AS Field, COUNT(*) AS Total,
    SUM(CASE WHEN GiaBanSi IS NOT NULL THEN 1 ELSE 0 END) AS HasSi FROM SanPham;

-- Verify UNIQUE đã gỡ
SELECT CASE WHEN NOT EXISTS (
    SELECT 1 FROM sys.key_constraints WHERE name = 'UQ_LoHang_MaSP_SoLo')
    THEN N'✅ UNIQUE đã gỡ' ELSE N'❌ UNIQUE vẫn còn!' END AS Status;

PRINT N'✅ Migration 08 hoàn tất!';
GO
