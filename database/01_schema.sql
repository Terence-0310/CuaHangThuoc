-- ============================================================
-- MerPhar — Schema tổng hợp (Tạo Database + Bảng + SP)
-- Chạy file này ĐẦU TIÊN trên SQL Server
-- ============================================================
USE master;
GO
IF EXISTS (SELECT name FROM sys.databases WHERE name = N'QuanLyCuaHangThuoc')
BEGIN
    ALTER DATABASE QuanLyCuaHangThuoc SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE QuanLyCuaHangThuoc;
END
GO
CREATE DATABASE QuanLyCuaHangThuoc COLLATE Vietnamese_CI_AS;
GO
USE QuanLyCuaHangThuoc;
GO

-- ============================================================
-- 1. BẢNG CHÍNH
-- ============================================================

-- 1.1 NguoiDung
CREATE TABLE NguoiDung (
    MaND        INT IDENTITY(1,1) PRIMARY KEY,
    TenDangNhap NVARCHAR(50)  NOT NULL,
    MatKhau     NVARCHAR(255) NOT NULL,
    HoTen       NVARCHAR(100) NOT NULL,
    VaiTro      NVARCHAR(20)  NOT NULL DEFAULT N'NhanVien',
    TrangThai   BIT           NOT NULL DEFAULT 1,
    NgayTao     DATETIME      NOT NULL DEFAULT GETDATE(),
    DangOnline  BIT           NOT NULL DEFAULT 0,
    CONSTRAINT UQ_NguoiDung_TenDangNhap UNIQUE (TenDangNhap),
    CONSTRAINT CK_NguoiDung_VaiTro CHECK (VaiTro IN (N'Admin', N'NhanVien'))
);
GO

-- 1.2 NhaCungCap
CREATE TABLE NhaCungCap (
    MaNCC     INT IDENTITY(1,1) PRIMARY KEY,
    TenNCC    NVARCHAR(200) NOT NULL,
    SoDT      VARCHAR(15)   NULL,
    DiaChi    NVARCHAR(300) NULL,
    Email     VARCHAR(100)  NULL,
    TrangThai BIT           NOT NULL DEFAULT 1,
    NgayTao   DATETIME      NOT NULL DEFAULT GETDATE()
);
GO

-- 1.3 SanPham
CREATE TABLE SanPham (
    MaSP      INT IDENTITY(1,1) PRIMARY KEY,
    TenSP     NVARCHAR(200) NOT NULL,
    DonViTinh NVARCHAR(50)  NOT NULL,
    GiaBan    DECIMAL(18,0) NOT NULL,
    GiaBanSi  DECIMAL(18,0) NULL,
    TrangThai BIT           NOT NULL DEFAULT 1,
    NgayTao   DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT CK_SanPham_GiaBan CHECK (GiaBan >= 0)
);
GO

-- 1.4 PhieuNhap
CREATE TABLE PhieuNhap (
    MaPN     INT IDENTITY(1,1) PRIMARY KEY,
    MaND     INT           NOT NULL,
    NgayNhap DATETIME      NOT NULL DEFAULT GETDATE(),
    TongTien DECIMAL(18,0) NOT NULL DEFAULT 0,
    GhiChu   NVARCHAR(500) NULL,
    MaNCC    INT           NULL,
    CONSTRAINT FK_PhieuNhap_NguoiDung  FOREIGN KEY (MaND)  REFERENCES NguoiDung(MaND),
    CONSTRAINT FK_PhieuNhap_NhaCungCap FOREIGN KEY (MaNCC) REFERENCES NhaCungCap(MaNCC)
);
GO

-- 1.5 LoHang
CREATE TABLE LoHang (
    MaLo      INT IDENTITY(1,1) PRIMARY KEY,
    MaSP      INT           NOT NULL,
    SoLo      NVARCHAR(50)  NOT NULL,
    HanSuDung DATE          NOT NULL,
    SoLuong   INT           NOT NULL DEFAULT 0,
    NgayNhap  DATETIME      NOT NULL DEFAULT GETDATE(),
    GiaNhap   DECIMAL(18,0) NOT NULL DEFAULT 0,
    MaPN      INT           NULL,
    CONSTRAINT FK_LoHang_SanPham   FOREIGN KEY (MaSP) REFERENCES SanPham(MaSP),
    CONSTRAINT FK_LoHang_PhieuNhap FOREIGN KEY (MaPN) REFERENCES PhieuNhap(MaPN),
    CONSTRAINT CK_LoHang_SoLuong   CHECK (SoLuong >= 0)
);
GO

-- 1.6 KhachHang
CREATE TABLE KhachHang (
    MaKH     INT IDENTITY(1,1) PRIMARY KEY,
    SoDT     VARCHAR(15)   NOT NULL,
    TenKH    NVARCHAR(100) NULL,
    NgayTao  DATETIME      NOT NULL DEFAULT GETDATE(),
    GioiTinh NVARCHAR(10)  NULL,
    CONSTRAINT UQ_KhachHang_SoDT UNIQUE (SoDT)
);
GO

-- 1.7 HoaDon
CREATE TABLE HoaDon (
    MaHD         INT IDENTITY(1,1) PRIMARY KEY,
    MaKH         INT           NULL,
    MaND         INT           NOT NULL,
    NgayBan      DATETIME      NOT NULL DEFAULT GETDATE(),
    TongTien     DECIMAL(18,0) NOT NULL DEFAULT 0,
    PhuongThucTT NVARCHAR(20)  NOT NULL DEFAULT N'TienMat',
    TrangThai    NVARCHAR(20)  NOT NULL DEFAULT N'Thanh cong',
    LyDoHuy      NVARCHAR(500) NULL,
    LoaiHD       NVARCHAR(10)  NOT NULL DEFAULT N'SALE',
    MaHDGoc      INT           NULL,
    CONSTRAINT FK_HoaDon_KhachHang FOREIGN KEY (MaKH)    REFERENCES KhachHang(MaKH),
    CONSTRAINT FK_HoaDon_NguoiDung FOREIGN KEY (MaND)    REFERENCES NguoiDung(MaND),
    CONSTRAINT FK_HoaDon_HoaDonGoc FOREIGN KEY (MaHDGoc) REFERENCES HoaDon(MaHD),
    CONSTRAINT CK_HoaDon_LoaiHD    CHECK (LoaiHD IN (N'SALE', N'RETURN'))
);
GO

-- 1.8 ChiTietHoaDon
CREATE TABLE ChiTietHoaDon (
    MaCTHD    INT IDENTITY(1,1) PRIMARY KEY,
    MaHD      INT           NOT NULL,
    MaLo      INT           NOT NULL,
    MaSP      INT           NOT NULL,
    SoLuong   INT           NOT NULL,
    DonGia    DECIMAL(18,0) NOT NULL,
    ThanhTien DECIMAL(18,0) NOT NULL,
    GiaVon    DECIMAL(18,0) NOT NULL DEFAULT 0,
    CONSTRAINT FK_CTHD_HoaDon  FOREIGN KEY (MaHD) REFERENCES HoaDon(MaHD),
    CONSTRAINT FK_CTHD_LoHang  FOREIGN KEY (MaLo) REFERENCES LoHang(MaLo),
    CONSTRAINT FK_CTHD_SanPham FOREIGN KEY (MaSP) REFERENCES SanPham(MaSP)
);
GO

-- ============================================================
-- 2. BẢNG KHO: Trả hàng NCC, Hủy hàng, Kiểm kho
-- ============================================================

CREATE TABLE PhieuTraHang (
    MaPTH    INT IDENTITY(1,1) PRIMARY KEY,
    MaPN     INT           NOT NULL,
    MaNCC    INT           NOT NULL,
    MaND     INT           NOT NULL,
    NgayTra  DATETIME      NULL DEFAULT GETDATE(),
    TongTien DECIMAL(18,0) NULL DEFAULT 0,
    TrangThai NVARCHAR(30) NULL DEFAULT N'Chờ duyệt',
    GhiChu   NVARCHAR(500) NULL,
    CONSTRAINT FK_PTH_PN  FOREIGN KEY (MaPN)  REFERENCES PhieuNhap(MaPN),
    CONSTRAINT FK_PTH_NCC FOREIGN KEY (MaNCC) REFERENCES NhaCungCap(MaNCC),
    CONSTRAINT FK_PTH_ND  FOREIGN KEY (MaND)  REFERENCES NguoiDung(MaND)
);
GO

CREATE TABLE ChiTietTraHang (
    MaCTTH     INT IDENTITY(1,1) PRIMARY KEY,
    MaPTH      INT           NOT NULL,
    MaLo       INT           NOT NULL,
    MaSP       INT           NOT NULL,
    SoLuongTra INT           NOT NULL,
    DonGia     DECIMAL(18,0) NOT NULL,
    ThanhTien  DECIMAL(18,0) NOT NULL,
    LyDoTra    NVARCHAR(200) NULL,
    CONSTRAINT FK_CTTH_PTH FOREIGN KEY (MaPTH) REFERENCES PhieuTraHang(MaPTH),
    CONSTRAINT FK_CTTH_LH  FOREIGN KEY (MaLo)  REFERENCES LoHang(MaLo),
    CONSTRAINT FK_CTTH_SP  FOREIGN KEY (MaSP)  REFERENCES SanPham(MaSP)
);
GO

CREATE TABLE PhieuHuyHang (
    MaPHH      INT IDENTITY(1,1) PRIMARY KEY,
    MaND       INT           NOT NULL,
    NgayHuy    DATETIME      NULL DEFAULT GETDATE(),
    TongTonThat DECIMAL(18,0) NULL DEFAULT 0,
    LyDo       NVARCHAR(500) NULL,
    GhiChu     NVARCHAR(500) NULL,
    CONSTRAINT FK_PHH_ND FOREIGN KEY (MaND) REFERENCES NguoiDung(MaND)
);
GO

CREATE TABLE ChiTietHuyHang (
    MaCTHH     INT IDENTITY(1,1) PRIMARY KEY,
    MaPHH      INT           NOT NULL,
    MaLo       INT           NOT NULL,
    MaSP       INT           NOT NULL,
    SoLuongHuy INT           NOT NULL,
    DonGia     DECIMAL(18,0) NOT NULL,
    ThanhTien  DECIMAL(18,0) NOT NULL,
    LyDoHuy    NVARCHAR(200) NULL,
    CONSTRAINT FK_CTHH_PHH FOREIGN KEY (MaPHH) REFERENCES PhieuHuyHang(MaPHH),
    CONSTRAINT FK_CTHH_LH  FOREIGN KEY (MaLo)  REFERENCES LoHang(MaLo),
    CONSTRAINT FK_CTHH_SP  FOREIGN KEY (MaSP)  REFERENCES SanPham(MaSP)
);
GO

CREATE TABLE PhieuKiemKho (
    MaPKK    INT IDENTITY(1,1) PRIMARY KEY,
    MaND     INT           NOT NULL,
    NgayKiem DATETIME      NULL DEFAULT GETDATE(),
    TrangThai NVARCHAR(30) NULL DEFAULT N'Đang kiểm',
    GhiChu   NVARCHAR(500) NULL,
    CONSTRAINT FK_PKK_ND FOREIGN KEY (MaND) REFERENCES NguoiDung(MaND)
);
GO

CREATE TABLE ChiTietKiemKho (
    MaCTKK     INT IDENTITY(1,1) PRIMARY KEY,
    MaPKK      INT           NOT NULL,
    MaLo       INT           NOT NULL,
    MaSP       INT           NOT NULL,
    TonHeThong INT           NOT NULL,
    TonThucTe  INT           NOT NULL,
    ChenhLech  INT           NOT NULL,
    GhiChu     NVARCHAR(200) NULL,
    CONSTRAINT FK_CTKK_PKK FOREIGN KEY (MaPKK) REFERENCES PhieuKiemKho(MaPKK),
    CONSTRAINT FK_CTKK_LH  FOREIGN KEY (MaLo)  REFERENCES LoHang(MaLo),
    CONSTRAINT FK_CTKK_SP  FOREIGN KEY (MaSP)  REFERENCES SanPham(MaSP)
);
GO

-- Bảng cũ (legacy) — giữ tương thích
CREATE TABLE HuyHang (
    MaHuy        INT IDENTITY(1,1) PRIMARY KEY,
    MaLo         INT NOT NULL, MaSP INT NOT NULL, SoLuongHuy INT NOT NULL,
    DonGiaVon    DECIMAL(18,0) NULL, TongThietHai DECIMAL(18,0) NULL,
    PhanLoaiLyDo NVARCHAR(100) NULL, ChiTietLyDo  NVARCHAR(500) NOT NULL,
    MaND         INT NOT NULL, NgayHuy DATETIME NULL DEFAULT GETDATE(),
    GiaNhapLo    DECIMAL(18,0) NULL, TongTienHoan DECIMAL(18,0) NULL,
    HinhThucHoan NVARCHAR(50) NULL
);
GO

CREATE TABLE TraHangKhach (
    MaTraKH    INT IDENTITY(1,1) PRIMARY KEY,
    MaHD       INT NOT NULL, MaCTHD INT NOT NULL, MaLo INT NOT NULL, MaSP INT NOT NULL,
    SoLuongTra INT NOT NULL, DonGia DECIMAL(18,2) NOT NULL, TienHoan DECIMAL(18,2) NOT NULL,
    LyDo       NVARCHAR(500) NULL, MaND INT NOT NULL, NgayTra DATETIME NULL DEFAULT GETDATE(),
    CONSTRAINT FK_TKH_HD   FOREIGN KEY (MaHD)   REFERENCES HoaDon(MaHD),
    CONSTRAINT FK_TKH_CTHD FOREIGN KEY (MaCTHD) REFERENCES ChiTietHoaDon(MaCTHD),
    CONSTRAINT FK_TKH_LH   FOREIGN KEY (MaLo)   REFERENCES LoHang(MaLo),
    CONSTRAINT FK_TKH_SP   FOREIGN KEY (MaSP)   REFERENCES SanPham(MaSP),
    CONSTRAINT FK_TKH_ND   FOREIGN KEY (MaND)   REFERENCES NguoiDung(MaND)
);
GO

CREATE TABLE TraHangNCC (
    MaTra        INT IDENTITY(1,1) PRIMARY KEY,
    MaLo INT NOT NULL, MaSP INT NOT NULL, SoLuongTra INT NOT NULL,
    GiaNhapLo    DECIMAL(18,0) NULL, TongTienHoan DECIMAL(18,0) NULL,
    HinhThucHoan NVARCHAR(50) NULL, LyDo NVARCHAR(500) NULL,
    MaND         INT NOT NULL, NgayTra DATETIME NULL DEFAULT GETDATE(),
    TongThietHai DECIMAL(18,0) NULL, PhanLoaiLyDo NVARCHAR(100) NULL,
    ChiTietLyDo  NVARCHAR(500) NULL, TinhTrang NVARCHAR(100) NULL,
    GhiChu       NVARCHAR(500) NULL
);
GO

CREATE TABLE SystemLogs (
    MaLog    INT IDENTITY(1,1) PRIMARY KEY,
    MaND     INT           NULL,
    TenUser  NVARCHAR(100) NOT NULL,
    HanhDong NVARCHAR(50)  NOT NULL,
    DoiTuong NVARCHAR(200) NOT NULL,
    ChiTiet  NVARCHAR(500) NULL,
    ThoiGian DATETIME      NOT NULL DEFAULT GETDATE()
);
GO

-- ============================================================
-- 3. MODULE HRM
-- ============================================================

CREATE TABLE HR_Shifts (
    ShiftID          INT IDENTITY(1,1) PRIMARY KEY,
    ShiftName        NVARCHAR(50) NOT NULL,
    DefaultStartTime TIME         NOT NULL,
    DefaultEndTime   TIME         NOT NULL
);
GO

CREATE TABLE HR_Employees (
    EmpID       INT IDENTITY(1,1) PRIMARY KEY,
    FullName    NVARCHAR(100) NOT NULL,
    PinCode     VARCHAR(10)   NOT NULL,
    HourlyRate  DECIMAL(18,2) NOT NULL DEFAULT 0,
    OvertimeRate DECIMAL(18,2) NOT NULL DEFAULT 0,
    Phone       VARCHAR(20)   NULL,
    HireDate    DATE          NULL,
    ResignDate  DATE          NULL,
    Status      NVARCHAR(20)  NOT NULL DEFAULT N'Đang làm',
    CreatedAt   DATETIME      NOT NULL DEFAULT GETDATE(),
    MaND        INT           NULL,
    CONSTRAINT UQ_HR_Employees_PinCode UNIQUE (PinCode)
);
GO

CREATE TABLE HR_Schedules (
    ScheduleID  INT IDENTITY(1,1) PRIMARY KEY,
    EmpID       INT  NOT NULL,
    ShiftID     INT  NOT NULL,
    WorkDate    DATE NOT NULL,
    ActualStart TIME NOT NULL,
    ActualEnd   TIME NOT NULL,
    CreatedAt   DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_Schedule_Employee FOREIGN KEY (EmpID)   REFERENCES HR_Employees(EmpID),
    CONSTRAINT FK_Schedule_Shift    FOREIGN KEY (ShiftID) REFERENCES HR_Shifts(ShiftID),
    CONSTRAINT UQ_Schedule_EmpDateShift UNIQUE (EmpID, WorkDate, ShiftID)
);
GO

CREATE TABLE HR_Attendances (
    AttendanceID INT IDENTITY(1,1) PRIMARY KEY,
    EmpID        INT          NOT NULL,
    ScheduleID   INT          NOT NULL DEFAULT 0,
    ClockIn      DATETIME     NOT NULL,
    ClockOut     DATETIME     NULL,
    LateReason   NVARCHAR(255) NULL,
    TotalHours   DECIMAL(18,2) NOT NULL DEFAULT 0,
    DailyEarned  DECIMAL(18,2) NOT NULL DEFAULT 0,
    CreatedAt    DATETIME      NOT NULL DEFAULT GETDATE(),
    SnapshotRate  DECIMAL(18,2) NULL,
    SnapshotStart TIME          NULL,
    SnapshotEnd   TIME          NULL,
    CONSTRAINT FK_Att_Employee FOREIGN KEY (EmpID)      REFERENCES HR_Employees(EmpID),
    CONSTRAINT FK_Att_Schedule FOREIGN KEY (ScheduleID) REFERENCES HR_Schedules(ScheduleID)
);
GO

CREATE TABLE HR_Config (
    ConfigID          INT IDENTITY(1,1) PRIMARY KEY,
    DefaultWeeklyLeave INT NOT NULL DEFAULT 1,
    DefaultAnnualLeave INT NOT NULL DEFAULT 12
);
GO
INSERT INTO HR_Config (DefaultWeeklyLeave, DefaultAnnualLeave) VALUES (1, 12);
GO

CREATE TABLE HR_Payroll (
    PayrollID     INT IDENTITY(1,1) PRIMARY KEY,
    EmpID         INT           NOT NULL,
    Thang         INT           NOT NULL,
    Nam           INT           NOT NULL,
    TongGio       DECIMAL(18,2) NOT NULL DEFAULT 0,
    TongTien      DECIMAL(18,0) NOT NULL DEFAULT 0,
    TrangThai     NVARCHAR(50)  NOT NULL DEFAULT N'Đã thanh toán',
    NgayThanhToan DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_Payroll_Employee FOREIGN KEY (EmpID) REFERENCES HR_Employees(EmpID),
    CONSTRAINT UQ_Payroll_EmpMonthYear UNIQUE (EmpID, Thang, Nam)
);
GO

-- ============================================================
-- 4. INDEXES
-- ============================================================
CREATE NONCLUSTERED INDEX IX_LoHang_MaSP        ON LoHang (MaSP);
CREATE NONCLUSTERED INDEX IX_KhachHang_SoDT     ON KhachHang (SoDT);
CREATE NONCLUSTERED INDEX IX_HoaDon_NgayBan     ON HoaDon (NgayBan DESC);
CREATE NONCLUSTERED INDEX IX_CTHD_MaHD          ON ChiTietHoaDon (MaHD);
CREATE NONCLUSTERED INDEX IX_CTHD_MaSP          ON ChiTietHoaDon (MaSP);
CREATE NONCLUSTERED INDEX IX_SanPham_TrangThai   ON SanPham (TrangThai) WHERE TrangThai = 1;
CREATE NONCLUSTERED INDEX IX_SanPham_TenSP       ON SanPham (TenSP);
CREATE NONCLUSTERED INDEX IX_SanPham_GiaBan      ON SanPham (GiaBan);
CREATE NONCLUSTERED INDEX IX_SanPham_GiaBanSi    ON SanPham (GiaBanSi);
CREATE NONCLUSTERED INDEX IX_SanPham_TrangThai_TenSP ON SanPham (TrangThai, TenSP);
CREATE NONCLUSTERED INDEX IX_PhieuNhap_NgayNhap  ON PhieuNhap (NgayNhap);
GO

-- ============================================================
-- 5. STORED PROCEDURE: sp_TraHangKhach
-- ============================================================
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

        IF NOT EXISTS (
            SELECT 1 FROM HoaDon
            WHERE MaHD = @MaHDGoc AND LoaiHD = N'SALE' AND TrangThai = N'Thanh cong'
        )
        BEGIN
            RAISERROR(N'Hóa đơn gốc không hợp lệ hoặc đã bị hủy.', 16, 1);
        END;

        DECLARE @Items TABLE (MaLo INT, MaSP INT, SoLuong INT, DonGia DECIMAL(18,0));
        INSERT INTO @Items (MaLo, MaSP, SoLuong, DonGia)
        SELECT
            item.value('@maLo', 'INT'), item.value('@maSP', 'INT'),
            item.value('@soLuong', 'INT'), item.value('@donGia', 'DECIMAL(18,0)')
        FROM @ChiTietTra.nodes('/items/i') AS T(item);

        IF EXISTS (
            SELECT 1 FROM @Items it
            LEFT JOIN ChiTietHoaDon ct ON ct.MaHD = @MaHDGoc AND ct.MaLo = it.MaLo AND ct.MaSP = it.MaSP
            LEFT JOIN (
                SELECT ct2.MaLo, ct2.MaSP, SUM(ABS(ct2.SoLuong)) AS DaTra
                FROM ChiTietHoaDon ct2 JOIN HoaDon hd2 ON ct2.MaHD = hd2.MaHD
                WHERE hd2.MaHDGoc = @MaHDGoc AND hd2.LoaiHD = N'RETURN'
                GROUP BY ct2.MaLo, ct2.MaSP
            ) prev ON prev.MaLo = it.MaLo AND prev.MaSP = it.MaSP
            WHERE it.SoLuong > (ISNULL(ct.SoLuong, 0) - ISNULL(prev.DaTra, 0))
        )
        BEGIN
            RAISERROR(N'Số lượng trả vượt quá số lượng còn lại có thể trả!', 16, 1);
        END;

        DECLARE @TongTienTra DECIMAL(18,0);
        SELECT @TongTienTra = -SUM(SoLuong * DonGia) FROM @Items;

        DECLARE @MaKH INT;
        SELECT @MaKH = MaKH FROM HoaDon WHERE MaHD = @MaHDGoc;

        INSERT INTO HoaDon (MaKH, MaND, NgayBan, TongTien, PhuongThucTT, TrangThai, LoaiHD, MaHDGoc, LyDoHuy)
        VALUES (@MaKH, @MaND, GETDATE(), @TongTienTra, N'TienMat', N'Thanh cong', N'RETURN', @MaHDGoc, @LyDo);

        DECLARE @MaHDTra INT = SCOPE_IDENTITY();

        INSERT INTO ChiTietHoaDon (MaHD, MaLo, MaSP, SoLuong, DonGia, ThanhTien, GiaVon)
        SELECT @MaHDTra, it.MaLo, it.MaSP, -it.SoLuong, it.DonGia, -(it.SoLuong * it.DonGia), ISNULL(l.GiaNhap, 0)
        FROM @Items it JOIN LoHang l ON it.MaLo = l.MaLo;

        UPDATE l SET l.SoLuong = l.SoLuong + it.SoLuong
        FROM LoHang l JOIN @Items it ON l.MaLo = it.MaLo;

        COMMIT TRANSACTION;
        SELECT @MaHDTra AS MaHDTra;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        DECLARE @ErrMsg NVARCHAR(4000) = ERROR_MESSAGE();
        RAISERROR(@ErrMsg, 16, 1);
    END CATCH;
END;
GO

PRINT N'=== Schema MerPhar tạo thành công! ===';
GO
