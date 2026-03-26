package infrastructure.repository;

import domain.dto.InvoiceDetailDTO;
import domain.dto.InvoiceFilterCriteria;
import domain.dto.InvoiceListDTO;
import domain.entity.Invoice;
import domain.repository.IInvoiceRepository;
import infrastructure.database.DatabaseHelper;

import java.math.BigDecimal;
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

    // ================================================================
    //  Phân trang & Filter hóa đơn
    // ================================================================

    @Override
    public List<InvoiceListDTO> searchInvoices(InvoiceFilterCriteria criteria, int page, int pageSize) {
        Object[] built = buildFilterQuery(criteria, false);
        String sql = (String) built[0];
        @SuppressWarnings("unchecked")
        List<Object> params = (List<Object>) built[1];

        // Sorting & paging
        sql += " ORDER BY hd.NgayBan DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        int offset = (page - 1) * pageSize;
        params.add(offset);
        params.add(pageSize);

        List<InvoiceListDTO> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new InvoiceListDTO(
                            rs.getInt("MaHD"),
                            rs.getTimestamp("NgayBan").toLocalDateTime(),
                            rs.getNString("TenKH"),
                            rs.getString("SoDT"),
                            rs.getNString("TenNV"),
                            rs.getNString("PhuongThucTT"),
                            rs.getBigDecimal("TongTien"),
                            rs.getNString("LoaiHD"),
                            rs.getNString("TrangThai")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm HoaDon", e);
        }
        return list;
    }

    @Override
    public int countInvoices(InvoiceFilterCriteria criteria) {
        Object[] built = buildFilterQuery(criteria, true);
        String sql = (String) built[0];
        @SuppressWarnings("unchecked")
        List<Object> params = (List<Object>) built[1];

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi đếm HoaDon", e);
        }
        return 0;
    }

    @Override
    public void voidInvoice(int maHD, String reason) throws SQLException {
        String sql = "UPDATE HoaDon SET TrangThai = N'Da huy', LyDoHuy = ? WHERE MaHD = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, reason);
            ps.setInt(2, maHD);
            ps.executeUpdate();
        }
    }

    @Override
    public Invoice getInvoiceHeader(int maHD) {
        String sql = "SELECT hd.*, kh.TenKH, kh.SoDT, nd.HoTen AS TenNhanVien FROM HoaDon hd " +
                     "LEFT JOIN KhachHang kh ON hd.MaKH = kh.MaKH " +
                     "JOIN NguoiDung nd ON hd.MaND = nd.MaND " +
                     "WHERE hd.MaHD = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maHD);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn HoaDon header", e);
        }
        return null;
    }

    @Override
    public List<InvoiceDetailDTO> getInvoiceDetails(int maHD) {
        String sql = "SELECT sp.TenSP, l.SoLo, ct.SoLuong, ct.DonGia, ct.ThanhTien " +
                     "FROM ChiTietHoaDon ct " +
                     "JOIN SanPham sp ON ct.MaSP = sp.MaSP " +
                     "JOIN LoHang l ON ct.MaLo = l.MaLo " +
                     "WHERE ct.MaHD = ? ORDER BY sp.TenSP";
        List<InvoiceDetailDTO> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maHD);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new InvoiceDetailDTO(
                            rs.getNString("TenSP"),
                            rs.getNString("SoLo"),
                            rs.getInt("SoLuong"),
                            rs.getBigDecimal("DonGia"),
                            rs.getBigDecimal("ThanhTien")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn chi tiết HoaDon", e);
        }
        return list;
    }

    // ================================================================
    //  Private helpers
    // ================================================================

    private Object[] buildFilterQuery(InvoiceFilterCriteria c, boolean countOnly) {
        List<Object> params = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        if (countOnly) {
            sb.append("SELECT COUNT(1) ");
        } else {
            sb.append("SELECT hd.MaHD, hd.NgayBan, ISNULL(kh.TenKH, N'Khách vãng lai') AS TenKH, ")
              .append("kh.SoDT, nd.HoTen AS TenNV, hd.PhuongThucTT, hd.TongTien, ")
              .append("ISNULL(hd.LoaiHD, N'SALE') AS LoaiHD, ")
              .append("ISNULL(hd.TrangThai, N'Thanh cong') AS TrangThai ");
        }
        sb.append("FROM HoaDon hd ")
          .append("LEFT JOIN KhachHang kh ON hd.MaKH = kh.MaKH ")
          .append("JOIN NguoiDung nd ON hd.MaND = nd.MaND ")
          .append("WHERE 1=1 ");

        if (c != null) {
            // Date range
            if (c.getFromDate() != null) {
                sb.append("AND CAST(hd.NgayBan AS DATE) >= ? ");
                params.add(Date.valueOf(c.getFromDate()));
            }
            if (c.getToDate() != null) {
                sb.append("AND CAST(hd.NgayBan AS DATE) <= ? ");
                params.add(Date.valueOf(c.getToDate()));
            }
            // Time range
            if (c.getFromTime() != null && !c.getFromTime().isEmpty()) {
                sb.append("AND FORMAT(hd.NgayBan, 'HH:mm') >= ? ");
                params.add(c.getFromTime());
            }
            if (c.getToTime() != null && !c.getToTime().isEmpty()) {
                sb.append("AND FORMAT(hd.NgayBan, 'HH:mm') <= ? ");
                params.add(c.getToTime());
            }
            // Search KH
            if (c.getSearchKH() != null && !c.getSearchKH().trim().isEmpty()) {
                sb.append("AND kh.TenKH LIKE ? ");
                params.add("%" + c.getSearchKH().trim() + "%");
            }
            // Search MaHD
            if (c.getSearchMaHD() != null && !c.getSearchMaHD().trim().isEmpty()) {
                sb.append("AND CAST(hd.MaHD AS NVARCHAR) LIKE ? ");
                params.add("%" + c.getSearchMaHD().trim() + "%");
            }
            // Search SP
            if (c.getSearchSP() != null && !c.getSearchSP().trim().isEmpty()) {
                sb.append("AND EXISTS (SELECT 1 FROM ChiTietHoaDon ct2 JOIN SanPham sp2 ON ct2.MaSP = sp2.MaSP WHERE ct2.MaHD = hd.MaHD AND sp2.TenSP LIKE ?) ");
                params.add("%" + c.getSearchSP().trim() + "%");
            }
            // Status filter
            if (c.getStatusFilter() == 1) {
                sb.append("AND ISNULL(hd.TrangThai, N'Thanh cong') = N'Thanh cong' ");
            } else if (c.getStatusFilter() == 2) {
                sb.append("AND hd.TrangThai = N'Da huy' ");
            }
            // Customer type filter
            if (c.getCustomerTypeFilter() == 1) {
                sb.append("AND hd.MaKH IS NOT NULL ");
            } else if (c.getCustomerTypeFilter() == 2) {
                sb.append("AND hd.MaKH IS NULL ");
            }
            // Invoice type filter
            if (c.getInvoiceTypeFilter() == 1) {
                sb.append("AND ISNULL(hd.LoaiHD, N'SALE') = N'SALE' ");
            } else if (c.getInvoiceTypeFilter() == 2) {
                sb.append("AND hd.LoaiHD = N'RETURN' ");
            }
        }

        return new Object[]{sb.toString(), params};
    }

    private void setParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object p = params.get(i);
            if (p instanceof Integer) {
                ps.setInt(i + 1, (Integer) p);
            } else if (p instanceof String) {
                ps.setNString(i + 1, (String) p);
            } else if (p instanceof Date) {
                ps.setDate(i + 1, (Date) p);
            } else if (p instanceof BigDecimal) {
                ps.setBigDecimal(i + 1, (BigDecimal) p);
            } else {
                ps.setObject(i + 1, p);
            }
        }
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
        try { inv.setTrangThai(rs.getNString("TrangThai")); } catch (SQLException ignored) {}
        try { inv.setLyDoHuy(rs.getNString("LyDoHuy")); } catch (SQLException ignored) {}
        try { inv.setTenNhanVien(rs.getNString("TenNhanVien")); } catch (SQLException ignored) {}
        try { inv.setTenKH(rs.getNString("TenKH")); } catch (SQLException ignored) {}
        try { inv.setSoDT(rs.getString("SoDT")); } catch (SQLException ignored) {}
        return inv;
    }
}
