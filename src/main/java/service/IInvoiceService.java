package service;

import domain.entity.Invoice;
import domain.entity.InvoiceDetail;
import java.util.List;
import domain.dto.InvoiceFilterCriteria;
import domain.dto.InvoiceListDTO;
import domain.dto.InvoiceDetailDTO;
import domain.dto.PagedResult;

/**
 * Service Interface: Xem hóa đơn & lịch sử (ISP — tách khỏi ISaleService)
 */
public interface IInvoiceService {
    List<Invoice> getByCustomerId(int maKH);
    List<InvoiceDetail> getDetails(int maHD);
    List<Invoice> getAll();
    
    // MVP Refactoring
    PagedResult<InvoiceListDTO> searchInvoices(InvoiceFilterCriteria criteria, int page, int pageSize);
    void voidInvoice(int maHD, String reason) throws java.sql.SQLException;
    Invoice getInvoiceHeader(int maHD);
    List<InvoiceDetailDTO> getInvoiceDetails(int maHD);

    // Clean Arch: ReturnInvoiceDialog
    List<Object[]> getReturnBatchData(int maHDGoc);
    int executeReturnInvoice(int maHDGoc, int maND, String lyDo, String xmlItems) throws java.sql.SQLException;
}
