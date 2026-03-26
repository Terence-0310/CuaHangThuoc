package service.impl;

import domain.entity.Invoice;
import domain.entity.InvoiceDetail;
import domain.repository.IInvoiceDetailRepository;
import domain.repository.IInvoiceRepository;
import service.IInvoiceService;
import java.util.List;
import domain.dto.InvoiceFilterCriteria;
import domain.dto.InvoiceListDTO;
import domain.dto.InvoiceDetailDTO;
import domain.dto.PagedResult;

/**
 * Service Impl: Xem hóa đơn & lịch sử (ISP — tách khỏi SaleService)
 */
public class InvoiceServiceImpl implements IInvoiceService {

    private final IInvoiceRepository invoiceRepo;
    private final IInvoiceDetailRepository detailRepo;

    public InvoiceServiceImpl(IInvoiceRepository invoiceRepo, IInvoiceDetailRepository detailRepo) {
        this.invoiceRepo = invoiceRepo;
        this.detailRepo = detailRepo;
    }

    @Override
    public List<Invoice> getByCustomerId(int maKH) {
        return invoiceRepo.getByCustomerId(maKH);
    }

    @Override
    public List<InvoiceDetail> getDetails(int maHD) {
        return detailRepo.getByInvoiceId(maHD);
    }

    @Override
    public List<Invoice> getAll() {
        return invoiceRepo.getAll();
    }

    @Override
    public PagedResult<InvoiceListDTO> searchInvoices(InvoiceFilterCriteria criteria, int page, int pageSize) {
        List<InvoiceListDTO> data = invoiceRepo.searchInvoices(criteria, page, pageSize);
        int totalRecords = invoiceRepo.countInvoices(criteria);
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRecords / pageSize));
        return new PagedResult<>(data, totalPages);
    }

    @Override
    public void voidInvoice(int maHD, String reason) throws java.sql.SQLException {
        invoiceRepo.voidInvoice(maHD, reason);
    }

    @Override
    public Invoice getInvoiceHeader(int maHD) {
        return invoiceRepo.getInvoiceHeader(maHD);
    }

    @Override
    public List<InvoiceDetailDTO> getInvoiceDetails(int maHD) {
        return invoiceRepo.getInvoiceDetails(maHD);
    }

    @Override
    public java.util.List<Object[]> getReturnBatchData(int maHDGoc) {
        String sql =
            "SELECT ct.MaLo, ct.MaSP, ct.SoLuong AS SoLuongMua, ct.DonGia, " +
            "       sp.TenSP, l.SoLo, " +
            "       FORMAT(l.HanSuDung, 'dd/MM/yyyy') AS HSD, " +
            "       ISNULL(prev.DaTra, 0) AS DaTra " +
            "FROM ChiTietHoaDon ct " +
            "JOIN SanPham sp ON ct.MaSP = sp.MaSP " +
            "JOIN LoHang l ON ct.MaLo = l.MaLo " +
            "LEFT JOIN ( " +
            "    SELECT ct2.MaLo, ct2.MaSP, SUM(ABS(ct2.SoLuong)) AS DaTra " +
            "    FROM ChiTietHoaDon ct2 " +
            "    JOIN HoaDon hd2 ON ct2.MaHD = hd2.MaHD " +
            "    WHERE hd2.MaHDGoc = ? AND hd2.LoaiHD = N'RETURN' " +
            "    GROUP BY ct2.MaLo, ct2.MaSP " +
            ") prev ON prev.MaLo = ct.MaLo AND prev.MaSP = ct.MaSP " +
            "WHERE ct.MaHD = ? AND ct.SoLuong > 0 " +
            "ORDER BY sp.TenSP, l.HanSuDung";

        java.util.List<Object[]> result = new java.util.ArrayList<>();
        try (java.sql.Connection conn = infrastructure.database.DatabaseHelper.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maHDGoc);
            ps.setInt(2, maHDGoc);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int soLuongMua = rs.getInt("SoLuongMua");
                    int daTra = rs.getInt("DaTra");
                    int conLai = soLuongMua - daTra;
                    if (conLai > 0) {
                        result.add(new Object[]{
                            rs.getInt("MaLo"), rs.getInt("MaSP"), soLuongMua, daTra,
                            rs.getBigDecimal("DonGia"),
                            rs.getNString("TenSP"), rs.getNString("SoLo"), rs.getString("HSD")
                        });
                    }
                }
            }
        } catch (java.sql.SQLException e) { e.printStackTrace(); }
        return result;
    }

    @Override
    public int executeReturnInvoice(int maHDGoc, int maND, String lyDo, String xmlItems) throws java.sql.SQLException {
        try (java.sql.Connection conn = infrastructure.database.DatabaseHelper.getConnection();
             java.sql.CallableStatement cs = conn.prepareCall("{CALL sp_TraHangKhach(?, ?, ?, ?)}")) {
            cs.setInt(1, maHDGoc);
            cs.setInt(2, maND);
            cs.setNString(3, lyDo);
            cs.setNString(4, xmlItems);
            try (java.sql.ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("MaHDTra");
                }
            }
        }
        throw new java.sql.SQLException("Stored procedure did not return MaHDTra");
    }
}
