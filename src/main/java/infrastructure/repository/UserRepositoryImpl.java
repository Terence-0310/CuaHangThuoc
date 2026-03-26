package infrastructure.repository;

import domain.dto.UserListDTO;
import domain.entity.User;
import domain.repository.IUserRepository;
import infrastructure.database.DatabaseHelper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository Impl: Người dùng (LSP — có thể swap với Mock)
 */
public class UserRepositoryImpl implements IUserRepository {

    @Override
    public User findByCredentials(String tenDangNhap, String matKhau) {
        String sql = "SELECT MaND, TenDangNhap, MatKhau, HoTen, VaiTro, TrangThai " +
                     "FROM NguoiDung WHERE TenDangNhap = ? AND MatKhau = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, tenDangNhap);
            ps.setNString(2, matKhau);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn NguoiDung", e);
        }
        return null;
    }

    // ================================================================
    //  Tìm kiếm, phân trang, sắp xếp
    // ================================================================

    @Override
    public List<UserListDTO> searchUsers(String keyword, String roleFilter, String sortCol,
                                          boolean sortAsc, int offset, int pageSize) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT MaND, TenDangNhap, MatKhau, HoTen, VaiTro, TrangThai, NgayTao " +
                "FROM NguoiDung WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, keyword, roleFilter);

        String safeCol = mapSortColumn(sortCol);
        sql.append("ORDER BY ").append(safeCol).append(sortAsc ? " ASC" : " DESC");
        if (!"MaND".equals(safeCol)) sql.append(", MaND ASC");
        sql.append(" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add(offset);
        params.add(pageSize);

        List<UserListDTO> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UserListDTO dto = new UserListDTO();
                    dto.setMaND(rs.getInt("MaND"));
                    dto.setTenDangNhap(rs.getNString("TenDangNhap"));
                    dto.setMatKhau(rs.getNString("MatKhau"));
                    dto.setHoTen(rs.getNString("HoTen"));
                    dto.setVaiTro(rs.getNString("VaiTro"));
                    dto.setTrangThai(rs.getBoolean("TrangThai"));
                    try {
                        Date ngayTao = rs.getDate("NgayTao");
                        if (ngayTao != null) dto.setNgayTao(ngayTao.toLocalDate());
                    } catch (SQLException ignored) {}
                    list.add(dto);
                }
            }
        }
        return list;
    }

    @Override
    public int countUsers(String keyword, String roleFilter) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(1) FROM NguoiDung WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, keyword, roleFilter);

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    // ================================================================
    //  CRUD
    // ================================================================

    @Override
    public boolean isDuplicateUsername(String tenDN, int excludeMaND) throws SQLException {
        String sql = "SELECT COUNT(1) FROM NguoiDung WHERE TenDangNhap = ? AND MaND <> ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, tenDN);
            ps.setInt(2, excludeMaND);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    @Override
    public void insertUser(User user) throws SQLException {
        String sql = "INSERT INTO NguoiDung (TenDangNhap, MatKhau, HoTen, VaiTro, TrangThai) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, user.getTenDangNhap());
            ps.setNString(2, user.getMatKhau());
            ps.setNString(3, user.getHoTen());
            ps.setNString(4, user.getVaiTro());
            ps.setBoolean(5, user.isTrangThai());
            ps.executeUpdate();
        }
    }

    @Override
    public void updateUser(User user, boolean updatePassword) throws SQLException {
        String sql;
        if (updatePassword) {
            sql = "UPDATE NguoiDung SET TenDangNhap = ?, MatKhau = ?, HoTen = ?, VaiTro = ?, TrangThai = ? WHERE MaND = ?";
        } else {
            sql = "UPDATE NguoiDung SET TenDangNhap = ?, HoTen = ?, VaiTro = ?, TrangThai = ? WHERE MaND = ?";
        }
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setNString(idx++, user.getTenDangNhap());
            if (updatePassword) {
                ps.setNString(idx++, user.getMatKhau());
            }
            ps.setNString(idx++, user.getHoTen());
            ps.setNString(idx++, user.getVaiTro());
            ps.setBoolean(idx++, user.isTrangThai());
            ps.setInt(idx, user.getMaND());
            ps.executeUpdate();
        }
    }

    @Override
    public void updateStatus(int maND, boolean newStatus) throws SQLException {
        String sql = "UPDATE NguoiDung SET TrangThai = ? WHERE MaND = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, newStatus);
            ps.setInt(2, maND);
            ps.executeUpdate();
        }
    }

    @Override
    public void updatePassword(int maND, String newPassword) throws SQLException {
        String sql = "UPDATE NguoiDung SET MatKhau = ? WHERE MaND = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, newPassword);
            ps.setInt(2, maND);
            ps.executeUpdate();
        }
    }

    // ================================================================
    //  Private helpers
    // ================================================================

    private void appendFilters(StringBuilder sql, List<Object> params, String keyword, String roleFilter) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (HoTen LIKE ? OR TenDangNhap LIKE ?) ");
            params.add("%" + keyword.trim() + "%");
            params.add("%" + keyword.trim() + "%");
        }
        if (roleFilter != null && !roleFilter.isEmpty() && !"all".equalsIgnoreCase(roleFilter)) {
            sql.append("AND VaiTro = ? ");
            params.add(roleFilter);
        }
    }

    private String mapSortColumn(String uiCol) {
        if (uiCol == null) return "MaND";
        switch (uiCol) {
            case "MaND":         return "MaND";
            case "TenDangNhap":  return "TenDangNhap";
            case "HoTen":        return "HoTen";
            case "VaiTro":       return "VaiTro";
            case "TrangThai":    return "TrangThai";
            case "NgayTao":      return "NgayTao";
            default:             return "MaND";
        }
    }

    private void setParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object p = params.get(i);
            if (p instanceof Integer) {
                ps.setInt(i + 1, (Integer) p);
            } else if (p instanceof String) {
                ps.setNString(i + 1, (String) p);
            } else if (p instanceof Boolean) {
                ps.setBoolean(i + 1, (Boolean) p);
            } else {
                ps.setObject(i + 1, p);
            }
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setMaND(rs.getInt("MaND"));
        u.setTenDangNhap(rs.getNString("TenDangNhap"));
        u.setMatKhau(rs.getNString("MatKhau"));
        u.setHoTen(rs.getNString("HoTen"));
        u.setVaiTro(rs.getNString("VaiTro"));
        u.setTrangThai(rs.getBoolean("TrangThai"));
        return u;
    }
}
