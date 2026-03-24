package service.impl;

import domain.entity.Customer;
import domain.repository.ICustomerRepository;
import service.ICustomerService;
import java.util.List;

/**
 * Service Impl: Khách hàng (SRP + DIP)
 */
public class CustomerServiceImpl implements ICustomerService {

    private final ICustomerRepository customerRepo;

    public CustomerServiceImpl(ICustomerRepository customerRepo) {
        this.customerRepo = customerRepo;
    }

    @Override
    public Customer findOrCreate(String soDT, String tenKH) {
        if (soDT == null || soDT.trim().isEmpty()) {
            return null; // Khách vãng lai
        }
        Customer existing = customerRepo.findByPhone(soDT.trim());
        if (existing != null) {
            return existing;
        }
        // Tạo khách mới
        Customer newCustomer = new Customer();
        newCustomer.setSoDT(soDT.trim());
        newCustomer.setTenKH(tenKH != null ? tenKH.trim() : null);
        // Dùng non-transactional insert cho findOrCreate đơn giản
        // Transactional version sẽ dùng trong SaleServiceImpl
        return existing; // placeholder — sẽ implement đầy đủ khi có RepoImpl
    }

    @Override
    public List<Customer> getAll() {
        return customerRepo.getAll();
    }

    @Override
    public List<Customer> search(String keyword) {
        return customerRepo.search(keyword);
    }
}
