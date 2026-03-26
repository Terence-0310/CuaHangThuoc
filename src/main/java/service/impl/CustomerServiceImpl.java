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
        if (existing != null) return existing;
        Customer c = new Customer();
        c.setSoDT(soDT.trim());
        c.setTenKH(tenKH != null ? tenKH.trim() : null);
        int id = customerRepo.insert(c);
        c.setMaKH(id);
        return c;
    }

    @Override
    public Customer getById(int maKH) {
        return customerRepo.getById(maKH);
    }

    @Override
    public List<Customer> getAll() {
        return customerRepo.getAll();
    }

    @Override
    public List<Customer> search(String keyword) {
        return customerRepo.search(keyword);
    }

    @Override
    public int insert(Customer customer) {
        return customerRepo.insert(customer);
    }

    @Override
    public boolean update(Customer customer) {
        return customerRepo.update(customer);
    }

    @Override
    public boolean delete(int maKH) {
        return customerRepo.delete(maKH);
    }

    @Override
    public List<Customer> getPagedList(int offset, int pageSize, String keyword,
                                        String sortCol, String sortDir) {
        return customerRepo.getPagedList(offset, pageSize, keyword, sortCol, sortDir);
    }

    @Override
    public int countFiltered(String keyword) {
        return customerRepo.countFiltered(keyword);
    }
    
    @Override
    public java.util.List<domain.dto.CustomerPurchaseHistoryDTO> getCustomerPurchaseHistory(int maKH) {
        return customerRepo.getCustomerPurchaseHistory(maKH);
    }
}
