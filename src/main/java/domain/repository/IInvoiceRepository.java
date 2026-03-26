package domain.repository;

import domain.dto.InvoiceDetailDTO;
import domain.dto.InvoiceFilterCriteria;
import domain.dto.InvoiceListDTO;
import domain.entity.Invoice;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Repository Interface: Hóa đơn
 */
public interface IInvoiceRepository {
    int insert(Connection conn, Invoice invoice);
    boolean updateTotal(Connection conn, int maHD);
    List<Invoice> getByCustomerId(int maKH);
    List<Invoice> getAll();

    /** Tìm kiếm hóa đơn với phân trang & filter */
    List<InvoiceListDTO> searchInvoices(InvoiceFilterCriteria criteria, int page, int pageSize);
    int countInvoices(InvoiceFilterCriteria criteria);

    /** Huỷ hóa đơn */
    void voidInvoice(int maHD, String reason) throws SQLException;

    /** Lấy header hóa đơn */
    Invoice getInvoiceHeader(int maHD);

    /** Chi tiết hóa đơn (DTO) */
    List<InvoiceDetailDTO> getInvoiceDetails(int maHD);
}
