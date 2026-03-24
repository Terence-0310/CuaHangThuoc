package service;

import domain.entity.Invoice;
import domain.entity.InvoiceDetail;
import java.util.List;

/**
 * Service Interface: Xem hóa đơn & lịch sử (ISP — tách khỏi ISaleService)
 */
public interface IInvoiceService {
    List<Invoice> getByCustomerId(int maKH);
    List<InvoiceDetail> getDetails(int maHD);
    List<Invoice> getAll();
}
