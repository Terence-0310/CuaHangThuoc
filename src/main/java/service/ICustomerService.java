package service;

import domain.entity.Customer;
import java.util.List;

/**
 * Service Interface: Quản lý khách hàng
 */
public interface ICustomerService {
    Customer findOrCreate(String soDT, String tenKH);
    List<Customer> getAll();
    List<Customer> search(String keyword);
}
