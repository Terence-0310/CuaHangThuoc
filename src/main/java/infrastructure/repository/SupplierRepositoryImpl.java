package infrastructure.repository;

import domain.entity.Supplier;
import domain.repository.ISupplierRepository;
import infrastructure.database.DatabaseHelper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository Impl: Nhà cung cấp
 */
public class SupplierRepositoryImpl implements ISupplierRepository {

    @Override
    public int insert(Supplier s) {
        String sql = "INSERT INTO NhaCungCap (TenNCC, SoDT, DiaChi, Email) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setNString(1, s.getTenNCC());
            ps.setString(2, s.getSoDT());
            ps.setNString(3, s.getDiaChi());
            ps.setString(4, s.getEmail());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi thêm NCC", e);
        }
        return -1;
    }

    @Override
    public boolean update(Supplier s) {
        String sql = "UPDATE NhaCungCap SET TenNCC = ?, SoDT = ?, DiaChi = ?, Email = ? WHERE MaNCC = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, s.getTenNCC());
            ps.setString(2, s.getSoDT());
            ps.setNString(3, s.getDiaChi());
            ps.setString(4, s.getEmail());
            ps.setInt(5, s.getMaNCC());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật NCC", e);
        }
    }

    @Override
    public boolean softDelete(int maNCC) {
        String sql = "UPDATE NhaCungCap SET TrangThai = 0 WHERE MaNCC = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maNCC);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi xóa NCC", e);
        }
    }

    @Override
    public Supplier getById(int maNCC) {
        String sql = "SELECT * FROM NhaCungCap WHERE MaNCC = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maNCC);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn NCC", e);
        }
        return null;
    }

    @Override
    public List<Supplier> getAll() {
        return queryList("SELECT * FROM NhaCungCap ORDER BY TenNCC");
    }

    @Override
    public List<Supplier> getActive() {
        return queryList("SELECT * FROM NhaCungCap WHERE TrangThai = 1 ORDER BY TenNCC");
    }

    @Override
    public List<Supplier> search(String keyword) {
        String sql = "SELECT * FROM NhaCungCap WHERE TrangThai = 1 AND TenNCC LIKE ? ORDER BY TenNCC";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                List<Supplier> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm NCC", e);
        }
    }

    private List<Supplier> queryList(String sql) {
        List<Supplier> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn NCC", e);
        }
        return list;
    }

    @Override
    public List<Supplier> getPagedList(int offset, int pageSize, String keyword,
                                        String statusFilter, String sortCol, String sortDir) {
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM NhaCungCap WHERE 1=1 ");
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (TenNCC LIKE ? OR SoDT LIKE ? OR Email LIKE ?) ");
        }
        if ("active".equalsIgnoreCase(statusFilter)) {
            sql.append("AND TrangThai = 1 ");
        } else if ("inactive".equalsIgnoreCase(statusFilter)) {
            sql.append("AND TrangThai = 0 ");
        }
        String safeCol = mapSortColumn(sortCol);
        String safeDir = "DESC".equalsIgnoreCase(sortDir) ? "DESC" : "ASC";
        sql.append("ORDER BY ").append(safeCol).append(" ").append(safeDir);
        if (!"MaNCC".equals(safeCol)) sql.append(", MaNCC ASC");
        sql.append(" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        List<Supplier> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = "%" + keyword.trim() + "%";
                ps.setNString(idx++, kw);
                ps.setString(idx++, kw);
                ps.setString(idx++, kw);
            }
            ps.setInt(idx++, offset);
            ps.setInt(idx, pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi phân trang NCC", e);
        }
        return list;
    }

    @Override
    public int countFiltered(String keyword, String statusFilter) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(1) FROM NhaCungCap WHERE 1=1 ");
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (TenNCC LIKE ? OR SoDT LIKE ? OR Email LIKE ?) ");
        }
        if ("active".equalsIgnoreCase(statusFilter)) {
            sql.append("AND TrangThai = 1 ");
        } else if ("inactive".equalsIgnoreCase(statusFilter)) {
            sql.append("AND TrangThai = 0 ");
        }
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = "%" + keyword.trim() + "%";
                ps.setNString(idx++, kw);
                ps.setString(idx++, kw);
                ps.setString(idx, kw);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi đếm NCC", e);
        }
        return 0;
    }

    private String mapSortColumn(String uiCol) {
        if (uiCol == null) return "MaNCC";
        switch (uiCol) {
            case "MaNCC":     return "MaNCC";
            case "TenNCC":    return "TenNCC";
            case "SoDT":      return "SoDT";
            case "Email":     return "Email";
            case "TrangThai": return "TrangThai";
            case "NgayTao":   return "NgayTao";
            default:          return "MaNCC";
        }
    }

    private Supplier mapRow(ResultSet rs) throws SQLException {
        Supplier s = new Supplier();
        s.setMaNCC(rs.getInt("MaNCC"));
        s.setTenNCC(rs.getNString("TenNCC"));
        s.setSoDT(rs.getString("SoDT"));
        s.setDiaChi(rs.getNString("DiaChi"));
        s.setEmail(rs.getString("Email"));
        s.setTrangThai(rs.getBoolean("TrangThai"));
        s.setNgayTao(rs.getTimestamp("NgayTao").toLocalDateTime());
        return s;
    }
}

