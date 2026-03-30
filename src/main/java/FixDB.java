import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class FixDB {
    public static void main(String[] args) {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            Connection conn = DriverManager.getConnection(
                "jdbc:sqlserver://localhost:1433;databaseName=QuanLyCuaHangThuoc;encrypt=false;", 
                "sa", "123456"
            );
            Statement stmt = conn.createStatement();
            
            stmt.executeUpdate("UPDATE NguoiDung SET HoTen = N'Nguyễn Văn Admin', VaiTro = N'Admin', TrangThai = 1 WHERE TenDangNhap = 'admin'");
            stmt.executeUpdate("UPDATE NguoiDung SET HoTen = N'Lê Văn Minh', VaiTro = N'NhanVien', TrangThai = 1 WHERE TenDangNhap = 'nhanvien01'");
            stmt.executeUpdate("UPDATE NguoiDung SET HoTen = N'Phạm Thị Bán Hàng', VaiTro = N'NhanVien', TrangThai = 1 WHERE TenDangNhap = 'nhanvien02'");
            stmt.executeUpdate("UPDATE NguoiDung SET HoTen = N'Hoàng Văn Đức', VaiTro = N'NhanVien', TrangThai = 1 WHERE TenDangNhap = 'nhanvien03'");
            
            stmt.executeUpdate("UPDATE e SET e.FullName = n.HoTen FROM HR_Employees e JOIN NguoiDung n ON e.MaND = n.MaND");
            stmt.executeUpdate("UPDATE HR_Employees SET Status = N'Đang làm'");
            
            stmt.executeUpdate("UPDATE HR_Shifts SET ShiftName = N'Ca Sáng' WHERE ShiftID = 1");
            stmt.executeUpdate("UPDATE HR_Shifts SET ShiftName = N'Ca Chiều' WHERE ShiftID = 2");
            stmt.executeUpdate("UPDATE HR_Shifts SET ShiftName = N'Nghỉ Phép Tuần' WHERE ShiftID = 3");
            stmt.executeUpdate("UPDATE HR_Shifts SET ShiftName = N'Nghỉ Phép Năm' WHERE ShiftID = 4");
            stmt.executeUpdate("UPDATE HR_Shifts SET ShiftName = N'Nghỉ Không Lương' WHERE ShiftID = 5");
            
            System.out.println("FIX SUCCESS!");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
