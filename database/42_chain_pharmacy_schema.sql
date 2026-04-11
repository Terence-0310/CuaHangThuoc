USE QuanLyCuaHangThuoc;
GO

/* ============================================================
   CHAIN PHARMACY CORE SCHEMA
   ============================================================ */

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'KhuVuc')
BEGIN
    CREATE TABLE KhuVuc (
        MaKhuVuc     INT IDENTITY(1,1) PRIMARY KEY,
        MaKhuVucCode NVARCHAR(30) NOT NULL,
        TenKhuVuc    NVARCHAR(120) NOT NULL,
        TinhThanh    NVARCHAR(100) NULL,
        TrangThai    BIT NOT NULL DEFAULT 1,
        NgayTao      DATETIME NOT NULL DEFAULT GETDATE(),
        CONSTRAINT UQ_KhuVuc_Code UNIQUE (MaKhuVucCode)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'ChiNhanh')
BEGIN
    CREATE TABLE ChiNhanh (
        MaCN         INT IDENTITY(1,1) PRIMARY KEY,
        MaCNCode     NVARCHAR(30) NOT NULL,
        TenChiNhanh  NVARCHAR(150) NOT NULL,
        DiaChi       NVARCHAR(255) NULL,
        QuanHuyen    NVARCHAR(120) NULL,
        MaKhuVuc     INT NOT NULL,
        TrangThai    BIT NOT NULL DEFAULT 1,
        NgayTao      DATETIME NOT NULL DEFAULT GETDATE(),
        CONSTRAINT UQ_ChiNhanh_Code UNIQUE (MaCNCode),
        CONSTRAINT FK_ChiNhanh_KhuVuc FOREIGN KEY (MaKhuVuc) REFERENCES KhuVuc(MaKhuVuc)
    );
END
GO

/* Compatibility: if ChiNhanh was created by other migration, add missing columns */
IF COL_LENGTH('ChiNhanh', 'MaCNCode') IS NULL
BEGIN
    ALTER TABLE ChiNhanh ADD MaCNCode NVARCHAR(30) NULL;
END
GO

IF COL_LENGTH('ChiNhanh', 'DiaChi') IS NULL
BEGIN
    ALTER TABLE ChiNhanh ADD DiaChi NVARCHAR(255) NULL;
END
GO

IF COL_LENGTH('ChiNhanh', 'MaKhuVuc') IS NULL
BEGIN
    ALTER TABLE ChiNhanh ADD MaKhuVuc INT NULL;
END
GO

IF COL_LENGTH('ChiNhanh', 'TenChiNhanh') IS NULL
BEGIN
    ALTER TABLE ChiNhanh ADD TenChiNhanh NVARCHAR(150) NULL;
END
GO

IF COL_LENGTH('ChiNhanh', 'TrangThai') IS NULL
BEGIN
    ALTER TABLE ChiNhanh ADD TrangThai BIT NOT NULL CONSTRAINT DF_ChiNhanh_TrangThai DEFAULT 1;
END
GO

IF COL_LENGTH('ChiNhanh', 'NgayTao') IS NULL
BEGIN
    ALTER TABLE ChiNhanh ADD NgayTao DATETIME NOT NULL CONSTRAINT DF_ChiNhanh_NgayTao DEFAULT GETDATE();
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('NguoiDung') AND name = 'MaCN')
BEGIN
    ALTER TABLE NguoiDung ADD MaCN INT NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_NguoiDung_ChiNhanh')
BEGIN
    ALTER TABLE NguoiDung
    ADD CONSTRAINT FK_NguoiDung_ChiNhanh FOREIGN KEY (MaCN) REFERENCES ChiNhanh(MaCN);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'MaCN')
BEGIN
    ALTER TABLE HoaDon ADD MaCN INT NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_HoaDon_ChiNhanh')
BEGIN
    ALTER TABLE HoaDon
    ADD CONSTRAINT FK_HoaDon_ChiNhanh FOREIGN KEY (MaCN) REFERENCES ChiNhanh(MaCN);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('PhieuNhap') AND name = 'MaCN')
BEGIN
    ALTER TABLE PhieuNhap ADD MaCN INT NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_PhieuNhap_ChiNhanh')
BEGIN
    ALTER TABLE PhieuNhap
    ADD CONSTRAINT FK_PhieuNhap_ChiNhanh FOREIGN KEY (MaCN) REFERENCES ChiNhanh(MaCN);
END
GO

/* Ensure KhuVuc relation can be linked for pre-existing ChiNhanh rows */
IF EXISTS (SELECT 1 FROM sys.tables WHERE name = 'KhuVuc')
BEGIN
    DECLARE @DefaultRegionIdCompat INT;
    SELECT TOP 1 @DefaultRegionIdCompat = MaKhuVuc FROM KhuVuc ORDER BY MaKhuVuc;
    IF @DefaultRegionIdCompat IS NOT NULL
    BEGIN
        UPDATE ChiNhanh
        SET MaKhuVuc = @DefaultRegionIdCompat
        WHERE MaKhuVuc IS NULL;
    END
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_ChiNhanh_KhuVuc')
BEGIN
    IF COL_LENGTH('ChiNhanh', 'MaKhuVuc') IS NOT NULL
    BEGIN
        ALTER TABLE ChiNhanh
        ADD CONSTRAINT FK_ChiNhanh_KhuVuc FOREIGN KEY (MaKhuVuc) REFERENCES KhuVuc(MaKhuVuc);
    END
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'TonKhoChiNhanh')
BEGIN
    CREATE TABLE TonKhoChiNhanh (
        MaCN             INT NOT NULL,
        MaSP             INT NOT NULL,
        TonHienTai       INT NOT NULL DEFAULT 0,
        MucTonToiThieu   INT NOT NULL DEFAULT 0,
        MucTonMucTieu    INT NOT NULL DEFAULT 0,
        SoNgayLeadTime   INT NOT NULL DEFAULT 2,
        SoNgayTonAnToan  INT NOT NULL DEFAULT 7,
        CapNhatLuc       DATETIME NOT NULL DEFAULT GETDATE(),
        CONSTRAINT PK_TonKhoChiNhanh PRIMARY KEY (MaCN, MaSP),
        CONSTRAINT FK_TonKhoChiNhanh_ChiNhanh FOREIGN KEY (MaCN) REFERENCES ChiNhanh(MaCN),
        CONSTRAINT FK_TonKhoChiNhanh_SanPham FOREIGN KEY (MaSP) REFERENCES SanPham(MaSP),
        CONSTRAINT CK_TonKhoChiNhanh_Values CHECK (
            TonHienTai >= 0
            AND MucTonToiThieu >= 0
            AND MucTonMucTieu >= 0
            AND SoNgayLeadTime > 0
            AND SoNgayTonAnToan >= 0
        )
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'DieuChuyenKho')
BEGIN
    CREATE TABLE DieuChuyenKho (
        MaDieuChuyen     INT IDENTITY(1,1) PRIMARY KEY,
        MaCNTu           INT NOT NULL,
        MaCNDen          INT NOT NULL,
        TrangThai        NVARCHAR(30) NOT NULL DEFAULT N'DeXuat',
        GhiChu           NVARCHAR(400) NULL,
        NgayTao          DATETIME NOT NULL DEFAULT GETDATE(),
        MaNDTao          INT NULL,
        CONSTRAINT FK_DieuChuyenKho_CNTu FOREIGN KEY (MaCNTu) REFERENCES ChiNhanh(MaCN),
        CONSTRAINT FK_DieuChuyenKho_CNDen FOREIGN KEY (MaCNDen) REFERENCES ChiNhanh(MaCN),
        CONSTRAINT FK_DieuChuyenKho_NguoiTao FOREIGN KEY (MaNDTao) REFERENCES NguoiDung(MaND),
        CONSTRAINT CK_DieuChuyenKho_State CHECK (TrangThai IN (N'DeXuat', N'DaDuyet', N'DaNhan', N'Huy'))
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'DieuChuyenKhoChiTiet')
BEGIN
    CREATE TABLE DieuChuyenKhoChiTiet (
        MaDieuChuyenCT   INT IDENTITY(1,1) PRIMARY KEY,
        MaDieuChuyen     INT NOT NULL,
        MaSP             INT NOT NULL,
        SoLuong          INT NOT NULL,
        CONSTRAINT FK_DieuChuyenKhoCT_Header FOREIGN KEY (MaDieuChuyen) REFERENCES DieuChuyenKho(MaDieuChuyen),
        CONSTRAINT FK_DieuChuyenKhoCT_SanPham FOREIGN KEY (MaSP) REFERENCES SanPham(MaSP),
        CONSTRAINT CK_DieuChuyenKhoCT_SoLuong CHECK (SoLuong > 0)
    );
END
GO

/* Seed minimum one region/branch for backward compatibility */
IF NOT EXISTS (SELECT 1 FROM KhuVuc WHERE MaKhuVucCode = N'KV-MACDINH')
BEGIN
    INSERT INTO KhuVuc (MaKhuVucCode, TenKhuVuc, TinhThanh)
    VALUES (N'KV-MACDINH', N'Khu vuc mac dinh', N'Ho Chi Minh');
END
GO

IF COL_LENGTH('ChiNhanh', 'MaCNCode') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM ChiNhanh WHERE MaCNCode = N'CN-001')
BEGIN
    DECLARE @DefaultRegionId INT;
    SELECT TOP 1 @DefaultRegionId = MaKhuVuc FROM KhuVuc WHERE MaKhuVucCode = N'KV-MACDINH';

    INSERT INTO ChiNhanh (MaCNCode, TenChiNhanh, DiaChi, QuanHuyen, MaKhuVuc, MaVung, TinhThanh, TrangThai)
    VALUES (N'CN-001', N'Chi nhanh trung tam', N'Chi nhanh khoi tao migration', N'Quan 1', @DefaultRegionId, N'R-01', N'Ho Chi Minh', 1);
END
GO

/* backfill nullable references */
DECLARE @DefaultBranchId INT;
SELECT TOP 1 @DefaultBranchId = MaCN FROM ChiNhanh WHERE MaCNCode = N'CN-001';

UPDATE NguoiDung SET MaCN = @DefaultBranchId WHERE MaCN IS NULL;
UPDATE HoaDon SET MaCN = @DefaultBranchId WHERE MaCN IS NULL;
UPDATE PhieuNhap SET MaCN = @DefaultBranchId WHERE MaCN IS NULL;
GO

/* ============================================================
   DATA MART LAYER
   ============================================================ */
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'DimTime')
BEGIN
    CREATE TABLE DimTime (
        DateKey       INT PRIMARY KEY,    -- yyyyMMdd
        FullDate      DATE NOT NULL,
        DayInMonth    INT NOT NULL,
        MonthNum      INT NOT NULL,
        QuarterNum    INT NOT NULL,
        YearNum       INT NOT NULL,
        WeekOfYear    INT NOT NULL,
        DayOfWeekNum  INT NOT NULL
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'FactSalesDaily')
BEGIN
    CREATE TABLE FactSalesDaily (
        DateKey             INT NOT NULL,
        MaSP                INT NOT NULL,
        MaCN                INT NOT NULL,
        SoLuongBan          INT NOT NULL,
        DoanhThu            DECIMAL(18,0) NOT NULL,
        GiaVonUocTinh       DECIMAL(18,0) NOT NULL DEFAULT 0,
        SoHoaDon            INT NOT NULL DEFAULT 0,
        CONSTRAINT PK_FactSalesDaily PRIMARY KEY (DateKey, MaSP, MaCN),
        CONSTRAINT FK_FactSalesDaily_Time FOREIGN KEY (DateKey) REFERENCES DimTime(DateKey),
        CONSTRAINT FK_FactSalesDaily_Product FOREIGN KEY (MaSP) REFERENCES SanPham(MaSP),
        CONSTRAINT FK_FactSalesDaily_Branch FOREIGN KEY (MaCN) REFERENCES ChiNhanh(MaCN)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'FactInventoryDaily')
BEGIN
    CREATE TABLE FactInventoryDaily (
        DateKey             INT NOT NULL,
        MaSP                INT NOT NULL,
        MaCN                INT NOT NULL,
        TonCuoiNgay         INT NOT NULL,
        GiaTriTonUocTinh    DECIMAL(18,0) NOT NULL DEFAULT 0,
        CONSTRAINT PK_FactInventoryDaily PRIMARY KEY (DateKey, MaSP, MaCN),
        CONSTRAINT FK_FactInventoryDaily_Time FOREIGN KEY (DateKey) REFERENCES DimTime(DateKey),
        CONSTRAINT FK_FactInventoryDaily_Product FOREIGN KEY (MaSP) REFERENCES SanPham(MaSP),
        CONSTRAINT FK_FactInventoryDaily_Branch FOREIGN KEY (MaCN) REFERENCES ChiNhanh(MaCN)
    );
END
GO

CREATE OR ALTER VIEW vw_DemandByRegionDaily AS
SELECT
    f.DateKey,
    kv.MaKhuVuc,
    kv.TenKhuVuc,
    f.MaSP,
    SUM(f.SoLuongBan) AS NhuCau
FROM FactSalesDaily f
JOIN ChiNhanh cn ON f.MaCN = cn.MaCN
JOIN KhuVuc kv ON cn.MaKhuVuc = kv.MaKhuVuc
GROUP BY f.DateKey, kv.MaKhuVuc, kv.TenKhuVuc, f.MaSP;
GO

PRINT N'42_chain_pharmacy_schema.sql executed successfully';
GO
