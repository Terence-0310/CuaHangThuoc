import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class FixHRM_MaND {
    public static void main(String[] args) {
        String dbUrl = "jdbc:sqlserver://localhost:1433;databaseName=QuanLyCuaHangThuoc;encrypt=false;sendStringParametersAsUnicode=true;";
        try (Connection conn = DriverManager.getConnection(dbUrl, "sa", "123456");
             Statement stmt = conn.createStatement()) {

            System.out.println("Bắt đầu sửa lỗi MaND và Tên Nhân Viên...");

            // 1. Array of exact names for Employees 1..5
            String[] names = {
                "Lê Văn Minh",
                "Nguyễn Thị Lan",
                "Trần Văn Hùng",
                "Phạm Thị Mai",
                "Hoàng Văn Đức"
            };
            
            String[] logins = {
                "nhanvien01",
                "nhanvien02",
                "nhanvien03",
                "nhanvien04",
                "nhanvien05"
            };

            for(int i=0; i<5; i++) {
                int empId = i + 1;
                String hoTen = names[i];
                String username = logins[i];
                
                // Ensure User account exists
                int maNd = -1;
                try (PreparedStatement ps = conn.prepareStatement("SELECT MaND FROM NguoiDung WHERE TenDangNhap = ?")) {
                    ps.setString(1, username);
                    ResultSet rs = ps.executeQuery();
                    if(rs.next()) {
                        maNd = rs.getInt(1);
                        // Update hoten just in case
                        PreparedStatement pu = conn.prepareStatement("UPDATE NguoiDung SET HoTen = ?, VaiTro = N'NhanVien' WHERE MaND = ?");
                        pu.setNString(1, hoTen);
                        pu.setInt(2, maNd);
                        pu.executeUpdate();
                        pu.close();
                    } else {
                        // Create it
                        PreparedStatement pi = conn.prepareStatement(
                            "INSERT INTO NguoiDung (TenDangNhap, MatKhau, HoTen, VaiTro, TrangThai) VALUES (?, '123', ?, N'NhanVien', 1)",
                            Statement.RETURN_GENERATED_KEYS
                        );
                        pi.setString(1, username);
                        pi.setNString(2, hoTen);
                        pi.executeUpdate();
                        ResultSet gk = pi.getGeneratedKeys();
                        if(gk.next()) maNd = gk.getInt(1);
                        pi.close();
                    }
                }
                
                // Link MaND and fix FullName in HR_Employees
                if(maNd > 0) {
                    try (PreparedStatement pe = conn.prepareStatement("UPDATE HR_Employees SET MaND = ?, FullName = ? WHERE EmpID = ?")) {
                        pe.setInt(1, maNd);
                        pe.setNString(2, hoTen);
                        pe.setInt(3, empId);
                        int rows = pe.executeUpdate();
                        System.out.println("Đã link " + username + " (MaND: " + maNd + ") cho EmpID: " + empId + " (" + hoTen + ") -> " + rows + " rows");
                    }
                }
            }
            
            // Clean up admin user names
            stmt.executeUpdate("UPDATE NguoiDung SET HoTen = N'Nguyễn Văn Admin' WHERE TenDangNhap = 'admin'");
            
            System.out.println("Sửa lỗi thành công! Mọi liên kết từ người dùng đến nhân sự đã chuẩn!");

        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
