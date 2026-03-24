package infrastructure.repository;

import domain.entity.Invoice;
import domain.repository.IInvoiceRepository;
import infrastructure.database.DatabaseHelper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository Impl: Hóa đơn
 */
public class InvoiceRepositoryImpl implements IInvoiceRepository {

    @Override
    public int insert(Connection conn, Invoice invoice) {
        String sql = "INSERT INTO HoaDon (MaKH, MaND, TongTien, PhuongThucTT) VALUES (?, ?, 0, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (invoice.getMaKH() != null) {
                ps.setInt(1, invoice.getMaKH());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setInt(2, invoice.getMaND());
            ps.setNString(3, invoice.getPhuongThucTT() != null ? invoice.getPhuongThucTT() : "TienMat");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tạo HoaDon", e);
        }
        return -1;
    }

    @Override
    public boolean updateTotal(Connection conn, int maHD) {
        String sql = "UPDATE HoaDon SET TongTien = " +
                     "(SELECT ISNULL(SUM(ThanhTien), 0) FROM ChiTietHoaDon WHERE MaHD = ?) " +
                     "WHERE MaHD = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maHD);
            ps.setInt(2, maHD);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật tổng tiền", e);
        }
    }

    @Override
    public List<Invoice> getByCustomerId(int maKH) {
        String sql = "SELECT hd.*, nd.HoTen AS TenNhanVien FROM HoaDon hd " +
                     "JOIN NguoiDung nd ON hd.MaND = nd.MaND " +
                     "WHERE hd.MaKH = ? ORDER BY hd.NgayBan DESC";
        List<Invoice> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maKH);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn HoaDon theo KH", e);
        }
        return list;
    }

    @Override
    public List<Invoice> getAll() {
        String sql = "SELECT hd.*, kh.TenKH, kh.SoDT, nd.HoTen AS TenNhanVien FROM HoaDon hd " +
                     "LEFT JOIN KhachHang kh ON hd.MaKH = kh.MaKH " +
                     "JOIN NguoiDung nd ON hd.MaND = nd.MaND " +
                     "ORDER BY hd.NgayBan DESC";
        List<Invoice> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn HoaDon", e);
        }
        return list;
    }

    private Invoice mapRow(ResultSet rs) throws SQLException {
        Invoice inv = new Invoice();
        inv.setMaHD(rs.getInt("MaHD"));
        int maKH = rs.getInt("MaKH");
        inv.setMaKH(rs.wasNull() ? null : maKH);
        inv.setMaND(rs.getInt("MaND"));
        inv.setNgayBan(rs.getTimestamp("NgayBan").toLocalDateTime());
        inv.setTongTien(rs.getBigDecimal("TongTien"));
        try { inv.setPhuongThucTT(rs.getNString("PhuongThucTT")); } catch (SQLException ignored) {}
        try { inv.setTenNhanVien(rs.getNString("TenNhanVien")); } catch (SQLException ignored) {}
        try { inv.setTenKH(rs.getNString("TenKH")); } catch (SQLException ignored) {}
        try { inv.setSoDT(rs.getString("SoDT")); } catch (SQLException ignored) {}
        return inv;
    }
}
