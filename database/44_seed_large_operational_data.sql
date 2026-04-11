USE QuanLyCuaHangThuoc;
GO

SET NOCOUNT ON;

-- Idempotency marker: skip if already seeded.
IF EXISTS (SELECT 1 FROM SystemLogs WHERE HanhDong = N'SEED_LARGE_DATA_V1')
BEGIN
    PRINT N'Large operational seed already applied. Skipped.';
    RETURN;
END
GO

-- 1) Ensure enough regions
;WITH nums AS (
    SELECT TOP (8) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n
    FROM sys.all_objects
)
INSERT INTO KhuVuc (MaKhuVucCode, TenKhuVuc, TinhThanh, TrangThai)
SELECT
    CONCAT(N'KV-', RIGHT(CONCAT('00', n), 2)),
    CONCAT(N'Khu vuc ', n),
    CHOOSE(((n - 1) % 5) + 1, N'Ho Chi Minh', N'Ha Noi', N'Da Nang', N'Can Tho', N'Hai Phong'),
    1
FROM nums
WHERE NOT EXISTS (
    SELECT 1 FROM KhuVuc kv WHERE kv.MaKhuVucCode = CONCAT(N'KV-', RIGHT(CONCAT('00', nums.n), 2))
);
GO

-- 2) Ensure enough branches
;WITH nums AS (
    SELECT TOP (24) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n
    FROM sys.all_objects
),
regions AS (
    SELECT MaKhuVuc, ROW_NUMBER() OVER (ORDER BY MaKhuVuc) AS rn
    FROM KhuVuc
)
INSERT INTO ChiNhanh (MaCNCode, TenChiNhanh, DiaChi, QuanHuyen, MaKhuVuc, MaVung, TinhThanh, TrangThai)
SELECT
    CONCAT(N'CN-', RIGHT(CONCAT('000', n), 3)),
    CONCAT(N'Chi nhanh ', n),
    CONCAT(N'So ', 10 + n, N' Duong Trung Tam'),
    CONCAT(N'Quan/Huyen ', ((n - 1) % 12) + 1),
    r.MaKhuVuc,
    CONCAT(N'R-', RIGHT(CONCAT('00', r.MaKhuVuc), 2)),
    CHOOSE(((n - 1) % 5) + 1, N'Ho Chi Minh', N'Ha Noi', N'Da Nang', N'Can Tho', N'Hai Phong'),
    1
FROM nums n
JOIN regions r ON r.rn = ((n.n - 1) % (SELECT COUNT(*) FROM regions)) + 1
WHERE NOT EXISTS (
    SELECT 1 FROM ChiNhanh cn WHERE cn.MaCNCode = CONCAT(N'CN-', RIGHT(CONCAT('000', n.n), 3))
);
GO

-- 3) Add more active suppliers
;WITH nums AS (
    SELECT TOP (60) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n
    FROM sys.all_objects
)
INSERT INTO NhaCungCap (TenNCC, SoDT, DiaChi, Email, TrangThai)
SELECT
    CONCAT(N'Nha cung cap ', n),
    CONCAT('09', RIGHT(CONCAT('00000000', n * 137), 8)),
    CONCAT(N'Dia chi NCC ', n),
    CONCAT('ncc', n, '@example.com'),
    1
FROM nums
WHERE NOT EXISTS (
    SELECT 1 FROM NhaCungCap x WHERE x.SoDT = CONCAT('09', RIGHT(CONCAT('00000000', nums.n * 137), 8))
);
GO

-- 4) Add more products
;WITH nums AS (
    SELECT TOP (240) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n
    FROM sys.all_objects a CROSS JOIN sys.all_objects b
)
INSERT INTO SanPham (TenSP, DonViTinh, GiaBan, GiaBanSi, TrangThai)
SELECT
    CONCAT(N'Thuoc tong hop ', n),
    CHOOSE(((n - 1) % 4) + 1, N'Hop', N'Vien', N'Chai', N'Vỉ'),
    CAST(25000 + (n * 1300 % 320000) AS DECIMAL(18,0)),
    CAST((25000 + (n * 1300 % 320000)) * 0.92 AS DECIMAL(18,0)),
    1
FROM nums
WHERE NOT EXISTS (
    SELECT 1 FROM SanPham sp WHERE sp.TenSP = CONCAT(N'Thuoc tong hop ', nums.n)
);
GO

-- 5) Add users per branch (1 manager + 3 staff each)
DECLARE @DefaultPassword NVARCHAR(255) = N'123456';

;WITH b AS (
    SELECT MaCN, MaCNCode, ROW_NUMBER() OVER (ORDER BY MaCN) AS rn
    FROM ChiNhanh
),
roleNums AS (
    SELECT v.rn AS roleIdx, v.roleName
    FROM (VALUES (1, N'Admin'), (2, N'NhanVien'), (3, N'NhanVien'), (4, N'NhanVien')) v(rn, roleName)
)
INSERT INTO NguoiDung (TenDangNhap, MatKhau, HoTen, VaiTro, TrangThai, MaCN)
SELECT
    LOWER(CONCAT(N'user_', REPLACE(b.MaCNCode, N'-', N''), N'_', roleNums.roleIdx)),
    @DefaultPassword,
    CONCAT(N'Nhan su ', b.MaCNCode, N' #', roleNums.roleIdx),
    roleNums.roleName,
    1,
    b.MaCN
FROM b
CROSS JOIN roleNums
WHERE NOT EXISTS (
    SELECT 1 FROM NguoiDung u
    WHERE u.TenDangNhap = LOWER(CONCAT(N'user_', REPLACE(b.MaCNCode, N'-', N''), N'_', roleNums.roleIdx))
);
GO

-- 6) Add customers
;WITH nums AS (
    SELECT TOP (12000) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n
    FROM sys.all_objects a CROSS JOIN sys.all_objects b
)
INSERT INTO KhachHang (SoDT, TenKH)
SELECT
    CONCAT('08', RIGHT(CONCAT('00000000', n * 97), 8)),
    CONCAT(N'Khach hang ', n)
FROM nums
WHERE NOT EXISTS (
    SELECT 1 FROM KhachHang kh WHERE kh.SoDT = CONCAT('08', RIGHT(CONCAT('00000000', nums.n * 97), 8))
);
GO

-- 7) Add import orders + lots (history in last 18 months)
DECLARE @RowsToImport INT = 18000;

;WITH nums AS (
    SELECT TOP (@RowsToImport) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n
    FROM sys.all_objects a CROSS JOIN sys.all_objects b
),
pickBranch AS (
    SELECT MaCN, ROW_NUMBER() OVER (ORDER BY MaCN) AS rn FROM ChiNhanh
),
pickSupplier AS (
    SELECT MaNCC, ROW_NUMBER() OVER (ORDER BY MaNCC) AS rn FROM NhaCungCap WHERE TrangThai = 1
),
pickUser AS (
    SELECT MaND, MaCN, ROW_NUMBER() OVER (PARTITION BY MaCN ORDER BY MaND) AS rn FROM NguoiDung WHERE TrangThai = 1
),
pickProduct AS (
    SELECT MaSP, ROW_NUMBER() OVER (ORDER BY MaSP) AS rn FROM SanPham WHERE TrangThai = 1
)
INSERT INTO PhieuNhap (MaND, NgayNhap, TongTien, GhiChu, MaNCC, MaCN)
SELECT
    u.MaND,
    DATEADD(DAY, -1 * (ABS(CHECKSUM(NEWID())) % 540), CAST(GETDATE() AS DATE)),
    0,
    N'Seed operational import',
    s.MaNCC,
    b.MaCN
FROM nums n
JOIN pickBranch b ON b.rn = ((n.n - 1) % (SELECT COUNT(*) FROM pickBranch)) + 1
JOIN pickSupplier s ON s.rn = ((n.n - 1) % (SELECT COUNT(*) FROM pickSupplier)) + 1
JOIN pickUser u ON u.MaCN = b.MaCN AND u.rn = 1;
GO

;WITH src AS (
    SELECT TOP (18000)
        pn.MaPN,
        p.MaSP,
        CONCAT(N'LO-', pn.MaPN, N'-', p.MaSP) AS SoLo,
        DATEADD(DAY, 120 + ABS(CHECKSUM(NEWID())) % 720, CAST(pn.NgayNhap AS DATE)) AS HanSuDung,
        80 + ABS(CHECKSUM(NEWID())) % 920 AS SoLuong,
        CAST(12000 + (ABS(CHECKSUM(NEWID())) % 220000) AS DECIMAL(18,0)) AS GiaNhap
    FROM PhieuNhap pn
    JOIN SanPham p ON p.MaSP = ((pn.MaPN % (SELECT COUNT(*) FROM SanPham)) + 1)
    WHERE pn.GhiChu = N'Seed operational import'
)
INSERT INTO LoHang (MaSP, SoLo, HanSuDung, SoLuong, NgayNhap, MaPN, GiaNhap)
SELECT
    s.MaSP, s.SoLo, s.HanSuDung, s.SoLuong, GETDATE(), s.MaPN, s.GiaNhap
FROM src s
WHERE NOT EXISTS (SELECT 1 FROM LoHang l WHERE l.SoLo = s.SoLo);
GO

UPDATE pn
SET TongTien = x.TongTien
FROM PhieuNhap pn
JOIN (
    SELECT l.MaPN, SUM(l.GiaNhap * l.SoLuong) AS TongTien
    FROM LoHang l
    GROUP BY l.MaPN
) x ON x.MaPN = pn.MaPN;
GO

-- 8) Seed branch inventory policy rows
INSERT INTO TonKhoChiNhanh (MaCN, MaSP, TonHienTai, MucTonToiThieu, MucTonMucTieu, SoNgayLeadTime, SoNgayTonAnToan)
SELECT
    cn.MaCN,
    sp.MaSP,
    80 + ABS(CHECKSUM(NEWID())) % 420,
    30 + ABS(CHECKSUM(NEWID())) % 100,
    140 + ABS(CHECKSUM(NEWID())) % 260,
    2 + ABS(CHECKSUM(NEWID())) % 6,
    5 + ABS(CHECKSUM(NEWID())) % 12
FROM ChiNhanh cn
CROSS JOIN (SELECT TOP (220) MaSP FROM SanPham WHERE TrangThai = 1 ORDER BY MaSP) sp
WHERE NOT EXISTS (
    SELECT 1 FROM TonKhoChiNhanh t WHERE t.MaCN = cn.MaCN AND t.MaSP = sp.MaSP
);
GO

INSERT INTO InventoryPolicy (MaSP, MaCN, LeadTimeDays, ServiceLevel, SafetyStock, ReorderPoint, IsActive)
SELECT
    tk.MaSP,
    tk.MaCN,
    tk.SoNgayLeadTime,
    CAST(0.93 + (ABS(CHECKSUM(NEWID())) % 7) * 0.01 AS DECIMAL(5,2)),
    tk.MucTonToiThieu,
    tk.MucTonMucTieu,
    1
FROM TonKhoChiNhanh tk
WHERE NOT EXISTS (
    SELECT 1 FROM InventoryPolicy ip WHERE ip.MaSP = tk.MaSP AND ip.MaCN = tk.MaCN
);
GO

-- 9) Seed invoices + details (last 12 months)
DECLARE @InvoiceCount INT = 48000;

;WITH nums AS (
    SELECT TOP (@InvoiceCount) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n
    FROM sys.all_objects a CROSS JOIN sys.all_objects b
),
customers AS (
    SELECT MaKH, ROW_NUMBER() OVER (ORDER BY MaKH) AS rn FROM KhachHang
),
usersByBranch AS (
    SELECT MaND, MaCN, ROW_NUMBER() OVER (PARTITION BY MaCN ORDER BY MaND) AS rn
    FROM NguoiDung WHERE TrangThai = 1
),
branches AS (
    SELECT MaCN, ROW_NUMBER() OVER (ORDER BY MaCN) AS rn FROM ChiNhanh WHERE TrangThai = 1
),
products AS (
    SELECT TOP (260) MaSP, GiaBan, ROW_NUMBER() OVER (ORDER BY MaSP) AS rn
    FROM SanPham WHERE TrangThai = 1 ORDER BY MaSP
)
INSERT INTO HoaDon (MaKH, MaND, NgayBan, TongTien, PhuongThucTT, TrangThai, LyDoHuy, LoaiHD, MaCN)
SELECT
    c.MaKH,
    u.MaND,
    DATEADD(DAY, -1 * (ABS(CHECKSUM(NEWID())) % 365), CAST(GETDATE() AS DATE)),
    0,
    CASE ((ABS(CHECKSUM(NEWID())) % 3) + 1)
        WHEN 1 THEN N'TienMat'
        WHEN 2 THEN N'QR'
        ELSE N'ChuyenKhoan'
    END,
    N'Thanh cong',
    N'SEED44',
    N'SALE',
    b.MaCN
FROM nums n
JOIN branches b ON b.rn = ((n.n - 1) % (SELECT COUNT(*) FROM branches)) + 1
JOIN usersByBranch u ON u.MaCN = b.MaCN AND u.rn = 1
JOIN customers c ON c.rn = ((n.n - 1) % (SELECT COUNT(*) FROM customers)) + 1;
GO

;WITH newHD AS (
    SELECT MaHD
    FROM HoaDon
    WHERE LyDoHuy = N'SEED44'
),
lineNums AS (
    SELECT v.i FROM (VALUES (1),(2),(3),(4)) v(i)
),
products AS (
    SELECT TOP (260) MaSP, GiaBan, ROW_NUMBER() OVER (ORDER BY MaSP) AS rn
    FROM SanPham WHERE TrangThai = 1 ORDER BY MaSP
)
INSERT INTO ChiTietHoaDon (MaHD, MaLo, MaSP, SoLuong, DonGia, GiaVon, ThanhTien)
SELECT
    h.MaHD,
    lot.MaLo,
    p.MaSP,
    qty.SoLuong,
    p.GiaBan,
    lot.GiaNhap,
    p.GiaBan * qty.SoLuong
FROM newHD h
JOIN lineNums ln ON ln.i <= (1 + ABS(CHECKSUM(NEWID())) % 3)
JOIN products p ON p.rn = ((ABS(CHECKSUM(NEWID())) % (SELECT COUNT(*) FROM products)) + 1)
CROSS APPLY (
    SELECT TOP 1 l.MaLo, l.GiaNhap
    FROM LoHang l
    WHERE l.MaSP = p.MaSP
    ORDER BY NEWID()
) lot
CROSS APPLY (
    SELECT 1 + ABS(CHECKSUM(NEWID())) % 6 AS SoLuong
) qty;
GO

UPDATE hd
SET TongTien = x.TongTien
FROM HoaDon hd
JOIN (
    SELECT MaHD, SUM(ThanhTien) AS TongTien
    FROM ChiTietHoaDon
    GROUP BY MaHD
) x ON x.MaHD = hd.MaHD
WHERE hd.LyDoHuy = N'SEED44';
GO

-- 10) Refresh datamart + forecasting + optimization
DECLARE @seedToday DATE = CAST(GETDATE() AS DATE);
DECLARE @seedFrom365 DATE = DATEADD(DAY, -365, @seedToday);

EXEC sp_DemandPlanning_RefreshDataMart @FromDate = @seedFrom365, @ToDate = @seedToday;
EXEC sp_DemandPlanning_GenerateForecast @HorizonDays = 30, @LookbackDays = 60;
EXEC sp_DemandPlanning_RunOptimization @HorizonDays = 30;
GO

INSERT INTO SystemLogs (MaND, TenUser, HanhDong, DoiTuong, ChiTiet, ThoiGian)
SELECT TOP 1
    u.MaND,
    COALESCE(u.HoTen, u.TenDangNhap),
    N'SEED_LARGE_DATA_V1',
    N'DATABASE',
    N'Seed large operational dataset completed',
    GETDATE()
FROM NguoiDung u
ORDER BY u.MaND;
GO

PRINT N'44_seed_large_operational_data.sql completed';
GO
