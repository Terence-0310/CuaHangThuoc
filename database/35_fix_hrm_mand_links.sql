USE QuanLyCuaHangThuoc;
GO

-- Xoá kết quả của các lỗi cũ
UPDATE NguoiDung SET HoTen = N'Lê Văn Minh' WHERE TenDangNhap = 'nhanvien01';
UPDATE NguoiDung SET HoTen = N'Phạm Thị Mai' WHERE TenDangNhap = 'nhanvien02';

-- Đảm bảo có nhanvien03 (Nguyễn Thị Lan)
IF NOT EXISTS (SELECT 1 FROM NguoiDung WHERE TenDangNhap = 'nhanvien03')
BEGIN
    INSERT INTO NguoiDung (TenDangNhap, MatKhau, HoTen, VaiTro, TrangThai) 
    VALUES ('nhanvien03', '123', N'Nguyễn Thị Lan', N'NhanVien', 1);
END
ELSE
BEGIN
    UPDATE NguoiDung SET HoTen = N'Nguyễn Thị Lan' WHERE TenDangNhap = 'nhanvien03';
END

-- Đảm bảo có nhanvien04 (Trần Văn Hùng)
IF NOT EXISTS (SELECT 1 FROM NguoiDung WHERE TenDangNhap = 'nhanvien04')
BEGIN
    INSERT INTO NguoiDung (TenDangNhap, MatKhau, HoTen, VaiTro, TrangThai) 
    VALUES ('nhanvien04', '123', N'Trần Văn Hùng', N'NhanVien', 1);
END
ELSE
BEGIN
    UPDATE NguoiDung SET HoTen = N'Trần Văn Hùng', TrangThai = 0 WHERE TenDangNhap = 'nhanvien04';
END

-- Đảm bảo có nhanvien05 (Hoàng Văn Đức)
IF NOT EXISTS (SELECT 1 FROM NguoiDung WHERE TenDangNhap = 'nhanvien05')
BEGIN
    INSERT INTO NguoiDung (TenDangNhap, MatKhau, HoTen, VaiTro, TrangThai) 
    VALUES ('nhanvien05', '123', N'Hoàng Văn Đức', N'NhanVien', 1);
END
ELSE
BEGIN
    UPDATE NguoiDung SET HoTen = N'Hoàng Văn Đức' WHERE TenDangNhap = 'nhanvien05';
END


-- Map HR_Employees to NguoiDung
UPDATE e 
SET e.FullName = N'Lê Văn Minh', e.MaND = (SELECT MaND FROM NguoiDung WHERE TenDangNhap = 'nhanvien01') 
FROM HR_Employees e WHERE e.EmpID = 1;

UPDATE e 
SET e.FullName = N'Nguyễn Thị Lan', e.MaND = (SELECT MaND FROM NguoiDung WHERE TenDangNhap = 'nhanvien03') 
FROM HR_Employees e WHERE e.EmpID = 2;

UPDATE e 
SET e.FullName = N'Trần Văn Hùng', e.MaND = (SELECT MaND FROM NguoiDung WHERE TenDangNhap = 'nhanvien04') 
FROM HR_Employees e WHERE e.EmpID = 3;

UPDATE e 
SET e.FullName = N'Phạm Thị Mai', e.MaND = (SELECT MaND FROM NguoiDung WHERE TenDangNhap = 'nhanvien02') 
FROM HR_Employees e WHERE e.EmpID = 4;

UPDATE e 
SET e.FullName = N'Hoàng Văn Đức', e.MaND = (SELECT MaND FROM NguoiDung WHERE TenDangNhap = 'nhanvien05') 
FROM HR_Employees e WHERE e.EmpID = 5;

-- Update admin name
UPDATE NguoiDung SET HoTen = N'Nguyễn Văn Admin' WHERE TenDangNhap = 'admin';

PRINT N'✅ Đã ánh xạ thành công 100% MaND cho HR_Employees, Sidebar sẽ mở khoá bình thường!';
GO
