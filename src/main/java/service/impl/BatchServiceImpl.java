package service.impl;

import domain.entity.Batch;
import domain.repository.IBatchRepository;
import domain.repository.IProductRepository;
import service.IBatchService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service Impl: Nhập kho / Quản lý lô (SRP + DIP)
 * ★ Mỗi lần nhập = 1 dòng LoHang mới (thuộc 1 PhieuNhap)
 *   FEFO tự biết cách trừ dần, không lo trùng lô
 */
public class BatchServiceImpl implements IBatchService {

    private final IBatchRepository batchRepo;
    private final IProductRepository productRepo;

    public BatchServiceImpl(IBatchRepository batchRepo, IProductRepository productRepo) {
        this.batchRepo = batchRepo;
        this.productRepo = productRepo;
    }

    @Override
    public int importBatch(Batch batch) {
        // === VALIDATE ===
        if (batch.getMaSP() <= 0) {
            throw new IllegalArgumentException("Vui lòng chọn sản phẩm");
        }
        if (productRepo.getById(batch.getMaSP()) == null) {
            throw new IllegalArgumentException("Sản phẩm không tồn tại");
        }
        if (batch.getSoLo() == null || batch.getSoLo().trim().isEmpty()) {
            throw new IllegalArgumentException("Số lô không được để trống");
        }
        if (batch.getHanSuDung() == null || !batch.getHanSuDung().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Hạn sử dụng phải sau ngày hôm nay");
        }
        if (batch.getSoLuong() <= 0) {
            throw new IllegalArgumentException("Số lượng phải > 0");
        }
        if (batch.getGiaNhap() == null || batch.getGiaNhap().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá nhập phải >= 0");
        }

        // ★ Luôn INSERT mới — mỗi lần nhập = 1 dòng riêng (thuộc PhieuNhap khác nhau)
        return batchRepo.insert(batch);
    }

    @Override
    public List<Batch> getByProductId(int maSP) {
        return batchRepo.getByProductId(maSP);
    }

    @Override
    public List<Batch> getAllWithProduct() {
        return batchRepo.getAllWithProduct();
    }

    // ================================================================
    //  Trả hàng NCC (Transaction)
    // ================================================================
    @Override
    public void returnBatch(int maLo, int maSP, int soLuong, long giaNhapLo,
                            long tongTien, String hinhThucHoan, String tinhTrang, String ghiChu)
            throws java.sql.SQLException {
        java.sql.Connection conn = null;
        try {
            conn = infrastructure.database.DatabaseHelper.getConnection();
            conn.setAutoCommit(false);

            verifyAndDeductStock(conn, maLo, soLuong);

            int maND = common.Session.getCurrentUser().getMaND();
            ensureTable(conn, "TraHangNCC",
                "CREATE TABLE TraHangNCC (MaTra INT IDENTITY(1,1) PRIMARY KEY, " +
                "MaLo INT NOT NULL, MaSP INT NOT NULL, SoLuongTra INT NOT NULL, " +
                "GiaNhapLo DECIMAL(18,0) DEFAULT 0, TongTienHoan DECIMAL(18,0) DEFAULT 0, " +
                "HinhThucHoan NVARCHAR(50) DEFAULT N'Tiền mặt', " +
                "TinhTrang NVARCHAR(100) DEFAULT N'', " +
                "GhiChu NVARCHAR(500) DEFAULT N'', " +
                "MaND INT NOT NULL, NgayTra DATETIME DEFAULT GETDATE())");

            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO TraHangNCC (MaLo,MaSP,SoLuongTra,GiaNhapLo,TongTienHoan,HinhThucHoan,TinhTrang,GhiChu,LyDo,MaND) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?)")) {
                ps.setInt(1, maLo); ps.setInt(2, maSP); ps.setInt(3, soLuong);
                ps.setLong(4, giaNhapLo); ps.setLong(5, tongTien);
                ps.setNString(6, hinhThucHoan);
                ps.setNString(7, tinhTrang != null ? tinhTrang : "");
                ps.setNString(8, ghiChu);
                ps.setNString(9, ghiChu);
                ps.setInt(10, maND);
                ps.executeUpdate();
            }

            String doiTuong = "MaLo=" + maLo + " - MaSP=" + maSP;
            String chiTiet = "SL=" + soLuong + ", Tiền=" + tongTien + " VNĐ, " + hinhThucHoan;
            common.SystemLogger.logInTransaction(conn, "TRA_HANG", doiTuong, chiTiet);

            conn.commit();
        } catch (java.sql.SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (java.sql.SQLException ignored) {}
            throw e;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (java.sql.SQLException ignored) {}
        }
    }

    // ================================================================
    //  Hủy hàng (Transaction)
    // ================================================================
    @Override
    public void destroyBatch(int maLo, int maSP, int soLuong, long giaNhapLo,
                             long tongTien, String phanLoaiLyDo, String chiTietLyDo)
            throws java.sql.SQLException {
        java.sql.Connection conn = null;
        try {
            conn = infrastructure.database.DatabaseHelper.getConnection();
            conn.setAutoCommit(false);

            verifyAndDeductStock(conn, maLo, soLuong);

            int maND = common.Session.getCurrentUser().getMaND();
            ensureTable(conn, "HuyHang",
                "CREATE TABLE HuyHang (MaHuy INT IDENTITY(1,1) PRIMARY KEY, " +
                "MaLo INT NOT NULL, MaSP INT NOT NULL, SoLuongHuy INT NOT NULL, " +
                "GiaNhapLo DECIMAL(18,0) DEFAULT 0, TongThietHai DECIMAL(18,0) DEFAULT 0, " +
                "PhanLoaiLyDo NVARCHAR(100) DEFAULT N'Hết hạn sử dụng', ChiTietLyDo NVARCHAR(500) NOT NULL, " +
                "MaND INT NOT NULL, NgayHuy DATETIME DEFAULT GETDATE())");

            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO HuyHang (MaLo,MaSP,SoLuongHuy,GiaNhapLo,TongThietHai,PhanLoaiLyDo,ChiTietLyDo,MaND) " +
                    "VALUES (?,?,?,?,?,?,?,?)")) {
                ps.setInt(1, maLo); ps.setInt(2, maSP); ps.setInt(3, soLuong);
                ps.setLong(4, giaNhapLo); ps.setLong(5, tongTien);
                ps.setNString(6, phanLoaiLyDo); ps.setNString(7, chiTietLyDo); ps.setInt(8, maND);
                ps.executeUpdate();
            }

            String doiTuong = "MaLo=" + maLo + " - MaSP=" + maSP;
            String chiTiet = "SL=" + soLuong + ", Tiền=" + tongTien + " VNĐ, " + phanLoaiLyDo;
            common.SystemLogger.logInTransaction(conn, "HUY_HANG", doiTuong, chiTiet);

            conn.commit();
        } catch (java.sql.SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (java.sql.SQLException ignored) {}
            throw e;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (java.sql.SQLException ignored) {}
        }
    }

    // ================================================================
    //  Lịch sử bán theo lô (cho Product Recall)
    // ================================================================
    @Override
    public List<Object[]> getSalesHistoryByBatch(int maLo) {
        String sql =
            "SELECT hd.MaHD, hd.NgayBan, ct.SoLuong AS SLBan, " +
            "ISNULL(kh.HoTen, N'Khách vãng lai') AS TenKH, " +
            "ISNULL(kh.SoDienThoai, N'---') AS SDT, " +
            "ISNULL(kh.DiaChi, N'---') AS DiaChi " +
            "FROM ChiTietHoaDon ct " +
            "JOIN HoaDon hd ON ct.MaHD = hd.MaHD " +
            "LEFT JOIN KhachHang kh ON hd.MaKH = kh.MaKH " +
            "WHERE ct.MaLo = ? " +
            "ORDER BY hd.NgayBan DESC";

        List<Object[]> result = new java.util.ArrayList<>();
        try (java.sql.Connection conn = infrastructure.database.DatabaseHelper.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maLo);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.sql.Timestamp ngay = rs.getTimestamp("NgayBan");
                    String ngayStr = ngay != null
                            ? ngay.toLocalDateTime().format(
                                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                            : "---";
                    result.add(new Object[]{
                        rs.getInt("MaHD"),
                        ngayStr,
                        rs.getInt("SLBan"),
                        rs.getNString("TenKH"),
                        rs.getNString("SDT"),
                        rs.getNString("DiaChi")
                    });
                }
            }
        } catch (java.sql.SQLException ignored) {}
        return result;
    }

    // ================================================================
    //  Private helpers (Transaction support)
    // ================================================================
    private void verifyAndDeductStock(java.sql.Connection conn, int maLo, int soLuong) throws java.sql.SQLException {
        int currentStock;
        try (java.sql.PreparedStatement ps = conn.prepareStatement(
                "SELECT SoLuong FROM LoHang WHERE MaLo = ?")) {
            ps.setInt(1, maLo);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new java.sql.SQLException("Lô hàng không tồn tại!");
                currentStock = rs.getInt("SoLuong");
            }
        }
        if (soLuong > currentStock) {
            throw new java.sql.SQLException("Tồn kho đã thay đổi! Hiện còn " + currentStock);
        }
        try (java.sql.PreparedStatement ps = conn.prepareStatement(
                "UPDATE LoHang SET SoLuong = SoLuong - ? WHERE MaLo = ? AND SoLuong >= ?")) {
            ps.setInt(1, soLuong); ps.setInt(2, maLo); ps.setInt(3, soLuong);
            if (ps.executeUpdate() == 0) throw new java.sql.SQLException("Không thể trừ tồn kho!");
        }
    }

    private void ensureTable(java.sql.Connection conn, String name, String ddl) {
        try (java.sql.Statement s = conn.createStatement()) {
            s.execute("IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name='" + name + "') " + ddl);
        } catch (java.sql.SQLException ignored) {}
        ensureColumn(conn, name, "GiaNhapLo", "DECIMAL(18,0) DEFAULT 0");
        ensureColumn(conn, name, "TongTienHoan", "DECIMAL(18,0) DEFAULT 0");
        ensureColumn(conn, name, "TongThietHai", "DECIMAL(18,0) DEFAULT 0");
        ensureColumn(conn, name, "HinhThucHoan", "NVARCHAR(50) DEFAULT N'Tiền mặt'");
        ensureColumn(conn, name, "TinhTrang", "NVARCHAR(100) DEFAULT N''");
        ensureColumn(conn, name, "GhiChu", "NVARCHAR(500) DEFAULT N''");
        ensureColumn(conn, name, "PhanLoaiLyDo", "NVARCHAR(100) DEFAULT N'Hết hạn sử dụng'");
        ensureColumn(conn, name, "ChiTietLyDo", "NVARCHAR(500) DEFAULT N''");
        allowNullColumn(conn, name, "LyDo", "NVARCHAR(500)");
        allowNullColumn(conn, name, "ChiTietLyDo", "NVARCHAR(500)");
    }

    private void ensureColumn(java.sql.Connection conn, String table, String col, String colDef) {
        try (java.sql.Statement s = conn.createStatement()) {
            java.sql.ResultSet rs = s.executeQuery(
                "SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('" + table + "') AND name = '" + col + "'");
            if (!rs.next()) {
                s.execute("ALTER TABLE " + table + " ADD " + col + " " + colDef);
            }
        } catch (java.sql.SQLException ignored) {}
    }

    private void allowNullColumn(java.sql.Connection conn, String table, String col, String colType) {
        try (java.sql.Statement s = conn.createStatement()) {
            java.sql.ResultSet rs = s.executeQuery(
                "SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('" + table + "') AND name = '" + col + "'");
            if (rs.next()) {
                s.execute("ALTER TABLE " + table + " ALTER COLUMN " + col + " " + colType + " NULL");
            }
        } catch (java.sql.SQLException ignored) {}
    }
}
