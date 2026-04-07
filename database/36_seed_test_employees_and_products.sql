USE QuanLyCuaHangThuoc;
GO

SET NOCOUNT ON;

PRINT N'=== START: SEED TEST NHAN VIEN + CA + SAN PHAM ===';

-- ============================================================
-- A) HRM TEST DATA: tao ca + nhan vien + lich ca de test ngay
-- ============================================================

-- 1) Ensure shifts
IF NOT EXISTS (SELECT 1 FROM HR_Shifts WHERE ShiftName = N'Ca Sáng')
BEGIN
    INSERT INTO HR_Shifts (ShiftName, DefaultStartTime, DefaultEndTime)
    VALUES (N'Ca Sáng', '06:00', '14:00');
END;

IF NOT EXISTS (SELECT 1 FROM HR_Shifts WHERE ShiftName = N'Ca Chiều')
BEGIN
    INSERT INTO HR_Shifts (ShiftName, DefaultStartTime, DefaultEndTime)
    VALUES (N'Ca Chiều', '14:00', '22:00');
END;

IF NOT EXISTS (SELECT 1 FROM HR_Shifts WHERE ShiftName = N'Ca Đêm')
BEGIN
    INSERT INTO HR_Shifts (ShiftName, DefaultStartTime, DefaultEndTime)
    VALUES (N'Ca Đêm', '22:00', '06:00');
END;

DECLARE @ShiftSang INT = (SELECT TOP 1 ShiftID FROM HR_Shifts WHERE ShiftName = N'Ca Sáng' ORDER BY ShiftID);
DECLARE @ShiftChieu INT = (SELECT TOP 1 ShiftID FROM HR_Shifts WHERE ShiftName = N'Ca Chiều' ORDER BY ShiftID);

-- 2) Tao nhan vien test neu chua co
IF NOT EXISTS (SELECT 1 FROM HR_Employees WHERE PinCode = '2001')
BEGIN
    INSERT INTO HR_Employees (FullName, PinCode, Phone, HourlyRate, OvertimeRate, HireDate, Status)
    VALUES (N'NV Test Ca Sáng', '2001', '0909002001', 45000, 56000, CAST(GETDATE() AS DATE), N'Đang làm');
END;

IF NOT EXISTS (SELECT 1 FROM HR_Employees WHERE PinCode = '2002')
BEGIN
    INSERT INTO HR_Employees (FullName, PinCode, Phone, HourlyRate, OvertimeRate, HireDate, Status)
    VALUES (N'NV Test Ca Chiều', '2002', '0909002002', 42000, 52000, CAST(GETDATE() AS DATE), N'Đang làm');
END;

DECLARE @EmpSang INT = (SELECT TOP 1 EmpID FROM HR_Employees WHERE PinCode = '2001');
DECLARE @EmpChieu INT = (SELECT TOP 1 EmpID FROM HR_Employees WHERE PinCode = '2002');

-- 3) Link MaND cho nhan vien test (neu co cot MaND va co user NV hoat dong)
IF COL_LENGTH('HR_Employees', 'MaND') IS NOT NULL
BEGIN
    DECLARE @UserNV1 INT = (
        SELECT TOP 1 nd.MaND
        FROM NguoiDung nd
        WHERE nd.TrangThai = 1
          AND nd.VaiTro <> N'Admin'
          AND NOT EXISTS (SELECT 1 FROM HR_Employees e WHERE e.MaND = nd.MaND AND e.EmpID <> @EmpSang)
        ORDER BY nd.MaND
    );

    DECLARE @UserNV2 INT = (
        SELECT TOP 1 nd.MaND
        FROM NguoiDung nd
        WHERE nd.TrangThai = 1
          AND nd.VaiTro <> N'Admin'
          AND nd.MaND <> ISNULL(@UserNV1, -1)
          AND NOT EXISTS (SELECT 1 FROM HR_Employees e WHERE e.MaND = nd.MaND AND e.EmpID <> @EmpChieu)
        ORDER BY nd.MaND
    );

    IF @UserNV1 IS NOT NULL
        UPDATE HR_Employees SET MaND = @UserNV1 WHERE EmpID = @EmpSang AND (MaND IS NULL OR MaND = @UserNV1);

    IF @UserNV2 IS NOT NULL
        UPDATE HR_Employees SET MaND = @UserNV2 WHERE EmpID = @EmpChieu AND (MaND IS NULL OR MaND = @UserNV2);
END;

-- 4) Tao lich ca cho hom nay + ngay mai de test timeclock ngay lap tuc
DECLARE @Today DATE = CAST(GETDATE() AS DATE);
DECLARE @Tomorrow DATE = DATEADD(DAY, 1, @Today);

IF NOT EXISTS (SELECT 1 FROM HR_Schedules WHERE EmpID = @EmpSang AND WorkDate = @Today)
BEGIN
    INSERT INTO HR_Schedules
        (EmpID, ShiftID, WorkDate, ActualStart, ActualEnd, ShiftDefaultStart, ShiftDefaultEnd, CreatedAt)
    VALUES
        (@EmpSang, @ShiftSang, @Today, '06:00', '14:00', '06:00', '14:00', GETDATE());
END;

IF NOT EXISTS (SELECT 1 FROM HR_Schedules WHERE EmpID = @EmpChieu AND WorkDate = @Today)
BEGIN
    INSERT INTO HR_Schedules
        (EmpID, ShiftID, WorkDate, ActualStart, ActualEnd, ShiftDefaultStart, ShiftDefaultEnd, CreatedAt)
    VALUES
        (@EmpChieu, @ShiftChieu, @Today, '14:00', '22:00', '14:00', '22:00', GETDATE());
END;

IF NOT EXISTS (SELECT 1 FROM HR_Schedules WHERE EmpID = @EmpSang AND WorkDate = @Tomorrow)
BEGIN
    INSERT INTO HR_Schedules
        (EmpID, ShiftID, WorkDate, ActualStart, ActualEnd, ShiftDefaultStart, ShiftDefaultEnd, CreatedAt)
    VALUES
        (@EmpSang, @ShiftChieu, @Tomorrow, '14:00', '22:00', '14:00', '22:00', GETDATE());
END;

IF NOT EXISTS (SELECT 1 FROM HR_Schedules WHERE EmpID = @EmpChieu AND WorkDate = @Tomorrow)
BEGIN
    INSERT INTO HR_Schedules
        (EmpID, ShiftID, WorkDate, ActualStart, ActualEnd, ShiftDefaultStart, ShiftDefaultEnd, CreatedAt)
    VALUES
        (@EmpChieu, @ShiftSang, @Tomorrow, '06:00', '14:00', '06:00', '14:00', GETDATE());
END;

PRINT N'HRM test data: OK (PIN 2001, 2002 + lich hom nay/ngay mai).';

-- ============================================================
-- B) PRODUCT TEST DATA: bo sung san pham + lo hang de test POS/Kho
-- ============================================================

-- 1) Ensure NhaCungCap test
IF NOT EXISTS (SELECT 1 FROM NhaCungCap WHERE TenNCC = N'NCC Test Seed')
BEGIN
    INSERT INTO NhaCungCap (TenNCC, SoDT, DiaChi, Email, TrangThai)
    VALUES (N'NCC Test Seed', '0909888777', N'Quan 1, TP.HCM', 'ncc.test.seed@example.com', 1);
END;

DECLARE @MaNCC_Test INT = (SELECT TOP 1 MaNCC FROM NhaCungCap WHERE TenNCC = N'NCC Test Seed' ORDER BY MaNCC);

-- 2) Upsert bo san pham test
IF NOT EXISTS (SELECT 1 FROM SanPham WHERE TenSP = N'Paracetamol Test 500mg')
BEGIN
    INSERT INTO SanPham (TenSP, DonViTinh, GiaBan, GiaBanSi, TrangThai)
    VALUES (N'Paracetamol Test 500mg', N'Hộp', 35000, 32000, 1);
END;

IF NOT EXISTS (SELECT 1 FROM SanPham WHERE TenSP = N'Amoxicillin Test 500mg')
BEGIN
    INSERT INTO SanPham (TenSP, DonViTinh, GiaBan, GiaBanSi, TrangThai)
    VALUES (N'Amoxicillin Test 500mg', N'Hộp', 54000, 49000, 1);
END;

IF NOT EXISTS (SELECT 1 FROM SanPham WHERE TenSP = N'Vitamin C Test 1000mg')
BEGIN
    INSERT INTO SanPham (TenSP, DonViTinh, GiaBan, GiaBanSi, TrangThai)
    VALUES (N'Vitamin C Test 1000mg', N'Tuýp', 48000, 43000, 1);
END;

DECLARE @MaSP_Para INT = (SELECT TOP 1 MaSP FROM SanPham WHERE TenSP = N'Paracetamol Test 500mg' ORDER BY MaSP);
DECLARE @MaSP_Amox INT = (SELECT TOP 1 MaSP FROM SanPham WHERE TenSP = N'Amoxicillin Test 500mg' ORDER BY MaSP);
DECLARE @MaSP_VitC INT = (SELECT TOP 1 MaSP FROM SanPham WHERE TenSP = N'Vitamin C Test 1000mg' ORDER BY MaSP);

-- 3) Tao PhieuNhap test
DECLARE @MaND_Admin INT = (SELECT TOP 1 MaND FROM NguoiDung WHERE VaiTro = N'Admin' ORDER BY MaND);
IF @MaND_Admin IS NULL
    SET @MaND_Admin = 1;

INSERT INTO PhieuNhap (MaND, NgayNhap, TongTien, GhiChu, MaNCC)
VALUES (@MaND_Admin, GETDATE(), 0, N'Seed test du lieu san pham/lo hang', @MaNCC_Test);

DECLARE @MaPN_Test INT = SCOPE_IDENTITY();

-- 4) Them lo hang test (han dung con xa de hien tren POS)
IF NOT EXISTS (SELECT 1 FROM LoHang WHERE SoLo = N'TEST-PARA-001')
BEGIN
    INSERT INTO LoHang (MaSP, SoLo, HanSuDung, SoLuong, NgayNhap, GiaNhap, MaPN)
    VALUES (@MaSP_Para, N'TEST-PARA-001', DATEADD(MONTH, 18, @Today), 300, GETDATE(), 7500000, @MaPN_Test);
END;

IF NOT EXISTS (SELECT 1 FROM LoHang WHERE SoLo = N'TEST-AMOX-001')
BEGIN
    INSERT INTO LoHang (MaSP, SoLo, HanSuDung, SoLuong, NgayNhap, GiaNhap, MaPN)
    VALUES (@MaSP_Amox, N'TEST-AMOX-001', DATEADD(MONTH, 20, @Today), 180, GETDATE(), 6200000, @MaPN_Test);
END;

IF NOT EXISTS (SELECT 1 FROM LoHang WHERE SoLo = N'TEST-VITC-001')
BEGIN
    INSERT INTO LoHang (MaSP, SoLo, HanSuDung, SoLuong, NgayNhap, GiaNhap, MaPN)
    VALUES (@MaSP_VitC, N'TEST-VITC-001', DATEADD(MONTH, 24, @Today), 220, GETDATE(), 5400000, @MaPN_Test);
END;

-- 5) Cap nhat TongTien cho PhieuNhap test
UPDATE PhieuNhap
SET TongTien = (
    SELECT ISNULL(SUM(lh.GiaNhap), 0)
    FROM LoHang lh
    WHERE lh.MaPN = PhieuNhap.MaPN
)
WHERE MaPN = @MaPN_Test;

PRINT N'Product test data: OK (3 san pham + 3 lo hang test).';

-- 6) Summary nhanh
SELECT TOP 2 EmpID, FullName, PinCode, Status
FROM HR_Employees
WHERE PinCode IN ('2001', '2002')
ORDER BY EmpID;

SELECT TOP 4 s.ScheduleID, e.FullName, s.WorkDate, sh.ShiftName, s.ActualStart, s.ActualEnd
FROM HR_Schedules s
JOIN HR_Employees e ON s.EmpID = e.EmpID
JOIN HR_Shifts sh ON s.ShiftID = sh.ShiftID
WHERE e.PinCode IN ('2001', '2002')
ORDER BY s.WorkDate, s.ScheduleID;

SELECT TOP 10 MaSP, TenSP, DonViTinh, GiaBan, TrangThai
FROM SanPham
WHERE TenSP LIKE N'%Test%'
ORDER BY MaSP;

PRINT N'=== DONE: SEED TEST NHAN VIEN + CA + SAN PHAM ===';
GO
