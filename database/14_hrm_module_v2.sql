-- ============================================================
-- FILE: 14_hrm_module_v2.sql
-- MO TA: Tao cac bang cho Module Quan ly Nhan su (HRM) V2
--        Co xep ca, IsUnderTime, Seed Data theo yeu cau
-- DBMS:  SQL Server
-- ============================================================

USE QuanLyCuaHangThuoc;
GO

-- Xoa bang cu neu ton tai de tao lai tu dau cho sach se
IF OBJECT_ID('BangLuong', 'U') IS NOT NULL DROP TABLE BangLuong;
IF OBJECT_ID('ThuongPhat', 'U') IS NOT NULL DROP TABLE ThuongPhat;
IF OBJECT_ID('ChamCong', 'U') IS NOT NULL DROP TABLE ChamCong;
IF OBJECT_ID('LichLamViec', 'U') IS NOT NULL DROP TABLE LichLamViec;
IF OBJECT_ID('ThongTinNhanSu', 'U') IS NOT NULL DROP TABLE ThongTinNhanSu;
GO

-- 1. Bảng mở rộng thông tin Nhân Viên (Thêm Lương/Giờ)
CREATE TABLE ThongTinNhanSu (
    MaND INT PRIMARY KEY,
    LuongMotGio DECIMAL(18,0) NOT NULL DEFAULT 20000, -- VD: 20k/h
    LoaiHinh NVARCHAR(50) NOT NULL DEFAULT N'Full-time',
    NgayVaoLam DATE NOT NULL DEFAULT GETDATE(),
    
    CONSTRAINT FK_ThongTinNhanSu_NguoiDung FOREIGN KEY (MaND) REFERENCES NguoiDung(MaND)
);
GO

-- 2. Bảng Xếp Ca (Schedules) - Admin xếp lịch trước
CREATE TABLE LichLamViec (
    MaLich INT IDENTITY(1,1) PRIMARY KEY,
    MaND INT NOT NULL,
    NgayLam DATE NOT NULL,
    CaLam INT NOT NULL, -- 1 = (06:00 - 14:00), 2 = (14:00 - 22:00)
    GhiChu NVARCHAR(255) NULL,

    CONSTRAINT FK_LichLam_NguoiDung FOREIGN KEY (MaND) REFERENCES NguoiDung(MaND),
    CONSTRAINT CK_LichLam_Ca CHECK (CaLam IN (1, 2)),
    CONSTRAINT UQ_LichLam_Nguoi_Ngay_Ca UNIQUE (MaND, NgayLam, CaLam) -- Một người 1 ngày 1 ca hoặc 2 ca khác nhau, ko trùng 1 ca
);
GO

-- 3. Bảng Chấm Công (Attendance)
CREATE TABLE ChamCong (
    MaCC INT IDENTITY(1,1) PRIMARY KEY,
    MaLich INT NOT NULL UNIQUE, -- Mỗi lịch làm việc chỉ có 1 row chấm công tương ứng
    GioVao DATETIME NULL,
    GioRa DATETIME NULL,
    SoGioLam DECIMAL(5,2) NULL,
    IsUnderTime BIT NOT NULL DEFAULT 0, -- 1: < 8 tiếng
    GhiChu NVARCHAR(255) NULL,
    
    CONSTRAINT FK_ChamCong_LichLam FOREIGN KEY (MaLich) REFERENCES LichLamViec(MaLich)
);
GO

-- 4. Bảng Phạt / Thưởng / Ứng Lương (Deductions & Bonuses)
CREATE TABLE ThuongPhat (
    MaTP INT IDENTITY(1,1) PRIMARY KEY,
    MaND INT NOT NULL,
    NgayGhiNhan DATE NOT NULL DEFAULT GETDATE(),
    Loai NVARCHAR(20) NOT NULL DEFAULT N'Thưởng',
    SoTien DECIMAL(18,0) NOT NULL,
    LyDo NVARCHAR(255) NOT NULL,
    DaTinhLuong BIT NOT NULL DEFAULT 0,

    CONSTRAINT FK_ThuongPhat_NguoiDung FOREIGN KEY (MaND) REFERENCES NguoiDung(MaND),
    CONSTRAINT CK_ThuongPhat_Loai CHECK (Loai IN (N'Thưởng', N'Phạt', N'Ứng Lương')),
    CONSTRAINT CK_ThuongPhat_SoTien CHECK (SoTien > 0)
);
GO

-- 5. Bảng Chốt Lương (Payroll)
CREATE TABLE BangLuong (
    MaBL INT IDENTITY(1,1) PRIMARY KEY,
    MaND INT NOT NULL,
    KyLuongThang INT NOT NULL,
    KyLuongNam INT NOT NULL,
    TongGioLam DECIMAL(8,2) NOT NULL DEFAULT 0,
    LuongCoBan DECIMAL(18,0) NOT NULL,
    Thuong DECIMAL(18,0) NOT NULL DEFAULT 0,
    Phat DECIMAL(18,0) NOT NULL DEFAULT 0,
    UngLuong DECIMAL(18,0) NOT NULL DEFAULT 0,
    ThucLanh DECIMAL(18,0) NOT NULL DEFAULT 0,
    NgayChot DATETIME NOT NULL DEFAULT GETDATE(),
    TrangThai NVARCHAR(50) NOT NULL DEFAULT N'Đã chốt',

    CONSTRAINT FK_BangLuong_NguoiDung FOREIGN KEY (MaND) REFERENCES NguoiDung(MaND),
    CONSTRAINT UQ_BangLuong_KyLuong UNIQUE (MaND, KyLuongThang, KyLuongNam)
);
GO

-- ============================================================
-- PHẦN SEED DATA THEO YÊU CẦU
-- ============================================================

-- BƯỚC 2: Cập nhật thông tin nhân sự (HR) - Admin=1, NV01=3, NV02=4
INSERT INTO ThongTinNhanSu (MaND, LuongMotGio, LoaiHinh, NgayVaoLam) VALUES 
(1, 50000, N'Full-time', '2024-01-01'),
(3, 25000, N'Full-time', '2025-05-10'),
(4, 22000, N'Part-time', '2026-01-15');
GO

-- BƯỚC 3: Xếp Lịch Làm Việc (Schedules) cho 2 ngày gần nhất
DECLARE @Today DATE = GETDATE();
DECLARE @Yesterday DATE = DATEADD(DAY, -1, @Today);
DECLARE @Tomorrow DATE = DATEADD(DAY, 1, @Today);

-- Lịch hôm qua
INSERT INTO LichLamViec (MaND, NgayLam, CaLam, GhiChu) VALUES 
(3, @Yesterday, 1, N'Ca 1 NV A'),
(4, @Yesterday, 2, N'Ca 2 NV B');

-- Lịch hôm nay
INSERT INTO LichLamViec (MaND, NgayLam, CaLam, GhiChu) VALUES 
(3, @Today, 1, N'Ca sáng'),
(4, @Today, 2, N'Ca tối');

-- Lịch ngày mai (Chưa đi làm)
INSERT INTO LichLamViec (MaND, NgayLam, CaLam, GhiChu) VALUES 
(3, @Tomorrow, 1, N'Ca sáng'),
(4, @Tomorrow, 2, N'Ca tối');
GO

-- BƯỚC 4: Chấm công (Attendance) giả lập
DECLARE @Yesterday DATE = DATEADD(DAY, -1, GETDATE());
DECLARE @Today DATE = GETDATE();

DECLARE @MaLich_NV3_Yest INT = (SELECT TOP 1 MaLich FROM LichLamViec WHERE MaND=3 AND NgayLam=@Yesterday);
DECLARE @MaLich_NV4_Yest INT = (SELECT TOP 1 MaLich FROM LichLamViec WHERE MaND=4 AND NgayLam=@Yesterday);

-- Hôm qua NV A làm đủ 8.5 tiếng (06:00 -> 14:30)
INSERT INTO ChamCong (MaLich, GioVao, GioRa, SoGioLam, IsUnderTime, GhiChu) 
VALUES (@MaLich_NV3_Yest, DATEADD(HOUR, 6, CAST(@Yesterday AS DATETIME)), DATEADD(MINUTE, 30, DATEADD(HOUR, 14, CAST(@Yesterday AS DATETIME))), 8.5, 0, N'Làm đủ giờ');

-- Hôm qua NV B đi về sớm 2 tiếng rưỡi (14:00 -> 19:30 => 5.5 tiếng) => IsUnderTime = 1
INSERT INTO ChamCong (MaLich, GioVao, GioRa, SoGioLam, IsUnderTime, GhiChu) 
VALUES (@MaLich_NV4_Yest, DATEADD(HOUR, 14, CAST(@Yesterday AS DATETIME)), DATEADD(MINUTE, 30, DATEADD(HOUR, 19, CAST(@Yesterday AS DATETIME))), 5.5, 1, N'Về sớm việc riêng');
GO

PRINT N'Hoàn tất tạo HRM V2!';
GO
