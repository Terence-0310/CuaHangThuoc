-- ============================================================
-- FILE: 09_seed_25_products.sql
-- MO TA: Thêm 25 sản phẩm mới để test phân trang
-- ============================================================
USE QuanLyCuaHangThuoc;
GO

-- Xóa data cũ bị lỗi Unicode (không có N'')
DELETE FROM SanPham WHERE MaSP > 25;
GO

INSERT INTO SanPham (TenSP, DonViTinh, GiaBan, GiaBanSi) VALUES
(N'Ambroxol 30mg',                 N'Viên',   1800,   1620),
(N'Bromhexine 8mg',                N'Viên',   1200,   1080),
(N'Clopidogrel 75mg',              N'Viên',   3500,   3150),
(N'Diclofenac 50mg',               N'Viên',   2000,   1800),
(N'Enalapril 10mg',                N'Viên',   2500,   2250),
(N'Furosemide 40mg',               N'Viên',   1500,   1350),
(N'Glimepiride 2mg',               N'Viên',   3000,   2700),
(N'Hydrochlorothiazide 25mg',      N'Viên',   1200,   1080),
(N'Insulin NPH 100IU',             N'Lọ',     85000,  76500),
(N'Ketoprofen 100mg',              N'Viên',   2800,   2520),
(N'Levofloxacin 500mg',            N'Viên',   5000,   4500),
(N'Metformin 500mg',               N'Viên',   1500,   1350),
(N'Nifedipine 20mg',               N'Viên',   2200,   1980),
(N'Ofloxacin nhỏ mắt 0.3%',       N'Lọ',     18000,  16200),
(N'Prednisolone 5mg',              N'Viên',   1000,    900),
(N'Ranitidine 150mg',              N'Viên',   2000,   1800),
(N'Salbutamol 2mg',                N'Viên',   1200,   1080),
(N'Tramadol 50mg',                 N'Viên',   3500,   3150),
(N'Ursodeoxycholic Acid 250mg',    N'Viên',   5500,   4950),
(N'Valsartan 80mg',                N'Viên',   4000,   3600),
(N'Warfarin 5mg',                  N'Viên',   3000,   2700),
(N'Xylometazoline 0.1% nhỏ mũi',  N'Lọ',     22000,  19800),
(N'Dầu gió Thiên Thảo',           N'Chai',   15000,  13500),
(N'Thuốc ho Bảo Thanh',           N'Chai',   35000,  31500),
(N'Cao dán Salonpas',             N'Miếng',   5000,   4500);
GO

PRINT N'✅ Đã seed lại 25 sản phẩm với N'''' Unicode chuẩn!';
GO
