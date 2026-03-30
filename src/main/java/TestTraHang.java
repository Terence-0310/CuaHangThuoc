import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;

public class TestTraHang {
    public static void main(String[] args) {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            Connection c = DriverManager.getConnection("jdbc:sqlserver://localhost:1433;databaseName=QuanLyCuaHangThuoc;encrypt=false;", "sa", "123456");
            DatabaseMetaData dbm = c.getMetaData();
            
            ResultSet rs = dbm.getProcedures(null, null, "sp_TraHangKhach");
            if (rs.next()) {
                System.out.println("Java sees sp_TraHangKhach!");
            } else {
                System.out.println("Java DOES NOT see sp_TraHangKhach!");
            }
            
            ResultSet rs2 = dbm.getColumns(null, null, "HoaDon", "TrangThai");
            if (rs2.next()) {
                System.out.println("Java sees HoaDon.TrangThai!");
            } else {
                System.out.println("Java DOES NOT see HoaDon.TrangThai!");
            }
            
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
