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
