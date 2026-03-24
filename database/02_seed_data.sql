-- ============================================================
-- FILE: 02_seed_data.sql
-- MO TA: Seed du lieu mau
-- ============================================================

USE QuanLyCuaHangThuoc;
GO

-- ============================================================
-- 1. NGUOI DUNG
-- ============================================================
INSERT INTO NguoiDung (TenDangNhap, MatKhau, HoTen, VaiTro) VALUES
(N'admin',       N'admin123',  N'Nguyễn Văn Admin',    N'Admin'),
(N'admin02',     N'admin123',  N'Trần Thị Quản Lý',   N'Admin'),
(N'nhanvien01',  N'nv123',     N'Lê Văn Nhân Viên',   N'NhanVien'),
(N'nhanvien02',  N'nv123',     N'Phạm Thị Bán Hàng',  N'NhanVien');
GO

-- ============================================================
-- 2. SAN PHAM (25 thuoc)
-- ============================================================
INSERT INTO SanPham (TenSP, DonViTinh, GiaNhap, GiaBan) VALUES
(N'Paracetamol 500mg',             N'Viên',   800,    1500),
(N'Ibuprofen 400mg',               N'Viên',   1200,   2500),
(N'Aspirin 81mg',                  N'Viên',   600,    1200),
(N'Amoxicillin 500mg',             N'Viên',   1500,   3000),
(N'Azithromycin 250mg',            N'Viên',   3000,   5500),
(N'Cefixime 200mg',                N'Viên',   4000,   7000),
(N'Ciprofloxacin 500mg',           N'Viên',   2000,   4000),
(N'Omeprazole 20mg',               N'Viên',   1000,   2200),
(N'Pantoprazole 40mg',             N'Viên',   2500,   4500),
(N'Gaviscon Suspension',           N'Chai',   45000,  75000),
(N'Dextromethorphan 15mg',         N'Viên',   800,    1800),
(N'Acetylcysteine 200mg',          N'Gói',    1500,   3000),
(N'Tiffy Dey',                     N'Viên',   500,    1200),
(N'Decolgen ND',                   N'Viên',   600,    1500),
(N'Vitamin C 1000mg',              N'Viên',   1000,   2500),
(N'Vitamin B Complex',             N'Viên',   800,    1800),
(N'Calcium 600mg + D3',            N'Viên',   1200,   2800),
(N'Omega-3 Fish Oil 1000mg',       N'Viên',   2500,   5000),
(N'Cetirizine 10mg',               N'Viên',   600,    1500),
(N'Loratadine 10mg',               N'Viên',   700,    1600),
(N'Smecta 3g',                     N'Gói',    3000,   5500),
(N'Domperidone 10mg',              N'Viên',   800,    1800),
(N'Betadine 10% 125ml',            N'Chai',   35000,  55000),
(N'Clotrimazole Cream 1%',         N'Tuýp',   15000,  28000),
(N'Natri Clorid 0.9% (Nhỏ mắt)',  N'Lọ',     8000,   15000);
GO

-- ============================================================
-- 3. LO HANG (20 lo)
-- ============================================================

-- Paracetamol 500mg (MaSP=1): 3 lo
INSERT INTO LoHang (MaSP, SoLo, HanSuDung, SoLuong, NgayNhap) VALUES
(1, N'LOT-PAR-001', '2026-05-15', 200, '2026-01-10'),
(1, N'LOT-PAR-002', '2027-03-20', 500, '2026-02-15'),
(1, N'LOT-PAR-003', '2027-08-01', 300, '2026-03-01');

-- Amoxicillin 500mg (MaSP=4): 2 lo
INSERT INTO LoHang (MaSP, SoLo, HanSuDung, SoLuong, NgayNhap) VALUES
(4, N'LOT-AMO-001', '2026-06-30', 150, '2026-01-20'),
(4, N'LOT-AMO-002', '2027-06-30', 400, '2026-03-05');

-- Azithromycin 250mg (MaSP=5): 1 lo
INSERT INTO LoHang (MaSP, SoLo, HanSuDung, SoLuong, NgayNhap) VALUES
(5, N'LOT-AZI-001', '2027-12-01', 200, '2026-02-20');

-- Omeprazole 20mg (MaSP=8): 2 lo
INSERT INTO LoHang (MaSP, SoLo, HanSuDung, SoLuong, NgayNhap) VALUES
(8, N'LOT-OME-001', '2026-04-10', 100, '2025-12-01'),
(8, N'LOT-OME-002', '2027-09-15', 350, '2026-03-10');

-- Gaviscon (MaSP=10): 1 lo
INSERT INTO LoHang (MaSP, SoLo, HanSuDung, SoLuong, NgayNhap) VALUES
(10, N'LOT-GAV-001', '2027-07-20', 80, '2026-02-01');

-- Vitamin C (MaSP=15): 2 lo
INSERT INTO LoHang (MaSP, SoLo, HanSuDung, SoLuong, NgayNhap) VALUES
(15, N'LOT-VTC-001', '2026-06-01', 300, '2025-11-15'),
(15, N'LOT-VTC-002', '2027-12-31', 600, '2026-03-01');

-- Ibuprofen (MaSP=2): 1 lo
INSERT INTO LoHang (MaSP, SoLo, HanSuDung, SoLuong, NgayNhap) VALUES
(2, N'LOT-IBU-001', '2027-11-30', 400, '2026-03-05');

-- Cetirizine (MaSP=19): 1 lo
INSERT INTO LoHang (MaSP, SoLo, HanSuDung, SoLuong, NgayNhap) VALUES
(19, N'LOT-CET-001', '2027-10-15', 250, '2026-01-25');

-- Betadine (MaSP=23): 1 lo
INSERT INTO LoHang (MaSP, SoLo, HanSuDung, SoLuong, NgayNhap) VALUES
(23, N'LOT-BET-001', '2028-01-01', 60, '2026-02-10');

-- Tiffy Dey (MaSP=13): 2 lo
INSERT INTO LoHang (MaSP, SoLo, HanSuDung, SoLuong, NgayNhap) VALUES
(13, N'LOT-TIF-001', '2026-05-20', 500, '2025-10-10'),
(13, N'LOT-TIF-002', '2027-05-20', 800, '2026-02-20');

-- Smecta (MaSP=21): 1 lo
INSERT INTO LoHang (MaSP, SoLo, HanSuDung, SoLuong, NgayNhap) VALUES
(21, N'LOT-SME-001', '2027-08-08', 200, '2026-01-30');

-- Decolgen ND (MaSP=14): 1 lo
INSERT INTO LoHang (MaSP, SoLo, HanSuDung, SoLuong, NgayNhap) VALUES
(14, N'LOT-DEC-001', '2027-04-15', 350, '2026-02-28');

-- Calcium D3 (MaSP=17): 1 lo
INSERT INTO LoHang (MaSP, SoLo, HanSuDung, SoLuong, NgayNhap) VALUES
(17, N'LOT-CAL-001', '2027-10-01', 180, '2026-03-15');
GO

-- ============================================================
-- 4. KHACH HANG
-- ============================================================
INSERT INTO KhachHang (SoDT, TenKH) VALUES
('0901234567', N'Nguyễn Thị Lan'),
('0912345678', N'Trần Văn Minh'),
('0923456789', N'Lê Hoàng Anh'),
('0934567890', N'Phạm Thị Hương'),
('0945678901', N'Võ Đình Khoa');
GO

-- ============================================================
-- 5. HOA DON + CHI TIET + TRU KHO
-- ============================================================

-- HD 1: Lan mua Paracetamol + Vitamin C (hom nay)
INSERT INTO HoaDon (MaKH, MaND, NgayBan, TongTien) VALUES (1, 3, GETDATE(), 27500);
INSERT INTO ChiTietHoaDon (MaHD, MaLo, MaSP, SoLuong, DonGia, ThanhTien) VALUES
(1, 1, 1, 10, 1500, 15000),
(1, 10, 15, 5, 2500, 12500);
UPDATE LoHang SET SoLuong = SoLuong - 10 WHERE MaLo = 1;
UPDATE LoHang SET SoLuong = SoLuong - 5  WHERE MaLo = 10;
GO

-- HD 2: Minh mua Amoxicillin + Omeprazole (hom nay)
INSERT INTO HoaDon (MaKH, MaND, NgayBan, TongTien) VALUES (2, 3, GETDATE(), 43000);
INSERT INTO ChiTietHoaDon (MaHD, MaLo, MaSP, SoLuong, DonGia, ThanhTien) VALUES
(2, 4, 4, 7, 3000, 21000),
(2, 7, 8, 10, 2200, 22000);
UPDATE LoHang SET SoLuong = SoLuong - 7  WHERE MaLo = 4;
UPDATE LoHang SET SoLuong = SoLuong - 10 WHERE MaLo = 7;
GO

-- HD 3: Vang lai mua Tiffy + Decolgen (hom nay)
INSERT INTO HoaDon (MaKH, MaND, NgayBan, TongTien) VALUES (NULL, 4, GETDATE(), 46500);
INSERT INTO ChiTietHoaDon (MaHD, MaLo, MaSP, SoLuong, DonGia, ThanhTien) VALUES
(3, 15, 13, 20, 1200, 24000),
(3, 18, 14, 15, 1500, 22500);
UPDATE LoHang SET SoLuong = SoLuong - 20 WHERE MaLo = 15;
UPDATE LoHang SET SoLuong = SoLuong - 15 WHERE MaLo = 18;
GO

-- HD 4: Hoang Anh mua Gaviscon + Smecta (3 ngay truoc)
INSERT INTO HoaDon (MaKH, MaND, NgayBan, TongTien) VALUES (3, 3, DATEADD(DAY, -3, GETDATE()), 130000);
INSERT INTO ChiTietHoaDon (MaHD, MaLo, MaSP, SoLuong, DonGia, ThanhTien) VALUES
(4, 9, 10, 1, 75000, 75000),
(4, 17, 21, 10, 5500, 55000);
UPDATE LoHang SET SoLuong = SoLuong - 1  WHERE MaLo = 9;
UPDATE LoHang SET SoLuong = SoLuong - 10 WHERE MaLo = 17;
GO

-- HD 5: Huong mua Betadine + Cetirizine (1 tuan truoc)
INSERT INTO HoaDon (MaKH, MaND, NgayBan, TongTien) VALUES (4, 4, DATEADD(DAY, -7, GETDATE()), 122000);
INSERT INTO ChiTietHoaDon (MaHD, MaLo, MaSP, SoLuong, DonGia, ThanhTien) VALUES
(5, 14, 23, 2, 55000, 110000),
(5, 13, 19, 8, 1500, 12000);
UPDATE LoHang SET SoLuong = SoLuong - 2 WHERE MaLo = 14;
UPDATE LoHang SET SoLuong = SoLuong - 8 WHERE MaLo = 13;
GO

-- HD 6: Lan mua lai Paracetamol (hom qua - KH VIP)
INSERT INTO HoaDon (MaKH, MaND, NgayBan, TongTien) VALUES (1, 3, DATEADD(DAY, -1, GETDATE()), 45000);
INSERT INTO ChiTietHoaDon (MaHD, MaLo, MaSP, SoLuong, DonGia, ThanhTien) VALUES
(6, 1, 1, 30, 1500, 45000);
UPDATE LoHang SET SoLuong = SoLuong - 30 WHERE MaLo = 1;
GO

PRINT N'Seed data thanh cong!';
GO
