-- ============================================================
-- Migration 12: Thêm cột LoaiHD + MaHDGoc cho nghiệp vụ Trả Hàng
-- ============================================================
USE QuanLyCuaHangThuoc;
GO

-- 1. Thêm cột LoaiHD (SALE / RETURN) vào HoaDon
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='HoaDon' AND COLUMN_NAME='LoaiHD')
BEGIN
    ALTER TABLE HoaDon ADD LoaiHD NVARCHAR(10) NOT NULL DEFAULT N'SALE';
    PRINT N'✅ Thêm cột LoaiHD vào HoaDon';
END
GO

-- 2. Thêm cột MaHDGoc (FK → HoaDon gốc khi trả hàng)
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='HoaDon' AND COLUMN_NAME='MaHDGoc')
BEGIN
    ALTER TABLE HoaDon ADD MaHDGoc INT NULL;
    PRINT N'✅ Thêm cột MaHDGoc vào HoaDon';
END
GO

-- 3. FK MaHDGoc → HoaDon(MaHD)
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_HoaDon_HoaDonGoc')
BEGIN
    ALTER TABLE HoaDon ADD CONSTRAINT FK_HoaDon_HoaDonGoc
        FOREIGN KEY (MaHDGoc) REFERENCES HoaDon(MaHD);
    PRINT N'✅ Thêm FK MaHDGoc → HoaDon';
END
GO

-- 4. Constraint LoaiHD chỉ nhận SALE / RETURN
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_HoaDon_LoaiHD')
BEGIN
    ALTER TABLE HoaDon ADD CONSTRAINT CK_HoaDon_LoaiHD
        CHECK (LoaiHD IN (N'SALE', N'RETURN'));
    PRINT N'✅ Thêm constraint CK_HoaDon_LoaiHD';
END
GO

-- 5. Drop constraint cũ chặn SoLuong > 0 (cần cho phép SoLuong âm khi trả hàng)
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_CTHD_SoLuong')
BEGIN
    ALTER TABLE ChiTietHoaDon DROP CONSTRAINT CK_CTHD_SoLuong;
    PRINT N'✅ Đã drop CK_CTHD_SoLuong (cho phép SoLuong âm khi RETURN)';
END
GO

-- 6. Drop constraint cũ chặn ThanhTien >= 0
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_CTHD_ThanhTien')
BEGIN
    ALTER TABLE ChiTietHoaDon DROP CONSTRAINT CK_CTHD_ThanhTien;
    PRINT N'✅ Đã drop CK_CTHD_ThanhTien (cho phép ThanhTien âm khi RETURN)';
END
GO

-- 7. Drop constraint cũ chặn TongTien >= 0
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_HoaDon_TongTien')
BEGIN
    ALTER TABLE HoaDon DROP CONSTRAINT CK_HoaDon_TongTien;
    PRINT N'✅ Đã drop CK_HoaDon_TongTien (cho phép TongTien âm khi RETURN)';
END
GO

-- ============================================================
-- Stored Procedure: sp_TraHangKhach
-- Xử lý trả hàng từ hóa đơn gốc, hỗ trợ trả nhiều lô
--
-- @MaHDGoc     : Mã hóa đơn gốc
-- @MaND        : Mã nhân viên xử lý
-- @LyDo        : Lý do trả hàng
-- @ChiTietTra  : XML chứa danh sách lô + số lượng trả
--   Format: <items><i maLo="1" maSP="2" soLuong="3" donGia="50000"/></items>
-- ============================================================
IF OBJECT_ID('sp_TraHangKhach', 'P') IS NOT NULL
    DROP PROCEDURE sp_TraHangKhach;
GO

SET ANSI_NULLS ON;
GO
SET QUOTED_IDENTIFIER ON;
GO

CREATE PROCEDURE sp_TraHangKhach
    @MaHDGoc    INT,
    @MaND       INT,
    @LyDo       NVARCHAR(500),
    @ChiTietTra XML
AS
BEGIN
    SET NOCOUNT ON;

    BEGIN TRY
        BEGIN TRANSACTION;

        -- 1. Validate hóa đơn gốc
        IF NOT EXISTS (
            SELECT 1 FROM HoaDon
            WHERE MaHD = @MaHDGoc
              AND LoaiHD = N'SALE'
              AND TrangThai = N'Thanh cong'
        )
        BEGIN
            RAISERROR(N'Hóa đơn gốc không hợp lệ hoặc đã bị hủy.', 16, 1);
        END;

        -- 2. Parse XML → temp table
        DECLARE @Items TABLE (
            MaLo    INT,
            MaSP    INT,
            SoLuong INT,
            DonGia  DECIMAL(18,0)
        );

        INSERT INTO @Items (MaLo, MaSP, SoLuong, DonGia)
        SELECT
            item.value('@maLo', 'INT'),
            item.value('@maSP', 'INT'),
            item.value('@soLuong', 'INT'),
            item.value('@donGia', 'DECIMAL(18,0)')
        FROM @ChiTietTra.nodes('/items/i') AS T(item);

        -- 3. Validate: SL trả <= SL đã mua từng lô (trừ SL đã trả trước đó)
        IF EXISTS (
            SELECT 1
            FROM @Items it
            LEFT JOIN ChiTietHoaDon ct
                ON ct.MaHD = @MaHDGoc AND ct.MaLo = it.MaLo AND ct.MaSP = it.MaSP
            LEFT JOIN (
                -- Tổng SL đã trả trước đó cho cùng lô/SP từ cùng HĐ gốc
                SELECT ct2.MaLo, ct2.MaSP, SUM(ABS(ct2.SoLuong)) AS DaTra
                FROM ChiTietHoaDon ct2
                JOIN HoaDon hd2 ON ct2.MaHD = hd2.MaHD
                WHERE hd2.MaHDGoc = @MaHDGoc AND hd2.LoaiHD = N'RETURN'
                GROUP BY ct2.MaLo, ct2.MaSP
            ) prev ON prev.MaLo = it.MaLo AND prev.MaSP = it.MaSP
            WHERE it.SoLuong > (ISNULL(ct.SoLuong, 0) - ISNULL(prev.DaTra, 0))
        )
        BEGIN
            RAISERROR(N'Số lượng trả vượt quá số lượng còn lại có thể trả!', 16, 1);
        END;

        -- 4. Tính tổng tiền trả (âm)
        DECLARE @TongTienTra DECIMAL(18,0);
        SELECT @TongTienTra = -SUM(SoLuong * DonGia) FROM @Items;

        -- 5. INSERT HoaDon trả (RETURN)
        DECLARE @MaKH INT;
        SELECT @MaKH = MaKH FROM HoaDon WHERE MaHD = @MaHDGoc;

        INSERT INTO HoaDon (MaKH, MaND, NgayBan, TongTien, PhuongThucTT, TrangThai, LoaiHD, MaHDGoc, LyDoHuy)
        VALUES (@MaKH, @MaND, GETDATE(), @TongTienTra, N'TienMat', N'Thanh cong', N'RETURN', @MaHDGoc, @LyDo);

        DECLARE @MaHDTra INT = SCOPE_IDENTITY();

        -- 6. INSERT ChiTietHoaDon (SoLuong âm, ThanhTien âm)
        INSERT INTO ChiTietHoaDon (MaHD, MaLo, MaSP, SoLuong, DonGia, ThanhTien, GiaVon)
        SELECT
            @MaHDTra,
            it.MaLo,
            it.MaSP,
            -it.SoLuong,           -- SL âm
            it.DonGia,
            -(it.SoLuong * it.DonGia),  -- ThanhTien âm
            ISNULL(l.GiaNhap, 0)   -- GiaVon snapshot
        FROM @Items it
        JOIN LoHang l ON it.MaLo = l.MaLo;

        -- 7. UPDATE LoHang: hoàn trả tồn kho
        UPDATE l
        SET l.SoLuong = l.SoLuong + it.SoLuong
        FROM LoHang l
        JOIN @Items it ON l.MaLo = it.MaLo;

        COMMIT TRANSACTION;

        -- Return MaHD trả
        SELECT @MaHDTra AS MaHDTra;

    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;

        DECLARE @ErrMsg NVARCHAR(4000) = ERROR_MESSAGE();
        RAISERROR(@ErrMsg, 16, 1);
    END CATCH;
END;
GO

PRINT N'✅ Migration 12: Trả hàng khách — HOÀN TẤT';
