-- ============================================================
-- FILE: 13_hrm_module.sql
-- MO TA: Tao cac bang cho Module Quan ly Nhan su (HRM)
-- DBMS:  SQL Server
-- ============================================================

USE QuanLyCuaHangThuoc;
GO

-- 1. Bảng mở rộng thông tin Nhân Viên (lấy NguoiDung làm lõi)
CREATE TABLE ThongTinNhanSu (
    MaND INT PRIMARY KEY, -- Khóa ngoại trỏ về NguoiDung
    LuongCoBan DECIMAL(18,0) NOT NULL DEFAULT 0, -- Lương cơ bản theo giờ hoặc tháng tùy cấu hình
    LoaiHinh NVARCHAR(50) NOT NULL DEFAULT N'Full-time', -- Full-time, Part-time
    NgayVaoLam DATE NOT NULL DEFAULT GETDATE(),
    
    CONSTRAINT FK_ThongTinNhanSu_NguoiDung FOREIGN KEY (MaND) REFERENCES NguoiDung(MaND)
);
GO

-- 2. Bảng Chấm Công (Check-in/Check-out)
CREATE TABLE ChamCong (
    MaCC INT IDENTITY(1,1) PRIMARY KEY,
    MaND INT NOT NULL,
    NgayLamViec DATE NOT NULL DEFAULT GETDATE(),
    GioVao DATETIME NOT NULL, -- Timestamp lúc check in
    GioRa DATETIME NULL,      -- Timestamp lúc check out
    SoGioLam DECIMAL(5,2) NULL, -- Tính tự động = (GioRa - GioVao) ra giờ
    TrangThai NVARCHAR(50) NOT NULL DEFAULT N'Đang ca', -- Đang ca, Hoàn thành, Thiếu checkout
    GhiChu NVARCHAR(255) NULL,
    
    CONSTRAINT FK_ChamCong_NguoiDung FOREIGN KEY (MaND) REFERENCES NguoiDung(MaND)
);
GO

-- 3. Bảng Phạt / Thưởng / Ứng Lương (Deductions & Bonuses)
CREATE TABLE ThuongPhat (
    MaTP INT IDENTITY(1,1) PRIMARY KEY,
    MaND INT NOT NULL,
    NgayGhiNhan DATE NOT NULL DEFAULT GETDATE(),
    Loai NVARCHAR(20) NOT NULL DEFAULT N'Thưởng', -- 'Thưởng', 'Phạt', 'Ứng Lương'
    SoTien DECIMAL(18,0) NOT NULL,
    LyDo NVARCHAR(255) NOT NULL,
    DaTinhLuong BIT NOT NULL DEFAULT 0, -- 1 = Đã gộp vào kỳ lương, 0 = Chưa

    CONSTRAINT FK_ThuongPhat_NguoiDung FOREIGN KEY (MaND) REFERENCES NguoiDung(MaND),
    CONSTRAINT CK_ThuongPhat_Loai CHECK (Loai IN (N'Thưởng', N'Phạt', N'Ứng Lương')),
    CONSTRAINT CK_ThuongPhat_SoTien CHECK (SoTien > 0)
);
GO

-- 4. Bảng Chốt Lương (Payroll)
CREATE TABLE BangLuong (
    MaBL INT IDENTITY(1,1) PRIMARY KEY,
    MaND INT NOT NULL,
    KyLuongThang INT NOT NULL, -- VD: 1, 2... 12
    KyLuongNam INT NOT NULL,   -- VD: 2026
    TongGioLam DECIMAL(8,2) NOT NULL DEFAULT 0,
    LuongCoBan DECIMAL(18,0) NOT NULL,
    Thuong DECIMAL(18,0) NOT NULL DEFAULT 0,
    Phat DECIMAL(18,0) NOT NULL DEFAULT 0,
    UngLuong DECIMAL(18,0) NOT NULL DEFAULT 0,
    ThucLanh DECIMAL(18,0) NOT NULL DEFAULT 0,
    NgayChot DATETIME NOT NULL DEFAULT GETDATE(),
    TrangThai NVARCHAR(50) NOT NULL DEFAULT N'Đã chốt',

    CONSTRAINT FK_BangLuong_NguoiDung FOREIGN KEY (MaND) REFERENCES NguoiDung(MaND),
    CONSTRAINT UQ_BangLuong_KyLuong UNIQUE (MaND, KyLuongThang, KyLuongNam) -- 1 tháng NV chỉ nhận 1 phiếu lương
);
GO

-- Index cho HRM
CREATE NONCLUSTERED INDEX IX_ChamCong_Ngay 
ON ChamCong (NgayLamViec DESC);
GO

CREATE NONCLUSTERED INDEX IX_ChamCong_MaND
ON ChamCong (MaND, NgayLamViec DESC);
GO

CREATE NONCLUSTERED INDEX IX_BangLuong_KyLuong
ON BangLuong (KyLuongNam, KyLuongThang);
GO
