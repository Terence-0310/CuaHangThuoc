package infrastructure.repository;

import domain.entity.Customer;
import domain.repository.ICustomerRepository;
import infrastructure.database.DatabaseHelper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository Impl: Khách hàng
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
        String sql = "SELECT * FROM KhachHang WHERE SoDT = ?";
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
    public int insert(Connection conn, Customer customer) {
        String sql = "INSERT INTO KhachHang (SoDT, TenKH) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, customer.getSoDT());
            ps.setNString(2, customer.getTenKH());
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
    public List<Customer> getAll() {
        String sql = "SELECT * FROM KhachHang ORDER BY TenKH";
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

    @Override
    public List<Customer> search(String keyword) {
        String sql = "SELECT * FROM KhachHang WHERE TenKH LIKE ? OR SoDT LIKE ? ORDER BY TenKH";
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

    private Customer mapRow(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setMaKH(rs.getInt("MaKH"));
        c.setSoDT(rs.getString("SoDT"));
        c.setTenKH(rs.getNString("TenKH"));
        return c;
    }
}
