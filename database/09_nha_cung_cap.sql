-- ============================================================
-- FILE: 09_nha_cung_cap.sql
-- MO TA: Thêm bảng NhaCungCap + FK vào PhieuNhap
-- ============================================================

USE QuanLyCuaHangThuoc;
GO

-- ============================================================
-- 1. Tạo bảng NhaCungCap
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'NhaCungCap')
BEGIN
    CREATE TABLE NhaCungCap (
        MaNCC     INT IDENTITY(1,1) PRIMARY KEY,
        TenNCC    NVARCHAR(200)  NOT NULL,
        SoDT      VARCHAR(15)    NULL,
        DiaChi    NVARCHAR(300)  NULL,
        Email     VARCHAR(100)   NULL,
        TrangThai BIT            NOT NULL DEFAULT 1,   -- 1=Active, 0=Ngừng hợp tác
        NgayTao   DATETIME       NOT NULL DEFAULT GETDATE()
    );
    PRINT N'✅ Tạo bảng NhaCungCap';
END
GO

-- ============================================================
-- 2. Thêm MaNCC vào PhieuNhap
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('PhieuNhap') AND name = 'MaNCC')
BEGIN
    ALTER TABLE PhieuNhap ADD MaNCC INT NULL;
    PRINT N'✅ Thêm cột MaNCC vào PhieuNhap';
END
GO

-- ============================================================
-- 3. Seed NCC mẫu
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM NhaCungCap)
BEGIN
    INSERT INTO NhaCungCap (TenNCC, SoDT, DiaChi, Email) VALUES
    (N'Dược Hậu Giang (DHG)',     '02923891433', N'288 Bis Nguyễn Văn Cừ, Cần Thơ',       'info@dhgpharma.com.vn'),
    (N'Pymepharco',                '02573822051', N'166 Nguyễn Huệ, Phú Yên',              'info@pymepharco.com'),
    (N'Traphaco',                  '02438543076', N'75 Yên Ninh, Ba Đình, Hà Nội',         'info@traphaco.com.vn'),
    (N'Dược phẩm Imexpharm',      '02773823305', N'04 Đường 30/4, Cao Lãnh, Đồng Tháp',   'info@imexpharm.com'),
    (N'Dược phẩm OPC',            '02838460135', N'1017 Hồng Bàng, Q6, TP.HCM',           'info@opcpharma.com');
    PRINT N'✅ Seed 5 NCC mẫu';
END
GO

-- ============================================================
-- 4. FK PhieuNhap → NhaCungCap
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_PhieuNhap_NhaCungCap')
BEGIN
    ALTER TABLE PhieuNhap ADD CONSTRAINT FK_PhieuNhap_NhaCungCap
        FOREIGN KEY (MaNCC) REFERENCES NhaCungCap(MaNCC);
    PRINT N'✅ FK PhieuNhap → NhaCungCap';
END
GO

-- Backfill: gán NCC đầu tiên cho phiếu nhập cũ
UPDATE PhieuNhap SET MaNCC = 1 WHERE MaNCC IS NULL;
PRINT N'✅ Backfill MaNCC cho PhieuNhap cũ';
GO

-- ============================================================
-- VERIFY
-- ============================================================
PRINT N'';
PRINT N'=== VERIFY ===';
SELECT MaNCC, TenNCC, SoDT FROM NhaCungCap;
SELECT MaPN, MaND, MaNCC, TongTien FROM PhieuNhap;
PRINT N'✅ Migration 09 hoàn tất!';
GO
