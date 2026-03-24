# 🗄️ DATABASE DESIGN — CỬA HÀNG THUỐC (PHARMACY STORE)

> **DBMS:** SQL Server | **Database:** `QuanLyCuaHangThuoc` | **Collation:** Vietnamese_CI_AS  
> **Cập nhật:** 22/03/2026

---

## 📊 ERD

```
┌──────────────────┐        ┌──────────────┐         ┌───────────────────┐
│   NguoiDung      │        │   SanPham    │         │      LoHang       │
│──────────────────│        │──────────────│         │───────────────────│
│ PK MaND          │◄─┐     │ PK MaSP     │◄────────│ PK MaLo           │
│ TenDangNhap (UQ) │  │     │ TenSP       │  1:N    │ FK MaSP           │
│ MatKhau          │  │     │ DonViTinh   │         │ SoLo              │
│ HoTen            │  │     │ GiaBan      │         │ HanSuDung         │
│ VaiTro           │  │     │ TrangThai   │         │ SoLuong           │
│ TrangThai        │  │     │ NgayTao     │         │ GiaNhap ★         │
│ NgayTao          │  │     └──────┬──────┘         │ NgayNhap          │
└──────┬───────────┘  │            │                 │ FK MaND ★         │
       │ 1:N          └────────────┼─────────────────│ UQ(MaSP,SoLo) ★   │
       │                           │                 └────────┬──────────┘
┌──────┴───────┐          ┌────────┴──────────────────┐       │ 1:N
│   HoaDon     │          │      ChiTietHoaDon        │       │
│──────────────│          │───────────────────────────│       │
│ PK MaHD      │◄─────────│ PK MaCTHD                 │       │
│ FK MaKH      │    1:N   │ FK MaHD                   │       │
│ FK MaND      │          │ FK MaLo ──────────────────┼───────┘
│ NgayBan      │          │ FK MaSP                   │
│ TongTien     │          │ SoLuong, DonGia           │
│ PhuongThucTT★│          │ GiaVon ★, ThanhTien       │
└──────────────┘          └───────────────────────────┘
       ▲ 1:N
┌──────┴───────┐
│  KhachHang   │   ★ = Thêm sau migration (fix kiến trúc tài chính)
│──────────────│
│ PK MaKH      │
│ SoDT (UQ)    │
│ TenKH, NgayTao│
└──────────────┘
```

---

## 📋 CHI TIẾT CÁC BẢNG

### Bảng 1: `NguoiDung`
| Cột | Kiểu | Null | Default | Ghi chú |
|-----|------|------|---------|---------|
| MaND | INT IDENTITY | NO | — | PK |
| TenDangNhap | NVARCHAR(50) | NO | — | UNIQUE |
| MatKhau | NVARCHAR(255) | NO | — | |
| HoTen | NVARCHAR(100) | NO | — | |
| VaiTro | NVARCHAR(20) | NO | N'NhanVien' | CHECK: Admin/NhanVien |
| TrangThai | BIT | NO | 1 | 1=Active, 0=Locked |
| NgayTao | DATETIME | NO | GETDATE() | |

> **Trigger `trg_PreventAdminLock`**: Chặn nếu sau UPDATE/DELETE không còn Admin active nào → ROLLBACK

### Bảng 2: `SanPham` (Master Data — chỉ lưu giá bán, KHÔNG lưu giá nhập)
| Cột | Kiểu | Null | Default | Ghi chú |
|-----|------|------|---------|---------|
| MaSP | INT IDENTITY | NO | — | PK |
| TenSP | NVARCHAR(200) | NO | — | |
| DonViTinh | NVARCHAR(50) | NO | — | Viên, Hộp, Chai... |
| GiaBan | DECIMAL(18,0) | NO | — | Giá niêm yết |
| TrangThai | BIT | NO | 1 | Soft delete |
| NgayTao | DATETIME | NO | GETDATE() | |

### Bảng 3: `LoHang` (Tồn kho thực tế — giá vốn theo lô)
| Cột | Kiểu | Null | Default | Ghi chú |
|-----|------|------|---------|---------|
| MaLo | INT IDENTITY | NO | — | PK |
| MaSP | INT | NO | — | FK → SanPham |
| SoLo | NVARCHAR(50) | NO | — | UNIQUE(MaSP,SoLo) |
| HanSuDung | DATE | NO | — | |
| SoLuong | INT | NO | 0 | CHECK ≥ 0, giảm khi bán |
| **GiaNhap** | DECIMAL(18,0) | NO | 0 | ★ Giá vốn theo lô |
| NgayNhap | DATETIME | NO | GETDATE() | |
| **MaND** | INT | YES | NULL | ★ FK → NguoiDung (ai nhập) |

### Bảng 4: `KhachHang`
| Cột | Kiểu | Null | Default |
|-----|------|------|---------|
| MaKH | INT IDENTITY | NO | PK |
| SoDT | VARCHAR(15) | NO | UNIQUE |
| TenKH | NVARCHAR(100) | YES | NULL |
| NgayTao | DATETIME | NO | GETDATE() |

### Bảng 5: `HoaDon`
| Cột | Kiểu | Null | Default | Ghi chú |
|-----|------|------|---------|---------|
| MaHD | INT IDENTITY | NO | — | PK |
| MaKH | INT | YES | NULL | FK, NULL=vãng lai |
| MaND | INT | NO | — | FK → NguoiDung |
| NgayBan | DATETIME | NO | GETDATE() | |
| TongTien | DECIMAL(18,0) | NO | 0 | |
| **PhuongThucTT** | NVARCHAR(20) | NO | N'TienMat' | ★ TienMat/ChuyenKhoan/QR |

### Bảng 6: `ChiTietHoaDon`
| Cột | Kiểu | Null | Default | Ghi chú |
|-----|------|------|---------|---------|
| MaCTHD | INT IDENTITY | NO | — | PK |
| MaHD | INT | NO | — | FK → HoaDon |
| MaLo | INT | NO | — | FK → LoHang (FEFO chọn) |
| MaSP | INT | NO | — | FK → SanPham |
| SoLuong | INT | NO | — | CHECK > 0 |
| DonGia | DECIMAL(18,0) | NO | — | Giá bán snapshot |
| **GiaVon** | DECIMAL(18,0) | NO | 0 | ★ Giá vốn snapshot từ lô |
| ThanhTien | DECIMAL(18,0) | NO | — | SoLuong × DonGia |

> `Lợi nhuận = (DonGia - GiaVon) × SoLuong` — luôn chính xác dù giá nhập thay đổi

---

## 📊 VIEWS (4) + STORED PROCEDURES (8) + TRIGGER (1) + INDEXES (8)

| Loại | Tên | Mục đích |
|------|-----|----------|
| VIEW | `vw_TonKhoChiTiet` | Tồn kho chi tiết (JOIN SP+LoHang+NguoiDung) |
| VIEW | `vw_TonKhoTheoSanPham` | Tổng tồn kho gộp theo SP |
| VIEW | `vw_LoiNhuan` ★ | Lợi nhuận dựa trên GiaVon snapshot |
| VIEW | `vw_DoiSoatCuoiNgay` ★ | Đối soát cuối ngày theo PT thanh toán |
| SP | `sp_DoanhThu(@LoaiThoiGian)` | Doanh thu TODAY/MONTH/QUARTER |
| SP | `sp_CanhBaoHetHan` | Lô sắp hết hạn ≤ 3 tháng |
| SP | `sp_TopBanChay(@TopN)` | Top N SP bán chạy |
| SP | `sp_TopKhachVIP(@TopN)` | Top N KH VIP |
| SP | `sp_GetFEFO(@MaSP)` | FEFO + **UPDLOCK, ROWLOCK** ★ |
| SP | `sp_LichSuKhachHang(@MaKH)` | Lịch sử hóa đơn KH |
| SP | `sp_ChiTietHoaDon(@MaHD)` | Chi tiết 1 hóa đơn |
| SP | `sp_TimSanPham(@Keyword)` | Tìm thuốc cho POS |
| TRIGGER | `trg_PreventAdminLock` | Chống xóa/khóa Admin cuối |

---

## 🛡️ CƠ CHẾ BẢO VỆ DỮ LIỆU

| Rủi ro | Cơ chế |
|--------|--------|
| 2 NV bán cùng hộp thuốc cuối | `WITH (UPDLOCK, ROWLOCK)` + Transaction |
| Admin tự khóa chính mình | `trg_PreventAdminLock` → ROLLBACK |
| Nhập trùng lô → dữ liệu rác | `UNIQUE(MaSP,SoLo)` + findByLot() |
| Giá nhập đổi → sai báo cáo quá khứ | GiaVon snapshot trong ChiTietHoaDon |
| Tồn kho âm | `CHECK(SoLuong >= 0)` + FEFO rollback |
| Gian lận tiền mặt | `PhuongThucTT` + View đối soát |

---

## 🔄 MIGRATION HISTORY (7 files)

| # | File | Mô tả |
|---|------|-------|
| 1 | `01_create_database.sql` | Tạo DB + 6 bảng + indexes |
| 2 | `02_seed_data.sql` | Seed data mẫu |
| 3 | `03_stored_procedures.sql` | 2 Views + 8 SPs (bản gốc) |
| 4 | `04_verify_data.sql` | Script kiểm tra |
| 5 | `05_migration_financial_fix.sql` | ★ Dời GiaNhap, thêm GiaVon + PhuongThucTT |
| 6 | `06_edge_case_fixes.sql` | ★ UPDLOCK, Trigger Admin, MaND vào LoHang |
| 7 | `07_unique_lot_constraint.sql` | ★ UNIQUE(MaSP,SoLo) |

> **Thứ tự chạy bắt buộc:** 01 → 02 → 03 → 04 → 05 → 06 → 07

---

## 📄 TOÀN BỘ SQL SCRIPTS

> ⚠️ Các file 01-03 dưới đây là **bản GỐC** (chưa migration). Phải chạy tiếp file 05, 06, 07 để có cấu trúc DB hiện tại.  
> Nếu setup từ đầu: chạy lần lượt 01 → 02 → 03 → 05 → 06 → 07.

### File 1: `01_create_database.sql`

```sql
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

CREATE TABLE KhachHang (
    MaKH    INT IDENTITY(1,1) PRIMARY KEY,
    SoDT    VARCHAR(15)   NOT NULL,
    TenKH   NVARCHAR(100) NULL,
    NgayTao DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT UQ_KhachHang_SoDT UNIQUE (SoDT)
);
GO

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

-- INDEXES
CREATE NONCLUSTERED INDEX IX_LoHang_FEFO ON LoHang (MaSP, HanSuDung ASC) WHERE SoLuong > 0;
CREATE NONCLUSTERED INDEX IX_KhachHang_SoDT ON KhachHang (SoDT);
CREATE NONCLUSTERED INDEX IX_HoaDon_MaKH ON HoaDon (MaKH) WHERE MaKH IS NOT NULL;
CREATE NONCLUSTERED INDEX IX_HoaDon_NgayBan ON HoaDon (NgayBan DESC);
CREATE NONCLUSTERED INDEX IX_CTHD_MaHD ON ChiTietHoaDon (MaHD);
CREATE NONCLUSTERED INDEX IX_CTHD_MaSP ON ChiTietHoaDon (MaSP);
CREATE NONCLUSTERED INDEX IX_SanPham_TrangThai ON SanPham (TrangThai) WHERE TrangThai = 1;
CREATE NONCLUSTERED INDEX IX_LoHang_MaSP ON LoHang (MaSP);
GO
```

---

### File 2: `02_seed_data.sql`

```sql
USE QuanLyCuaHangThuoc;
GO

INSERT INTO NguoiDung (TenDangNhap, MatKhau, HoTen, VaiTro) VALUES
(N'admin',       N'admin123',  N'Nguyễn Văn Admin',    N'Admin'),
(N'admin02',     N'admin123',  N'Trần Thị Quản Lý',   N'Admin'),
(N'nhanvien01',  N'nv123',     N'Lê Văn Nhân Viên',   N'NhanVien'),
(N'nhanvien02',  N'nv123',     N'Phạm Thị Bán Hàng',  N'NhanVien');
GO

INSERT INTO SanPham (TenSP, DonViTinh, GiaNhap, GiaBan) VALUES
(N'Paracetamol 500mg',           N'Viên',  800,   1500),
(N'Ibuprofen 400mg',             N'Viên',  1200,  2500),
(N'Aspirin 81mg',                N'Viên',  600,   1200),
(N'Amoxicillin 500mg',           N'Viên',  1500,  3000),
(N'Azithromycin 250mg',          N'Viên',  3000,  5500),
(N'Cefixime 200mg',              N'Viên',  4000,  7000),
(N'Ciprofloxacin 500mg',         N'Viên',  2000,  4000),
(N'Omeprazole 20mg',             N'Viên',  1000,  2200),
(N'Pantoprazole 40mg',           N'Viên',  2500,  4500),
(N'Gaviscon Suspension',         N'Chai',  45000, 75000),
(N'Dextromethorphan 15mg',       N'Viên',  800,   1800),
(N'Acetylcysteine 200mg',        N'Gói',   1500,  3000),
(N'Tiffy Dey',                   N'Viên',  500,   1200),
(N'Decolgen ND',                 N'Viên',  600,   1500),
(N'Vitamin C 1000mg',            N'Viên',  1000,  2500),
(N'Vitamin B Complex',           N'Viên',  800,   1800),
(N'Calcium 600mg + D3',          N'Viên',  1200,  2800),
(N'Omega-3 Fish Oil 1000mg',     N'Viên',  2500,  5000),
(N'Cetirizine 10mg',             N'Viên',  600,   1500),
(N'Loratadine 10mg',             N'Viên',  700,   1600),
(N'Smecta 3g',                   N'Gói',   3000,  5500),
(N'Domperidone 10mg',            N'Viên',  800,   1800),
(N'Betadine 10% 125ml',          N'Chai',  35000, 55000),
(N'Clotrimazole Cream 1%',       N'Tuýp',  15000, 28000),
(N'Natri Clorid 0.9% (Nhỏ mắt)',N'Lọ',    8000,  15000);
GO

-- LÔ HÀNG (19 lô)
INSERT INTO LoHang (MaSP, SoLo, HanSuDung, SoLuong, NgayNhap) VALUES
(1,  N'LOT-PAR-001', '2026-05-15', 200, '2026-01-10'),
(1,  N'LOT-PAR-002', '2027-03-20', 500, '2026-02-15'),
(1,  N'LOT-PAR-003', '2027-08-01', 300, '2026-03-01'),
(4,  N'LOT-AMO-001', '2026-06-30', 150, '2026-01-20'),
(4,  N'LOT-AMO-002', '2027-06-30', 400, '2026-03-05'),
(5,  N'LOT-AZI-001', '2027-12-01', 200, '2026-02-20'),
(8,  N'LOT-OME-001', '2026-04-10', 100, '2025-12-01'),
(8,  N'LOT-OME-002', '2027-09-15', 350, '2026-03-10'),
(10, N'LOT-GAV-001', '2027-07-20', 80,  '2026-02-01'),
(15, N'LOT-VTC-001', '2026-06-01', 300, '2025-11-15'),
(15, N'LOT-VTC-002', '2027-12-31', 600, '2026-03-01'),
(2,  N'LOT-IBU-001', '2027-11-30', 400, '2026-03-05'),
(19, N'LOT-CET-001', '2027-10-15', 250, '2026-01-25'),
(23, N'LOT-BET-001', '2028-01-01', 60,  '2026-02-10'),
(13, N'LOT-TIF-001', '2026-05-20', 500, '2025-10-10'),
(13, N'LOT-TIF-002', '2027-05-20', 800, '2026-02-20'),
(21, N'LOT-SME-001', '2027-08-08', 200, '2026-01-30'),
(14, N'LOT-DEC-001', '2027-04-15', 350, '2026-02-28'),
(17, N'LOT-CAL-001', '2027-10-01', 180, '2026-03-15');
GO

-- KHÁCH HÀNG + HÓA ĐƠN (xem file gốc 02_seed_data.sql)
INSERT INTO KhachHang (SoDT, TenKH) VALUES
('0901234567', N'Nguyễn Thị Lan'),  ('0912345678', N'Trần Văn Minh'),
('0923456789', N'Lê Hoàng Anh'),    ('0934567890', N'Phạm Thị Hương'),
('0945678901', N'Võ Đình Khoa');
GO

-- 6 HÓA ĐƠN MẪU + CHI TIẾT + TRỪ KHO (chi tiết xem file gốc)
```

---

### File 3: `03_stored_procedures.sql`

```sql
USE QuanLyCuaHangThuoc;
GO

-- VIEW 1: Tồn kho chi tiết
CREATE OR ALTER VIEW vw_TonKhoChiTiet AS
SELECT sp.MaSP, sp.TenSP, sp.DonViTinh, sp.GiaNhap, sp.GiaBan,
       lh.MaLo, lh.SoLo, lh.HanSuDung, lh.SoLuong AS SoLuongConLai, lh.NgayNhap,
       CASE WHEN lh.HanSuDung <= GETDATE() THEN N'ĐÃ HẾT HẠN'
            WHEN lh.HanSuDung <= DATEADD(MONTH, 3, GETDATE()) THEN N'SẮP HẾT HẠN'
            ELSE N'Còn hạn' END AS TrangThaiHSD
FROM LoHang lh INNER JOIN SanPham sp ON lh.MaSP = sp.MaSP
WHERE lh.SoLuong > 0 AND sp.TrangThai = 1;
GO

-- VIEW 2: Tồn kho gộp theo SP
CREATE OR ALTER VIEW vw_TonKhoTheoSanPham AS
SELECT sp.MaSP, sp.TenSP, sp.DonViTinh, sp.GiaNhap, sp.GiaBan, sp.TrangThai,
       ISNULL(SUM(lh.SoLuong), 0) AS TongTonKho
FROM SanPham sp LEFT JOIN LoHang lh ON sp.MaSP = lh.MaSP AND lh.SoLuong > 0
GROUP BY sp.MaSP, sp.TenSP, sp.DonViTinh, sp.GiaNhap, sp.GiaBan, sp.TrangThai;
GO

-- SP 1-8: (xem file gốc 03_stored_procedures.sql)
-- sp_DoanhThu, sp_CanhBaoHetHan, sp_TopBanChay, sp_TopKhachVIP,
-- sp_GetFEFO, sp_LichSuKhachHang, sp_ChiTietHoaDon, sp_TimSanPham
```

---

### File 5: `05_migration_financial_fix.sql` ★ QUAN TRỌNG

```sql
USE QuanLyCuaHangThuoc;
GO

-- ★ FIX 1: Dời GiaNhap từ SanPham → LoHang
ALTER TABLE LoHang ADD GiaNhap DECIMAL(18,0) NOT NULL DEFAULT 0;
UPDATE lh SET lh.GiaNhap = sp.GiaNhap FROM LoHang lh JOIN SanPham sp ON lh.MaSP = sp.MaSP;
ALTER TABLE LoHang ADD CONSTRAINT CK_LoHang_GiaNhap CHECK (GiaNhap >= 0);
ALTER TABLE SanPham DROP CONSTRAINT CK_SanPham_Gia;
ALTER TABLE SanPham DROP CONSTRAINT CK_SanPham_GiaNhap;
ALTER TABLE SanPham DROP COLUMN GiaNhap;
GO

-- ★ FIX 2: Thêm GiaVon vào ChiTietHoaDon (snapshot giá vốn lúc bán)
ALTER TABLE ChiTietHoaDon ADD GiaVon DECIMAL(18,0) NOT NULL DEFAULT 0;
UPDATE ct SET ct.GiaVon = lh.GiaNhap FROM ChiTietHoaDon ct JOIN LoHang lh ON ct.MaLo = lh.MaLo;
ALTER TABLE ChiTietHoaDon ADD CONSTRAINT CK_CTHD_GiaVon CHECK (GiaVon >= 0);
GO

-- ★ FIX 3: Thêm PhuongThucTT vào HoaDon
ALTER TABLE HoaDon ADD PhuongThucTT NVARCHAR(20) NOT NULL DEFAULT N'TienMat';
ALTER TABLE HoaDon ADD CONSTRAINT CK_HoaDon_PhuongThucTT
    CHECK (PhuongThucTT IN (N'TienMat', N'ChuyenKhoan', N'QR'));
GO

-- ★ View lợi nhuận
CREATE VIEW vw_LoiNhuan AS
SELECT hd.MaHD, hd.NgayBan, sp.TenSP, ct.SoLuong,
       ct.DonGia AS GiaBan, ct.GiaVon,
       (ct.DonGia - ct.GiaVon) * ct.SoLuong AS LoiNhuan, ct.ThanhTien AS DoanhThu
FROM ChiTietHoaDon ct JOIN HoaDon hd ON ct.MaHD = hd.MaHD JOIN SanPham sp ON ct.MaSP = sp.MaSP;
GO

-- ★ View đối soát cuối ngày
CREATE VIEW vw_DoiSoatCuoiNgay AS
SELECT CAST(NgayBan AS DATE) AS Ngay, PhuongThucTT,
       COUNT(*) AS SoHoaDon, SUM(TongTien) AS TongTien
FROM HoaDon GROUP BY CAST(NgayBan AS DATE), PhuongThucTT;
GO
```

---

### File 6: `06_edge_case_fixes.sql` ★ QUAN TRỌNG

```sql
USE QuanLyCuaHangThuoc;
GO

-- ★ FIX 1: FEFO với UPDLOCK chống Race Condition
CREATE OR ALTER PROCEDURE sp_GetFEFO @MaSP INT
AS BEGIN
    SET NOCOUNT ON;
    SELECT MaLo, MaSP, SoLo, HanSuDung, SoLuong, GiaNhap
    FROM LoHang WITH (UPDLOCK, ROWLOCK)
    WHERE MaSP = @MaSP AND SoLuong > 0 AND HanSuDung > GETDATE()
    ORDER BY HanSuDung ASC;
END;
GO

-- ★ FIX 2: Trigger chống Admin tự khóa/xóa
CREATE TRIGGER trg_PreventAdminLock ON NguoiDung AFTER UPDATE, DELETE
AS BEGIN
    SET NOCOUNT ON;
    IF (SELECT COUNT(*) FROM NguoiDung WHERE VaiTro = N'Admin' AND TrangThai = 1) = 0
    BEGIN
        ROLLBACK TRANSACTION;
        RAISERROR(N'Hệ thống phải có ít nhất 1 Admin hoạt động!', 16, 1);
    END
END;
GO

-- ★ FIX 3: Thêm MaND (người nhập) vào LoHang
ALTER TABLE LoHang ADD MaND INT NULL;
ALTER TABLE LoHang ADD CONSTRAINT FK_LoHang_NguoiDung FOREIGN KEY (MaND) REFERENCES NguoiDung(MaND);
UPDATE LoHang SET MaND = (SELECT TOP 1 MaND FROM NguoiDung WHERE VaiTro = N'Admin');
GO

-- ★ Cập nhật Views (GiaNhap từ LoHang, thêm NguoiNhap)
CREATE OR ALTER VIEW vw_TonKhoChiTiet AS
SELECT sp.MaSP, sp.TenSP, sp.DonViTinh, lh.GiaNhap, sp.GiaBan,
       lh.MaLo, lh.SoLo, lh.HanSuDung, lh.SoLuong AS SoLuongConLai,
       lh.NgayNhap, lh.MaND AS MaNguoiNhap, nd.HoTen AS TenNguoiNhap,
       CASE WHEN lh.HanSuDung <= GETDATE() THEN N'ĐÃ HẾT HẠN'
            WHEN lh.HanSuDung <= DATEADD(MONTH, 3, GETDATE()) THEN N'SẮP HẾT HẠN'
            ELSE N'Còn hạn' END AS TrangThaiHSD
FROM LoHang lh JOIN SanPham sp ON lh.MaSP = sp.MaSP
LEFT JOIN NguoiDung nd ON lh.MaND = nd.MaND
WHERE lh.SoLuong > 0 AND sp.TrangThai = 1;
GO

CREATE OR ALTER VIEW vw_TonKhoTheoSanPham AS
SELECT sp.MaSP, sp.TenSP, sp.DonViTinh, sp.GiaBan, sp.TrangThai,
       ISNULL(SUM(lh.SoLuong), 0) AS TongTonKho
FROM SanPham sp LEFT JOIN LoHang lh ON sp.MaSP = lh.MaSP AND lh.SoLuong > 0
GROUP BY sp.MaSP, sp.TenSP, sp.DonViTinh, sp.GiaBan, sp.TrangThai;
GO
```

---

### File 7: `07_unique_lot_constraint.sql` ★ QUAN TRỌNG

```sql
USE QuanLyCuaHangThuoc;
GO

-- Gộp dữ liệu trùng (nếu có)
;WITH Duplicates AS (
    SELECT MaLo, MaSP, SoLo, SoLuong,
           ROW_NUMBER() OVER (PARTITION BY MaSP, SoLo ORDER BY MaLo ASC) AS RowNum
    FROM LoHang
)
UPDATE lh SET lh.SoLuong = lh.SoLuong + dup.TotalExtra
FROM LoHang lh
JOIN (SELECT MaSP, SoLo, SUM(SoLuong) AS TotalExtra
      FROM Duplicates WHERE RowNum > 1 GROUP BY MaSP, SoLo) dup
ON lh.MaSP = dup.MaSP AND lh.SoLo = dup.SoLo
WHERE lh.MaLo = (SELECT TOP 1 d2.MaLo FROM Duplicates d2
    WHERE d2.MaSP = lh.MaSP AND d2.SoLo = lh.SoLo AND d2.RowNum = 1);
GO

;WITH Duplicates AS (
    SELECT MaLo, ROW_NUMBER() OVER (PARTITION BY MaSP, SoLo ORDER BY MaLo ASC) AS RowNum
    FROM LoHang
)
DELETE FROM Duplicates WHERE RowNum > 1;
GO

-- ★ UNIQUE: 1 sản phẩm + 1 số lô = 1 dòng duy nhất
ALTER TABLE LoHang ADD CONSTRAINT UQ_LoHang_MaSP_SoLo UNIQUE (MaSP, SoLo);
GO
```
