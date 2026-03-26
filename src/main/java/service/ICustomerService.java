package service;

import domain.entity.Customer;
import java.util.List;
import domain.dto.CustomerPurchaseHistoryDTO;

/**
 * Service Interface: Quản lý khách hàng
 */
public interface ICustomerService {
    Customer findOrCreate(String soDT, String tenKH);
    Customer getById(int maKH);
    List<Customer> getAll();
    List<Customer> search(String keyword);
    int insert(Customer customer);
    boolean update(Customer customer);
    boolean delete(int maKH) throws RuntimeException;
    List<CustomerPurchaseHistoryDTO> getCustomerPurchaseHistory(int maKH);
    List<Customer> getPagedList(int offset, int pageSize, String keyword, String sortCol, String sortDir);
    int countFiltered(String keyword);
}
