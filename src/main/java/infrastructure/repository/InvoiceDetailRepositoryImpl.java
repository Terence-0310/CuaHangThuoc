package infrastructure.repository;

import domain.entity.InvoiceDetail;
import domain.repository.IInvoiceDetailRepository;
import infrastructure.database.DatabaseHelper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository Impl: Chi tiết hóa đơn
 */
public class InvoiceDetailRepositoryImpl implements IInvoiceDetailRepository {

    @Override
    public int insert(Connection conn, InvoiceDetail detail) {
        String sql = "INSERT INTO ChiTietHoaDon (MaHD, MaLo, MaSP, SoLuong, DonGia, GiaVon, ThanhTien) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, detail.getMaHD());
            ps.setInt(2, detail.getMaLo());
            ps.setInt(3, detail.getMaSP());
            ps.setInt(4, detail.getSoLuong());
            ps.setBigDecimal(5, detail.getDonGia());
            ps.setBigDecimal(6, detail.getGiaVon());
            ps.setBigDecimal(7, detail.getThanhTien());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi thêm ChiTietHoaDon", e);
        }
        return -1;
    }

    @Override
    public List<InvoiceDetail> getByInvoiceId(int maHD) {
        String sql = "SELECT ct.*, sp.TenSP, sp.DonViTinh, lh.SoLo " +
                     "FROM ChiTietHoaDon ct " +
                     "JOIN SanPham sp ON ct.MaSP = sp.MaSP " +
                     "JOIN LoHang lh ON ct.MaLo = lh.MaLo " +
                     "WHERE ct.MaHD = ? ORDER BY ct.MaCTHD";
        List<InvoiceDetail> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maHD);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn ChiTietHoaDon", e);
        }
        return list;
    }

    private InvoiceDetail mapRow(ResultSet rs) throws SQLException {
        InvoiceDetail d = new InvoiceDetail();
        d.setMaCTHD(rs.getInt("MaCTHD"));
        d.setMaHD(rs.getInt("MaHD"));
        d.setMaLo(rs.getInt("MaLo"));
        d.setMaSP(rs.getInt("MaSP"));
        d.setSoLuong(rs.getInt("SoLuong"));
        d.setDonGia(rs.getBigDecimal("DonGia"));
        try { d.setGiaVon(rs.getBigDecimal("GiaVon")); } catch (SQLException ignored) {}
        d.setThanhTien(rs.getBigDecimal("ThanhTien"));
        try { d.setTenSP(rs.getNString("TenSP")); } catch (SQLException ignored) {}
        try { d.setDonViTinh(rs.getNString("DonViTinh")); } catch (SQLException ignored) {}
        try { d.setSoLo(rs.getNString("SoLo")); } catch (SQLException ignored) {}
        return d;
    }
}
