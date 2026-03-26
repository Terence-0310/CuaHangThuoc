package service.impl;

import domain.entity.Product;
import domain.repository.IProductRepository;
import service.IProductService;
import java.util.List;

/**
 * Service Impl: Quản lý sản phẩm (SRP + DIP)
 */
public class ProductServiceImpl implements IProductService {

    private final IProductRepository productRepo;

    public ProductServiceImpl(IProductRepository productRepo) {
        this.productRepo = productRepo;
    }

    @Override
    public List<Product> getAll() {
        return productRepo.getAll();
    }

    @Override
    public List<Product> getActive() {
        return productRepo.getActive();
    }

    @Override
    public List<Product> getAllWithStock() {
        return productRepo.getAllWithStock();
    }

    @Override
    public Product getById(int maSP) {
        return productRepo.getById(maSP);
    }

    @Override
    public List<Product> search(String keyword) {
        return productRepo.search(keyword);
    }

    @Override
    public boolean existsByNameAndUnit(String tenSP, String donViTinh) {
        return productRepo.existsByNameAndUnit(tenSP, donViTinh);
    }

    @Override
    public List<Product> getPagedWithStock(int offset, int pageSize, String keyword, String statusFilter,
                                            String sortColumn, String sortDirection) {
        return productRepo.getPagedWithStock(offset, pageSize, keyword, statusFilter, sortColumn, sortDirection);
    }

    @Override
    public int countFiltered(String keyword, String statusFilter) {
        return productRepo.countFiltered(keyword, statusFilter);
    }

    @Override
    public int insert(Product product) {
        validate(product);
        return productRepo.insert(product);
    }

    @Override
    public boolean update(Product product) {
        validate(product);
        return productRepo.update(product);
    }

    @Override
    public boolean softDelete(int maSP) {
        return productRepo.softDelete(maSP);
    }

    private void validate(Product p) {
        if (p.getTenSP() == null || p.getTenSP().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");
        }
        if (p.getDonViTinh() == null || p.getDonViTinh().trim().isEmpty()) {
            throw new IllegalArgumentException("Đơn vị tính không được để trống");
        }
        if (p.getGiaBan() == null || p.getGiaBan().signum() < 0) {
            throw new IllegalArgumentException("Giá bán phải >= 0");
        }
    }

    @Override
    public int bulkUpdateStatus(java.util.List<Integer> ids, boolean trangThai) {
        return productRepo.bulkUpdateStatus(ids, trangThai);
    }

    @Override
    public java.util.List<domain.entity.Batch> getBatchesByProduct(int maSP) {
        infrastructure.repository.NhapKhoDAO dao = new infrastructure.repository.NhapKhoDAO();
        return dao.getBatchesByMaSP(maSP);
    }

    @Override
    public java.util.List<Object[]> getSalesHistoryByProduct(int maSP) {
        String sql =
            "SELECT hd.MaHD, hd.NgayBan, l.SoLo, ct.SoLuong AS SLBan, " +
            "(ct.SoLuong * ct.DonGia) AS ThanhTien, " +
            "ISNULL(kh.TenKH, N'Khách vãng lai') AS TenKH, " +
            "ISNULL(kh.SoDT, N'---') AS SDT " +
            "FROM ChiTietHoaDon ct " +
            "JOIN HoaDon hd ON ct.MaHD = hd.MaHD " +
            "LEFT JOIN KhachHang kh ON hd.MaKH = kh.MaKH " +
            "JOIN LoHang l ON ct.MaLo = l.MaLo " +
            "WHERE ct.MaSP = ? " +
            "ORDER BY hd.NgayBan DESC";

        java.util.List<Object[]> result = new java.util.ArrayList<>();
        try (java.sql.Connection conn = infrastructure.database.DatabaseHelper.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maSP);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.sql.Timestamp ngay = rs.getTimestamp("NgayBan");
                    String ngayStr = ngay != null
                            ? ngay.toLocalDateTime().format(
                                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                            : "---";
                    java.math.BigDecimal thanhTien = rs.getBigDecimal("ThanhTien");
                    String thanhTienStr = thanhTien != null
                            ? String.format("%,.0f VNĐ", thanhTien) : "---";
                    result.add(new Object[]{
                        rs.getInt("MaHD"),
                        ngayStr,
                        rs.getNString("SoLo"),
                        rs.getInt("SLBan"),
                        thanhTienStr,
                        rs.getNString("TenKH"),
                        rs.getString("SDT")
                    });
                }
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Lỗi truy vấn lịch sử bán: " + e.getMessage(), e);
        }
        return result;
    }
}
