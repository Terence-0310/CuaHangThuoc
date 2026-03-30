-- ============================================================
-- FILE: 03_stored_procedures.sql
-- MO TA: View + Stored Procedures
-- ============================================================

USE QuanLyCuaHangThuoc;
GO

-- ============================================================
-- VIEW 1: Ton kho chi tiet
-- ============================================================
CREATE OR ALTER VIEW vw_TonKhoChiTiet AS
SELECT
    sp.MaSP,
    sp.TenSP,
    sp.DonViTinh,
    sp.GiaNhap,
    sp.GiaBan,
    lh.MaLo,
    lh.SoLo,
    lh.HanSuDung,
    lh.SoLuong     AS SoLuongConLai,
    lh.NgayNhap,
    CASE
        WHEN lh.HanSuDung <= GETDATE() THEN N'ĐÃ HẾT HẠN'
        WHEN lh.HanSuDung <= DATEADD(MONTH, 3, GETDATE()) THEN N'SẮP HẾT HẠN'
        ELSE N'Còn hạn'
    END AS TrangThaiHSD
FROM LoHang lh
INNER JOIN SanPham sp ON lh.MaSP = sp.MaSP
WHERE lh.SoLuong > 0 AND sp.TrangThai = 1;
GO

-- ============================================================
-- VIEW 2: Ton kho theo san pham (gop tat ca lo)
-- ============================================================
CREATE OR ALTER VIEW vw_TonKhoTheoSanPham AS
SELECT
    sp.MaSP,
    sp.TenSP,
    sp.DonViTinh,
    sp.GiaNhap,
    sp.GiaBan,
    sp.TrangThai,
    ISNULL(SUM(lh.SoLuong), 0) AS TongTonKho
FROM SanPham sp
LEFT JOIN LoHang lh ON sp.MaSP = lh.MaSP AND lh.SoLuong > 0
GROUP BY sp.MaSP, sp.TenSP, sp.DonViTinh, sp.GiaNhap, sp.GiaBan, sp.TrangThai;
GO

-- ============================================================
-- SP 1: Doanh thu
-- ============================================================
CREATE OR ALTER PROCEDURE sp_DoanhThu
    @LoaiThoiGian NVARCHAR(10)
AS
BEGIN
    SET NOCOUNT ON;

    IF @LoaiThoiGian = 'TODAY'
        SELECT ISNULL(SUM(TongTien), 0) AS DoanhThu
        FROM HoaDon
        WHERE CAST(NgayBan AS DATE) = CAST(GETDATE() AS DATE);

    ELSE IF @LoaiThoiGian = 'MONTH'
        SELECT ISNULL(SUM(TongTien), 0) AS DoanhThu
        FROM HoaDon
        WHERE MONTH(NgayBan) = MONTH(GETDATE())
          AND YEAR(NgayBan) = YEAR(GETDATE());

    ELSE IF @LoaiThoiGian = 'QUARTER'
        SELECT ISNULL(SUM(TongTien), 0) AS DoanhThu
        FROM HoaDon
        WHERE DATEPART(QUARTER, NgayBan) = DATEPART(QUARTER, GETDATE())
          AND YEAR(NgayBan) = YEAR(GETDATE());
END;
GO

-- ============================================================
-- SP 2: Canh bao het han (<= 3 thang)
-- ============================================================
CREATE OR ALTER PROCEDURE sp_CanhBaoHetHan
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        sp.TenSP,
        sp.DonViTinh,
        lh.SoLo,
        lh.HanSuDung,
        lh.SoLuong AS SoLuongConLai,
        DATEDIFF(DAY, GETDATE(), lh.HanSuDung) AS SoNgayConLai,
        CASE
            WHEN lh.HanSuDung <= GETDATE() THEN N'DA HET HAN'
            WHEN DATEDIFF(DAY, GETDATE(), lh.HanSuDung) <= 30 THEN N'< 1 thang'
            ELSE N'< 3 thang'
        END AS MucDoKhanCap
    FROM LoHang lh
    INNER JOIN SanPham sp ON lh.MaSP = sp.MaSP
    WHERE lh.SoLuong > 0
      AND lh.HanSuDung <= DATEADD(MONTH, 3, GETDATE())
    ORDER BY lh.HanSuDung ASC;
END;
GO

-- ============================================================
-- SP 3: Top N ban chay
-- ============================================================
CREATE OR ALTER PROCEDURE sp_TopBanChay
    @TopN INT = 5
AS
BEGIN
    SET NOCOUNT ON;

    SELECT TOP (@TopN)
        sp.TenSP,
        sp.DonViTinh,
        SUM(ct.SoLuong) AS TongSoLuongBan,
        SUM(ct.ThanhTien) AS TongDoanhThu
    FROM ChiTietHoaDon ct
    INNER JOIN SanPham sp ON ct.MaSP = sp.MaSP
    GROUP BY sp.TenSP, sp.DonViTinh
    ORDER BY TongSoLuongBan DESC;
END;
GO

-- ============================================================
-- SP 4: Top N khach VIP
-- ============================================================
CREATE OR ALTER PROCEDURE sp_TopKhachVIP
    @TopN INT = 5
AS
BEGIN
    SET NOCOUNT ON;

    SELECT TOP (@TopN)
        kh.TenKH,
        kh.SoDT,
        COUNT(DISTINCT hd.MaHD) AS SoLanMua,
        SUM(hd.TongTien) AS TongTienMua
    FROM HoaDon hd
    INNER JOIN KhachHang kh ON hd.MaKH = kh.MaKH
    GROUP BY kh.MaKH, kh.TenKH, kh.SoDT
    ORDER BY TongTienMua DESC;
END;
GO

-- ============================================================
-- SP 5: FEFO - First Expired First Out
-- ============================================================
CREATE OR ALTER PROCEDURE sp_GetFEFO
    @MaSP INT
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        MaLo,
        MaSP,
        SoLo,
        HanSuDung,
        SoLuong
    FROM LoHang
    WHERE MaSP = @MaSP
      AND SoLuong > 0
      AND HanSuDung > GETDATE()
    ORDER BY HanSuDung ASC;
END;
GO

-- ============================================================
-- SP 6: Lich su mua hang cua khach
-- ============================================================
CREATE OR ALTER PROCEDURE sp_LichSuKhachHang
    @MaKH INT
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        hd.MaHD,
        hd.NgayBan,
        hd.TongTien,
        nd.HoTen AS NhanVienBan,
        (SELECT COUNT(*) FROM ChiTietHoaDon WHERE MaHD = hd.MaHD) AS SoMatHang
    FROM HoaDon hd
    INNER JOIN NguoiDung nd ON hd.MaND = nd.MaND
    WHERE hd.MaKH = @MaKH
    ORDER BY hd.NgayBan DESC;
END;
GO

-- ============================================================
-- SP 7: Chi tiet 1 hoa don
-- ============================================================
CREATE OR ALTER PROCEDURE sp_ChiTietHoaDon
    @MaHD INT
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        ct.MaCTHD,
        sp.TenSP,
        sp.DonViTinh,
        lh.SoLo,
        ct.SoLuong,
        ct.DonGia,
        ct.ThanhTien
    FROM ChiTietHoaDon ct
    INNER JOIN SanPham sp ON ct.MaSP = sp.MaSP
    INNER JOIN LoHang lh ON ct.MaLo = lh.MaLo
    WHERE ct.MaHD = @MaHD
    ORDER BY ct.MaCTHD;
END;
GO

-- ============================================================
-- SP 8: Tim kiem san pham (cho POS)
-- ============================================================
CREATE OR ALTER PROCEDURE sp_TimSanPham
    @Keyword NVARCHAR(100)
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        sp.MaSP,
        sp.TenSP,
        sp.DonViTinh,
        sp.GiaBan,
        ISNULL(SUM(lh.SoLuong), 0) AS TonKho
    FROM SanPham sp
    LEFT JOIN LoHang lh ON sp.MaSP = lh.MaSP
        AND lh.SoLuong > 0
        AND lh.HanSuDung > GETDATE()
    WHERE sp.TrangThai = 1
      AND sp.TenSP LIKE N'%' + @Keyword + N'%'
    GROUP BY sp.MaSP, sp.TenSP, sp.DonViTinh, sp.GiaBan
    ORDER BY sp.TenSP;
END;
GO

PRINT N'Tao View va Stored Procedures thanh cong!';
GO
