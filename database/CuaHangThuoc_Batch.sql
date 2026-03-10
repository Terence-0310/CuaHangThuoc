/* =====================================================================
   DATABASE: CuaHangThuoc_Batch
   =====================================================================

========================= 1. BỐI CẢNH NGHIỆP VỤ =========================

Hệ thống được xây dựng để mô phỏng hoạt động thực tế của một nhà thuốc,
trong đó việc quản lý tồn kho và bán hàng phải tuân thủ theo LÔ HÀNG
(BATCH/LOT) và nguyên tắc FEFO (First Expire First Out).

1.1. Nguyên tắc quản lý dữ liệu
--------------------------------
- SanPham:
  + Là bảng MASTER, lưu thông tin chung của sản phẩm:
    tên, loại, đơn vị tính, giá bán đề xuất, mô tả, ngưỡng tồn tối thiểu.
  + KHÔNG lưu tồn kho và hạn dùng trực tiếp.

- LoHang (Batch/Lot):
  + Là bảng QUAN TRỌNG NHẤT trong nghiệp vụ nhà thuốc.
  + Quản lý tồn kho và hạn dùng theo từng lô nhập.
  + Mỗi lô có: Số lô, hạn sử dụng, giá nhập, số lượng nhập, số lượng tồn.
  + Một sản phẩm có thể có nhiều lô khác nhau.

- Tồn kho thực tế của sản phẩm:
  + Được tính bằng TỔNG SoLuongTon của các lô còn hiệu lực trong LoHang.

1.2. Nguyên tắc bán hàng (FEFO)
--------------------------------
- FEFO = First Expire First Out:
  + Lô nào có hạn sử dụng gần nhất thì phải xuất trước.
- Hệ thống KHÔNG cho bán:
  + Lô đã hết hạn.
  + Lô không còn tồn.
- Nếu số lượng bán > tồn của một lô:
  + Tự động tách sang lô tiếp theo theo FEFO.

1.3. Nguyên tắc nhập hàng
--------------------------------
- Nhập hàng thông qua Phiếu nhập.
- Mỗi dòng nhập tương ứng với một lô (SoLo + HanSuDung).
- Không cho nhập lô đã quá hạn.
- Nếu lô đã tồn tại (MaSanPham + SoLo):
  + Cộng tồn vào lô cũ.
- Nếu lô chưa tồn tại:
  + Tạo mới một bản ghi LoHang.

1.4. Cảnh báo nghiệp vụ
--------------------------------
Hệ thống hỗ trợ các cảnh báo quan trọng:
- Hết hàng:
  + Tổng tồn của sản phẩm <= MucTonToiThieu.
- Sắp hết hạn:
  + Lô còn tồn và hạn sử dụng trong N ngày tới (mặc định 30 ngày).
- Đã hết hạn:
  + Lô còn tồn nhưng hạn sử dụng < ngày hiện tại.

=======================================================================

====================== 2. FLOW NGHIỆP VỤ THEO VAI TRÒ ===================

2.1. NHÂN VIÊN (NhanVien)
--------------------------------
Mục tiêu chính:
- Bán hàng
- Tra cứu sản phẩm, lô hàng
- Theo dõi cảnh báo

FLOW BÁN HÀNG:
Step 1: Tạo hóa đơn
    EXEC sp_HoaDonBan_Create
         @MaNguoiDung,
         @MaKhachHang,
         @GhiChu;
    -> Trả về MaHoaDon

Step 2: Bán sản phẩm theo FEFO
    EXEC sp_HoaDonBan_Sell_FEFO
         @MaHoaDon,
         @MaSanPham,
         @SoLuongCanBan,
         @GiaBan = NULL;
    Proc tự động:
    (1) Kiểm tra tổng tồn hợp lệ theo lô
    (2) Chọn lô theo FEFO (hạn gần nhất)
    (3) Tách số lượng bán nếu cần
    (4) Ghi ChiTietHoaDon theo từng lô
    (5) Trừ tồn từng lô
    (6) Cập nhật tổng tiền hóa đơn

Step 3: Xem kết quả
    SELECT * FROM HoaDonBan;
    SELECT * FROM ChiTietHoaDon;

-----------------------------------------------------------------------

2.2. QUẢN LÝ (QuanLy)
--------------------------------
Quyền hạn:
- Có toàn bộ quyền của Nhân viên
- Nhập hàng
- Quản lý tồn kho, ngưỡng tồn tối thiểu

FLOW NHẬP HÀNG:
Step 1: Tạo phiếu nhập
    EXEC sp_PhieuNhap_Create
         @MaNguoiDung,
         @MaNCC,
         @GhiChu;
    -> Trả về MaPhieuNhap

Step 2: Thêm dòng nhập theo lô
    EXEC sp_PhieuNhap_AddItem_Batch
         @MaPhieuNhap,
         @MaSanPham,
         @SoLo,
         @HanSuDung,
         @GiaNhap,
         @SoLuong;

Proc tự động:
- Chặn nhập lô đã quá hạn
- Lưu ChiTietPhieuNhap
- Tạo mới hoặc cộng tồn LoHang
- Cập nhật TongTien phiếu nhập

Quản lý tồn:
    UPDATE SanPham
    SET MucTonToiThieu = ?
    WHERE MaSanPham = ?;

-----------------------------------------------------------------------

2.3. ADMIN
--------------------------------
Chức năng:
- Quản lý người dùng
- Phân quyền hệ thống

Ràng buộc:
- Không cho xóa cứng Admin
- DELETE user khác => soft delete (DaXoa = 1)
- Phục vụ phân quyền UI và nghiệp vụ

=======================================================================

====================== 3. FLOW GIAO DIỆN WINDOWBUILDER ==================

3.1. Cấu trúc giao diện
--------------------------------
- LoginFrame:
  + Nhập username / password
  + Gọi sp_Login
  + Nhận VaiTro -> mở MainFrame

- MainFrame (JTabbedPane / CardLayout):
  1) BanHangPanel           (NhanVien / QuanLy / Admin)
  2) NhapHangPanel          (QuanLy / Admin)
  3) SanPhamPanel           (All)
  4) LoHangPanel            (All)
  5) KhachHangPanel         (All)
  6) DashboardCanhBaoPanel  (All)
  7) NguoiDungPanel         (Admin)

3.2. Phân quyền UI
--------------------------------
- NhanVien:
  + Disable NhapHangPanel
  + Disable NguoiDungPanel
- QuanLy:
  + Disable NguoiDungPanel
- Admin:
  + Full quyền

3.3. Dashboard cảnh báo
--------------------------------
Khi login hoặc mở Dashboard:
- Cảnh báo hết hàng:
    SELECT * FROM v_CanhBaoHetHang;
- Cảnh báo sắp hết hạn:
    EXEC sp_CanhBao_LoSapHetHan @SoNgay = 30;
- Cảnh báo đã hết hạn:
    EXEC sp_CanhBao_LoHetHan;

Nếu có dữ liệu:
- Hiển thị JTable
- Popup thông báo bằng JOptionPane

=======================================================================

=========================== 4. RULE NGHIỆP VỤ ===========================

- Áp dụng FEFO tuyệt đối:
    ORDER BY HanSuDung ASC, NgayNhap ASC
- Không bán lô đã hết hạn
- Không nhập lô đã hết hạn
- Kiểm tra tồn kho theo lô hợp lệ
- Soft delete người dùng
- Thiết kế chuẩn hóa, dễ mở rộng, dễ bảo trì
======================================================================= */

USE master;
GO
IF DB_ID('CuaHangThuoc_Batch') IS NOT NULL
BEGIN
    ALTER DATABASE CuaHangThuoc_Batch SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE CuaHangThuoc_Batch;
END
GO

CREATE DATABASE CuaHangThuoc_Batch;
GO
USE CuaHangThuoc_Batch;
GO

/* =========================
   1) NGƯỜI DÙNG + PHÂN QUYỀN
   ========================= */
CREATE TABLE dbo.NguoiDung (
    MaNguoiDung INT IDENTITY(1,1) PRIMARY KEY,
    TenDangNhap NVARCHAR(255) NOT NULL UNIQUE,
    MatKhau NVARCHAR(255) NOT NULL,
    VaiTro NVARCHAR(50) NOT NULL CHECK (VaiTro IN (N'Admin', N'QuanLy', N'NhanVien')),
    HoTen NVARCHAR(255) NOT NULL,
    Email NVARCHAR(255) NOT NULL UNIQUE,
    SoDienThoai NVARCHAR(15) NULL,
    DaXoa BIT NOT NULL DEFAULT 0,
    NgayTao DATETIME NOT NULL DEFAULT GETDATE()
);
GO

/* Trigger: Soft delete user khi DELETE, cấm xóa Admin */
CREATE OR ALTER TRIGGER dbo.trg_NguoiDung_BlockDeleteAdmin_SoftDelete
ON dbo.NguoiDung
INSTEAD OF DELETE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (SELECT 1 FROM deleted WHERE VaiTro = N'Admin')
    BEGIN
        RAISERROR(N'Không thể xóa người dùng Admin!', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END

    UPDATE nd
    SET DaXoa = 1
    FROM dbo.NguoiDung nd
    JOIN deleted d ON nd.MaNguoiDung = d.MaNguoiDung;
END;
GO

/* =========================
   2) KHÁCH HÀNG + NHÀ CUNG CẤP
   ========================= */
CREATE TABLE dbo.KhachHang (
    MaKhachHang INT IDENTITY(1,1) PRIMARY KEY,
    HoTen NVARCHAR(255) NOT NULL,
    SoDienThoai NVARCHAR(15) NULL,
    Email NVARCHAR(255) NULL,
    DiaChi NVARCHAR(255) NULL,
    HoSoBenhAn NVARCHAR(MAX) NULL,
    DaXoa BIT NOT NULL DEFAULT 0,
    NgayTao DATETIME NOT NULL DEFAULT GETDATE()
);
GO

CREATE TABLE dbo.NhaCungCap (
    MaNCC INT IDENTITY(1,1) PRIMARY KEY,
    TenNCC NVARCHAR(255) NOT NULL,
    SoDienThoai NVARCHAR(15) NULL,
    Email NVARCHAR(255) NULL,
    DiaChi NVARCHAR(255) NULL,
    DaXoa BIT NOT NULL DEFAULT 0,
    NgayTao DATETIME NOT NULL DEFAULT GETDATE()
);
GO

/* =========================
   3) SẢN PHẨM (MASTER)
   - Không lưu hạn dùng ở đây
   - Tồn kho tổng sẽ được tính từ các lô (LoHang)
   ========================= */
CREATE TABLE dbo.SanPham (
    MaSanPham INT IDENTITY(1,1) PRIMARY KEY,
    TenSanPham NVARCHAR(255) NOT NULL,
    DonViTinh NVARCHAR(50) NOT NULL DEFAULT N'Hộp',
    GiaBanDeXuat DECIMAL(18,2) NOT NULL DEFAULT 0 CHECK (GiaBanDeXuat >= 0),
    LoaiSanPham NVARCHAR(50) NOT NULL CHECK (LoaiSanPham IN (N'Thuoc', N'DuocMiPham', N'ThucPhamChucNang', N'ChamSocCaNhan', N'ThietBiYTe')),
    MoTa NVARCHAR(500) NULL,
    MucTonToiThieu INT NOT NULL DEFAULT 10 CHECK (MucTonToiThieu >= 0),
    DaXoa BIT NOT NULL DEFAULT 0,
    NgayTao DATETIME NOT NULL DEFAULT GETDATE()
);
GO
/* =========================================================
   EXTENSION TABLES: thuộc tính riêng theo từng loại sản phẩm
   (1 sản phẩm chỉ thuộc 1 loại => mỗi bảng tối đa 1 dòng / MaSanPham)
   ========================================================= */

-- THUỐC
CREATE TABLE dbo.SP_Thuoc (
    MaSanPham INT PRIMARY KEY,
    HoatChatChinh NVARCHAR(255) NULL,
    HamLuong NVARCHAR(100) NULL,
    DangBaoChe NVARCHAR(100) NULL,      -- viên, siro, ống...
    QuyCachDongGoi NVARCHAR(100) NULL,  -- hộp 10 vỉ x 10 viên...
    SoDangKy NVARCHAR(100) NULL,
    HangSanXuat NVARCHAR(255) NULL,
    NuocSanXuat NVARCHAR(100) NULL,
    ChiDinh NVARCHAR(MAX) NULL,
    ChongChiDinh NVARCHAR(MAX) NULL,
    LieuDung NVARCHAR(MAX) NULL,
    TacDungPhu NVARCHAR(MAX) NULL,
    CONSTRAINT FK_SP_Thuoc_SanPham FOREIGN KEY (MaSanPham) REFERENCES dbo.SanPham(MaSanPham)
);
GO

-- DƯỢC MỸ PHẨM
CREATE TABLE dbo.SP_DuocMiPham (
    MaSanPham INT PRIMARY KEY,
    CongDung NVARCHAR(MAX) NULL,
    LoaiDaPhuHop NVARCHAR(100) NULL,    -- da dầu/khô/nhạy cảm...
    ThanhPhanNoiBat NVARCHAR(MAX) NULL,
    HuongDanSuDung NVARCHAR(MAX) NULL,
    XuatXu NVARCHAR(100) NULL,
    CONSTRAINT FK_SP_DuocMiPham_SanPham FOREIGN KEY (MaSanPham) REFERENCES dbo.SanPham(MaSanPham)
);
GO

-- THỰC PHẨM CHỨC NĂNG
CREATE TABLE dbo.SP_ThucPhamChucNang (
    MaSanPham INT PRIMARY KEY,
    ThanhPhan NVARCHAR(MAX) NULL,
    CongDung NVARCHAR(MAX) NULL,
    LieuDung NVARCHAR(MAX) NULL,
    DoiTuongSuDung NVARCHAR(255) NULL,
    LuuY NVARCHAR(MAX) NULL,
    CONSTRAINT FK_SP_TPCN_SanPham FOREIGN KEY (MaSanPham) REFERENCES dbo.SanPham(MaSanPham)
);
GO

-- CHĂM SÓC CÁ NHÂN
CREATE TABLE dbo.SP_ChamSocCaNhan (
    MaSanPham INT PRIMARY KEY,
    CongDung NVARCHAR(MAX) NULL,
    HuongDanSuDung NVARCHAR(MAX) NULL,
    DoiTuongSuDung NVARCHAR(255) NULL,
    ChatLieu_MuiHuong NVARCHAR(255) NULL,
    CONSTRAINT FK_SP_CSCN_SanPham FOREIGN KEY (MaSanPham) REFERENCES dbo.SanPham(MaSanPham)
);
GO

-- THIẾT BỊ Y TẾ
CREATE TABLE dbo.SP_ThietBiYTe (
    MaSanPham INT PRIMARY KEY,
    ThuongHieu NVARCHAR(255) NULL,
    Model NVARCHAR(100) NULL,
    ThongSoKyThuat NVARCHAR(MAX) NULL,
    BaoHanh NVARCHAR(100) NULL,
    HuongDanSuDung NVARCHAR(MAX) NULL,
    CONSTRAINT FK_SP_TBYT_SanPham FOREIGN KEY (MaSanPham) REFERENCES dbo.SanPham(MaSanPham)
);
GO

/* =========================
   4) LÔ HÀNG (BATCH/LOT)
   - Quản lý tồn + hạn dùng theo lô
   - (MaSanPham, SoLo) unique
   ========================= */
CREATE TABLE dbo.LoHang (
    MaLoHang INT IDENTITY(1,1) PRIMARY KEY,
    MaSanPham INT NOT NULL,
    SoLo NVARCHAR(50) NOT NULL,
    MaNCC INT NULL,
    MaPhieuNhap INT NULL,
    NgaySanXuat DATE NULL,
    HanSuDung DATE NOT NULL,
    GiaNhap DECIMAL(18,2) NOT NULL CHECK (GiaNhap >= 0),
    SoLuongNhap INT NOT NULL CHECK (SoLuongNhap > 0),
    SoLuongTon INT NOT NULL CHECK (SoLuongTon >= 0),
    NgayNhap DATETIME NOT NULL DEFAULT GETDATE(),
    TrangThai NVARCHAR(30) NOT NULL DEFAULT N'Đang bán'
        CHECK (TrangThai IN (N'Đang bán', N'Ngưng bán', N'Hết hàng')),
    CONSTRAINT FK_LoHang_SanPham FOREIGN KEY (MaSanPham) REFERENCES dbo.SanPham(MaSanPham),
    CONSTRAINT FK_LoHang_NCC FOREIGN KEY (MaNCC) REFERENCES dbo.NhaCungCap(MaNCC),
    CONSTRAINT UX_LoHang UNIQUE (MaSanPham, SoLo)
);
GO
CREATE INDEX IX_LoHang_FEFO ON dbo.LoHang(MaSanPham, HanSuDung, NgayNhap);
GO

/* =========================
   5) PHIẾU NHẬP + CHI TIẾT NHẬP
   - Nhập theo lô
   ========================= */
CREATE TABLE dbo.PhieuNhap (
    MaPhieuNhap INT IDENTITY(1,1) PRIMARY KEY,
    MaNguoiDung INT NOT NULL,
    MaNCC INT NULL,
    TongTien DECIMAL(18,2) NOT NULL DEFAULT 0,
    NgayNhap DATETIME NOT NULL DEFAULT GETDATE(),
    GhiChu NVARCHAR(500) NULL,
    CONSTRAINT FK_PhieuNhap_NguoiDung FOREIGN KEY (MaNguoiDung) REFERENCES dbo.NguoiDung(MaNguoiDung)
);
GO

ALTER TABLE dbo.LoHang
ADD CONSTRAINT FK_LoHang_PhieuNhap FOREIGN KEY (MaPhieuNhap) REFERENCES dbo.PhieuNhap(MaPhieuNhap);
GO

CREATE TABLE dbo.ChiTietPhieuNhap (
    MaCTPN INT IDENTITY(1,1) PRIMARY KEY,
    MaPhieuNhap INT NOT NULL,
    MaSanPham INT NOT NULL,
    SoLo NVARCHAR(50) NOT NULL,
    HanSuDung DATE NOT NULL,
    GiaNhap DECIMAL(18,2) NOT NULL CHECK (GiaNhap >= 0),
    SoLuong INT NOT NULL CHECK (SoLuong > 0),
    ThanhTien AS (SoLuong * GiaNhap) PERSISTED,
    CONSTRAINT FK_CTPN_PhieuNhap FOREIGN KEY (MaPhieuNhap) REFERENCES dbo.PhieuNhap(MaPhieuNhap),
    CONSTRAINT FK_CTPN_SanPham FOREIGN KEY (MaSanPham) REFERENCES dbo.SanPham(MaSanPham)
);
GO

/* =========================
   6) HÓA ĐƠN BÁN + CHI TIẾT BÁN
   - Chuẩn theo lô: mỗi dòng bán gắn MaLoHang
   ========================= */
CREATE TABLE dbo.HoaDonBan (
    MaHoaDon INT IDENTITY(1,1) PRIMARY KEY,
    MaNguoiDung INT NOT NULL,
    MaKhachHang INT NULL,
    TongTien DECIMAL(18,2) NOT NULL DEFAULT 0,
    NgayBan DATETIME NOT NULL DEFAULT GETDATE(),
    GhiChu NVARCHAR(500) NULL,
    CONSTRAINT FK_HDB_NguoiDung FOREIGN KEY (MaNguoiDung) REFERENCES dbo.NguoiDung(MaNguoiDung),
    CONSTRAINT FK_HDB_KhachHang FOREIGN KEY (MaKhachHang) REFERENCES dbo.KhachHang(MaKhachHang)
);
GO

CREATE TABLE dbo.ChiTietHoaDon (
    MaCTHD INT IDENTITY(1,1) PRIMARY KEY,
    MaHoaDon INT NOT NULL,
    MaLoHang INT NOT NULL,
    MaSanPham INT NOT NULL,
    SoLuong INT NOT NULL CHECK (SoLuong > 0),
    GiaBan DECIMAL(18,2) NOT NULL CHECK (GiaBan >= 0),
    ThanhTien AS (SoLuong * GiaBan) PERSISTED,
    CONSTRAINT FK_CTHD_HDB FOREIGN KEY (MaHoaDon) REFERENCES dbo.HoaDonBan(MaHoaDon),
    CONSTRAINT FK_CTHD_LoHang FOREIGN KEY (MaLoHang) REFERENCES dbo.LoHang(MaLoHang),
    CONSTRAINT FK_CTHD_SanPham FOREIGN KEY (MaSanPham) REFERENCES dbo.SanPham(MaSanPham)
);
GO

/* =========================
   7) VIEW: TỔNG TỒN THEO SẢN PHẨM (từ các lô)
   - Dashboard hết hàng dùng view này
   ========================= */
CREATE OR ALTER VIEW dbo.v_TonKhoSanPham AS
SELECT
    sp.MaSanPham,
    sp.TenSanPham,
    sp.MucTonToiThieu,
    SUM(CASE WHEN lh.TrangThai <> N'Ngưng bán' THEN lh.SoLuongTon ELSE 0 END) AS TongTon
FROM dbo.SanPham sp
LEFT JOIN dbo.LoHang lh ON lh.MaSanPham = sp.MaSanPham
WHERE sp.DaXoa = 0
GROUP BY sp.MaSanPham, sp.TenSanPham, sp.MucTonToiThieu;
GO

CREATE OR ALTER VIEW dbo.v_CanhBaoHetHang AS
SELECT * FROM dbo.v_TonKhoSanPham
WHERE TongTon <= MucTonToiThieu;
GO

CREATE OR ALTER VIEW dbo.v_CanhBaoLoSapHetHan AS
SELECT
    lh.MaLoHang, lh.MaSanPham, sp.TenSanPham, lh.SoLo, lh.HanSuDung, lh.SoLuongTon,
    DATEDIFF(DAY, CAST(GETDATE() AS DATE), lh.HanSuDung) AS ConLai_Ngay
FROM dbo.LoHang lh
JOIN dbo.SanPham sp ON sp.MaSanPham = lh.MaSanPham
WHERE sp.DaXoa = 0
  AND lh.SoLuongTon > 0
  AND lh.HanSuDung >= CAST(GETDATE() AS DATE)
  AND lh.HanSuDung <= DATEADD(DAY, 30, CAST(GETDATE() AS DATE));
GO

CREATE OR ALTER VIEW dbo.v_CanhBaoLoHetHan AS
SELECT
    lh.MaLoHang, lh.MaSanPham, sp.TenSanPham, lh.SoLo, lh.HanSuDung, lh.SoLuongTon
FROM dbo.LoHang lh
JOIN dbo.SanPham sp ON sp.MaSanPham = lh.MaSanPham
WHERE sp.DaXoa = 0
  AND lh.SoLuongTon > 0
  AND lh.HanSuDung < CAST(GETDATE() AS DATE);
GO

/* =========================
   8) PROC: LOGIN (gọi khi bấm Login)
   ========================= */
CREATE OR ALTER PROC dbo.sp_Login
    @TenDangNhap NVARCHAR(255),
    @MatKhau NVARCHAR(255)
AS
BEGIN
    SET NOCOUNT ON;
    SELECT TOP 1 MaNguoiDung, TenDangNhap, VaiTro, HoTen, Email, SoDienThoai
    FROM dbo.NguoiDung
    WHERE TenDangNhap=@TenDangNhap AND MatKhau=@MatKhau AND DaXoa=0;
END
GO

/* =========================
   9) PROC: NHẬP HÀNG THEO LÔ (QuanLy/Admin)
   ========================= */
CREATE OR ALTER PROC dbo.sp_PhieuNhap_Create
    @MaNguoiDung INT,
    @MaNCC INT = NULL,
    @GhiChu NVARCHAR(500) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.PhieuNhap(MaNguoiDung, MaNCC, GhiChu) VALUES (@MaNguoiDung, @MaNCC, @GhiChu);
    SELECT SCOPE_IDENTITY() AS MaPhieuNhap;
END
GO

CREATE OR ALTER PROC dbo.sp_PhieuNhap_AddItem_Batch
    @MaPhieuNhap INT,
    @MaSanPham INT,
    @SoLo NVARCHAR(50),
    @HanSuDung DATE,
    @GiaNhap DECIMAL(18,2),
    @SoLuong INT
AS
BEGIN
    SET NOCOUNT ON;

    -- Rule: không nhập lô đã quá hạn
    IF @HanSuDung < CAST(GETDATE() AS DATE)
    BEGIN
        RAISERROR(N'Hạn sử dụng đã quá hạn. Không thể nhập lô này!', 16, 1);
        RETURN;
    END

    -- Lưu chi tiết phiếu nhập
    INSERT INTO dbo.ChiTietPhieuNhap(MaPhieuNhap, MaSanPham, SoLo, HanSuDung, GiaNhap, SoLuong)
    VALUES (@MaPhieuNhap, @MaSanPham, @SoLo, @HanSuDung, @GiaNhap, @SoLuong);

    -- Nếu lô tồn tại: cộng tồn
    IF EXISTS (SELECT 1 FROM dbo.LoHang WHERE MaSanPham=@MaSanPham AND SoLo=@SoLo)
    BEGIN
        UPDATE dbo.LoHang
        SET SoLuongTon = SoLuongTon + @SoLuong,
            SoLuongNhap = SoLuongNhap + @SoLuong,
            HanSuDung = CASE WHEN HanSuDung < @HanSuDung THEN @HanSuDung ELSE HanSuDung END,
            GiaNhap = @GiaNhap,
            TrangThai = N'Đang bán'
        WHERE MaSanPham=@MaSanPham AND SoLo=@SoLo;
    END
    ELSE
    BEGIN
        DECLARE @MaNCC INT;
        SELECT @MaNCC = MaNCC FROM dbo.PhieuNhap WHERE MaPhieuNhap=@MaPhieuNhap;

        INSERT INTO dbo.LoHang(MaSanPham, SoLo, MaNCC, MaPhieuNhap, HanSuDung, GiaNhap, SoLuongNhap, SoLuongTon)
        VALUES (@MaSanPham, @SoLo, @MaNCC, @MaPhieuNhap, @HanSuDung, @GiaNhap, @SoLuong, @SoLuong);
    END

    -- Update tổng tiền phiếu nhập
    UPDATE pn
    SET TongTien = (SELECT SUM(ThanhTien) FROM dbo.ChiTietPhieuNhap WHERE MaPhieuNhap=pn.MaPhieuNhap)
    FROM dbo.PhieuNhap pn
    WHERE pn.MaPhieuNhap = @MaPhieuNhap;
END
GO

/* =========================
   10) PROC: BÁN HÀNG FEFO (NhanVien/QuanLy/Admin)
   ========================= */
CREATE OR ALTER PROC dbo.sp_HoaDonBan_Create
    @MaNguoiDung INT,
    @MaKhachHang INT = NULL,
    @GhiChu NVARCHAR(500) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.HoaDonBan(MaNguoiDung, MaKhachHang, GhiChu) VALUES (@MaNguoiDung, @MaKhachHang, @GhiChu);
    SELECT SCOPE_IDENTITY() AS MaHoaDon;
END
GO

CREATE OR ALTER PROC dbo.sp_HoaDonBan_Sell_FEFO
    @MaHoaDon INT,
    @MaSanPham INT,
    @SoLuongCanBan INT,
    @GiaBan DECIMAL(18,2) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    IF @SoLuongCanBan <= 0
    BEGIN
        RAISERROR(N'Số lượng bán phải > 0', 16, 1);
        RETURN;
    END

    -- Giá bán: nếu NULL thì lấy GiaBanDeXuat
    DECLARE @GiaThucTe DECIMAL(18,2);
    SELECT @GiaThucTe = CASE WHEN @GiaBan IS NULL THEN GiaBanDeXuat ELSE @GiaBan END
    FROM dbo.SanPham
    WHERE MaSanPham=@MaSanPham AND DaXoa=0;

    IF @GiaThucTe IS NULL
    BEGIN
        RAISERROR(N'Sản phẩm không tồn tại hoặc đã bị xóa!', 16, 1);
        RETURN;
    END

    -- Tổng tồn hợp lệ theo lô: còn tồn, đang bán, chưa hết hạn
    DECLARE @TongTon INT;
    SELECT @TongTon = ISNULL(SUM(SoLuongTon),0)
    FROM dbo.LoHang
    WHERE MaSanPham=@MaSanPham
      AND SoLuongTon > 0
      AND TrangThai = N'Đang bán'
      AND HanSuDung >= CAST(GETDATE() AS DATE);

    IF @TongTon < @SoLuongCanBan
    BEGIN
        RAISERROR(N'Không đủ tồn kho (theo lô hợp lệ) để bán!', 16, 1);
        RETURN;
    END

    -- FEFO: bán lô gần hết hạn trước
    DECLARE @ConLai INT = @SoLuongCanBan;

    WHILE @ConLai > 0
    BEGIN
        DECLARE @MaLoHang INT, @TonLo INT;

        SELECT TOP 1
            @MaLoHang = MaLoHang,
            @TonLo = SoLuongTon
        FROM dbo.LoHang
        WHERE MaSanPham=@MaSanPham
          AND SoLuongTon > 0
          AND TrangThai = N'Đang bán'
          AND HanSuDung >= CAST(GETDATE() AS DATE)
        ORDER BY HanSuDung ASC, NgayNhap ASC;

        DECLARE @Xuat INT = CASE WHEN @TonLo >= @ConLai THEN @ConLai ELSE @TonLo END;

        INSERT INTO dbo.ChiTietHoaDon(MaHoaDon, MaLoHang, MaSanPham, SoLuong, GiaBan)
        VALUES (@MaHoaDon, @MaLoHang, @MaSanPham, @Xuat, @GiaThucTe);

        UPDATE dbo.LoHang
        SET SoLuongTon = SoLuongTon - @Xuat,
            TrangThai = CASE WHEN SoLuongTon - @Xuat = 0 THEN N'Hết hàng' ELSE TrangThai END
        WHERE MaLoHang = @MaLoHang;

        SET @ConLai = @ConLai - @Xuat;
    END

    -- Update tổng tiền hóa đơn
    UPDATE hd
    SET TongTien = (SELECT SUM(ThanhTien) FROM dbo.ChiTietHoaDon WHERE MaHoaDon=hd.MaHoaDon)
    FROM dbo.HoaDonBan hd
    WHERE hd.MaHoaDon = @MaHoaDon;
END
GO

/* =========================
   11) PROC: CẢNH BÁO (Dashboard)
   - gọi khi login/mở tab
   ========================= */
CREATE OR ALTER PROC dbo.sp_CanhBao_LoSapHetHan
    @SoNgay INT = 30
AS
BEGIN
    SET NOCOUNT ON;
    SELECT
        lh.MaLoHang, sp.TenSanPham, lh.SoLo, lh.HanSuDung, lh.SoLuongTon,
        DATEDIFF(DAY, CAST(GETDATE() AS DATE), lh.HanSuDung) AS ConLai_Ngay
    FROM dbo.LoHang lh
    JOIN dbo.SanPham sp ON sp.MaSanPham = lh.MaSanPham
    WHERE sp.DaXoa=0
      AND lh.SoLuongTon > 0
      AND lh.HanSuDung >= CAST(GETDATE() AS DATE)
      AND lh.HanSuDung <= DATEADD(DAY, @SoNgay, CAST(GETDATE() AS DATE))
    ORDER BY lh.HanSuDung;
END
GO

CREATE OR ALTER PROC dbo.sp_CanhBao_LoHetHan
AS
BEGIN
    SET NOCOUNT ON;
    SELECT
        lh.MaLoHang, sp.TenSanPham, lh.SoLo, lh.HanSuDung, lh.SoLuongTon
    FROM dbo.LoHang lh
    JOIN dbo.SanPham sp ON sp.MaSanPham = lh.MaSanPham
    WHERE sp.DaXoa=0
      AND lh.SoLuongTon > 0
      AND lh.HanSuDung < CAST(GETDATE() AS DATE)
    ORDER BY lh.HanSuDung;
END
GO

/* =========================
   12) SEED DATA (demo để test nhanh)
   ========================= */
INSERT dbo.NguoiDung(TenDangNhap, MatKhau, VaiTro, HoTen, Email, SoDienThoai)
VALUES
(N'admin', N'123', N'Admin',    N'Quản trị',   N'admin@shop.com', N'0900'),
(N'ql01',  N'123', N'QuanLy',   N'Quản lý 01', N'ql01@shop.com',  N'0901'),
(N'nv01',  N'123', N'NhanVien', N'Nhân viên',  N'nv01@shop.com',  N'0902');
GO

INSERT dbo.NhaCungCap(TenNCC, SoDienThoai, Email, DiaChi)
VALUES
(N'Dược A', N'028111', N'a@ncc.com', N'HCM'),
(N'Dược Hậu Giang', N'02923891234', N'dhg@dhg.com', N'Cần Thơ'),
(N'Pymepharco', N'02573822222', N'info@pymepharco.com', N'Phú Yên'),
(N'Traphaco', N'02438643646', N'info@traphaco.com', N'Hà Nội'),
(N'Sanofi Việt Nam', N'02838204500', N'contact@sanofi.vn', N'HCM');
GO

/* --- INSERT 150 SẢN PHẨM --- */
INSERT dbo.SanPham(TenSanPham, DonViTinh, GiaBanDeXuat, LoaiSanPham, MoTa, MucTonToiThieu) VALUES
-- ===== THUỐC (1-50) =====
(N'Paracetamol 500mg',      N'Hộp',  15000,  N'Thuoc', N'Giảm đau, hạ sốt',        10),
(N'Amoxicillin 500mg',      N'Hộp',  25000,  N'Thuoc', N'Kháng sinh penicillin',    10),
(N'Ibuprofen 400mg',        N'Hộp',  18000,  N'Thuoc', N'Chống viêm, giảm đau',     10),
(N'Cefuroxim 500mg',        N'Hộp',  85000,  N'Thuoc', N'Kháng sinh cephalosporin', 8),
(N'Azithromycin 250mg',     N'Hộp',  45000,  N'Thuoc', N'Kháng sinh macrolid',      8),
(N'Levofloxacin 500mg',     N'Hộp',  65000,  N'Thuoc', N'Kháng sinh quinolone',     8),
(N'Metformin 850mg',        N'Hộp',  22000,  N'Thuoc', N'Điều trị tiểu đường type 2', 10),
(N'Amlodipine 5mg',         N'Hộp',  35000,  N'Thuoc', N'Hạ huyết áp',             10),
(N'Losartan 50mg',          N'Hộp',  42000,  N'Thuoc', N'Điều trị tăng huyết áp',   10),
(N'Omeprazole 20mg',        N'Hộp',  28000,  N'Thuoc', N'Ức chế bơm proton, dạ dày',10),
(N'Pantoprazole 40mg',      N'Hộp',  55000,  N'Thuoc', N'Điều trị loét dạ dày',     8),
(N'Cetirizine 10mg',        N'Hộp',  12000,  N'Thuoc', N'Chống dị ứng',             15),
(N'Loratadine 10mg',        N'Hộp',  15000,  N'Thuoc', N'Kháng histamine',          15),
(N'Salbutamol 2mg',         N'Hộp',  20000,  N'Thuoc', N'Giãn phế quản',            10),
(N'Montelukast 10mg',       N'Hộp',  75000,  N'Thuoc', N'Dự phòng hen suyễn',       8),
(N'Prednisolone 5mg',       N'Hộp',  18000,  N'Thuoc', N'Chống viêm corticoid',     10),
(N'Dexamethasone 0.5mg',    N'Hộp',  12000,  N'Thuoc', N'Corticosteroid',           10),
(N'Diclofenac 75mg',        N'Hộp',  22000,  N'Thuoc', N'Chống viêm NSAID',         10),
(N'Meloxicam 15mg',         N'Hộp',  30000,  N'Thuoc', N'Giảm đau khớp',            10),
(N'Ciprofloxacin 500mg',    N'Hộp',  38000,  N'Thuoc', N'Kháng sinh fluoroquinolone',8),
(N'Clarithromycin 500mg',   N'Hộp',  72000,  N'Thuoc', N'Kháng sinh macrolid',      8),
(N'Clopidogrel 75mg',       N'Hộp',  55000,  N'Thuoc', N'Chống kết tập tiểu cầu',   8),
(N'Atorvastatin 20mg',      N'Hộp',  48000,  N'Thuoc', N'Giảm cholesterol',         10),
(N'Rosuvastatin 10mg',      N'Hộp',  65000,  N'Thuoc', N'Hạ mỡ máu statin',        10),
(N'Simvastatin 20mg',       N'Hộp',  32000,  N'Thuoc', N'Giảm lipid máu',           10),
(N'Metronidazole 250mg',    N'Hộp',  15000,  N'Thuoc', N'Kháng khuẩn, kháng nấm',   10),
(N'Fluconazole 150mg',      N'Viên', 25000,  N'Thuoc', N'Chống nấm',                10),
(N'Acyclovir 800mg',        N'Hộp',  40000,  N'Thuoc', N'Kháng virus herpes',       8),
(N'Domperidone 10mg',       N'Hộp',  18000,  N'Thuoc', N'Chống nôn',                10),
(N'Loperamide 2mg',         N'Hộp',  12000,  N'Thuoc', N'Cầm tiêu chảy',            15),
(N'Bisacodyl 5mg',          N'Hộp',  10000,  N'Thuoc', N'Nhuận tràng',              15),
(N'Spironolactone 25mg',    N'Hộp',  35000,  N'Thuoc', N'Lợi tiểu',                 8),
(N'Furosemide 40mg',        N'Hộp',  15000,  N'Thuoc', N'Lợi tiểu quai',            10),
(N'Captopril 25mg',         N'Hộp',  20000,  N'Thuoc', N'Ức chế men chuyển ACE',     10),
(N'Enalapril 5mg',          N'Hộp',  25000,  N'Thuoc', N'Hạ huyết áp ACE',          10),
(N'Glimepiride 2mg',        N'Hộp',  45000,  N'Thuoc', N'Hạ đường huyết',           8),
(N'Gliclazide 30mg MR',     N'Hộp',  55000,  N'Thuoc', N'Tiểu đường type 2',        8),
(N'Aspirin 81mg',           N'Hộp',  18000,  N'Thuoc', N'Chống kết tập tiểu cầu',   15),
(N'Tramadol 50mg',          N'Hộp',  35000,  N'Thuoc', N'Giảm đau opioid nhẹ',      5),
(N'Gabapentin 300mg',       N'Hộp',  60000,  N'Thuoc', N'Giảm đau thần kinh',       5),
(N'Amitriptyline 25mg',     N'Hộp',  15000,  N'Thuoc', N'Chống trầm cảm',           8),
(N'Sertraline 50mg',        N'Hộp',  75000,  N'Thuoc', N'Chống trầm cảm SSRI',      5),
(N'Diazepam 5mg',           N'Hộp',  20000,  N'Thuoc', N'An thần, giãn cơ',         5),
(N'Alprazolam 0.5mg',       N'Hộp',  28000,  N'Thuoc', N'Chống lo âu',              5),
(N'Phenobarbital 100mg',    N'Hộp',  12000,  N'Thuoc', N'Chống co giật',             8),
(N'Carbamazepine 200mg',    N'Hộp',  30000,  N'Thuoc', N'Chống động kinh',          8),
(N'Levothyroxine 50mcg',    N'Hộp',  45000,  N'Thuoc', N'Hormon tuyến giáp',        10),
(N'Doxycycline 100mg',      N'Hộp',  22000,  N'Thuoc', N'Kháng sinh tetracycline',   10),
(N'Nifedipine 20mg',        N'Hộp',  25000,  N'Thuoc', N'Chẹn kênh canxi',          10),
(N'Bisoprolol 5mg',         N'Hộp',  42000,  N'Thuoc', N'Chẹn beta, hạ HA',         10);
GO

INSERT dbo.SanPham(TenSanPham, DonViTinh, GiaBanDeXuat, LoaiSanPham, MoTa, MucTonToiThieu) VALUES
-- ===== DƯỢC MỸ PHẨM (51-80) =====
(N'Kem chống nắng La Roche-Posay SPF50', N'Tuýp', 420000, N'DuocMiPham', N'Chống nắng phổ rộng', 5),
(N'Serum Vitamin C Obagi',               N'Chai', 650000, N'DuocMiPham', N'Dưỡng sáng da',       5),
(N'Kem dưỡng ẩm Cetaphil',              N'Hũ',   280000, N'DuocMiPham', N'Dưỡng ẩm mọi loại da',5),
(N'Nước tẩy trang Bioderma 500ml',       N'Chai', 350000, N'DuocMiPham', N'Tẩy trang micellar',  5),
(N'Gel rửa mặt CeraVe',                 N'Chai', 320000, N'DuocMiPham', N'Làm sạch dịu nhẹ',    5),
(N'Kem trị mụn Differin 0.1%',          N'Tuýp', 180000, N'DuocMiPham', N'Adapalene trị mụn',    5),
(N'Kem trị sẹo Dermatix Ultra',         N'Tuýp', 280000, N'DuocMiPham', N'Trị sẹo silicone',     5),
(N'Dầu gội Head & Shoulders Clinical',  N'Chai', 155000, N'DuocMiPham', N'Trị gàu',              8),
(N'Toner Hada Labo Gokujyun',           N'Chai', 260000, N'DuocMiPham', N'Cấp ẩm hyaluronic',    5),
(N'Kem mắt Vichy Mineral 89',           N'Tuýp', 480000, N'DuocMiPham', N'Chống nhăn mắt',       3),
(N'Serum retinol The Ordinary 1%',       N'Chai', 350000, N'DuocMiPham', N'Chống lão hóa',        5),
(N'Kem body Vaseline 400ml',            N'Chai', 120000, N'DuocMiPham', N'Dưỡng da toàn thân',   8),
(N'Xịt khoáng Avène 300ml',             N'Chai', 320000, N'DuocMiPham', N'Làm dịu da nhạy cảm', 5),
(N'Kem chống nắng Anessa Perfect UV',    N'Chai', 520000, N'DuocMiPham', N'Chống nắng lâu trôi',  3),
(N'Sữa rửa mặt SVR Sebiaclear',        N'Tuýp', 280000, N'DuocMiPham', N'Kiểm soát dầu',        5),
(N'Mặt nạ đất sét Innisfree',           N'Hũ',   180000, N'DuocMiPham', N'Thải độc da',          5),
(N'Kem nền Maybelline Fit Me',          N'Tuýp', 150000, N'DuocMiPham', N'Kem nền phủ nhẹ',      5),
(N'Son dưỡng DHC Lip Cream',            N'Tuýp', 180000, N'DuocMiPham', N'Dưỡng môi vitamin E',  8),
(N'Kem trị nám Eucerin Pigment Control',N'Tuýp', 550000, N'DuocMiPham', N'Giảm nám sạm',         3),
(N'Tẩy tế bào chết Paula Choice BHA',   N'Chai', 680000, N'DuocMiPham', N'AHA/BHA tẩy da chết',  3),
(N'Nước hoa hồng Thayers',              N'Chai', 250000, N'DuocMiPham', N'Se khít lỗ chân lông', 5),
(N'Kem tay Neutrogena Intensive',        N'Tuýp',  85000, N'DuocMiPham', N'Dưỡng tay khô nẻ',    8),
(N'Dầu dưỡng tóc Moroccanoil',          N'Chai', 750000, N'DuocMiPham', N'Phục hồi tóc hư tổn',  3),
(N'Kem chống hăm Bepanthen',            N'Tuýp', 120000, N'DuocMiPham', N'Bảo vệ da em bé',     8),
(N'Gel trị mụn Benzac AC 5%',           N'Tuýp', 150000, N'DuocMiPham', N'Benzoyl peroxide',     5),
(N'Sữa tắm Eucerin pH5',               N'Chai', 280000, N'DuocMiPham', N'Dưỡng ẩm khi tắm',    5),
(N'Kem dưỡng Clinique Moisture Surge',  N'Hũ',   800000, N'DuocMiPham', N'Cấp ẩm 48h',          3),
(N'Serum HA The Ordinary',              N'Chai', 220000, N'DuocMiPham', N'Hyaluronic acid 2%',   5),
(N'Kem chống nắng Skin Aqua Tone Up',   N'Tuýp', 180000, N'DuocMiPham', N'Chống nắng nâng tông', 5),
(N'Dầu gội trị rụng tóc Ducray',        N'Chai', 380000, N'DuocMiPham', N'Kích thích mọc tóc',   3);
GO

INSERT dbo.SanPham(TenSanPham, DonViTinh, GiaBanDeXuat, LoaiSanPham, MoTa, MucTonToiThieu) VALUES
-- ===== THỰC PHẨM CHỨC NĂNG (81-120) =====
(N'Vitamin C 1000mg DHC',               N'Chai',  150000, N'ThucPhamChucNang', N'Tăng cường miễn dịch',     10),
(N'Omega 3 Fish Oil 1000mg',            N'Chai',  350000, N'ThucPhamChucNang', N'Bổ sung DHA EPA',          8),
(N'Vitamin D3 1000IU',                  N'Chai',  150000, N'ThucPhamChucNang', N'Hỗ trợ hấp thu canxi',     10),
(N'Canxi Nano MK7',                     N'Hộp',   280000, N'ThucPhamChucNang', N'Bổ sung canxi xương khớp', 10),
(N'Vitamin E 400IU Kirkland',           N'Chai',  320000, N'ThucPhamChucNang', N'Chống oxy hóa',            8),
(N'Sắt Folic Acid',                     N'Hộp',    85000, N'ThucPhamChucNang', N'Bổ máu cho bà bầu',        10),
(N'Kẽm Zinc Gluconate 70mg',            N'Chai',  120000, N'ThucPhamChucNang', N'Tăng cường sức khỏe',      10),
(N'B Complex Blackmores',               N'Chai',  280000, N'ThucPhamChucNang', N'Nhóm vitamin B tổng hợp',  8),
(N'Glucosamine 1500mg',                 N'Chai',  450000, N'ThucPhamChucNang', N'Hỗ trợ xương khớp',        8),
(N'Collagen Type 1&3 Neocell',          N'Chai',  550000, N'ThucPhamChucNang', N'Đẹp da, chống lão hóa',    5),
(N'Probiotics 50 Billion CFU',          N'Chai',  420000, N'ThucPhamChucNang', N'Men vi sinh đường ruột',    5),
(N'Melatonin 5mg',                      N'Chai',  180000, N'ThucPhamChucNang', N'Hỗ trợ giấc ngủ',          8),
(N'Multivitamin Centrum Adults',        N'Chai',  380000, N'ThucPhamChucNang', N'Đa vitamin tổng hợp',      8),
(N'Lutein 20mg Eye Support',            N'Chai',  320000, N'ThucPhamChucNang', N'Bổ mắt, chống ánh sáng xanh',5),
(N'Coenzyme Q10 200mg',                 N'Chai',  480000, N'ThucPhamChucNang', N'Hỗ trợ tim mạch',          5),
(N'Spirulina 500mg',                    N'Chai',  250000, N'ThucPhamChucNang', N'Tảo xoắn dinh dưỡng',      8),
(N'Đông trùng hạ thảo viên',            N'Hộp',   650000, N'ThucPhamChucNang', N'Bổ phổi, tăng sức bền',    3),
(N'Sâm Hàn Quốc 6 năm tuổi',           N'Hộp',   980000, N'ThucPhamChucNang', N'Bổ khí huyết',             3),
(N'Bột đạm Whey Protein',               N'Hộp',   750000, N'ThucPhamChucNang', N'Bổ sung protein cơ bắp',   5),
(N'Dầu cá Salmon Oil Kid',              N'Chai',  280000, N'ThucPhamChucNang', N'DHA cho trẻ em',           8),
(N'Viên uống tỏi đen',                  N'Hộp',   350000, N'ThucPhamChucNang', N'Hỗ trợ tim mạch',          5),
(N'Enzyme tiêu hóa Digestive',          N'Chai',  220000, N'ThucPhamChucNang', N'Hỗ trợ tiêu hóa',          8),
(N'Mầm đậu nành Isoflavone',            N'Chai',  180000, N'ThucPhamChucNang', N'Cân bằng nội tiết tố nữ', 8),
(N'Vitamin A 10000IU',                  N'Chai',  120000, N'ThucPhamChucNang', N'Bổ mắt, đẹp da',           10),
(N'Biotin 5000mcg',                     N'Chai',  250000, N'ThucPhamChucNang', N'Đẹp tóc da móng',          8),
(N'Curcumin Nano nghệ vàng',            N'Hộp',   320000, N'ThucPhamChucNang', N'Chống viêm dạ dày',        5),
(N'L-Arginine 1000mg',                  N'Chai',  380000, N'ThucPhamChucNang', N'Hỗ trợ tuần hoàn',         5),
(N'Viên uống giảm cân Detox',           N'Hộp',   280000, N'ThucPhamChucNang', N'Hỗ trợ giảm cân',          5),
(N'Vitamin K2 MK7 100mcg',              N'Chai',  280000, N'ThucPhamChucNang', N'Hỗ trợ hấp thu canxi',     8),
(N'Cốm bổ Lysine cho bé',              N'Hộp',   120000, N'ThucPhamChucNang', N'Giúp bé ăn ngon',          10),
(N'Bổ mắt Bilberry 1000mg',             N'Chai',  300000, N'ThucPhamChucNang', N'Hỗ trợ thị lực',           5),
(N'Men Lactobacillus trẻ em',           N'Hộp',   180000, N'ThucPhamChucNang', N'Hỗ trợ tiêu hóa bé',      8),
(N'Sữa Ensure Gold 850g',               N'Hộp',   680000, N'ThucPhamChucNang', N'Dinh dưỡng người lớn',     5),
(N'Bột ngũ cốc dinh dưỡng',             N'Hộp',   150000, N'ThucPhamChucNang', N'Bổ sung chất xơ',          10),
(N'Viên uống bổ gan Hovenia',           N'Hộp',   280000, N'ThucPhamChucNang', N'Giải độc gan',             5),
(N'Magnesium Bisglycinate 200mg',       N'Chai',  280000, N'ThucPhamChucNang', N'Giảm chuột rút, stress',   8),
(N'Viên uống tóc Perfectil',            N'Hộp',   450000, N'ThucPhamChucNang', N'Đẹp tóc da móng',          5),
(N'Vitamin C kẽm sủi bọt',              N'Tuýp',   45000, N'ThucPhamChucNang', N'Bổ sung vitamin C nhanh', 15),
(N'Tảo Chlorella 500mg',                N'Chai',  220000, N'ThucPhamChucNang', N'Thải độc cơ thể',          8),
(N'Nước yến sào Sanest',                N'Lốc',   280000, N'ThucPhamChucNang', N'Bổ dưỡng cơ thể',          5);
GO

INSERT dbo.SanPham(TenSanPham, DonViTinh, GiaBanDeXuat, LoaiSanPham, MoTa, MucTonToiThieu) VALUES
-- ===== CHĂM SÓC CÁ NHÂN (121-140) =====
(N'Nước rửa tay Lifebuoy 500ml',        N'Chai',   45000, N'ChamSocCaNhan', N'Diệt khuẩn 99.9%',         15),
(N'Kem đánh răng Sensodyne',            N'Tuýp',   65000, N'ChamSocCaNhan', N'Chống ê buốt',             10),
(N'Bàn chải Oral-B Pro Expert',         N'Cái',    45000, N'ChamSocCaNhan', N'Lông mềm chăm sóc nướu',  10),
(N'Nước súc miệng Listerine 750ml',     N'Chai',   85000, N'ChamSocCaNhan', N'Kháng khuẩn miệng',        8),
(N'Dung dịch rửa mũi NaCl 0.9%',       N'Chai',   25000, N'ChamSocCaNhan', N'Vệ sinh mũi hàng ngày',   15),
(N'Băng vệ sinh Diana Sensi',           N'Gói',    28000, N'ChamSocCaNhan', N'Siêu thấm, chống tràn',    15),
(N'Khăn ướt Bobby Care 100 tờ',         N'Gói',    35000, N'ChamSocCaNhan', N'Kháng khuẩn, an toàn bé', 10),
(N'Sữa tắm Dove dưỡng ẩm 900ml',       N'Chai',   120000, N'ChamSocCaNhan', N'Dưỡng ẩm gấp 4 lần',     8),
(N'Dầu gội Clear Men 650ml',            N'Chai',   110000, N'ChamSocCaNhan', N'Trị gàu cho nam',          8),
(N'Xịt khử mùi Nivea Men 150ml',        N'Chai',    65000, N'ChamSocCaNhan', N'Khử mùi 48h',              10),
(N'Bông tẩy trang Ipek 150 miếng',      N'Gói',    35000, N'ChamSocCaNhan', N'Mềm mịn, không xù',       10),
(N'Kem cạo râu Gillette',               N'Tuýp',   55000, N'ChamSocCaNhan', N'Bảo vệ da khi cạo',       8),
(N'Kem chống muỗi Soffell 60ml',        N'Tuýp',   22000, N'ChamSocCaNhan', N'Chống muỗi 8 giờ',        15),
(N'Gel rửa tay khô On1 500ml',          N'Chai',   55000, N'ChamSocCaNhan', N'Diệt khuẩn tay nhanh',    10),
(N'Nước giặt Ariel Matic 3.25kg',       N'Can',   185000, N'ChamSocCaNhan', N'Giặt sạch sâu, thơm lâu', 5),
(N'Tã dán Huggies Dry NB',              N'Gói',   180000, N'ChamSocCaNhan', N'Siêu thấm cho bé sơ sinh',8),
(N'Lăn khử mùi Rexona Women',           N'Chai',   55000, N'ChamSocCaNhan', N'Chống mồ hôi 48h',        10),
(N'Dầu xả Pantene 480ml',               N'Chai',   95000, N'ChamSocCaNhan', N'Phục hồi tóc hư',          8),
(N'Nước hoa Enchanteur Charming',        N'Chai',  185000, N'ChamSocCaNhan', N'Hương tự nhiên Pháp',      5),
(N'Bông gòn y tế cuộn 100g',            N'Cuộn',   15000, N'ChamSocCaNhan', N'Bông gòn tiệt trùng',     15);
GO

INSERT dbo.SanPham(TenSanPham, DonViTinh, GiaBanDeXuat, LoaiSanPham, MoTa, MucTonToiThieu) VALUES
-- ===== THIẾT BỊ Y TẾ (141-150) =====
(N'Nhiệt kế điện tử Omron MC-246',      N'Cái',    85000,  N'ThietBiYTe', N'Đo nhiệt nách/miệng',      3),
(N'Máy đo huyết áp Omron HEM-7156',     N'Cái',   1250000, N'ThietBiYTe', N'Đo bắp tay tự động',       3),
(N'Máy đo đường huyết Accu-Chek',       N'Cái',    850000, N'ThietBiYTe', N'Kiểm tra glucose nhanh',    3),
(N'Máy xông mũi họng Omron NE-C28',     N'Cái',    980000, N'ThietBiYTe', N'Xông hơi khí dung',         3),
(N'Máy đo SpO2 kẹp ngón',               N'Cái',    350000, N'ThietBiYTe', N'Đo nồng độ oxy máu',        5),
(N'Nẹp cổ tay y khoa',                  N'Cái',    120000, N'ThietBiYTe', N'Hỗ trợ cổ tay bong gân',    5),
(N'Bộ sơ cứu gia đình 25 món',          N'Bộ',    250000, N'ThietBiYTe', N'Đồ sơ cứu cơ bản',          5),
(N'Khẩu trang y tế 3 lớp (50 cái)',     N'Hộp',    45000,  N'ThietBiYTe', N'Kháng khuẩn, BFE 99%',     20),
(N'Găng tay y tế latex (100 cái)',       N'Hộp',    85000,  N'ThietBiYTe', N'Không bột, tiệt trùng',    10),
(N'Cân sức khỏe điện tử Tanita',        N'Cái',    480000, N'ThietBiYTe', N'Đo BMI, mỡ cơ thể',        3);
GO

/* Demo nhập hàng: tạo phiếu nhập + nhập 2 lô khác hạn cho cùng sản phẩm */
DECLARE @MaPhieuNhap INT, @HSD1 date, @HSD2 date;

DECLARE @tPN TABLE (MaPhieuNhap INT);
INSERT INTO @tPN(MaPhieuNhap)
EXEC dbo.sp_PhieuNhap_Create @MaNguoiDung=2, @MaNCC=1, @GhiChu=N'Nhập mẫu';

SELECT @MaPhieuNhap = MaPhieuNhap FROM @tPN;

SET @HSD1 = CAST(GETDATE() + 20 AS date);
SET @HSD2 = CAST(GETDATE() + 40 AS date);

EXEC dbo.sp_PhieuNhap_AddItem_Batch @MaPhieuNhap=@MaPhieuNhap, @MaSanPham=1, @SoLo=N'LO001',
    @HanSuDung=@HSD1, @GiaNhap=9000, @SoLuong=10;

EXEC dbo.sp_PhieuNhap_AddItem_Batch @MaPhieuNhap=@MaPhieuNhap, @MaSanPham=1, @SoLo=N'LO002',
    @HanSuDung=@HSD2, @GiaNhap=9500, @SoLuong=10;
GO
/* Demo bán hàng:
   - Tạo hóa đơn
   - Bán 15 hộp Paracetamol: Proc sẽ tự FEFO (xuất LO001 trước rồi LO002)
*/
DECLARE @MaHoaDon INT;
DECLARE @tHD TABLE (MaHoaDon INT);

INSERT INTO @tHD(MaHoaDon)
EXEC dbo.sp_HoaDonBan_Create @MaNguoiDung=3, @MaKhachHang=NULL, @GhiChu=N'Bán lẻ';

SELECT TOP 1 @MaHoaDon = MaHoaDon FROM @tHD;

EXEC dbo.sp_HoaDonBan_Sell_FEFO @MaHoaDon=@MaHoaDon, @MaSanPham=1, @SoLuongCanBan=15, @GiaBan=NULL;
GO


/* Test cảnh báo dashboard */
SELECT * FROM dbo.v_CanhBaoHetHang;
SELECT * FROM dbo.v_CanhBaoLoSapHetHan;
SELECT * FROM dbo.v_CanhBaoLoHetHan;

EXEC dbo.sp_CanhBao_LoSapHetHan @SoNgay=50;
EXEC dbo.sp_CanhBao_LoHetHan;
GO

/* =========================
   13) PROC: PHÂN TRANG SẢN PHẨM (v2)
   - Hỗ trợ tìm kiếm (@Keyword)
   - Hỗ trợ sắp xếp động (@SortColumn, @SortOrder)
   - Dùng OFFSET / FETCH NEXT
   - Trả về TotalRows + TotalPages
   ========================= */
CREATE OR ALTER PROC dbo.sp_SanPham_GetPage
    @PageNumber  INT = 1,
    @PageSize    INT = 10,
    @Keyword     NVARCHAR(255) = NULL,
    @SortColumn  NVARCHAR(50)  = N'MaSanPham',
    @SortOrder   NVARCHAR(4)   = N'ASC'
AS
BEGIN
    SET NOCOUNT ON;

    -- Validate sort order
    IF @SortOrder NOT IN (N'ASC', N'DESC')
        SET @SortOrder = N'ASC';

    -- Validate sort column
    IF @SortColumn NOT IN (N'MaSanPham', N'TenSanPham', N'DonViTinh',
                           N'GiaBanDeXuat', N'LoaiSanPham', N'MoTa', N'MucTonToiThieu')
        SET @SortColumn = N'MaSanPham';

    -- Trim keyword
    SET @Keyword = LTRIM(RTRIM(@Keyword));
    IF @Keyword = N'' SET @Keyword = NULL;

    -- Count total rows (keyword-aware)
    DECLARE @TotalRows INT;
    SELECT @TotalRows = COUNT(*)
    FROM dbo.SanPham
    WHERE DaXoa = 0
      AND (@Keyword IS NULL OR TenSanPham LIKE N'%' + @Keyword + N'%');

    -- Query with dynamic sort
    SELECT
        sp.MaSanPham,
        sp.TenSanPham,
        sp.DonViTinh,
        sp.GiaBanDeXuat,
        sp.LoaiSanPham,
        sp.MoTa,
        sp.MucTonToiThieu,
        ISNULL(tk.TongTon, 0) AS TongTon,
        @TotalRows              AS TotalRows,
        CEILING(CAST(@TotalRows AS FLOAT) / @PageSize) AS TotalPages
    FROM dbo.SanPham sp
    LEFT JOIN dbo.v_TonKhoSanPham tk ON tk.MaSanPham = sp.MaSanPham
    WHERE sp.DaXoa = 0
      AND (@Keyword IS NULL OR sp.TenSanPham LIKE N'%' + @Keyword + N'%')
    ORDER BY
        CASE WHEN @SortOrder = N'ASC' THEN
            CASE @SortColumn
                WHEN N'MaSanPham'      THEN RIGHT('0000000000' + CAST(sp.MaSanPham AS NVARCHAR), 10)
                WHEN N'TenSanPham'     THEN sp.TenSanPham
                WHEN N'DonViTinh'      THEN sp.DonViTinh
                WHEN N'GiaBanDeXuat'   THEN RIGHT('0000000000' + CAST(CAST(sp.GiaBanDeXuat AS BIGINT) AS NVARCHAR), 10)
                WHEN N'LoaiSanPham'    THEN sp.LoaiSanPham
                WHEN N'MoTa'           THEN ISNULL(sp.MoTa, N'')
                WHEN N'MucTonToiThieu' THEN RIGHT('0000000000' + CAST(sp.MucTonToiThieu AS NVARCHAR), 10)
            END
        END ASC,
        CASE WHEN @SortOrder = N'DESC' THEN
            CASE @SortColumn
                WHEN N'MaSanPham'      THEN RIGHT('0000000000' + CAST(sp.MaSanPham AS NVARCHAR), 10)
                WHEN N'TenSanPham'     THEN sp.TenSanPham
                WHEN N'DonViTinh'      THEN sp.DonViTinh
                WHEN N'GiaBanDeXuat'   THEN RIGHT('0000000000' + CAST(CAST(sp.GiaBanDeXuat AS BIGINT) AS NVARCHAR), 10)
                WHEN N'LoaiSanPham'    THEN sp.LoaiSanPham
                WHEN N'MoTa'           THEN ISNULL(sp.MoTa, N'')
                WHEN N'MucTonToiThieu' THEN RIGHT('0000000000' + CAST(sp.MucTonToiThieu AS NVARCHAR), 10)
            END
        END DESC
    OFFSET (@PageNumber - 1) * @PageSize ROWS
    FETCH NEXT @PageSize ROWS ONLY;
END
GO

ALTER TABLE dbo.PhieuNhap
ADD CONSTRAINT FK_PhieuNhap_NCC
FOREIGN KEY (MaNCC) REFERENCES dbo.NhaCungCap(MaNCC);