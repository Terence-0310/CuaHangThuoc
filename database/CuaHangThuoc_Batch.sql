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
VALUES (N'Dược A', N'028111', N'a@ncc.com', N'HCM');
GO

INSERT dbo.SanPham(TenSanPham, DonViTinh, GiaBanDeXuat, LoaiSanPham, MucTonToiThieu)
VALUES
(N'Paracetamol 500mg', N'Hộp', 15000, N'Thuoc', 10),
(N'Vitamin C',         N'Chai', 50000, N'ThucPhamChucNang', 10);
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

ALTER TABLE dbo.PhieuNhap
ADD CONSTRAINT FK_PhieuNhap_NCC
FOREIGN KEY (MaNCC) REFERENCES dbo.NhaCungCap(MaNCC);