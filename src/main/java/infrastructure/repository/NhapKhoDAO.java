package infrastructure.repository;

import common.Session;
import domain.dto.ImportCartItem;
import domain.entity.Product;
import domain.entity.Supplier;
import infrastructure.database.DatabaseHelper;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ★ NhapKhoDAO — DAO xử lý toàn bộ nghiệp vụ Nhập Kho
 *
 * Luồng chính: saveImportTicket()
 *   BEGIN TRANSACTION
 *   1. INSERT PhieuNhap → lấy MaPN
 *   2. Loop cart items:
 *      a. Check SP tồn tại (TenSP + DVT)
 *      b. IF NEW: INSERT SanPham → lấy MaSP
 *      c. IF EXISTS: UPDATE GiaBan, GiaBanSi
 *      d. INSERT LoHang (MaSP, SoLo, HSD, SL, GiaNhap, MaPN)
 *   3. UPDATE PhieuNhap.TongTien = SUM(GiaNhap * SoLuong) từ LoHang
 *   COMMIT
 *
 * ⚠️ Tham khảo: SaleServiceImpl.checkout() — cùng pattern Transaction.
 */
public class NhapKhoDAO {

    // ================================================================
    //  PUBLIC: Lấy danh sách SP cho Auto-Suggest
    // ================================================================

    public List<Product> getAllActiveProducts() {
        String sql = "SELECT MaSP, TenSP, DonViTinh, GiaBan, TrangThai " +
                     "FROM SanPham WHERE TrangThai = 1 ORDER BY TenSP";
        List<Product> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Product p = new Product();
                p.setMaSP(rs.getInt("MaSP"));
                p.setTenSP(rs.getString("TenSP"));
                p.setDonViTinh(rs.getString("DonViTinh"));
                p.setGiaBan(rs.getBigDecimal("GiaBan"));

                p.setTrangThai(rs.getBoolean("TrangThai"));
                list.add(p);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy danh sách SP", e);
        }
        return list;
    }

    /** Lay danh sach NCC active cho ComboBox */
    public List<Supplier> getActiveSuppliers() {
        String sql = "SELECT MaNCC, TenNCC, SoDT, DiaChi, Email, TrangThai, NgayTao " +
                     "FROM NhaCungCap WHERE TrangThai = 1 ORDER BY TenNCC";
        List<Supplier> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Supplier s = new Supplier();
                s.setMaNCC(rs.getInt("MaNCC"));
                s.setTenNCC(rs.getNString("TenNCC"));
                s.setSoDT(rs.getString("SoDT"));
                s.setDiaChi(rs.getNString("DiaChi"));
                s.setEmail(rs.getString("Email"));
                s.setTrangThai(rs.getBoolean("TrangThai"));
                list.add(s);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Loi lay danh sach NCC", e);
        }
        return list;
    }

    // ================================================================
    //  PUBLIC: Lay danh sach lo hang theo MaSP (cho ProductDetailDialog)
    // ================================================================

    public List<domain.entity.Batch> getBatchesByMaSP(int maSP) {
        String sql = "SELECT l.MaLo, l.MaSP, l.SoLo, l.HanSuDung, l.SoLuong, l.GiaNhap, l.MaPN, " +
                     "ISNULL(n.TenNCC, N'---') AS TenNCC " +
                     "FROM LoHang l " +
                     "LEFT JOIN PhieuNhap p ON l.MaPN = p.MaPN " +
                     "LEFT JOIN NhaCungCap n ON p.MaNCC = n.MaNCC " +
                     "WHERE l.MaSP = ? ORDER BY l.HanSuDung ASC";
        List<domain.entity.Batch> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maSP);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    domain.entity.Batch b = new domain.entity.Batch();
                    b.setMaLo(rs.getInt("MaLo"));
                    b.setMaSP(rs.getInt("MaSP"));
                    b.setSoLo(rs.getNString("SoLo"));
                    b.setHanSuDung(rs.getDate("HanSuDung") != null
                            ? rs.getDate("HanSuDung").toLocalDate() : null);
                    b.setSoLuong(rs.getInt("SoLuong"));
                    b.setGiaNhap(rs.getBigDecimal("GiaNhap"));
                    b.setMaPN(rs.getInt("MaPN"));
                    b.setTenNCC(rs.getNString("TenNCC"));
                    list.add(b);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Loi lay danh sach lo hang", e);
        }
        return list;
    }

    /**
     * Check so lo da ton tai trong DB chua (LoHang)
     */
    public boolean existsSoLo(String soLo) {
        String sql = "SELECT COUNT(1) FROM LoHang WHERE SoLo = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, soLo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Loi check trung so lo", e);
        }
        return false;
    }

    // ================================================================
    //  PUBLIC: Lưu phiếu nhập — TRANSACTION
    // ================================================================

    /**
     * ★ Lưu phiếu nhập kho: 1 PhieuNhap + N LoHang (atomic)
     * @param cartItems danh sách dòng trong giỏ nhập
     * @return MaPN (mã phiếu nhập vừa tạo)
     */
    public int saveImportTicket(List<ImportCartItem> cartItems, int maNCC) {
        int maND = Session.getCurrentUser().getMaND();
        Connection conn = null;
        try {
            conn = DatabaseHelper.getConnection();
            conn.setAutoCommit(false);  // BEGIN TRANSACTION

            // Buoc 1: Tao PhieuNhap (voi MaNCC)
            int maPN = insertPhieuNhap(conn, maND, maNCC);

            // Bước 2: Xử lý từng dòng cart
            // ★ Cache MaSP đã resolve trong session (tránh INSERT trùng SP mới 2 lần)
            Map<String, Integer> resolvedSP = new LinkedHashMap<>();

            for (ImportCartItem item : cartItems) {
                String spKey = item.getTenSP().trim().toLowerCase()
                             + "|" + item.getDonViTinh().trim().toLowerCase();

                int maSP;
                if (resolvedSP.containsKey(spKey)) {
                    // Đã xử lý SP này trong lượt trước → dùng lại MaSP
                    maSP = resolvedSP.get(spKey);
                    // Vẫn update giá nếu cần
                    updateProductPrice(conn, maSP, item.getGiaBan());
                } else {
                    // Check SP tồn tại trong DB
                    Integer existingMaSP = findProductByNameAndUnit(conn,
                            item.getTenSP(), item.getDonViTinh());

                    if (existingMaSP != null) {
                        // SP đã có → UPDATE giá bán
                        maSP = existingMaSP;
                        updateProductPrice(conn, maSP, item.getGiaBan());
                    } else {
                        // SP mới → INSERT
                        maSP = insertProduct(conn, item.getTenSP(), item.getDonViTinh(),
                                item.getGiaBan());
                    }
                    resolvedSP.put(spKey, maSP);
                }

                // Bước 2d: INSERT LoHang
                insertBatch(conn, maSP, item.getSoLo(), item.getHanSuDung(),
                        item.getSoLuong(), item.getGiaNhap(), maPN);
            }

            // Bước 3: Cập nhật TongTien bằng SQL (chính xác từ dữ liệu DB)
            updatePhieuNhapTotal(conn, maPN);

            conn.commit();  // ★ COMMIT
            return maPN;

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw new RuntimeException("Lỗi lưu phiếu nhập: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    // ================================================================
    //  PRIVATE: SQL Operations (dùng Connection ngoài cho Transaction)
    // ================================================================

    private int insertPhieuNhap(Connection conn, int maND, int maNCC) throws SQLException {
        String sql = "INSERT INTO PhieuNhap (MaND, MaNCC, TongTien, GhiChu) VALUES (?, ?, 0, N'Phieu nhap tu he thong')";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, maND);
            ps.setInt(2, maNCC);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Khong lay duoc MaPN sau INSERT");
    }

    private Integer findProductByNameAndUnit(Connection conn, String tenSP, String dvt)
            throws SQLException {
        String sql = "SELECT MaSP FROM SanPham WHERE TenSP = ? AND DonViTinh = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, tenSP.trim());
            ps.setNString(2, dvt.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("MaSP");
            }
        }
        return null;
    }

    private int insertProduct(Connection conn, String tenSP, String dvt,
                              BigDecimal giaBan) throws SQLException {
        String sql = "INSERT INTO SanPham (TenSP, DonViTinh, GiaBan, TrangThai) " +
                     "VALUES (?, ?, ?, 1)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setNString(1, tenSP.trim());
            ps.setNString(2, dvt.trim());
            ps.setBigDecimal(3, giaBan);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Không lấy được MaSP sau INSERT");
    }

    private void updateProductPrice(Connection conn, int maSP,
                                    BigDecimal giaBan) throws SQLException {
        String sql = "UPDATE SanPham SET GiaBan = ? WHERE MaSP = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, giaBan);
            ps.setInt(2, maSP);
            ps.executeUpdate();
        }
    }

    private void insertBatch(Connection conn, int maSP, String soLo, LocalDate hsd,
                             int soLuong, BigDecimal giaNhap, int maPN) throws SQLException {
        String sql = "INSERT INTO LoHang (MaSP, SoLo, HanSuDung, SoLuong, GiaNhap, MaPN) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maSP);
            ps.setNString(2, soLo.trim());
            ps.setDate(3, Date.valueOf(hsd));
            ps.setInt(4, soLuong);
            ps.setBigDecimal(5, giaNhap);
            ps.setInt(6, maPN);
            ps.executeUpdate();
        }
    }

    /** ★ Tính TongTien bằng SQL — GiaNhap = tổng tiền dòng (KHÔNG nhân SL) */
    private void updatePhieuNhapTotal(Connection conn, int maPN) throws SQLException {
        String sql = "UPDATE PhieuNhap SET TongTien = " +
                     "(SELECT ISNULL(SUM(GiaNhap), 0) FROM LoHang WHERE MaPN = ?) " +
                     "WHERE MaPN = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maPN);
            ps.setInt(2, maPN);
            ps.executeUpdate();
        }
    }
}
