import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class SqlRunner {
    public static void main(String[] args) {
        String dbUrl = "jdbc:sqlserver://localhost:1433;databaseName=QuanLyCuaHangThuoc;encrypt=false;sendStringParametersAsUnicode=true;";
        try (Connection conn = DriverManager.getConnection(dbUrl, "sa", "123456");
             Statement stmt = conn.createStatement()) {

            // Fix the procedure explicitly using JDBC which handles UTF-8 correctly
            String spCreation = 
                "IF OBJECT_ID('sp_TraHangKhach', 'P') IS NOT NULL DROP PROCEDURE sp_TraHangKhach;\n";
            stmt.execute(spCreation);

            String createSP = 
                "CREATE PROCEDURE sp_TraHangKhach " +
                "    @MaHD INT, " +
                "    @DS_SP_LyDo NVARCHAR(MAX), " +
                "    @MaND_ThucHien INT, " +
                "    @TrangThai NVARCHAR(50) OUTPUT " +
                "AS " +
                "BEGIN " +
                "    SET NOCOUNT ON; " +
                " " +
                "    IF NOT EXISTS (SELECT 1 FROM HoaDon WHERE MaHD = @MaHD AND ISNULL(TrangThai, N'Thanh cong') = N'Thanh cong') " +
                "    BEGIN " +
                "        RAISERROR(N'Hóa đơn không hợp lệ hoặc đã hủy.', 16, 1); " +
                "        RETURN; " +
                "    END " +
                " " +
                "    BEGIN TRY " +
                "        BEGIN TRANSACTION; " +
                " " +
                "        UPDATE HoaDon SET TrangThai = N'Đã hủy', LyDoHuy = @DS_SP_LyDo WHERE MaHD = @MaHD; " +
                " " +
                "        DECLARE @MaSP INT, @SoLo NVARCHAR(50), @SLTra INT, @SoTienHoan DECIMAL(18,2); " +
                "        DECLARE cur CURSOR LOCAL FAST_FORWARD FOR " +
                "        SELECT p.MaSP, tk.SoLo, ct.SoLuong, ct.ThanhTien " +
                "        FROM ChiTietHoaDon ct " +
                "        JOIN SanPham p ON ct.MaSP = p.MaSP " +
                "        JOIN TonKho tk ON tk.MaSP = p.MaSP AND tk.SoLo IS NOT NULL " +
                "        WHERE ct.MaHD = @MaHD AND ct.SoLuong > 0; " +
                " " +
                "        OPEN cur; " +
                "        FETCH NEXT FROM cur INTO @MaSP, @SoLo, @SLTra, @SoTienHoan; " +
                "        WHILE @@FETCH_STATUS = 0 " +
                "        BEGIN " +
                "            UPDATE TonKho SET TongTon = TongTon + @SLTra WHERE MaSP = @MaSP AND SoLo = @SoLo; " +
                "            FETCH NEXT FROM cur INTO @MaSP, @SoLo, @SLTra, @SoTienHoan; " +
                "        END " +
                "        CLOSE cur; DEALLOCATE cur; " +
                " " +
                "        SET @TrangThai = N'Trả hàng thành công'; " +
                "        COMMIT TRANSACTION; " +
                "    END TRY " +
                "    BEGIN CATCH " +
                "        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION; " +
                "        DECLARE @ErrMsg NVARCHAR(4000) = ERROR_MESSAGE(); " +
                "        RAISERROR(@ErrMsg, 16, 1); " +
                "    END CATCH " +
                "END;";
            
            stmt.execute(createSP);
            System.out.println("Stored Procedure sp_TraHangKhach created successfully in UTF-8!");
            
            System.out.println("Applying basic UTF-8 updates...");
            // Fix seed data
            stmt.executeUpdate("UPDATE NguoiDung SET HoTen = N'Nguyễn Văn Admin', VaiTro = N'Admin', TrangThai = 1 WHERE TenDangNhap = 'admin'");
            stmt.executeUpdate("UPDATE NguoiDung SET HoTen = N'Lê Văn Minh', VaiTro = N'NhanVien', TrangThai = 1 WHERE TenDangNhap = 'nhanvien01'");
            stmt.executeUpdate("UPDATE NguoiDung SET HoTen = N'Phạm Thị Bán Hàng', VaiTro = N'NhanVien', TrangThai = 1 WHERE TenDangNhap = 'nhanvien02'");
            stmt.executeUpdate("UPDATE NguoiDung SET HoTen = N'Hoàng Văn Đức', VaiTro = N'NhanVien', TrangThai = 1 WHERE TenDangNhap = 'nhanvien03'");
            
            stmt.executeUpdate("UPDATE HR_Employees SET Status = N'Đang làm'");
            stmt.executeUpdate("UPDATE e SET e.FullName = n.HoTen FROM HR_Employees e JOIN NguoiDung n ON e.MaND = n.MaND");
            
            stmt.executeUpdate("UPDATE HR_Shifts SET ShiftName = N'Ca Sáng' WHERE ShiftID = 1");
            stmt.executeUpdate("UPDATE HR_Shifts SET ShiftName = N'Ca Chiều' WHERE ShiftID = 2");
            stmt.executeUpdate("UPDATE HR_Shifts SET ShiftName = N'Nghỉ Phép Tuần' WHERE ShiftID = 3");
            stmt.executeUpdate("UPDATE HR_Shifts SET ShiftName = N'Nghỉ Phép Năm' WHERE ShiftID = 4");
            stmt.executeUpdate("UPDATE HR_Shifts SET ShiftName = N'Nghỉ Không Lương' WHERE ShiftID = 5");
            
            stmt.executeUpdate("UPDATE HoaDon SET TrangThai = N'Thanh cong' WHERE TrangThai IS NULL OR TrangThai LIKE 'Th?nh c%' OR TrangThai NOT LIKE N'% %'");
            
            System.out.println("Java UTF-8 DATA SEED FIX SUCCESS!");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
