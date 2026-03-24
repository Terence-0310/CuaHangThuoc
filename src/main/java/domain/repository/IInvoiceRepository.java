package domain.repository;

import domain.entity.Invoice;
import java.sql.Connection;
import java.util.List;

/**
 * Repository Interface: Hóa đơn
 */
public interface IInvoiceRepository {
    int insert(Connection conn, Invoice invoice);
    boolean updateTotal(Connection conn, int maHD);
    List<Invoice> getByCustomerId(int maKH);
    List<Invoice> getAll();
}
