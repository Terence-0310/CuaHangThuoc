package service.impl;

import domain.entity.Product;
import domain.repository.IProductRepository;
import service.IProductService;
import java.util.List;

/**
 * Service Impl: Quản lý sản phẩm (SRP + DIP)
 */
public class ProductServiceImpl implements IProductService {

    private final IProductRepository productRepo;

    public ProductServiceImpl(IProductRepository productRepo) {
        this.productRepo = productRepo;
    }

    @Override
    public List<Product> getAll() {
        return productRepo.getAll();
    }

    @Override
    public List<Product> getActive() {
        return productRepo.getActive();
    }

    @Override
    public List<Product> getAllWithStock() {
        return productRepo.getAllWithStock();
    }

    @Override
    public Product getById(int maSP) {
        return productRepo.getById(maSP);
    }

    @Override
    public List<Product> search(String keyword) {
        return productRepo.search(keyword);
    }

    @Override
    public boolean existsByNameAndUnit(String tenSP, String donViTinh) {
        return productRepo.existsByNameAndUnit(tenSP, donViTinh);
    }

    @Override
    public List<Product> getPagedWithStock(int offset, int pageSize, String keyword, String statusFilter,
                                            String sortColumn, String sortDirection) {
        return productRepo.getPagedWithStock(offset, pageSize, keyword, statusFilter, sortColumn, sortDirection);
    }

    @Override
    public int countFiltered(String keyword, String statusFilter) {
        return productRepo.countFiltered(keyword, statusFilter);
    }

    @Override
    public int insert(Product product) {
        validate(product);
        return productRepo.insert(product);
    }

    @Override
    public boolean update(Product product) {
        validate(product);
        return productRepo.update(product);
    }

    @Override
    public boolean softDelete(int maSP) {
        return productRepo.softDelete(maSP);
    }

    private void validate(Product p) {
        if (p.getTenSP() == null || p.getTenSP().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");
        }
        if (p.getDonViTinh() == null || p.getDonViTinh().trim().isEmpty()) {
            throw new IllegalArgumentException("Đơn vị tính không được để trống");
        }
        if (p.getGiaBan() == null || p.getGiaBan().signum() < 0) {
            throw new IllegalArgumentException("Giá bán phải >= 0");
        }
    }

    @Override
    public int bulkUpdateStatus(java.util.List<Integer> ids, boolean trangThai) {
        return productRepo.bulkUpdateStatus(ids, trangThai);
    }
}
