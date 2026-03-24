package infrastructure.repository;

import domain.entity.Product;
import domain.repository.IProductRepository;
import infrastructure.database.DatabaseHelper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository Impl: Sản phẩm
 * ⚠️ GiaNhap đã dời sang LoHang — SanPham chỉ còn GiaBan
 */
public class ProductRepositoryImpl implements IProductRepository {

    @Override
    public List<Product> getAll() {
        String sql = "SELECT * FROM SanPham ORDER BY TenSP";
        return queryList(sql);
    }

    @Override
    public List<Product> getActive() {
        String sql = "SELECT * FROM SanPham WHERE TrangThai = 1 ORDER BY TenSP";
        return queryList(sql);
    }

    @Override
    public List<Product> getAllWithStock() {
        String sql = "SELECT sp.*, ISNULL(v.TongTonKho, 0) AS TongTonKho " +
                     "FROM SanPham sp LEFT JOIN vw_TonKhoTheoSanPham v ON sp.MaSP = v.MaSP " +
                     "ORDER BY sp.TenSP";
        List<Product> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Product p = mapRow(rs);
                p.setTongTonKho(rs.getInt("TongTonKho"));
                list.add(p);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn SP + TonKho", e);
        }
        return list;
    }

    @Override
    public Product getById(int maSP) {
        String sql = "SELECT * FROM SanPham WHERE MaSP = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maSP);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn SanPham", e);
        }
        return null;
    }

    @Override
    public List<Product> search(String keyword) {
        String sql = "SELECT * FROM SanPham WHERE TrangThai = 1 AND TenSP LIKE ? ORDER BY TenSP";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                List<Product> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm kiếm SanPham", e);
        }
    }

    @Override
    public boolean existsByNameAndUnit(String tenSP, String donViTinh) {
        String sql = "SELECT COUNT(1) FROM SanPham WHERE TenSP = ? AND DonViTinh = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, tenSP);
            ps.setNString(2, donViTinh);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi check trùng SanPham", e);
        }
        return false;
    }

    @Override
    public List<Product> getPagedWithStock(int offset, int pageSize, String keyword, String statusFilter,
                                            String sortColumn, String sortDirection) {
        StringBuilder sql = new StringBuilder(
            "SELECT sp.*, ISNULL(v.TongTonKho, 0) AS TongTonKho " +
            "FROM SanPham sp LEFT JOIN vw_TonKhoTheoSanPham v ON sp.MaSP = v.MaSP WHERE 1=1 ");
        appendFilters(sql, keyword, statusFilter);

        // ★ Safe column mapping — chống SQL Injection (switch-case whitelist)
        String safeCol = mapSortColumn(sortColumn);
        String safeDir = "DESC".equalsIgnoreCase(sortDirection) ? "DESC" : "ASC";

        // ★ Tie-breaker: chỉ ghép MaSP khi sort cột KHÁC (tránh duplicate column error)
        sql.append(" ORDER BY ").append(safeCol).append(" ").append(safeDir);
        if (!"sp.MaSP".equals(safeCol)) {
            sql.append(", sp.MaSP ASC");
        }
        sql.append(" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        List<Product> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = bindFilters(ps, 1, keyword, statusFilter);
            ps.setInt(idx++, offset);
            ps.setInt(idx, pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Product p = mapRow(rs);
                    p.setTongTonKho(rs.getInt("TongTonKho"));
                    list.add(p);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi phân trang SanPham", e);
        }
        return list;
    }

    /**
     * ★ Whitelist mapping: tên cột UI → tên cột DB thật
     * Chỉ cho phép các cột hợp lệ, mặc định sp.MaSP
     */
    private String mapSortColumn(String uiColumn) {
        if (uiColumn == null) return "sp.MaSP";
        switch (uiColumn) {
            case "MaSP":      return "sp.MaSP";
            case "TenSP":     return "sp.TenSP";
            case "DonViTinh":  return "sp.DonViTinh";
            case "GiaBan":    return "sp.GiaBan";
            case "GiaBanSi":  return "sp.GiaBanSi";
            case "TongTonKho": return "TongTonKho";
            case "TrangThai": return "sp.TrangThai";
            default:          return "sp.MaSP";
        }
    }

    @Override
    public int countFiltered(String keyword, String statusFilter) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(1) FROM SanPham sp WHERE 1=1 ");
        appendFilters(sql, keyword, statusFilter);
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindFilters(ps, 1, keyword, statusFilter);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi đếm SanPham", e);
        }
        return 0;
    }

    /** Helper: thêm WHERE filters */
    private void appendFilters(StringBuilder sql, String keyword, String statusFilter) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND sp.TenSP LIKE ? ");
        }
        if ("Đang bán".equals(statusFilter)) {
            sql.append(" AND sp.TrangThai = 1 ");
        } else if ("Ngừng bán".equals(statusFilter)) {
            sql.append(" AND sp.TrangThai = 0 ");
        }
    }

    /** Helper: bind filter params, trả về next index */
    private int bindFilters(PreparedStatement ps, int idx, String keyword, String statusFilter) throws SQLException {
        if (keyword != null && !keyword.trim().isEmpty()) {
            ps.setNString(idx++, "%" + keyword.trim() + "%");
        }
        return idx;
    }

    @Override
    public int insert(Product p) {
        String sql = "INSERT INTO SanPham (TenSP, DonViTinh, GiaBan, GiaBanSi) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setNString(1, p.getTenSP());
            ps.setNString(2, p.getDonViTinh());
            ps.setBigDecimal(3, p.getGiaBan());
            ps.setBigDecimal(4, p.getGiaBanSi());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi thêm SanPham", e);
        }
        return -1;
    }

    @Override
    public boolean update(Product p) {
        String sql = "UPDATE SanPham SET TenSP = ?, DonViTinh = ?, GiaBan = ?, GiaBanSi = ?, TrangThai = ? WHERE MaSP = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, p.getTenSP());
            ps.setNString(2, p.getDonViTinh());
            ps.setBigDecimal(3, p.getGiaBan());
            ps.setBigDecimal(4, p.getGiaBanSi());
            ps.setBoolean(5, p.isTrangThai());
            ps.setInt(6, p.getMaSP());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật SanPham", e);
        }
    }

    @Override
    public boolean softDelete(int maSP) {
        String sql = "UPDATE SanPham SET TrangThai = 0 WHERE MaSP = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maSP);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi xóa SanPham", e);
        }
    }

    // === Private helpers ===

    private List<Product> queryList(String sql) {
        List<Product> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn SanPham", e);
        }
        return list;
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setMaSP(rs.getInt("MaSP"));
        p.setTenSP(rs.getNString("TenSP"));
        p.setDonViTinh(rs.getNString("DonViTinh"));
        p.setGiaBan(rs.getBigDecimal("GiaBan"));
        try { p.setGiaBanSi(rs.getBigDecimal("GiaBanSi")); } catch (SQLException ignored) {}
        p.setTrangThai(rs.getBoolean("TrangThai"));
        return p;
    }

    /**
     * ★ Bulk update: UPDATE SanPham SET TrangThai = ? WHERE MaSP IN (?, ?, ...)
     * An toàn: dùng PreparedStatement với dynamic placeholders (không ghép chuỗi ID)
     */
    @Override
    public int bulkUpdateStatus(List<Integer> ids, boolean trangThai) {
        if (ids == null || ids.isEmpty()) return 0;

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
        }

        String sql = "UPDATE SanPham SET TrangThai = ? WHERE MaSP IN (" + placeholders + ")";
        Connection conn = null;
        try {
            conn = DatabaseHelper.getConnection();
            conn.setAutoCommit(false);  // ★ BEGIN TRANSACTION

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBoolean(1, trangThai);
                for (int i = 0; i < ids.size(); i++) {
                    ps.setInt(i + 2, ids.get(i));
                }
                int affected = ps.executeUpdate();
                conn.commit();  // ★ COMMIT
                return affected;
            }
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}  // ★ ROLLBACK
            }
            throw new RuntimeException("Lỗi cập nhật trạng thái hàng loạt", e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }
}
