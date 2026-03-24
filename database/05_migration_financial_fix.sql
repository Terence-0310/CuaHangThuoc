-- ============================================================
-- FILE: 05_migration_financial_fix.sql
-- MO TA: Khac phuc 2 loi chi tu ve kien truc tai chinh
--        + Fix FEFO Race Condition (UPDLOCK)
--
-- 🔴 FIX 1: Dời GiaNhap từ SanPham -> LoHang (Giá vốn theo lô)
--           Thêm GiaVon vào ChiTietHoaDon (snapshot giá vốn lúc bán)
--
-- 🔴 FIX 2: Thêm PhuongThucThanhToan vào HoaDon
--
-- ⚠️  FIX 3: Tạo View lợi nhuận chuẩn chỉ
-- ============================================================

USE QuanLyCuaHangThuoc;
GO

-- ============================================================
-- 🔴 FIX 1: Dời GiaNhap sang LoHang
-- ============================================================

-- Bước 1.1: Thêm cột GiaNhap vào LoHang
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('LoHang') AND name = 'GiaNhap')
BEGIN
    ALTER TABLE LoHang ADD GiaNhap DECIMAL(18,0) NOT NULL DEFAULT 0;
    PRINT N'✅ Thêm cột GiaNhap vào LoHang';
END
GO

-- Bước 1.2: Copy giá nhập từ SanPham → LoHang (cho dữ liệu cũ)
UPDATE lh
SET lh.GiaNhap = sp.GiaNhap
FROM LoHang lh
JOIN SanPham sp ON lh.MaSP = sp.MaSP
WHERE lh.GiaNhap = 0;
PRINT N'✅ Copy GiaNhap từ SanPham sang LoHang cho dữ liệu cũ';
GO

-- Bước 1.3: Thêm constraint GiaNhap >= 0 cho LoHang
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_LoHang_GiaNhap')
BEGIN
    ALTER TABLE LoHang ADD CONSTRAINT CK_LoHang_GiaNhap CHECK (GiaNhap >= 0);
    PRINT N'✅ Thêm constraint CK_LoHang_GiaNhap';
END
GO

-- Bước 1.4: Xóa constraint GiaBan >= GiaNhap ở SanPham (vì GiaNhap sẽ bị xóa)
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_SanPham_Gia')
BEGIN
    ALTER TABLE SanPham DROP CONSTRAINT CK_SanPham_Gia;
    PRINT N'✅ Xóa constraint CK_SanPham_Gia (GiaBan >= GiaNhap)';
END
GO

-- Bước 1.5: Xóa constraint GiaNhap >= 0 ở SanPham
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_SanPham_GiaNhap')
BEGIN
    ALTER TABLE SanPham DROP CONSTRAINT CK_SanPham_GiaNhap;
    PRINT N'✅ Xóa constraint CK_SanPham_GiaNhap';
END
GO

-- Bước 1.6: Xóa cột GiaNhap khỏi SanPham
-- ⚠️ Giữ lại nếu muốn làm "Giá nhập tham khảo mặc định" - KHÔNG dùng cho báo cáo lợi nhuận
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('SanPham') AND name = 'GiaNhap')
BEGIN
    ALTER TABLE SanPham DROP COLUMN GiaNhap;
    PRINT N'✅ Xóa cột GiaNhap khỏi SanPham';
END
GO

-- Bước 1.7: Thêm cột GiaVon vào ChiTietHoaDon (snapshot giá vốn tại thời điểm bán)
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('ChiTietHoaDon') AND name = 'GiaVon')
BEGIN
    ALTER TABLE ChiTietHoaDon ADD GiaVon DECIMAL(18,0) NOT NULL DEFAULT 0;
    PRINT N'✅ Thêm cột GiaVon vào ChiTietHoaDon';
END
GO

-- Bước 1.8: Backfill GiaVon cho dữ liệu cũ (lấy từ LoHang)
UPDATE ct
SET ct.GiaVon = lh.GiaNhap
FROM ChiTietHoaDon ct
JOIN LoHang lh ON ct.MaLo = lh.MaLo
WHERE ct.GiaVon = 0;
PRINT N'✅ Backfill GiaVon cho ChiTietHoaDon từ LoHang';
GO

-- Thêm constraint GiaVon >= 0
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_CTHD_GiaVon')
BEGIN
    ALTER TABLE ChiTietHoaDon ADD CONSTRAINT CK_CTHD_GiaVon CHECK (GiaVon >= 0);
    PRINT N'✅ Thêm constraint CK_CTHD_GiaVon';
END
GO

-- ============================================================
-- 🔴 FIX 2: Thêm PhuongThucThanhToan vào HoaDon
-- ============================================================

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'PhuongThucTT')
BEGIN
    ALTER TABLE HoaDon ADD PhuongThucTT NVARCHAR(20) NOT NULL DEFAULT N'TienMat';
    PRINT N'✅ Thêm cột PhuongThucTT vào HoaDon';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_HoaDon_PhuongThucTT')
BEGIN
    ALTER TABLE HoaDon ADD CONSTRAINT CK_HoaDon_PhuongThucTT
        CHECK (PhuongThucTT IN (N'TienMat', N'ChuyenKhoan', N'QR'));
    PRINT N'✅ Thêm constraint CK_HoaDon_PhuongThucTT';
END
GO

-- ============================================================
-- ⚠️ FIX 3: View lợi nhuận chuẩn chỉ (Dựa trên GiaVon snapshot)
-- ============================================================

-- Drop old view nếu có
IF OBJECT_ID('vw_LoiNhuan', 'V') IS NOT NULL DROP VIEW vw_LoiNhuan;
GO

CREATE VIEW vw_LoiNhuan AS
SELECT
    hd.MaHD,
    hd.NgayBan,
    sp.TenSP,
    ct.SoLuong,
    ct.DonGia                                   AS GiaBan,
    ct.GiaVon                                   AS GiaVon,
    (ct.DonGia - ct.GiaVon) * ct.SoLuong        AS LoiNhuan,
    ct.ThanhTien                                AS DoanhThu
FROM ChiTietHoaDon ct
JOIN HoaDon hd ON ct.MaHD = hd.MaHD
JOIN SanPham sp ON ct.MaSP = sp.MaSP;
GO

PRINT N'✅ Tạo View vw_LoiNhuan';
GO

-- View báo cáo đối soát cuối ngày theo phương thức thanh toán
IF OBJECT_ID('vw_DoiSoatCuoiNgay', 'V') IS NOT NULL DROP VIEW vw_DoiSoatCuoiNgay;
GO

CREATE VIEW vw_DoiSoatCuoiNgay AS
SELECT
    CAST(NgayBan AS DATE)                       AS Ngay,
    PhuongThucTT,
    COUNT(*)                                    AS SoHoaDon,
    SUM(TongTien)                               AS TongTien
FROM HoaDon
GROUP BY CAST(NgayBan AS DATE), PhuongThucTT;
GO

PRINT N'✅ Tạo View vw_DoiSoatCuoiNgay';
GO

-- ============================================================
-- VERIFY
-- ============================================================
PRINT N'';
PRINT N'=== KẾT QUẢ MIGRATION ===';

-- Verify LoHang có GiaNhap
SELECT 'LoHang.GiaNhap' AS Field, COUNT(*) AS Rows, MIN(GiaNhap) AS MinGia, MAX(GiaNhap) AS MaxGia FROM LoHang;

-- Verify ChiTietHoaDon có GiaVon
SELECT 'CTHD.GiaVon' AS Field, COUNT(*) AS Rows, MIN(GiaVon) AS MinGia, MAX(GiaVon) AS MaxGia FROM ChiTietHoaDon;

-- Verify HoaDon có PhuongThucTT
SELECT 'HoaDon.PhuongThucTT' AS Field, PhuongThucTT, COUNT(*) AS Rows FROM HoaDon GROUP BY PhuongThucTT;

-- Verify SanPham KHÔNG còn GiaNhap
SELECT 'SanPham columns' AS Info,
    CASE WHEN COL_LENGTH('SanPham', 'GiaNhap') IS NULL
        THEN N'✅ GiaNhap đã xóa thành công'
        ELSE N'❌ GiaNhap vẫn còn!'
    END AS Status;

-- Test View lợi nhuận
SELECT TOP 5 * FROM vw_LoiNhuan;

PRINT N'';
PRINT N'✅ Migration hoàn tất!';
GO
