$connectionString = "Server=localhost;Database=QuanLyCuaHangThuoc;User Id=sa;Password=123456;"
$connection = New-Object System.Data.SqlClient.SqlConnection($connectionString)
$connection.Open()
$command = $connection.CreateCommand()

$queries = @(
    "UPDATE NguoiDung SET HoTen = N'Nguyễn Văn Admin', VaiTro = N'Admin', TrangThai = 1 WHERE TenDangNhap = 'admin';",
    "UPDATE NguoiDung SET HoTen = N'Lê Văn Minh', VaiTro = N'NhanVien', TrangThai = 1 WHERE TenDangNhap = 'nhanvien01';",
    "UPDATE NguoiDung SET HoTen = N'Phạm Thị Bán Hàng', VaiTro = N'NhanVien', TrangThai = 1 WHERE TenDangNhap = 'nhanvien02';",
    "UPDATE NguoiDung SET HoTen = N'Hoàng Văn Đức', VaiTro = N'NhanVien', TrangThai = 1 WHERE TenDangNhap = 'nhanvien03';",

    "UPDATE e SET e.FullName = n.HoTen FROM HR_Employees e JOIN NguoiDung n ON e.MaND = n.MaND;",
    "UPDATE HR_Employees SET Status = N'Đang làm';",

    "UPDATE HR_Shifts SET ShiftName = N'Ca Sáng' WHERE ShiftID = 1;",
    "UPDATE HR_Shifts SET ShiftName = N'Ca Chiều' WHERE ShiftID = 2;",
    "UPDATE HR_Shifts SET ShiftName = N'Nghỉ Phép Tuần' WHERE ShiftID = 3;",
    "UPDATE HR_Shifts SET ShiftName = N'Nghỉ Phép Năm' WHERE ShiftID = 4;",
    "UPDATE HR_Shifts SET ShiftName = N'Nghỉ Không Lương' WHERE ShiftID = 5;"
)

foreach ($q in $queries) {
    $command.CommandText = $q
    $affected = $command.ExecuteNonQuery()
    Write-Host "Executed: $q ($affected rows)"
}

$connection.Close()
Write-Host "Done fixing Vietnamese text!"
