package infrastructure.repository;

import domain.dto.InventoryBatchDTO;
import domain.repository.IInventoryRepository;
import infrastructure.database.DatabaseHelper;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InventoryRepositoryImpl implements IInventoryRepository {

    @Override
    public List<InventoryBatchDTO> getPagedBatches(int offset, int pageSize, String keyword, String statusFilter, String sortCol, String sortDir) {
        StringBuilder where = new StringBuilder("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append("AND (sp.TenSP LIKE ? OR l.SoLo LIKE ? OR nd.HoTen LIKE ? OR ncc.TenNCC LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            params.add(like); params.add(like); params.add(like); params.add(like);
        }

        String fromClause =
                "FROM LoHang l " +
                "JOIN SanPham sp ON l.MaSP = sp.MaSP " +
                "LEFT JOIN PhieuNhap p ON l.MaPN = p.MaPN " +
                "LEFT JOIN NguoiDung nd ON p.MaND = nd.MaND " +
                "LEFT JOIN NhaCungCap ncc ON p.MaNCC = ncc.MaNCC ";

        String dataSql =
                "SELECT l.MaLo, l.SoLo, sp.TenSP, sp.DonViTinh, " +
                "       l.SoLuong, l.GiaNhap, l.HanSuDung, " +
                "       ISNULL(p.NgayNhap, l.NgayNhap) AS NgayNhap, " +
                "       ISNULL(nd.HoTen, N'---') AS NguoiNhap, " +
                "       ISNULL(ncc.TenNCC, N'---') AS TenNCC " +
                fromClause + where +
                "ORDER BY " + sortCol + " " + sortDir + " " +
                "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        List<InventoryBatchDTO> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(dataSql)) {
             
            int pi = 1;
            for (Object p : params) {
                ps.setNString(pi++, (String) p);
            }
            ps.setInt(pi++, offset);
            ps.setInt(pi, pageSize);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InventoryBatchDTO dto = new InventoryBatchDTO();
                    dto.setMaLo(rs.getInt("MaLo"));
                    dto.setSoLo(rs.getNString("SoLo"));
                    dto.setTenSP(rs.getNString("TenSP"));
                    dto.setDonViTinh(rs.getNString("DonViTinh"));
                    dto.setSoLuong(rs.getInt("SoLuong"));
                    dto.setGiaNhap(rs.getBigDecimal("GiaNhap"));
                    
                    Date hsd = rs.getDate("HanSuDung");
                    if (hsd != null) dto.setHanSuDung(hsd.toLocalDate());
                    
                    Timestamp nn = rs.getTimestamp("NgayNhap");
                    if (nn != null) dto.setNgayNhap(nn.toLocalDateTime());
                    
                    dto.setNguoiNhap(rs.getNString("NguoiNhap"));
                    dto.setTenNCC(rs.getNString("TenNCC"));

                    // Determine status
                    dto.setStatus(calcStatus(dto.getSoLuong(), dto.getHanSuDung(), LocalDate.now()));
                    
                    // Filter locally because status is calculated
                    if (statusFilter != null && !"Tất cả".equals(statusFilter)) {
                        if (!dto.getStatus().equals(statusFilter)) {
                            continue;
                        }
                    }

                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi phân trang lô hàng", e);
        }
        return list;
    }

    @Override
    public int countBatches(String keyword, String statusFilter) {
        StringBuilder where = new StringBuilder("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append("AND (sp.TenSP LIKE ? OR l.SoLo LIKE ? OR nd.HoTen LIKE ? OR ncc.TenNCC LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            params.add(like); params.add(like); params.add(like); params.add(like);
        }

        String countSql = 
            "SELECT l.SoLuong, l.HanSuDung " +
            "FROM LoHang l " +
            "JOIN SanPham sp ON l.MaSP = sp.MaSP " +
            "LEFT JOIN PhieuNhap p ON l.MaPN = p.MaPN " +
            "LEFT JOIN NguoiDung nd ON p.MaND = nd.MaND " +
            "LEFT JOIN NhaCungCap ncc ON p.MaNCC = ncc.MaNCC " +
            where;

        int total = 0;
        LocalDate today = LocalDate.now();
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(countSql)) {
             
            for (int i = 0; i < params.size(); i++) {
                ps.setNString(i + 1, (String) params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int sl = rs.getInt("SoLuong");
                    Date date = rs.getDate("HanSuDung");
                    LocalDate hsd = date != null ? date.toLocalDate() : null;
                    String status = calcStatus(sl, hsd, today);
                    
                    if (statusFilter != null && !"Tất cả".equals(statusFilter)) {
                        if (status.equals(statusFilter)) total++;
                    } else {
                        total++;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi đếm số lượng lô hàng", e);
        }
        return total;
    }

    @Override
    public boolean updateBatch(int maLo, BigDecimal giaNhap, LocalDate hanSuDung, int newSoLuong) {
        String sql = "UPDATE LoHang SET HanSuDung = ?, SoLuong = ?, GiaNhap = ? WHERE MaLo = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(hanSuDung));
            ps.setInt(2, newSoLuong);
            ps.setBigDecimal(3, giaNhap);
            ps.setInt(4, maLo);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật lô hàng", e);
        }
    }

    @Override
    public InventoryBatchDTO getBatchInfo(String soLo, String tenSP) {
        String sql = "SELECT l.MaLo, l.GiaNhap, ISNULL(ncc.TenNCC, N'---') AS TenNCC " +
                     "FROM LoHang l " +
                     "JOIN SanPham sp ON l.MaSP = sp.MaSP " +
                     "LEFT JOIN PhieuNhap p ON l.MaPN = p.MaPN " +
                     "LEFT JOIN NhaCungCap ncc ON p.MaNCC = ncc.MaNCC " +
                     "WHERE l.SoLo = ? AND sp.TenSP = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, soLo);
            ps.setNString(2, tenSP);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    InventoryBatchDTO dto = new InventoryBatchDTO();
                    dto.setMaLo(rs.getInt("MaLo"));
                    dto.setGiaNhap(rs.getBigDecimal("GiaNhap"));
                    dto.setTenNCC(rs.getNString("TenNCC"));
                    return dto;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public boolean isDuplicateBatch(String soLo, String tenSP, int excludeMaLo) {
        String sql = "SELECT COUNT(*) FROM LoHang l JOIN SanPham sp ON l.MaSP = sp.MaSP " +
                     "WHERE l.SoLo = ? AND sp.TenSP = ?" + 
                     (excludeMaLo > 0 ? " AND l.MaLo <> ?" : "");
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, soLo);
            ps.setNString(2, tenSP);
            if (excludeMaLo > 0) {
                ps.setInt(3, excludeMaLo);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public String checkForeignKeyConstraints(int maLo) {
        String checkAllFKs =
                "SELECT t.name AS TableName " +
                "FROM sys.foreign_key_columns fkc " +
                "JOIN sys.tables t ON fkc.parent_object_id = t.object_id " +
                "JOIN sys.columns c ON fkc.parent_object_id = c.object_id AND fkc.parent_column_id = c.column_id " +
                "WHERE fkc.referenced_object_id = OBJECT_ID('LoHang') " +
                "AND c.name = 'MaLo'";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkAllFKs);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String refTable = rs.getString("TableName");
                String countSql = "SELECT COUNT(*) FROM [" + refTable + "] WHERE MaLo = ?";
                try (PreparedStatement countPs = conn.prepareStatement(countSql)) {
                    countPs.setInt(1, maLo);
                    try (ResultSet countRs = countPs.executeQuery()) {
                        if (countRs.next() && countRs.getInt(1) > 0) {
                            return refTable;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kiểm tra ràng buộc lô hàng", e);
        }
        return null;
    }

    @Override
    public boolean deleteBatch(int maLo) {
        String sql = "DELETE FROM LoHang WHERE MaLo = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maLo);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi xóa lô hàng", e);
        }
    }

    @Override
    public List<Object[]> getBatchHistory(int maLo) {
        String sql =
            "SELECT N'Trả hàng NCC' AS Loai, t.SoLuongTra AS SoLuong, " +
            "t.TongTienHoan AS SoTien, t.HinhThucHoan AS PhanLoai, " +
            "t.LyDo, nd.HoTen AS NguoiThucHien, t.NgayTra AS Ngay " +
            "FROM TraHangNCC t " +
            "JOIN NguoiDung nd ON t.MaND = nd.MaND " +
            "WHERE t.MaLo = ? " +
            "UNION ALL " +
            "SELECT N'Hủy hàng', h.SoLuongHuy, h.TongThietHai, h.PhanLoaiLyDo, " +
            "h.ChiTietLyDo, nd.HoTen, h.NgayHuy " +
            "FROM HuyHang h " +
            "JOIN NguoiDung nd ON h.MaND = nd.MaND " +
            "WHERE h.MaLo = ? " +
            "ORDER BY Ngay DESC";

        List<Object[]> result = new ArrayList<>();
        java.text.DecimalFormat moneyFmt = new java.text.DecimalFormat("#,##0");

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maLo);
            ps.setInt(2, maLo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long soTien = rs.getLong("SoTien");
                    Timestamp ngay = rs.getTimestamp("Ngay");
                    String ngayStr = ngay != null
                            ? ngay.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                            : "---";
                    result.add(new Object[]{
                        rs.getNString("Loai"),
                        rs.getInt("SoLuong"),
                        moneyFmt.format(soTien) + " VNĐ",
                        rs.getNString("PhanLoai"),
                        rs.getNString("LyDo"),
                        rs.getNString("NguoiThucHien"),
                        ngayStr
                    });
                }
            }
        } catch (SQLException e) {
            // Tables might not exist yet
            System.err.println("Lỗi truy vấn lịch sử: " + e.getMessage());
        }
        return result;
    }

    @Override
    public List<Object[]> getReturnHistory() {
        String sql = "SELECT t.MaTra, l.SoLo, sp.TenSP, t.SoLuongTra, t.GiaNhapLo, " +
                  "t.TongTienHoan, t.HinhThucHoan, " +
                  "ISNULL(t.TinhTrang, N'---') AS TinhTrang, " +
                  "ISNULL(t.GhiChu, ISNULL(t.LyDo, N'')) AS GhiChu, " +
                  "nd.HoTen, t.NgayTra " +
                  "FROM TraHangNCC t " +
                  "JOIN LoHang l ON t.MaLo = l.MaLo " +
                  "JOIN SanPham sp ON t.MaSP = sp.MaSP " +
                  "JOIN NguoiDung nd ON t.MaND = nd.MaND " +
                  "ORDER BY t.NgayTra DESC";
        return executeHistoryQuery(sql, true);
    }

    @Override
    public List<Object[]> getDestroyHistory() {
        String sql = "SELECT h.MaHuy, l.SoLo, sp.TenSP, h.SoLuongHuy, h.GiaNhapLo, " +
                  "h.TongThietHai, h.PhanLoaiLyDo, h.ChiTietLyDo, nd.HoTen, h.NgayHuy " +
                  "FROM HuyHang h " +
                  "JOIN LoHang l ON h.MaLo = l.MaLo " +
                  "JOIN SanPham sp ON h.MaSP = sp.MaSP " +
                  "JOIN NguoiDung nd ON h.MaND = nd.MaND " +
                  "ORDER BY h.NgayHuy DESC";
        return executeHistoryQuery(sql, false);
    }

    private List<Object[]> executeHistoryQuery(String sql, boolean isReturn) {
        List<Object[]> result = new ArrayList<>();
        java.text.DecimalFormat fmt = new java.text.DecimalFormat("#,##0");
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp ngay = rs.getTimestamp(isReturn ? "NgayTra" : "NgayHuy");
                String ngayStr = ngay != null
                        ? ngay.toLocalDateTime().format(
                            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : "---";
                if (isReturn) {
                    result.add(new Object[]{
                        rs.getInt("MaTra"), rs.getNString("SoLo"), rs.getNString("TenSP"),
                        rs.getInt("SoLuongTra"), fmt.format(rs.getLong("GiaNhapLo")) + " VNĐ",
                        fmt.format(rs.getLong("TongTienHoan")) + " VNĐ", rs.getNString("HinhThucHoan"),
                        rs.getNString("TinhTrang"), rs.getNString("GhiChu"), rs.getNString("HoTen"), ngayStr
                    });
                } else {
                    result.add(new Object[]{
                        rs.getInt("MaHuy"), rs.getNString("SoLo"), rs.getNString("TenSP"),
                        rs.getInt("SoLuongHuy"), fmt.format(rs.getLong("GiaNhapLo")) + " VNĐ",
                        fmt.format(rs.getLong("TongThietHai")) + " VNĐ", rs.getNString("PhanLoaiLyDo"),
                        rs.getNString("ChiTietLyDo"), rs.getNString("HoTen"), ngayStr
                    });
                }
            }
        } catch (SQLException e) {
            // Error ignored for history queries where tables might not exist
        }
        return result;
    }

    @Override
    public int getOriginalQuantity(int maLo) {
        String sql = "SELECT (l.SoLuong " +
                     " + ISNULL((SELECT SUM(SoLuong) FROM ChiTietHoaDon WHERE MaLo = l.MaLo), 0)" +
                     " + ISNULL((SELECT SUM(SoLuongTra) FROM TraHangNCC WHERE MaLo = l.MaLo), 0)" +
                     " + ISNULL((SELECT SUM(SoLuongHuy) FROM HuyHang WHERE MaLo = l.MaLo), 0)" +
                     ") AS SoLuongGoc " +
                     "FROM LoHang l WHERE l.MaLo = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maLo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("SoLuongGoc");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi đếm số lượng gốc", e);
        }
        return -1;
    }

    private String calcStatus(int soLuong, LocalDate hsd, LocalDate today) {
        if (soLuong <= 0) return "Hết hàng";
        if (hsd == null) return "Bình thường";
        if (!hsd.isAfter(today)) return "Hết HSD";
        if (hsd.isBefore(today.plusMonths(4))) return "Cận Date";
        return "Bình thường";
    }

    @Override
    public int countHetHSD() {
        return countByCustomStatus("Hết HSD");
    }

    @Override
    public int countCanDate() {
        return countByCustomStatus("Cận Date");
    }

    @Override
    public int countHetHang() {
        return countByCustomStatus("Hết hàng");
    }
    
    private int countByCustomStatus(String targetStatus) {
        String sql = "SELECT SoLuong, HanSuDung FROM LoHang";
        int c = 0;
        LocalDate today = LocalDate.now();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int sl = rs.getInt("SoLuong");
                Date date = rs.getDate("HanSuDung");
                LocalDate hsd = date != null ? date.toLocalDate() : null;
                String st = calcStatus(sl, hsd, today);
                if (st.equals(targetStatus)) c++;
            }
            return c;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
