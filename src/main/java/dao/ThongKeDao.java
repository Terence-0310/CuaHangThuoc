package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import common.ConnectDB;

/**
 * ThongKeDao - Data Access Object cho Dashboard Tổng Quan Nhà Thuốc.
 * Cung cấp các phương thức truy vấn thống kê phục vụ nghiệp vụ nhà thuốc.
 * 
 * Quy tắc:
 * - Tất cả method đều tự quản lý connection (try-with-resources)
 * - Trả về giá trị mặc định an toàn khi lỗi (0, empty list, ZERO)
 * - Không throw exception ra ngoài, chỉ log
 *
 * @version 4.0
 */
public class ThongKeDao {

    // ===================== INNER CLASSES (DTO) =====================

    /** Thống kê ngày: số hóa đơn & doanh thu */
    public static class ThongKeNgay {
        public int soHoaDon;
        public BigDecimal doanhThu;

        public ThongKeNgay(int soHoaDon, BigDecimal doanhThu) {
            this.soHoaDon = soHoaDon;
            this.doanhThu = doanhThu != null ? doanhThu : BigDecimal.ZERO;
        }
    }

    /** Cảnh báo lô hàng sắp hết hạn */
    public static class CanhBaoHetHan {
        public String maSanPham;
        public String tenSanPham;
        public String soLo;
        public java.sql.Date hanSuDung;
        public int soLuongTon;
        public int soNgayConLai;

        public CanhBaoHetHan(String maSanPham, String tenSanPham, String soLo,
                             java.sql.Date hanSuDung, int soLuongTon, int soNgayConLai) {
            this.maSanPham = maSanPham;
            this.tenSanPham = tenSanPham;
            this.soLo = soLo;
            this.hanSuDung = hanSuDung;
            this.soLuongTon = soLuongTon;
            this.soNgayConLai = soNgayConLai;
        }
    }

    /** Cảnh báo tồn kho thấp */
    public static class CanhBaoTonKho {
        public String maSanPham;
        public String tenSanPham;
        public String donViTinh;
        public int tongTon;
        public int mucTonToiThieu;

        public CanhBaoTonKho(String maSanPham, String tenSanPham, String donViTinh,
                             int tongTon, int mucTonToiThieu) {
            this.maSanPham = maSanPham;
            this.tenSanPham = tenSanPham;
            this.donViTinh = donViTinh;
            this.tongTon = tongTon;
            this.mucTonToiThieu = mucTonToiThieu;
        }
    }

    /** Sản phẩm bán chạy */
    public static class SanPhamBanChay {
        public String tenSanPham;
        public int tongSoLuong;
        public BigDecimal tongDoanhThu;

        public SanPhamBanChay(String tenSanPham, int tongSoLuong, BigDecimal tongDoanhThu) {
            this.tenSanPham = tenSanPham;
            this.tongSoLuong = tongSoLuong;
            this.tongDoanhThu = tongDoanhThu != null ? tongDoanhThu : BigDecimal.ZERO;
        }
    }

    /** Doanh thu theo ngày (cho biểu đồ) */
    public static class DoanhThuNgay {
        public String ngay;
        public BigDecimal doanhThu;
        public int soHoaDon;

        public DoanhThuNgay(String ngay, BigDecimal doanhThu, int soHoaDon) {
            this.ngay = ngay;
            this.doanhThu = doanhThu != null ? doanhThu : BigDecimal.ZERO;
            this.soHoaDon = soHoaDon;
        }
    }

    /** Hóa đơn gần đây (cho bảng hoạt động gần đây) */
    public static class HoaDonGanDay {
        public int maHoaDon;
        public String thoiGian;
        public String tenNhanVien;
        public String tenKhachHang;
        public BigDecimal tongTien;

        public HoaDonGanDay(int maHoaDon, String thoiGian, String tenNhanVien,
                            String tenKhachHang, BigDecimal tongTien) {
            this.maHoaDon = maHoaDon;
            this.thoiGian = thoiGian;
            this.tenNhanVien = tenNhanVien;
            this.tenKhachHang = tenKhachHang;
            this.tongTien = tongTien != null ? tongTien : BigDecimal.ZERO;
        }
    }

    // ===================== KPI QUERIES =====================

    /**
     * Thống kê doanh thu và số hóa đơn hôm nay
     */
    public ThongKeNgay getThongKeNgay() {
        String sql = "SELECT COUNT(*) as SoHoaDon, ISNULL(SUM(TongTien), 0) as DoanhThu " +
                     "FROM HoaDonBan " +
                     "WHERE CAST(NgayBan AS DATE) = CAST(GETDATE() AS DATE)";

        try (Connection con = ConnectDB.getCon();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return new ThongKeNgay(
                    rs.getInt("SoHoaDon"),
                    rs.getBigDecimal("DoanhThu")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ThongKeNgay(0, BigDecimal.ZERO);
    }

    /**
     * Doanh thu hôm qua (dùng để so sánh xu hướng)
     */
    public BigDecimal getDoanhThuHomQua() {
        String sql = "SELECT ISNULL(SUM(TongTien), 0) as DoanhThu FROM HoaDonBan " +
                     "WHERE CAST(NgayBan AS DATE) = CAST(DATEADD(DAY, -1, GETDATE()) AS DATE)";

        try (Connection con = ConnectDB.getCon();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                BigDecimal dt = rs.getBigDecimal("DoanhThu");
                return dt != null ? dt : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Đếm tổng số sản phẩm đang hoạt động
     */
    public int getTongSanPham() {
        String sql = "SELECT COUNT(*) FROM SanPham WHERE DaXoa = 0";
        try (Connection con = ConnectDB.getCon();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Đếm tổng số lô hàng còn tồn
     */
    public int getTongLoHang() {
        String sql = "SELECT COUNT(*) FROM LoHang WHERE SoLuongTon > 0";
        try (Connection con = ConnectDB.getCon();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Đếm tổng khách hàng
     */
    public int getTongKhachHang() {
        String sql = "SELECT COUNT(*) FROM KhachHang WHERE DaXoa = 0";
        try (Connection con = ConnectDB.getCon();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Đếm số sản phẩm tồn kho thấp (dưới mức tối thiểu)
     */
    public int getSoSanPhamTonThap() {
        String sql = "SELECT COUNT(*) FROM (" +
                     "  SELECT sp.MaSanPham " +
                     "  FROM SanPham sp " +
                     "  LEFT JOIN LoHang lh ON sp.MaSanPham = lh.MaSanPham " +
                     "  WHERE sp.DaXoa = 0 " +
                     "  GROUP BY sp.MaSanPham, sp.MucTonToiThieu " +
                     "  HAVING COALESCE(SUM(lh.SoLuongTon), 0) <= sp.MucTonToiThieu " +
                     ") t";
        try (Connection con = ConnectDB.getCon();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Đếm số lô sắp hết hạn trong N ngày
     */
    public int getSoLoSapHetHan(int days) {
        String sql = "SELECT COUNT(*) FROM LoHang lh " +
                     "JOIN SanPham sp ON lh.MaSanPham = sp.MaSanPham " +
                     "WHERE sp.DaXoa = 0 AND lh.SoLuongTon > 0 " +
                     "AND lh.HanSuDung >= CAST(GETDATE() AS DATE) " +
                     "AND DATEDIFF(day, CAST(GETDATE() AS DATE), lh.HanSuDung) <= ?";
        try (Connection con = ConnectDB.getCon();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, days);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Đếm tổng nhà cung cấp
     */
    public int getTongNhaCungCap() {
        String sql = "SELECT COUNT(*) FROM NhaCungCap WHERE DaXoa = 0";
        try (Connection con = ConnectDB.getCon();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Doanh thu tháng hiện tại
     */
    public BigDecimal getDoanhThuThang() {
        String sql = "SELECT ISNULL(SUM(TongTien), 0) as DoanhThu FROM HoaDonBan " +
                     "WHERE MONTH(NgayBan) = MONTH(GETDATE()) AND YEAR(NgayBan) = YEAR(GETDATE())";
        try (Connection con = ConnectDB.getCon();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                BigDecimal dt = rs.getBigDecimal("DoanhThu");
                return dt != null ? dt : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    // ===================== CẢNH BÁO =====================

    /**
     * Lấy danh sách lô hàng sắp hết hạn trong khoảng ngày quy định
     */
    public List<CanhBaoHetHan> getThuocSapHetHan(int days) {
        List<CanhBaoHetHan> list = new ArrayList<>();
        String sql = "SELECT CAST(sp.MaSanPham AS NVARCHAR) AS MaSP, sp.TenSanPham, lh.SoLo, lh.HanSuDung, lh.SoLuongTon, " +
                     "DATEDIFF(day, CAST(GETDATE() AS DATE), lh.HanSuDung) as SoNgayConLai " +
                     "FROM LoHang lh " +
                     "JOIN SanPham sp ON lh.MaSanPham = sp.MaSanPham " +
                     "WHERE lh.SoLuongTon > 0 AND sp.DaXoa = 0 " +
                     "AND lh.HanSuDung >= CAST(GETDATE() AS DATE) " +
                     "AND DATEDIFF(day, CAST(GETDATE() AS DATE), lh.HanSuDung) <= ? " +
                     "ORDER BY lh.HanSuDung ASC";

        try (Connection con = ConnectDB.getCon();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, days);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new CanhBaoHetHan(
                    rs.getString("MaSP"),
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
     */
    public List<CanhBaoTonKho> getThuocCanNhap() {
        List<CanhBaoTonKho> list = new ArrayList<>();
        String sql = "SELECT CAST(sp.MaSanPham AS NVARCHAR) AS MaSP, sp.TenSanPham, sp.DonViTinh, " +
                     "COALESCE(SUM(lh.SoLuongTon), 0) as TongTon, sp.MucTonToiThieu " +
                     "FROM SanPham sp " +
                     "LEFT JOIN LoHang lh ON sp.MaSanPham = lh.MaSanPham " +
                     "WHERE sp.DaXoa = 0 " +
                     "GROUP BY sp.MaSanPham, sp.TenSanPham, sp.DonViTinh, sp.MucTonToiThieu " +
                     "HAVING COALESCE(SUM(lh.SoLuongTon), 0) <= sp.MucTonToiThieu " +
                     "ORDER BY TongTon ASC";

        try (Connection con = ConnectDB.getCon();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new CanhBaoTonKho(
                    rs.getString("MaSP"),
                    rs.getString("TenSanPham"),
                    rs.getString("DonViTinh"),
                    rs.getInt("TongTon"),
                    rs.getInt("MucTonToiThieu")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ===================== HOẠT ĐỘNG GẦN ĐÂY =====================

    /**
     * Lấy danh sách hóa đơn gần nhất (top N)
     */
    public List<HoaDonGanDay> getHoaDonGanDay(int limit) {
        List<HoaDonGanDay> list = new ArrayList<>();
        String sql = "SELECT TOP (?) h.MaHoaDon, " +
                     "FORMAT(h.NgayBan, 'dd/MM/yyyy HH:mm') AS ThoiGian, " +
                     "nd.HoTen AS TenNhanVien, " +
                     "ISNULL(kh.HoTen, N'Khách lẻ') AS TenKhachHang, " +
                     "h.TongTien " +
                     "FROM HoaDonBan h " +
                     "JOIN NguoiDung nd ON h.MaNguoiDung = nd.MaNguoiDung " +
                     "LEFT JOIN KhachHang kh ON h.MaKhachHang = kh.MaKhachHang " +
                     "ORDER BY h.NgayBan DESC";

        try (Connection con = ConnectDB.getCon();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new HoaDonGanDay(
                    rs.getInt("MaHoaDon"),
                    rs.getString("ThoiGian"),
                    rs.getString("TenNhanVien"),
                    rs.getString("TenKhachHang"),
                    rs.getBigDecimal("TongTien")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ===================== THỐNG KÊ PHỤ =====================

    /**
     * Top N sản phẩm bán chạy nhất (tất cả thời gian)
     */
    public List<SanPhamBanChay> getTopSanPhamBanChay(int top) {
        List<SanPhamBanChay> list = new ArrayList<>();
        String sql = "SELECT TOP (?) sp.TenSanPham, SUM(ct.SoLuong) as TongSoLuong, SUM(ct.ThanhTien) as TongDoanhThu " +
                     "FROM ChiTietHoaDon ct " +
                     "JOIN SanPham sp ON ct.MaSanPham = sp.MaSanPham " +
                     "GROUP BY sp.MaSanPham, sp.TenSanPham " +
                     "ORDER BY TongSoLuong DESC";

        try (Connection con = ConnectDB.getCon();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, top);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new SanPhamBanChay(
                    rs.getString("TenSanPham"),
                    rs.getInt("TongSoLuong"),
                    rs.getBigDecimal("TongDoanhThu")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Top N sản phẩm bán chạy hôm nay
     */
    public List<SanPhamBanChay> getTopSanPhamBanChayHomNay(int top) {
        List<SanPhamBanChay> list = new ArrayList<>();
        String sql = "SELECT TOP (?) sp.TenSanPham, SUM(ct.SoLuong) as TongSoLuong, SUM(ct.ThanhTien) as TongDoanhThu " +
                     "FROM ChiTietHoaDon ct " +
                     "JOIN SanPham sp ON ct.MaSanPham = sp.MaSanPham " +
                     "JOIN HoaDonBan h ON ct.MaHoaDon = h.MaHoaDon " +
                     "WHERE CAST(h.NgayBan AS DATE) = CAST(GETDATE() AS DATE) " +
                     "GROUP BY sp.MaSanPham, sp.TenSanPham " +
                     "ORDER BY TongSoLuong DESC";

        try (Connection con = ConnectDB.getCon();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, top);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new SanPhamBanChay(
                    rs.getString("TenSanPham"),
                    rs.getInt("TongSoLuong"),
                    rs.getBigDecimal("TongDoanhThu")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Tỷ lệ tồn kho theo trạng thái (phục vụ progress bar)
     * - Còn hàng tốt: tồn > mức tối thiểu
     * - Tồn thấp: tồn <= mức tối thiểu
     * - Sắp hết hạn: lô hết hạn <= 30 ngày
     * - Hết hàng: tồn = 0
     */
    public Map<String, Integer> getTyLeTonKho() {
        Map<String, Integer> result = new LinkedHashMap<>();

        // Còn hàng tốt
        String sqlTot = "SELECT COUNT(*) FROM (" +
                        "  SELECT sp.MaSanPham FROM SanPham sp " +
                        "  LEFT JOIN LoHang lh ON sp.MaSanPham = lh.MaSanPham " +
                        "  WHERE sp.DaXoa = 0 " +
                        "  GROUP BY sp.MaSanPham, sp.MucTonToiThieu " +
                        "  HAVING COALESCE(SUM(lh.SoLuongTon), 0) > sp.MucTonToiThieu" +
                        ") t";

        // Tồn thấp
        String sqlThap = "SELECT COUNT(*) FROM (" +
                         "  SELECT sp.MaSanPham FROM SanPham sp " +
                         "  LEFT JOIN LoHang lh ON sp.MaSanPham = lh.MaSanPham " +
                         "  WHERE sp.DaXoa = 0 " +
                         "  GROUP BY sp.MaSanPham, sp.MucTonToiThieu " +
                         "  HAVING COALESCE(SUM(lh.SoLuongTon), 0) <= sp.MucTonToiThieu " +
                         "  AND COALESCE(SUM(lh.SoLuongTon), 0) > 0" +
                         ") t";

        // Hết hàng
        String sqlHet = "SELECT COUNT(*) FROM (" +
                        "  SELECT sp.MaSanPham FROM SanPham sp " +
                        "  LEFT JOIN LoHang lh ON sp.MaSanPham = lh.MaSanPham " +
                        "  WHERE sp.DaXoa = 0 " +
                        "  GROUP BY sp.MaSanPham " +
                        "  HAVING COALESCE(SUM(lh.SoLuongTon), 0) = 0" +
                        ") t";

        // Lô sắp hết hạn
        String sqlHetHan = "SELECT COUNT(DISTINCT lh.MaLoHang) FROM LoHang lh " +
                           "JOIN SanPham sp ON lh.MaSanPham = sp.MaSanPham " +
                           "WHERE sp.DaXoa = 0 AND lh.SoLuongTon > 0 " +
                           "AND lh.HanSuDung >= CAST(GETDATE() AS DATE) " +
                           "AND DATEDIFF(day, CAST(GETDATE() AS DATE), lh.HanSuDung) <= 30";

        try (Connection con = ConnectDB.getCon()) {
            result.put("Còn hàng tốt", executeCountQuery(con, sqlTot));
            result.put("Tồn thấp", executeCountQuery(con, sqlThap));
            result.put("Hết hàng", executeCountQuery(con, sqlHet));
            result.put("Sắp hết hạn", executeCountQuery(con, sqlHetHan));
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    /** Helper: thực thi count query */
    private int executeCountQuery(Connection con, String sql) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    /**
     * Doanh thu theo 7 ngày gần nhất (cho biểu đồ)
     */
    public List<DoanhThuNgay> getDoanhThu7NgayGanNhat() {
        List<DoanhThuNgay> list = new ArrayList<>();
        String sql = "SELECT FORMAT(d.Ngay, 'dd/MM') AS Ngay, " +
                     "    ISNULL(SUM(h.TongTien), 0) AS DoanhThu, " +
                     "    COUNT(h.MaHoaDon) AS SoHoaDon " +
                     "FROM ( " +
                     "    SELECT CAST(DATEADD(DAY, -v.number, GETDATE()) AS DATE) AS Ngay " +
                     "    FROM master.dbo.spt_values v " +
                     "    WHERE v.type = 'P' AND v.number BETWEEN 0 AND 6 " +
                     ") d " +
                     "LEFT JOIN HoaDonBan h ON CAST(h.NgayBan AS DATE) = d.Ngay " +
                     "GROUP BY d.Ngay " +
                     "ORDER BY d.Ngay ASC";

        try (Connection con = ConnectDB.getCon();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new DoanhThuNgay(
                    rs.getString("Ngay"),
                    rs.getBigDecimal("DoanhThu"),
                    rs.getInt("SoHoaDon")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Phân bố sản phẩm theo loại (cho biểu đồ tròn)
     */
    public Map<String, Integer> getPhanBoSanPhamTheoLoai() {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT LoaiSanPham, COUNT(*) as SoLuong FROM SanPham WHERE DaXoa = 0 GROUP BY LoaiSanPham ORDER BY SoLuong DESC";

        Map<String, String> tenLoai = new LinkedHashMap<>();
        tenLoai.put("Thuoc", "Thuốc");
        tenLoai.put("DuocMiPham", "Dược mỹ phẩm");
        tenLoai.put("ThucPhamChucNang", "TPCN");
        tenLoai.put("ChamSocCaNhan", "Chăm sóc CN");
        tenLoai.put("ThietBiYTe", "Thiết bị y tế");

        try (Connection con = ConnectDB.getCon();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String loai = rs.getString("LoaiSanPham");
                String tenViet = tenLoai.getOrDefault(loai, loai);
                map.put(tenViet, rs.getInt("SoLuong"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * Đếm số lô đã hết hạn nhưng còn tồn
     */
    public int getSoLoHetHan() {
        String sql = "SELECT COUNT(*) FROM LoHang lh JOIN SanPham sp ON lh.MaSanPham = sp.MaSanPham " +
                     "WHERE sp.DaXoa = 0 AND lh.SoLuongTon > 0 AND lh.HanSuDung < CAST(GETDATE() AS DATE)";
        try (Connection con = ConnectDB.getCon();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
