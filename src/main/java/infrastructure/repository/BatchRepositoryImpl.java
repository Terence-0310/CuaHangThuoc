package infrastructure.repository;

import domain.entity.Batch;
import domain.repository.IBatchRepository;
import infrastructure.database.DatabaseHelper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository Impl: Lô hàng (FEFO support)
 * ★ MaND đã chuyển sang PhieuNhap, LoHang giờ dùng MaPN
 */
public class BatchRepositoryImpl implements IBatchRepository {

    @Override
    public int insert(Batch batch) {
        String sql = "INSERT INTO LoHang (MaSP, SoLo, HanSuDung, SoLuong, GiaNhap, MaPN) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, batch.getMaSP());
            ps.setNString(2, batch.getSoLo());
            ps.setDate(3, Date.valueOf(batch.getHanSuDung()));
            ps.setInt(4, batch.getSoLuong());
            ps.setBigDecimal(5, batch.getGiaNhap());
            if (batch.getMaPN() != null) {
                ps.setInt(6, batch.getMaPN());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi nhập kho", e);
        }
        return -1;
    }

    @Override
    public List<Batch> getByProductId(int maSP) {
        String sql = "SELECT lh.*, sp.TenSP, sp.DonViTinh, nd.HoTen AS TenNguoiNhap FROM LoHang lh " +
                     "JOIN SanPham sp ON lh.MaSP = sp.MaSP " +
                     "LEFT JOIN PhieuNhap pn ON lh.MaPN = pn.MaPN " +
                     "LEFT JOIN NguoiDung nd ON pn.MaND = nd.MaND " +
                     "WHERE lh.MaSP = ? AND lh.SoLuong > 0 " +
                     "ORDER BY lh.HanSuDung ASC, lh.NgayNhap ASC, lh.MaLo ASC";
        List<Batch> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maSP);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn LoHang", e);
        }
        return list;
    }

    @Override
    public List<Batch> getAllWithProduct() {
        String sql = "SELECT lh.*, sp.TenSP, sp.DonViTinh, nd.HoTen AS TenNguoiNhap FROM LoHang lh " +
                     "JOIN SanPham sp ON lh.MaSP = sp.MaSP " +
                     "LEFT JOIN PhieuNhap pn ON lh.MaPN = pn.MaPN " +
                     "LEFT JOIN NguoiDung nd ON pn.MaND = nd.MaND " +
                     "WHERE lh.SoLuong > 0 AND sp.TrangThai = 1 " +
                     "ORDER BY sp.TenSP, lh.HanSuDung ASC, lh.NgayNhap ASC, lh.MaLo ASC";
        List<Batch> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn tồn kho", e);
        }
        return list;
    }

    @Override
    public List<Batch> getFEFO(Connection conn, int maSP) {
        // ★ UPDLOCK: Khóa dòng tránh Race Condition
        String sql = "SELECT MaLo, MaSP, SoLo, HanSuDung, SoLuong, GiaNhap, NgayNhap FROM LoHang WITH (UPDLOCK, ROWLOCK) " +
                     "WHERE MaSP = ? AND SoLuong > 0 AND HanSuDung > GETDATE() " +
                     "ORDER BY HanSuDung ASC, NgayNhap ASC, MaLo ASC";
        List<Batch> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maSP);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Batch b = new Batch();
                    b.setMaLo(rs.getInt("MaLo"));
                    b.setMaSP(rs.getInt("MaSP"));
                    b.setSoLo(rs.getNString("SoLo"));
                    b.setHanSuDung(rs.getDate("HanSuDung").toLocalDate());
                    b.setSoLuong(rs.getInt("SoLuong"));
                    b.setGiaNhap(rs.getBigDecimal("GiaNhap"));
                    Timestamp ngayNhap = rs.getTimestamp("NgayNhap");
                    if (ngayNhap != null) {
                        b.setNgayNhap(ngayNhap.toLocalDateTime());
                    }
                    list.add(b);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi FEFO query", e);
        }
        return list;
    }

    @Override
    public boolean updateQuantity(Connection conn, int maLo, int newQuantity) {
        String sql = "UPDATE LoHang SET SoLuong = ? WHERE MaLo = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newQuantity);
            ps.setInt(2, maLo);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật tồn kho", e);
        }
    }

    private Batch mapRow(ResultSet rs) throws SQLException {
        Batch b = new Batch();
        b.setMaLo(rs.getInt("MaLo"));
        b.setMaSP(rs.getInt("MaSP"));
        b.setSoLo(rs.getNString("SoLo"));
        b.setHanSuDung(rs.getDate("HanSuDung").toLocalDate());
        b.setSoLuong(rs.getInt("SoLuong"));
        b.setGiaNhap(rs.getBigDecimal("GiaNhap"));
        b.setNgayNhap(rs.getTimestamp("NgayNhap").toLocalDateTime());
        try { b.setMaPN(rs.getObject("MaPN") != null ? rs.getInt("MaPN") : null); } catch (SQLException ignored) {}
        try { b.setTenSP(rs.getNString("TenSP")); } catch (SQLException ignored) {}
        try { b.setDonViTinh(rs.getNString("DonViTinh")); } catch (SQLException ignored) {}
        try { b.setTenNguoiNhap(rs.getNString("TenNguoiNhap")); } catch (SQLException ignored) {}
        return b;
    }
}
