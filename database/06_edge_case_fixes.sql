-- ============================================================
-- FILE: 06_edge_case_fixes.sql
-- MO TA: Fix 3 van de:
--   🔴 1. Race Condition: Rewrite sp_GetFEFO voi UPDLOCK
--   🔴 2. Admin Self-Lock: Trigger chong tu khoa/xoa admin cuoi
--   🔴 3. LoHang tracking: Them MaND (nguoi nhap) vao LoHang
--   + Fix Views cu bi loi vi GiaNhap da doi sang LoHang
-- ============================================================

USE QuanLyCuaHangThuoc;
GO

-- ============================================================
-- 🔴 FIX 1: Race Condition — Rewrite sp_GetFEFO voi UPDLOCK
-- ============================================================

CREATE OR ALTER PROCEDURE sp_GetFEFO
    @MaSP INT
AS
BEGIN
    SET NOCOUNT ON;

    -- ★ WITH (UPDLOCK, ROWLOCK): Khoa dong, giao dich den sau phai CHO
    -- ★ Chi khoa dong dang doc, KHONG khoa toan bang
    SELECT
        MaLo,
        MaSP,
        SoLo,
        HanSuDung,
        SoLuong,
        GiaNhap
    FROM LoHang WITH (UPDLOCK, ROWLOCK)
    WHERE MaSP = @MaSP
      AND SoLuong > 0
      AND HanSuDung > GETDATE()
    ORDER BY HanSuDung ASC;
END;
GO

PRINT N'✅ Fix 1: sp_GetFEFO da them UPDLOCK, ROWLOCK';
GO

-- ============================================================
-- 🔴 FIX 2: Trigger chong Admin tu khoa/xoa chinh minh
-- ============================================================

-- Drop trigger cu neu co
IF OBJECT_ID('trg_PreventAdminLock', 'TR') IS NOT NULL
    DROP TRIGGER trg_PreventAdminLock;
GO

CREATE TRIGGER trg_PreventAdminLock
ON NguoiDung
AFTER UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    -- Dem so Admin con active SAU khi UPDATE/DELETE
    DECLARE @adminCount INT;
    SELECT @adminCount = COUNT(*)
    FROM NguoiDung
    WHERE VaiTro = N'Admin' AND TrangThai = 1;

    -- Neu khong con Admin nao -> ROLLBACK
    IF @adminCount = 0
    BEGIN
        ROLLBACK TRANSACTION;
        RAISERROR(N'Không thể thực hiện! Hệ thống phải có ít nhất 1 tài khoản Admin hoạt động.', 16, 1);
        RETURN;
    END
END;
GO

PRINT N'✅ Fix 2: Trigger trg_PreventAdminLock da tao';
GO

-- ============================================================
-- 🔴 FIX 3: Them MaND (nguoi nhap) vao LoHang
-- ============================================================

-- Buoc 3.1: Them cot MaND (nguoi nhap kho)
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('LoHang') AND name = 'MaND')
BEGIN
    ALTER TABLE LoHang ADD MaND INT NULL;

    ALTER TABLE LoHang ADD CONSTRAINT FK_LoHang_NguoiDung
        FOREIGN KEY (MaND) REFERENCES NguoiDung(MaND);

    PRINT N'✅ Thêm cột MaND vào LoHang';
END
GO

-- Buoc 3.2: Backfill — gan admin cho lo hang cu
UPDATE LoHang
SET MaND = (SELECT TOP 1 MaND FROM NguoiDung WHERE VaiTro = N'Admin' ORDER BY MaND)
WHERE MaND IS NULL;
GO

PRINT N'✅ Backfill MaND cho LoHang cũ';
GO

-- ============================================================
-- FIX Views cu: GiaNhap da doi tu SanPham sang LoHang
-- ============================================================

CREATE OR ALTER VIEW vw_TonKhoChiTiet AS
SELECT
    sp.MaSP,
    sp.TenSP,
    sp.DonViTinh,
    lh.GiaNhap,                         -- ★ Lay tu LoHang (khong phai SanPham)
    sp.GiaBan,
    lh.MaLo,
    lh.SoLo,
    lh.HanSuDung,
    lh.SoLuong     AS SoLuongConLai,
    lh.NgayNhap,
    lh.MaND        AS MaNguoiNhap,       -- ★ MOI: Ai nhap
    nd.HoTen       AS TenNguoiNhap,      -- ★ MOI: Ten nguoi nhap
    CASE
        WHEN lh.HanSuDung <= GETDATE() THEN N'ĐÃ HẾT HẠN'
        WHEN lh.HanSuDung <= DATEADD(MONTH, 3, GETDATE()) THEN N'SẮP HẾT HẠN'
        ELSE N'Còn hạn'
    END AS TrangThaiHSD
FROM LoHang lh
INNER JOIN SanPham sp ON lh.MaSP = sp.MaSP
LEFT JOIN NguoiDung nd ON lh.MaND = nd.MaND
WHERE lh.SoLuong > 0 AND sp.TrangThai = 1;
GO

CREATE OR ALTER VIEW vw_TonKhoTheoSanPham AS
SELECT
    sp.MaSP,
    sp.TenSP,
    sp.DonViTinh,
    sp.GiaBan,
    sp.TrangThai,
    ISNULL(SUM(lh.SoLuong), 0) AS TongTonKho
FROM SanPham sp
LEFT JOIN LoHang lh ON sp.MaSP = lh.MaSP AND lh.SoLuong > 0
GROUP BY sp.MaSP, sp.TenSP, sp.DonViTinh, sp.GiaBan, sp.TrangThai;
GO

PRINT N'✅ Views đã cập nhật (bỏ SanPham.GiaNhap, thêm NguoiNhap)';
GO

-- ============================================================
-- VERIFY
-- ============================================================
PRINT N'';
PRINT N'=== VERIFY FIX ===';

-- Verify LoHang có MaND
SELECT 'LoHang.MaND' AS Field, COUNT(*) AS TotalRows,
    SUM(CASE WHEN MaND IS NOT NULL THEN 1 ELSE 0 END) AS HasMaND
FROM LoHang;

-- Verify Trigger
SELECT name, type_desc FROM sys.triggers WHERE name = 'trg_PreventAdminLock';

-- Verify sp_GetFEFO có UPDLOCK
SELECT OBJECT_DEFINITION(OBJECT_ID('sp_GetFEFO')) AS SPDefinition;

-- Test trigger: Thu xoa admin duy nhat (phai FAIL)
-- BEGIN TRY
--     UPDATE NguoiDung SET TrangThai = 0 WHERE TenDangNhap = 'admin';
--     PRINT N'❌ Trigger KHÔNG hoạt động!';
-- END TRY
-- BEGIN CATCH
--     PRINT N'✅ Trigger hoạt động: ' + ERROR_MESSAGE();
-- END CATCH

PRINT N'';
PRINT N'✅ Tất cả fix hoàn tất!';
GO
