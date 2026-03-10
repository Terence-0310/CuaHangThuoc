package dao;
import common.ConnectDB;
import java.sql.*;
import java.time.LocalDate;
public class TestDB {
    public static void main(String[] args) throws Exception {
        try (Connection con = ConnectDB.getCon()) {
            System.out.println("Connection: " + (con != null ? "OK" : "FAILED"));
            if (con == null) return;
            
            // Try to create a PhieuNhap
            CallableStatement cs = con.prepareCall("{call sp_PhieuNhap_Create(?, ?, ?)}");
            cs.setInt(1, 2); // assuming NguoiDung 2 exists
            cs.setNull(2, java.sql.Types.INTEGER);
            cs.setNString(3, "Test from command line");
            ResultSet rs = cs.executeQuery();
            long maPhieuNhap = -1;
            if (rs.next()) {
                maPhieuNhap = rs.getLong(1);
                System.out.println("MaPhieuNhap: " + maPhieuNhap);
            } else {
                System.out.println("No MaPhieuNhap returned");
            }

            if (maPhieuNhap != -1) {
                CallableStatement cs2 = con.prepareCall("{call sp_PhieuNhap_AddItem_Batch(?, ?, ?, ?, ?, ?)}");
                cs2.setInt(1, (int) maPhieuNhap);
                cs2.setInt(2, 1);
                cs2.setNString(3, "LOMOI_001");
                cs2.setDate(4, Date.valueOf(LocalDate.now().plusDays(30)));
                cs2.setBigDecimal(5, new java.math.BigDecimal("10000"));
                cs2.setInt(6, 100);
                cs2.execute();
                System.out.println("addItemBatch: OK");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
