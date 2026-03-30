-- =================================================================
-- 25_hrm_link_user_account.sql
-- HRM: Thêm cột MaND liên kết HR_Employees → NguoiDung
-- Mục đích: Cho phép JOIN hóa đơn theo ca làm
-- =================================================================
USE QuanLyCuaHangThuoc;
GO

-- 1. Thêm cột MaND
IF COL_LENGTH('HR_Employees', 'MaND') IS NULL
BEGIN
    ALTER TABLE HR_Employees ADD MaND INT NULL;
    PRINT N'✅ Thêm cột MaND vào HR_Employees';
END
GO

-- 2. Backfill: map qua tên trùng
UPDATE e SET e.MaND = nd.MaND
FROM HR_Employees e
JOIN NguoiDung nd ON e.FullName = nd.HoTen
WHERE e.MaND IS NULL;
GO

PRINT N'✅ Backfill MaND cho HR_Employees';

-- 3. Verify
SELECT e.EmpID, e.FullName, e.MaND, nd.TenDangNhap
FROM HR_Employees e
LEFT JOIN NguoiDung nd ON e.MaND = nd.MaND;
GO

PRINT N'=== Migration 25 (Link User Account) hoàn tất! ===';
GO
