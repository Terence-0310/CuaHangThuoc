package service;

import domain.entity.Product;
import java.util.List;

/**
 * Service Interface: Quản lý sản phẩm
 */
public interface IProductService {
    List<Product> getAll();
    List<Product> getActive();
    List<Product> getAllWithStock();
    Product getById(int maSP);
    List<Product> search(String keyword);
    boolean existsByNameAndUnit(String tenSP, String donViTinh);
    List<Product> getPagedWithStock(int offset, int pageSize, String keyword, String statusFilter,
                                    String sortColumn, String sortDirection);
    int countFiltered(String keyword, String statusFilter);
    int insert(Product product);
    boolean update(Product product);
    boolean softDelete(int maSP);
    int bulkUpdateStatus(List<Integer> ids, boolean trangThai);
}
