-- ============================================================
-- FILE: 04_verify_data.sql
-- MO TA: Kiem tra du lieu sau khi seed
-- ============================================================

USE QuanLyCuaHangThuoc;
GO

PRINT N'=== 1. NGUOI DUNG ===';
SELECT MaND, TenDangNhap, HoTen, VaiTro, TrangThai FROM NguoiDung;

PRINT N'=== 2. SAN PHAM (Top 10) ===';
SELECT TOP 10 MaSP, TenSP, DonViTinh, GiaNhap, GiaBan FROM SanPham;

PRINT N'=== 3. TON KHO CHI TIET ===';
SELECT * FROM vw_TonKhoChiTiet ORDER BY TenSP, HanSuDung;

PRINT N'=== 4. TON KHO TONG HOP ===';
SELECT * FROM vw_TonKhoTheoSanPham WHERE TongTonKho > 0 ORDER BY TenSP;

PRINT N'=== 5. DOANH THU ===';
EXEC sp_DoanhThu @LoaiThoiGian = 'TODAY';
EXEC sp_DoanhThu @LoaiThoiGian = 'MONTH';
EXEC sp_DoanhThu @LoaiThoiGian = 'QUARTER';

PRINT N'=== 6. CANH BAO HET HAN ===';
EXEC sp_CanhBaoHetHan;

PRINT N'=== 7. TOP 5 BAN CHAY ===';
EXEC sp_TopBanChay @TopN = 5;

PRINT N'=== 8. TOP 5 KHACH VIP ===';
EXEC sp_TopKhachVIP @TopN = 5;

PRINT N'=== 9. LICH SU KH (Nguyen Thi Lan - MaKH=1) ===';
EXEC sp_LichSuKhachHang @MaKH = 1;

PRINT N'=== 10. TEST FEFO (Paracetamol - MaSP=1) ===';
EXEC sp_GetFEFO @MaSP = 1;

PRINT N'=== 11. TIM SAN PHAM (Keyword: para) ===';
EXEC sp_TimSanPham @Keyword = N'para';

PRINT N'Verify hoan tat!';
GO
