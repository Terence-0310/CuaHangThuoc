USE QuanLyCuaHangThuoc;
GO

-- 1. Fix NguoiDung
UPDATE NguoiDung 
SET HoTen = N'Nguyễn Văn Admin', VaiTro = N'Admin', TrangThai = 1 
WHERE TenDangNhap = 'admin';

UPDATE NguoiDung 
SET HoTen = N'Lê Văn Minh', VaiTro = N'NhanVien', TrangThai = 1 
WHERE TenDangNhap = 'nhanvien01';

UPDATE NguoiDung 
SET HoTen = N'Phạm Thị Bán Hàng', VaiTro = N'NhanVien', TrangThai = 1 
WHERE TenDangNhap = 'nhanvien02';

UPDATE NguoiDung 
SET HoTen = N'Hoàng Văn Đức', VaiTro = N'NhanVien', TrangThai = 1 
WHERE TenDangNhap = 'nhanvien03';

-- 2. Fix HR_Employees
UPDATE HR_Employees
SET Status = N'Đang làm'
WHERE Status <> N'Đang làm' OR Status IS NULL;

-- Also fix FullName to match the fixed NguoiDung names
UPDATE e
SET e.FullName = n.HoTen
FROM HR_Employees e
JOIN NguoiDung n ON e.MaND = n.MaND;

-- 3. Fix HR_Shifts encoding
UPDATE HR_Shifts SET ShiftName = N'Ca Sáng' WHERE ShiftID = 1;
UPDATE HR_Shifts SET ShiftName = N'Ca Chiều' WHERE ShiftID = 2;
UPDATE HR_Shifts SET ShiftName = N'Nghỉ Phép Tuần' WHERE ShiftID = 3;
UPDATE HR_Shifts SET ShiftName = N'Nghỉ Phép Năm' WHERE ShiftID = 4;

-- 4. Check results
SELECT MaND, HoTen, VaiTro FROM NguoiDung;
SELECT EmpID, FullName, PinCode, Status FROM HR_Employees;
SELECT ShiftID, ShiftName FROM HR_Shifts;
GO
