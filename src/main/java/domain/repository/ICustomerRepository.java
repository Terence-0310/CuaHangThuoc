package domain.repository;

import domain.dto.CustomerPurchaseHistoryDTO;
import domain.entity.Customer;
import java.sql.Connection;
import java.util.List;

/**
 * Repository Interface: Khách hàng
 */
public interface ICustomerRepository {
    Customer findByPhone(String soDT);
    Customer findByPhone(Connection conn, String soDT);
    Customer getById(int maKH);
    int insert(Connection conn, Customer customer);
    int insert(Customer customer);
    boolean update(Customer customer);
    boolean delete(int maKH);
    List<Customer> getAll();
    List<Customer> search(String keyword);
    List<Customer> getPagedList(int offset, int pageSize, String keyword, String sortCol, String sortDir);
    int countFiltered(String keyword);

    /** Lịch sử mua hàng của khách */
    List<CustomerPurchaseHistoryDTO> getCustomerPurchaseHistory(int maKH);
}
