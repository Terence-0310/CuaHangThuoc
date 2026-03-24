package domain.repository;

import domain.entity.Customer;
import java.sql.Connection;
import java.util.List;

/**
 * Repository Interface: Khách hàng
 */
public interface ICustomerRepository {
    Customer findByPhone(String soDT);
    Customer findByPhone(Connection conn, String soDT);
    int insert(Connection conn, Customer customer);
    List<Customer> getAll();
    List<Customer> search(String keyword);
}
