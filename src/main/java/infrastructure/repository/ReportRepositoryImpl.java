package infrastructure.repository;

import domain.dto.*;
import domain.entity.Batch;
import domain.repository.IReportRepository;
import infrastructure.database.DatabaseHelper;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository Impl: Báo cáo (ISP — tách riêng, chỉ lo query báo cáo)
 */
public class ReportRepositoryImpl implements IReportRepository {

    // ================================================================
    //  Legacy methods (giữ cho các module cũ)
    // ================================================================

    @Override
    public RevenueDTO getRevenue() {
        RevenueDTO dto = new RevenueDTO();
        try (Connection conn = DatabaseHelper.getConnection()) {
            dto.setToday(queryRevenue(conn, "TODAY"));
            dto.setMonth(queryRevenue(conn, "MONTH"));
            dto.setQuarter(queryRevenue(conn, "QUARTER"));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn doanh thu", e);
        }
        return dto;
    }

    private BigDecimal queryRevenue(Connection conn, String type) throws SQLException {
        String sql;
        switch (type) {
            case "TODAY":
                sql = "SELECT ISNULL(SUM(TongTien), 0) FROM HoaDon WHERE CAST(NgayBan AS DATE) = CAST(GETDATE() AS DATE)";
                break;
            case "MONTH":
                sql = "SELECT ISNULL(SUM(TongTien), 0) FROM HoaDon WHERE MONTH(NgayBan) = MONTH(GETDATE()) AND YEAR(NgayBan) = YEAR(GETDATE())";
                break;
            case "QUARTER":
                sql = "SELECT ISNULL(SUM(TongTien), 0) FROM HoaDon WHERE DATEPART(QUARTER, NgayBan) = DATEPART(QUARTER, GETDATE()) AND YEAR(NgayBan) = YEAR(GETDATE())";
                break;
            default:
                return BigDecimal.ZERO;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getBigDecimal(1);
        }
        return BigDecimal.ZERO;
    }

    @Override
    public List<Batch> getExpiringBatches() {
        String sql = "SELECT lh.*, sp.TenSP, sp.DonViTinh FROM LoHang lh " +
                     "JOIN SanPham sp ON lh.MaSP = sp.MaSP " +
                     "WHERE lh.SoLuong > 0 AND lh.HanSuDung <= DATEADD(MONTH, 3, GETDATE()) " +
                     "ORDER BY lh.HanSuDung ASC";
        List<Batch> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Batch b = new Batch();
                b.setMaLo(rs.getInt("MaLo"));
                b.setMaSP(rs.getInt("MaSP"));
                b.setSoLo(rs.getNString("SoLo"));
                b.setHanSuDung(rs.getDate("HanSuDung").toLocalDate());
                b.setSoLuong(rs.getInt("SoLuong"));
                b.setTenSP(rs.getNString("TenSP"));
                b.setDonViTinh(rs.getNString("DonViTinh"));
                list.add(b);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn cảnh báo hết hạn", e);
        }
        return list;
    }

    @Override
    public List<TopSellingDTO> getTopSelling(int topN) {
        String sql = "SELECT TOP (?) sp.TenSP, sp.DonViTinh, SUM(ct.SoLuong) AS TongSoLuongBan, " +
                     "SUM(ct.ThanhTien) AS TongDoanhThu FROM ChiTietHoaDon ct " +
                     "JOIN SanPham sp ON ct.MaSP = sp.MaSP " +
                     "GROUP BY sp.TenSP, sp.DonViTinh ORDER BY TongSoLuongBan DESC";
        List<TopSellingDTO> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, topN);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new TopSellingDTO(
                            rs.getNString("TenSP"),
                            rs.getNString("DonViTinh"),
                            rs.getInt("TongSoLuongBan"),
                            rs.getBigDecimal("TongDoanhThu")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn top bán chạy", e);
        }
        return list;
    }

    @Override
    public List<TopCustomerDTO> getTopCustomers(int topN) {
        String sql = "SELECT TOP (?) kh.TenKH, kh.SoDT, COUNT(DISTINCT hd.MaHD) AS SoLanMua, " +
                     "SUM(hd.TongTien) AS TongTienMua FROM HoaDon hd " +
                     "JOIN KhachHang kh ON hd.MaKH = kh.MaKH " +
                     "GROUP BY kh.MaKH, kh.TenKH, kh.SoDT ORDER BY TongTienMua DESC";
        List<TopCustomerDTO> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, topN);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new TopCustomerDTO(
                            rs.getNString("TenKH"),
                            rs.getString("SoDT"),
                            rs.getInt("SoLanMua"),
                            rs.getBigDecimal("TongTienMua")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn top khách VIP", e);
        }
        return list;
    }

    // ================================================================
    //  Mới: Báo cáo nâng cao theo năm/quý
    // ================================================================

    @Override
    public List<Integer> getAvailableYears() {
        String sql = "SELECT DISTINCT YEAR(NgayBan) AS Nam FROM HoaDon ORDER BY Nam DESC";
        List<Integer> years = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                years.add(rs.getInt("Nam"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn năm", e);
        }
        return years;
    }

    @Override
    public BusinessMetricDTO getKPIs(int year, int quarter) {
        String dateFilter = buildDateFilter(year, quarter);
        String sql = "SELECT " +
                     "ISNULL(SUM(CASE WHEN ISNULL(LoaiHD, 'SALE') = 'SALE' AND ISNULL(TrangThai, N'Thanh cong') = N'Thanh cong' THEN TongTien ELSE 0 END) " +
                     "     - SUM(CASE WHEN LoaiHD = 'RETURN' THEN ABS(TongTien) ELSE 0 END), 0) AS NetRevenue, " +
                     "COUNT(CASE WHEN ISNULL(LoaiHD, 'SALE') = 'SALE' AND ISNULL(TrangThai, N'Thanh cong') = N'Thanh cong' THEN 1 END) AS TotalSaleInvoices, " +
                     "(SELECT COUNT(*) FROM SanPham WHERE TrangThai = 1) AS TotalProducts " +
                     "FROM HoaDon WHERE " + dateFilter;
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new BusinessMetricDTO(
                        rs.getBigDecimal("NetRevenue"),
                        rs.getInt("TotalSaleInvoices"),
                        rs.getInt("TotalProducts")
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn KPI", e);
        }
        return new BusinessMetricDTO(BigDecimal.ZERO, 0, 0);
    }

    @Override
    public List<TopSellingDTO> getTopProducts(int year, int quarter) {
        String dateFilter = buildDateFilter(year, quarter);
        String sql = "SELECT TOP 10 sp.TenSP, sp.DonViTinh, SUM(ct.SoLuong) AS TongSoLuongBan, " +
                     "SUM(ct.ThanhTien) AS TongDoanhThu " +
                     "FROM ChiTietHoaDon ct " +
                     "JOIN SanPham sp ON ct.MaSP = sp.MaSP " +
                     "JOIN HoaDon hd ON ct.MaHD = hd.MaHD " +
                     "WHERE " + dateFilter + " AND ct.SoLuong > 0 " +
                     "GROUP BY sp.TenSP, sp.DonViTinh ORDER BY TongSoLuongBan DESC";
        List<TopSellingDTO> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new TopSellingDTO(
                        rs.getNString("TenSP"),
                        rs.getNString("DonViTinh"),
                        rs.getInt("TongSoLuongBan"),
                        rs.getBigDecimal("TongDoanhThu")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn top SP", e);
        }
        return list;
    }

    @Override
    public List<TopCustomerDTO> getTopCustomers(int year, int quarter) {
        String dateFilter = buildDateFilter(year, quarter);
        String sql = "SELECT TOP 10 kh.TenKH, kh.SoDT, COUNT(DISTINCT hd.MaHD) AS SoLanMua, " +
                     "SUM(hd.TongTien) AS TongTienMua " +
                     "FROM HoaDon hd " +
                     "JOIN KhachHang kh ON hd.MaKH = kh.MaKH " +
                     "WHERE " + dateFilter + " AND ISNULL(hd.LoaiHD, 'SALE') = 'SALE' " +
                     "GROUP BY kh.MaKH, kh.TenKH, kh.SoDT ORDER BY TongTienMua DESC";
        List<TopCustomerDTO> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new TopCustomerDTO(
                        rs.getNString("TenKH"),
                        rs.getString("SoDT"),
                        rs.getInt("SoLanMua"),
                        rs.getBigDecimal("TongTienMua")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn top KH theo quý", e);
        }
        return list;
    }

    @Override
    public List<TopSupplierDTO> getTopSuppliers(int year, int quarter) {
        String dateFilter = buildDateFilterImport(year, quarter);
        String sql = "SELECT TOP 10 ncc.TenNCC, ncc.SoDT, " +
                     "COUNT(DISTINCT pn.MaPN) AS SoPhieuNhap, " +
                     "ISNULL(SUM(pn.TongTien), 0) AS TongTienNhap " +
                     "FROM PhieuNhap pn " +
                     "JOIN NhaCungCap ncc ON pn.MaNCC = ncc.MaNCC " +
                     "WHERE " + dateFilter + " " +
                     "GROUP BY ncc.MaNCC, ncc.TenNCC, ncc.SoDT ORDER BY TongTienNhap DESC";
        List<TopSupplierDTO> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new TopSupplierDTO(
                        rs.getNString("TenNCC"),
                        rs.getString("SoDT"),
                        rs.getInt("SoPhieuNhap"),
                        rs.getBigDecimal("TongTienNhap")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn top NCC", e);
        }
        return list;
    }

    @Override
    public List<StockAlertDTO> getStockAlerts() {
        String sql = "SELECT sp.MaSP, sp.TenSP, " +
                     "ISNULL(SUM(l.SoLuong), 0) AS TongTon, " +
                     "COUNT(l.MaLo) AS SoLo, " +
                     "CASE " +
                     "  WHEN ISNULL(SUM(l.SoLuong), 0) = 0 THEN N'Hết hàng' " +
                     "  WHEN ISNULL(SUM(l.SoLuong), 0) <= 10 THEN N'Sắp hết' " +
                     "  WHEN EXISTS (SELECT 1 FROM LoHang l2 WHERE l2.MaSP = sp.MaSP AND l2.SoLuong > 0 AND l2.HanSuDung <= DATEADD(MONTH, 1, GETDATE())) THEN N'Sắp hết hạn' " +
                     "  ELSE N'Bình thường' " +
                     "END AS TrangThai " +
                     "FROM SanPham sp " +
                     "LEFT JOIN LoHang l ON sp.MaSP = l.MaSP AND l.SoLuong > 0 " +
                     "WHERE sp.TrangThai = 1 " +
                     "GROUP BY sp.MaSP, sp.TenSP " +
                     "HAVING ISNULL(SUM(l.SoLuong), 0) <= 10 " +
                     "    OR EXISTS (SELECT 1 FROM LoHang l2 WHERE l2.MaSP = sp.MaSP AND l2.SoLuong > 0 AND l2.HanSuDung <= DATEADD(MONTH, 1, GETDATE())) " +
                     "ORDER BY TongTon ASC";
        List<StockAlertDTO> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new StockAlertDTO(
                        rs.getInt("MaSP"),
                        rs.getNString("TenSP"),
                        rs.getInt("TongTon"),
                        rs.getInt("SoLo"),
                        rs.getNString("TrangThai")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn cảnh báo tồn kho", e);
        }
        return list;
    }

    // ================================================================
    //  Private helpers
    // ================================================================

    /** Build date filter for HoaDon: YEAR(hd.NgayBan) = year AND (quarter=0 OR DATEPART(QUARTER,...)=quarter) */
    private String buildDateFilter(int year, int quarter) {
        if (quarter <= 0 || quarter > 4) {
            return "YEAR(hd.NgayBan) = " + year;
        }
        return "YEAR(hd.NgayBan) = " + year + " AND DATEPART(QUARTER, hd.NgayBan) = " + quarter;
    }

    /** Build date filter for PhieuNhap */
    private String buildDateFilterImport(int year, int quarter) {
        if (quarter <= 0 || quarter > 4) {
            return "YEAR(pn.NgayNhap) = " + year;
        }
        return "YEAR(pn.NgayNhap) = " + year + " AND DATEPART(QUARTER, pn.NgayNhap) = " + quarter;
    }
}
