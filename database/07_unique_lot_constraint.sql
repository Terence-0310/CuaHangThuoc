-- ============================================================
-- FILE: 07_unique_lot_constraint.sql
-- MO TA: Them UNIQUE (MaSP, SoLo) vao LoHang
--        1 san pham + 1 so lo = chi 1 dong duy nhat
-- ============================================================

USE QuanLyCuaHangThuoc;
GO

-- Buoc 1: Kiem tra du lieu trung truoc khi tao constraint
PRINT N'=== Kiểm tra dữ liệu trùng lặp ===';
SELECT MaSP, SoLo, COUNT(*) AS SoLanXuatHien
FROM LoHang
GROUP BY MaSP, SoLo
HAVING COUNT(*) > 1;
GO

-- Buoc 2: Gop cac dong trung (neu co) — giu dong cu nhat, cong don SL
;WITH Duplicates AS (
    SELECT MaLo, MaSP, SoLo, SoLuong,
           ROW_NUMBER() OVER (PARTITION BY MaSP, SoLo ORDER BY MaLo ASC) AS RowNum
    FROM LoHang
)
-- Cong don so luong vao dong dau tien
UPDATE lh
SET lh.SoLuong = lh.SoLuong + dup.TotalExtra
FROM LoHang lh
JOIN (
    SELECT MaSP, SoLo, SUM(SoLuong) AS TotalExtra
    FROM Duplicates WHERE RowNum > 1
    GROUP BY MaSP, SoLo
) dup ON lh.MaSP = dup.MaSP AND lh.SoLo = dup.SoLo
WHERE lh.MaLo = (
    SELECT TOP 1 d2.MaLo FROM Duplicates d2
    WHERE d2.MaSP = lh.MaSP AND d2.SoLo = lh.SoLo AND d2.RowNum = 1
);
GO

-- Xoa cac dong trung (giu lai dong dau tien)
;WITH Duplicates AS (
    SELECT MaLo,
           ROW_NUMBER() OVER (PARTITION BY MaSP, SoLo ORDER BY MaLo ASC) AS RowNum
    FROM LoHang
)
DELETE FROM Duplicates WHERE RowNum > 1;
GO

PRINT N'✅ Đã gộp/xóa các dòng trùng lặp (nếu có)';
GO

-- Buoc 3: Tao UNIQUE constraint
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'UQ_LoHang_MaSP_SoLo'
    AND object_id = OBJECT_ID('LoHang')
)
BEGIN
    ALTER TABLE LoHang
    ADD CONSTRAINT UQ_LoHang_MaSP_SoLo UNIQUE (MaSP, SoLo);
    PRINT N'✅ Thêm UNIQUE constraint (MaSP, SoLo) vào LoHang';
END
ELSE
    PRINT N'ℹ️ Constraint UQ_LoHang_MaSP_SoLo đã tồn tại';
GO

-- Verify
PRINT N'';
PRINT N'=== VERIFY ===';
SELECT name, type_desc
FROM sys.key_constraints
WHERE parent_object_id = OBJECT_ID('LoHang') AND name = 'UQ_LoHang_MaSP_SoLo';

-- Test: Thu insert trung (phai FAIL)
-- INSERT INTO LoHang (MaSP, SoLo, HanSuDung, SoLuong, GiaNhap)
-- VALUES (1, N'LOT-001', '2099-01-01', 1, 100);

PRINT N'✅ Hoàn tất! 1 SanPham + 1 SoLo = 1 dòng duy nhất';
GO
