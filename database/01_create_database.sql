-- ============================================================
-- FILE: 01_create_database.sql
-- MO TA: Tao database va tat ca cac bang cho he thong
--         Quan ly Cua hang Thuoc
-- DBMS:  SQL Server
-- ============================================================

USE master;
GO

IF EXISTS (SELECT name FROM sys.databases WHERE name = N'QuanLyCuaHangThuoc')
BEGIN
    ALTER DATABASE QuanLyCuaHangThuoc SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE QuanLyCuaHangThuoc;
END
GO

CREATE DATABASE QuanLyCuaHangThuoc
COLLATE Vietnamese_CI_AS;
GO

USE QuanLyCuaHangThuoc;
GO

-- ------------------------------------------------------------
-- BANG 1: NguoiDung
-- ------------------------------------------------------------
CREATE TABLE NguoiDung (
    MaND        INT IDENTITY(1,1) PRIMARY KEY,
    TenDangNhap NVARCHAR(50)  NOT NULL,
    MatKhau     NVARCHAR(255) NOT NULL,
    HoTen       NVARCHAR(100) NOT NULL,
    VaiTro      NVARCHAR(20)  NOT NULL DEFAULT N'NhanVien',
    TrangThai   BIT           NOT NULL DEFAULT 1,
    NgayTao     DATETIME      NOT NULL DEFAULT GETDATE(),

    CONSTRAINT UQ_NguoiDung_TenDangNhap UNIQUE (TenDangNhap),
    CONSTRAINT CK_NguoiDung_VaiTro CHECK (VaiTro IN (N'Admin', N'NhanVien'))
);
GO

-- ------------------------------------------------------------
-- BANG 2: SanPham
-- ------------------------------------------------------------
CREATE TABLE SanPham (
    MaSP      INT IDENTITY(1,1) PRIMARY KEY,
    TenSP     NVARCHAR(200) NOT NULL,
    DonViTinh NVARCHAR(50)  NOT NULL,
    GiaNhap   DECIMAL(18,0) NOT NULL,
    GiaBan    DECIMAL(18,0) NOT NULL,
    TrangThai BIT           NOT NULL DEFAULT 1,
    NgayTao   DATETIME      NOT NULL DEFAULT GETDATE(),

    CONSTRAINT CK_SanPham_GiaNhap CHECK (GiaNhap >= 0),
    CONSTRAINT CK_SanPham_GiaBan  CHECK (GiaBan >= 0),
    CONSTRAINT CK_SanPham_Gia     CHECK (GiaBan >= GiaNhap)
);
GO

-- ------------------------------------------------------------
-- BANG 3: LoHang
-- ------------------------------------------------------------
CREATE TABLE LoHang (
    MaLo      INT IDENTITY(1,1) PRIMARY KEY,
    MaSP      INT           NOT NULL,
    SoLo      NVARCHAR(50)  NOT NULL,
    HanSuDung DATE          NOT NULL,
    SoLuong   INT           NOT NULL DEFAULT 0,
    NgayNhap  DATETIME      NOT NULL DEFAULT GETDATE(),

    CONSTRAINT FK_LoHang_SanPham FOREIGN KEY (MaSP) REFERENCES SanPham(MaSP),
    CONSTRAINT CK_LoHang_SoLuong CHECK (SoLuong >= 0)
);
GO

-- ------------------------------------------------------------
-- BANG 4: KhachHang
-- ------------------------------------------------------------
CREATE TABLE KhachHang (
    MaKH    INT IDENTITY(1,1) PRIMARY KEY,
    SoDT    VARCHAR(15)   NOT NULL,
    TenKH   NVARCHAR(100) NULL,
    NgayTao DATETIME      NOT NULL DEFAULT GETDATE(),

    CONSTRAINT UQ_KhachHang_SoDT UNIQUE (SoDT)
);
GO

-- ------------------------------------------------------------
-- BANG 5: HoaDon
-- ------------------------------------------------------------
CREATE TABLE HoaDon (
    MaHD     INT IDENTITY(1,1) PRIMARY KEY,
    MaKH     INT           NULL,
    MaND     INT           NOT NULL,
    NgayBan  DATETIME      NOT NULL DEFAULT GETDATE(),
    TongTien DECIMAL(18,0) NOT NULL DEFAULT 0,

    CONSTRAINT FK_HoaDon_KhachHang FOREIGN KEY (MaKH) REFERENCES KhachHang(MaKH),
    CONSTRAINT FK_HoaDon_NguoiDung FOREIGN KEY (MaND) REFERENCES NguoiDung(MaND),
    CONSTRAINT CK_HoaDon_TongTien CHECK (TongTien >= 0)
);
GO

-- ------------------------------------------------------------
-- BANG 6: ChiTietHoaDon
-- ------------------------------------------------------------
CREATE TABLE ChiTietHoaDon (
    MaCTHD    INT IDENTITY(1,1) PRIMARY KEY,
    MaHD      INT           NOT NULL,
    MaLo      INT           NOT NULL,
    MaSP      INT           NOT NULL,
    SoLuong   INT           NOT NULL,
    DonGia    DECIMAL(18,0) NOT NULL,
    ThanhTien DECIMAL(18,0) NOT NULL,

    CONSTRAINT FK_CTHD_HoaDon  FOREIGN KEY (MaHD) REFERENCES HoaDon(MaHD),
    CONSTRAINT FK_CTHD_LoHang  FOREIGN KEY (MaLo) REFERENCES LoHang(MaLo),
    CONSTRAINT FK_CTHD_SanPham FOREIGN KEY (MaSP) REFERENCES SanPham(MaSP),
    CONSTRAINT CK_CTHD_SoLuong   CHECK (SoLuong > 0),
    CONSTRAINT CK_CTHD_DonGia    CHECK (DonGia >= 0),
    CONSTRAINT CK_CTHD_ThanhTien CHECK (ThanhTien >= 0)
);
GO

-- ============================================================
-- INDEXES
-- ============================================================

CREATE NONCLUSTERED INDEX IX_LoHang_FEFO
ON LoHang (MaSP, HanSuDung ASC)
WHERE SoLuong > 0;
GO

CREATE NONCLUSTERED INDEX IX_KhachHang_SoDT
ON KhachHang (SoDT);
GO

CREATE NONCLUSTERED INDEX IX_HoaDon_MaKH
ON HoaDon (MaKH)
WHERE MaKH IS NOT NULL;
GO

CREATE NONCLUSTERED INDEX IX_HoaDon_NgayBan
ON HoaDon (NgayBan DESC);
GO

CREATE NONCLUSTERED INDEX IX_CTHD_MaHD
ON ChiTietHoaDon (MaHD);
GO

CREATE NONCLUSTERED INDEX IX_CTHD_MaSP
ON ChiTietHoaDon (MaSP);
GO

CREATE NONCLUSTERED INDEX IX_SanPham_TrangThai
ON SanPham (TrangThai)
WHERE TrangThai = 1;
GO

CREATE NONCLUSTERED INDEX IX_LoHang_MaSP
ON LoHang (MaSP);
GO

PRINT N'Tao database va tat ca bang thanh cong!';
GO
