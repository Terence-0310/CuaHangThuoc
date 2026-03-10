package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import common.ConnectDB;

public class ThongKeDao {

    // Helper class for Daily Stats
    public static class ThongKeNgay {
        public int soHoaDon;
        public java.math.BigDecimal doanhThu;

        public ThongKeNgay(int soHoaDon, java.math.BigDecimal doanhThu) {
            this.soHoaDon = soHoaDon;
            this.doanhThu = doanhThu;
        }
    }

    // Helper class for Expiring Product warnings
    public static class CanhBaoHetHan {
        public String tenSanPham;
        public String soLo;
        public java.sql.Date hanSuDung;
        public int soLuongTon;
        public int soNgayConLai;

        public CanhBaoHetHan(String tenSanPham, String soLo, java.sql.Date hanSuDung, int soLuongTon, int soNgayConLai) {
            this.tenSanPham = tenSanPham;
            this.soLo = soLo;
            this.hanSuDung = hanSuDung;
            this.soLuongTon = soLuongTon;
            this.soNgayConLai = soNgayConLai;
        }
    }

    // Helper class for Low Stock warnings
    public static class CanhBaoTonKho {
        public String tenSanPham;
        public int tongTon;
        public int mucTonToiThieu;

        public CanhBaoTonKho(String tenSanPham, int tongTon, int mucTonToiThieu) {
            this.tenSanPham = tenSanPham;
            this.tongTon = tongTon;
            this.mucTonToiThieu = mucTonToiThieu;
        }
    }

    /**
     * Lấy thống kê doanh thu và số hóa đơn trong ngày hiện tại
     * @return ThongKeNgay
     */
    public ThongKeNgay getThongKeNgay() {
        ThongKeNgay stats = new ThongKeNgay(0, java.math.BigDecimal.ZERO);
        String sql = "SELECT COUNT(*) as SoHoaDon, SUM(TongTien) as DoanhThu " +
                     "FROM HoaDonBan " +
                     "WHERE CAST(NgayBan AS DATE) = CAST(GETDATE() AS DATE)";

        try (Connection con = ConnectDB.getCon();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                java.math.BigDecimal dt = rs.getBigDecimal("DoanhThu");
                stats = new ThongKeNgay(
                    rs.getInt("SoHoaDon"),
                    dt != null ? dt : java.math.BigDecimal.ZERO
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }

    /**
     * Lấy danh sách lô hàng sắp hết hạn trong khoảng ngày quy định
     * @param days Số ngày cảnh báo (ví dụ: 90 ngày)
     * @return List<CanhBaoHetHan>
     */
    public List<CanhBaoHetHan> getThuocSapHetHan(int days) {
        List<CanhBaoHetHan> list = new ArrayList<>();
        // Query to find batches expiring within 'days' from now, but not yet expired
        String sql = "SELECT sp.TenSanPham, lh.SoLo, lh.HanSuDung, lh.SoLuongTon, DATEDIFF(day, GETDATE(), lh.HanSuDung) as SoNgayConLai " +
                     "FROM LoHang lh " +
                     "JOIN SanPham sp ON lh.MaSanPham = sp.MaSanPham " +
                     "WHERE lh.SoLuongTon > 0 " +
                     "AND DATEDIFF(day, GETDATE(), lh.HanSuDung) <= ? " +
                     "ORDER BY lh.HanSuDung ASC";

        try (Connection con = ConnectDB.getCon();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, days);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new CanhBaoHetHan(
                    rs.getString("TenSanPham"),
                    rs.getString("SoLo"),
                    rs.getDate("HanSuDung"),
                    rs.getInt("SoLuongTon"),
                    rs.getInt("SoNgayConLai")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Lấy danh sách sản phẩm dưới mức tồn tối thiểu
     * @return List<CanhBaoTonKho>
     */
    public List<CanhBaoTonKho> getThuocCanNhap() {
        List<CanhBaoTonKho> list = new ArrayList<>();
        // Group by product and sum stock. If sum < minStock, add to list.
        String sql = "SELECT sp.TenSanPham, COALESCE(SUM(lh.SoLuongTon), 0) as TongTon, sp.MucTonToiThieu " +
                     "FROM SanPham sp " +
                     "LEFT JOIN LoHang lh ON sp.MaSanPham = lh.MaSanPham " +
                     "WHERE sp.DaXoa = 0 " +
                     "GROUP BY sp.MaSanPham, sp.TenSanPham, sp.MucTonToiThieu " +
                     "HAVING COALESCE(SUM(lh.SoLuongTon), 0) <= sp.MucTonToiThieu " +
                     "ORDER BY TongTon ASC";

        try (Connection con = ConnectDB.getCon();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new CanhBaoTonKho(
                    rs.getString("TenSanPham"),
                    rs.getInt("TongTon"),
                    rs.getInt("MucTonToiThieu")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
