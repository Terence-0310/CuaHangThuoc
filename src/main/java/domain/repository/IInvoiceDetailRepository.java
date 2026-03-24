package domain.repository;

import domain.entity.InvoiceDetail;
import java.sql.Connection;
import java.util.List;

/**
 * Repository Interface: Chi tiết hóa đơn
 */
public interface IInvoiceDetailRepository {
    int insert(Connection conn, InvoiceDetail detail);
    List<InvoiceDetail> getByInvoiceId(int maHD);
}
