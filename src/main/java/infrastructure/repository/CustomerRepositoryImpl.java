package infrastructure.repository;

import domain.dto.CustomerPurchaseHistoryDTO;
import domain.entity.Customer;
import domain.repository.ICustomerRepository;
import infrastructure.database.DatabaseHelper;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository Impl: Khách hàng — full CRUD + phân trang
 */
public class CustomerRepositoryImpl implements ICustomerRepository {

    @Override
    public Customer findByPhone(String soDT) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            return findByPhone(conn, soDT);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm KhachHang", e);
        }
    }

    @Override
    public Customer findByPhone(Connection conn, String soDT) {
        String sql = "SELECT k.*, ISNULL(t.TongMua, 0) AS TongMua " +
                     "FROM KhachHang k " +
                     "LEFT JOIN (SELECT MaKH, SUM(TongTien) AS TongMua FROM HoaDon GROUP BY MaKH) t ON k.MaKH = t.MaKH " +
                     "WHERE k.SoDT = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, soDT);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm KhachHang", e);
        }
        return null;
    }

    @Override
    public Customer getById(int maKH) {
        String sql = "SELECT k.*, ISNULL(t.TongMua, 0) AS TongMua " +
                     "FROM KhachHang k " +
                     "LEFT JOIN (SELECT MaKH, SUM(TongTien) AS TongMua FROM HoaDon GROUP BY MaKH) t ON k.MaKH = t.MaKH " +
                     "WHERE k.MaKH = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maKH);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm KhachHang by ID", e);
        }
        return null;
    }

    @Override
    public int insert(Connection conn, Customer c) {
        String sql = "INSERT INTO KhachHang (SoDT, TenKH, GioiTinh) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getSoDT());
            ps.setNString(2, c.getTenKH());
            ps.setNString(3, c.getGioiTinh());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi thêm KhachHang", e);
        }
        return -1;
    }

    @Override
    public int insert(Customer c) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            return insert(conn, c);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi thêm KhachHang", e);
        }
    }

    @Override
    public boolean update(Customer c) {
        String sql = "UPDATE KhachHang SET TenKH = ?, SoDT = ?, GioiTinh = ? WHERE MaKH = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, c.getTenKH());
            ps.setString(2, c.getSoDT());
            ps.setNString(3, c.getGioiTinh());
            ps.setInt(4, c.getMaKH());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật KhachHang", e);
        }
    }

    @Override
    public boolean delete(int maKH) {
        // Kiểm tra xem khách có hóa đơn không
        String checkSql = "SELECT COUNT(1) FROM HoaDon WHERE MaKH = ?";
        String deleteSql = "DELETE FROM KhachHang WHERE MaKH = ?";
        try (Connection conn = DatabaseHelper.getConnection()) {
            try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                check.setInt(1, maKH);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new RuntimeException("KHÁCH CÓ HOÁ ĐƠN");
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                ps.setInt(1, maKH);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi xóa KhachHang", e);
        }
    }

    @Override
    public List<Customer> getAll() {
        String sql = "SELECT k.*, ISNULL(t.TongMua, 0) AS TongMua " +
                     "FROM KhachHang k " +
                     "LEFT JOIN (SELECT MaKH, SUM(TongTien) AS TongMua FROM HoaDon GROUP BY MaKH) t ON k.MaKH = t.MaKH " +
                     "ORDER BY k.TenKH";
        return queryList(sql);
    }

    @Override
    public List<Customer> search(String keyword) {
        String sql = "SELECT k.*, ISNULL(t.TongMua, 0) AS TongMua " +
                     "FROM KhachHang k " +
                     "LEFT JOIN (SELECT MaKH, SUM(TongTien) AS TongMua FROM HoaDon GROUP BY MaKH) t ON k.MaKH = t.MaKH " +
                     "WHERE k.TenKH LIKE ? OR k.SoDT LIKE ? ORDER BY k.TenKH";
        List<Customer> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm KhachHang", e);
        }
        return list;
    }

    @Override
    public List<Customer> getPagedList(int offset, int pageSize, String keyword,
                                        String sortCol, String sortDir) {
        StringBuilder sql = new StringBuilder(
            "SELECT k.*, ISNULL(t.TongMua, 0) AS TongMua " +
            "FROM KhachHang k " +
            "LEFT JOIN (SELECT MaKH, SUM(TongTien) AS TongMua FROM HoaDon GROUP BY MaKH) t ON k.MaKH = t.MaKH " +
            "WHERE 1=1 ");
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (k.TenKH LIKE ? OR k.SoDT LIKE ?) ");
        }
        String safeCol = mapSortColumn(sortCol);
        String safeDir = "DESC".equalsIgnoreCase(sortDir) ? "DESC" : "ASC";
        sql.append("ORDER BY ").append(safeCol).append(" ").append(safeDir);
        if (!"k.MaKH".equals(safeCol)) sql.append(", k.MaKH ASC");
        sql.append(" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        List<Customer> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (keyword != null && !keyword.trim().isEmpty()) {
                ps.setNString(idx++, "%" + keyword.trim() + "%");
                ps.setString(idx++, "%" + keyword.trim() + "%");
            }
            ps.setInt(idx++, offset);
            ps.setInt(idx, pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi phân trang KhachHang", e);
        }
        return list;
    }

    @Override
    public int countFiltered(String keyword) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(1) FROM KhachHang k WHERE 1=1 ");
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (k.TenKH LIKE ? OR k.SoDT LIKE ?) ");
        }
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (keyword != null && !keyword.trim().isEmpty()) {
                ps.setNString(idx++, "%" + keyword.trim() + "%");
                ps.setString(idx, "%" + keyword.trim() + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi đếm KhachHang", e);
        }
        return 0;
    }

    @Override
    public List<CustomerPurchaseHistoryDTO> getCustomerPurchaseHistory(int maKH) {
        String sql = "SELECT hd.MaHD, hd.NgayBan, hd.TongTien, " +
                     "COUNT(ct.MaSP) AS SoSP, nd.HoTen AS NhanVien " +
                     "FROM HoaDon hd " +
                     "JOIN NguoiDung nd ON hd.MaND = nd.MaND " +
                     "LEFT JOIN ChiTietHoaDon ct ON hd.MaHD = ct.MaHD " +
                     "WHERE hd.MaKH = ? " +
                     "GROUP BY hd.MaHD, hd.NgayBan, hd.TongTien, nd.HoTen " +
                     "ORDER BY hd.NgayBan DESC";
        List<CustomerPurchaseHistoryDTO> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maKH);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new CustomerPurchaseHistoryDTO(
                            rs.getInt("MaHD"),
                            rs.getTimestamp("NgayBan").toLocalDateTime(),
                            rs.getBigDecimal("TongTien"),
                            rs.getInt("SoSP"),
                            rs.getNString("NhanVien")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn lịch sử mua hàng", e);
        }
        return list;
    }

    // === Private helpers ===

    private String mapSortColumn(String uiCol) {
        if (uiCol == null) return "k.MaKH";
        switch (uiCol) {
            case "MaKH":     return "k.MaKH";
            case "TenKH":    return "k.TenKH";
            case "SoDT":     return "k.SoDT";
            case "GioiTinh": return "k.GioiTinh";
            case "TongMua":  return "TongMua";
            case "NgayTao":  return "k.NgayTao";
            default:         return "k.MaKH";
        }
    }

    private List<Customer> queryList(String sql) {
        List<Customer> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn KhachHang", e);
        }
        return list;
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setMaKH(rs.getInt("MaKH"));
        c.setSoDT(rs.getString("SoDT"));
        c.setTenKH(rs.getNString("TenKH"));
        try { c.setGioiTinh(rs.getNString("GioiTinh")); } catch (SQLException ignored) {}
        try {
            BigDecimal tong = rs.getBigDecimal("TongMua");
            c.setTongMua(tong != null ? tong : BigDecimal.ZERO);
        } catch (SQLException ignored) { c.setTongMua(BigDecimal.ZERO); }
        try {
            Timestamp ts = rs.getTimestamp("NgayTao");
            if (ts != null) c.setNgayTao(ts.toLocalDateTime());
        } catch (SQLException ignored) {}
        return c;
    }
}
