package infrastructure.repository;

import domain.entity.User;
import domain.repository.IUserRepository;
import infrastructure.database.DatabaseHelper;

import java.sql.*;

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
