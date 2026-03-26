package service.impl;

import domain.entity.Supplier;
import domain.repository.ISupplierRepository;
import service.ISupplierService;
import java.util.List;

/**
 * Service Impl: Nhà cung cấp (SRP + DIP)
 */
public class SupplierServiceImpl implements ISupplierService {

    private final ISupplierRepository supplierRepo;

    public SupplierServiceImpl(ISupplierRepository supplierRepo) {
        this.supplierRepo = supplierRepo;
    }

    @Override
    public int add(Supplier supplier) {
        if (supplier.getTenNCC() == null || supplier.getTenNCC().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên NCC không được để trống");
        }
        return supplierRepo.insert(supplier);
    }

    @Override
    public boolean update(Supplier supplier) {
        if (supplier.getTenNCC() == null || supplier.getTenNCC().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên NCC không được để trống");
        }
        return supplierRepo.update(supplier);
    }

    @Override
    public boolean delete(int maNCC) {
        return supplierRepo.softDelete(maNCC);
    }

    @Override
    public Supplier getById(int maNCC) {
        return supplierRepo.getById(maNCC);
    }

    @Override
    public List<Supplier> getAll() {
        return supplierRepo.getAll();
    }

    @Override
    public List<Supplier> getActive() {
        return supplierRepo.getActive();
    }

    @Override
    public List<Supplier> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getActive();
        }
        return supplierRepo.search(keyword.trim());
    }

    @Override
    public List<Supplier> getPagedList(int offset, int pageSize, String keyword, String statusFilter, String sortCol, String sortDir) {
        return supplierRepo.getPagedList(offset, pageSize, keyword, statusFilter, sortCol, sortDir);
    }

    @Override
    public int countFiltered(String keyword, String statusFilter) {
        return supplierRepo.countFiltered(keyword, statusFilter);
    }

    // ================================================================
    //  Clean Arch: Data access cho SupplierDetailDialog
    // ================================================================

    @Override
    public java.util.List<Object[]> getSupplierDetail(int maNCC) {
        String sql = "SELECT MaNCC, TenNCC, SoDT, DiaChi, Email, TrangThai, NgayTao, " +
                "(SELECT COUNT(*) FROM PhieuNhap WHERE MaNCC = n.MaNCC) AS SoPhieuNhap, " +
                "(SELECT ISNULL(SUM(l.SoLuong), 0) FROM LoHang l JOIN PhieuNhap p ON l.MaPN = p.MaPN WHERE p.MaNCC = n.MaNCC) AS TongSoLuong " +
                "FROM NhaCungCap n WHERE MaNCC = ?";
        java.util.List<Object[]> result = new java.util.ArrayList<>();
        try (java.sql.Connection conn = infrastructure.database.DatabaseHelper.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maNCC);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result.add(new Object[]{
                        rs.getInt("MaNCC"), rs.getNString("TenNCC"),
                        rs.getString("SoDT"), rs.getNString("DiaChi"), rs.getString("Email"),
                        rs.getBoolean("TrangThai"), rs.getInt("SoPhieuNhap"), rs.getInt("TongSoLuong")
                    });
                }
            }
        } catch (java.sql.SQLException e) { e.printStackTrace(); }
        return result;
    }

    @Override
    public java.util.List<Object[]> getImportTickets(int maNCC) {
        String sql = "SELECT p.MaPN, p.NgayNhap, nd.HoTen, p.TongTien, p.GhiChu " +
                "FROM PhieuNhap p " +
                "LEFT JOIN NguoiDung nd ON p.MaND = nd.MaND " +
                "WHERE p.MaNCC = ? " +
                "ORDER BY p.NgayNhap DESC";
        java.util.List<Object[]> result = new java.util.ArrayList<>();
        try (java.sql.Connection conn = infrastructure.database.DatabaseHelper.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maNCC);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.time.LocalDateTime ngayNhap = null;
                    java.sql.Timestamp ts = rs.getTimestamp("NgayNhap");
                    if (ts != null) ngayNhap = ts.toLocalDateTime();
                    java.math.BigDecimal tongTien = rs.getBigDecimal("TongTien");
                    if (tongTien == null) tongTien = java.math.BigDecimal.ZERO;
                    result.add(new Object[]{
                        rs.getInt("MaPN"),
                        "PN-" + rs.getInt("MaPN"),
                        ngayNhap,
                        rs.getNString("HoTen") != null ? rs.getNString("HoTen") : "---",
                        tongTien,
                        rs.getNString("GhiChu") != null ? rs.getNString("GhiChu") : ""
                    });
                }
            }
        } catch (java.sql.SQLException e) { e.printStackTrace(); }
        return result;
    }

    @Override
    public java.util.List<Object[]> getSupplierBatches(int maNCC) {
        String sql = "SELECT l.SoLo, sp.TenSP, sp.DonViTinh, l.SoLuong, l.GiaNhap, l.HanSuDung, l.MaPN " +
                "FROM LoHang l " +
                "JOIN SanPham sp ON l.MaSP = sp.MaSP " +
                "JOIN PhieuNhap p ON l.MaPN = p.MaPN " +
                "WHERE p.MaNCC = ? " +
                "ORDER BY p.NgayNhap DESC, l.MaLo ASC";
        java.util.List<Object[]> result = new java.util.ArrayList<>();
        try (java.sql.Connection conn = infrastructure.database.DatabaseHelper.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maNCC);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.time.LocalDate hsd = null;
                    java.sql.Date hsdDate = rs.getDate("HanSuDung");
                    if (hsdDate != null) hsd = hsdDate.toLocalDate();
                    java.math.BigDecimal giaNhap = rs.getBigDecimal("GiaNhap");
                    if (giaNhap == null) giaNhap = java.math.BigDecimal.ZERO;
                    result.add(new Object[]{
                        rs.getNString("SoLo"), rs.getNString("TenSP"), rs.getNString("DonViTinh"),
                        rs.getInt("SoLuong"), giaNhap, hsd, "PN-" + rs.getInt("MaPN")
                    });
                }
            }
        } catch (java.sql.SQLException e) { e.printStackTrace(); }
        return result;
    }
}
