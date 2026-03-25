-- ============================================================
-- FILE: 11_tra_hang_huy_hang.sql
-- MO TA: Tạo bảng TraHangNCC, HuyHang cho xử lý lô kho
--        GiaNhapLo = giá nhập CẢ LÔ (không phải đơn giá 1 SP)
-- ============================================================

USE QuanLyCuaHangThuoc;
GO

-- ============================================================
-- BẢNG 1: TraHangNCC (Trả hàng cho nhà cung cấp)
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'TraHangNCC')
BEGIN
    CREATE TABLE TraHangNCC (
        MaTra           INT IDENTITY(1,1) PRIMARY KEY,
        MaLo            INT           NOT NULL,
        MaSP            INT           NOT NULL,
        SoLuongTra      INT           NOT NULL,
        GiaNhapLo       DECIMAL(18,0) NOT NULL DEFAULT 0,  -- Giá nhập CẢ LÔ
        TongTienHoan    DECIMAL(18,0) NOT NULL DEFAULT 0,  -- User tự nhập
        HinhThucHoan    NVARCHAR(50)  NOT NULL DEFAULT N'Tiền mặt',
        LyDo            NVARCHAR(500) NOT NULL,
        MaND            INT           NOT NULL,
        NgayTra         DATETIME      NOT NULL DEFAULT GETDATE(),

        CONSTRAINT FK_TraHang_LoHang    FOREIGN KEY (MaLo) REFERENCES LoHang(MaLo),
        CONSTRAINT FK_TraHang_SanPham   FOREIGN KEY (MaSP) REFERENCES SanPham(MaSP),
        CONSTRAINT FK_TraHang_NguoiDung FOREIGN KEY (MaND) REFERENCES NguoiDung(MaND),
        CONSTRAINT CK_TraHang_SoLuong   CHECK (SoLuongTra > 0)
    );
    PRINT N'✅ Tạo bảng TraHangNCC';
END
ELSE
    PRINT N'✓ Bảng TraHangNCC đã tồn tại';
GO

-- ============================================================
-- BẢNG 2: HuyHang (Hủy hàng / tiêu hủy)
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'HuyHang')
BEGIN
    CREATE TABLE HuyHang (
        MaHuy           INT IDENTITY(1,1) PRIMARY KEY,
        MaLo            INT           NOT NULL,
        MaSP            INT           NOT NULL,
        SoLuongHuy      INT           NOT NULL,
        GiaNhapLo       DECIMAL(18,0) NOT NULL DEFAULT 0,  -- Giá nhập CẢ LÔ
        TongThietHai    DECIMAL(18,0) NOT NULL DEFAULT 0,  -- User tự nhập
        PhanLoaiLyDo    NVARCHAR(100) NOT NULL DEFAULT N'Hết hạn sử dụng',
        ChiTietLyDo     NVARCHAR(500) NOT NULL,
        MaND            INT           NOT NULL,
        NgayHuy         DATETIME      NOT NULL DEFAULT GETDATE(),

        CONSTRAINT FK_HuyHang_LoHang    FOREIGN KEY (MaLo) REFERENCES LoHang(MaLo),
        CONSTRAINT FK_HuyHang_SanPham   FOREIGN KEY (MaSP) REFERENCES SanPham(MaSP),
        CONSTRAINT FK_HuyHang_NguoiDung FOREIGN KEY (MaND) REFERENCES NguoiDung(MaND),
        CONSTRAINT CK_HuyHang_SoLuong   CHECK (SoLuongHuy > 0)
    );
    PRINT N'✅ Tạo bảng HuyHang';
END
ELSE
    PRINT N'✓ Bảng HuyHang đã tồn tại';
GO

-- INDEXES
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_TraHang_MaLo')
    CREATE NONCLUSTERED INDEX IX_TraHang_MaLo ON TraHangNCC (MaLo);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_HuyHang_MaLo')
    CREATE NONCLUSTERED INDEX IX_HuyHang_MaLo ON HuyHang (MaLo);
GO

PRINT N'✅ Hoàn tất';
GO
