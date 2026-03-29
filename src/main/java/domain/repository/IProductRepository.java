package domain.repository;

import domain.entity.Product;
import java.util.List;

/**
 * Repository Interface: Sản phẩm
 */
public interface IProductRepository {
    List<Product> getAll();
    List<Product> getActive();
    List<Product> getAllWithStock();
    Product getById(int maSP);
    List<Product> search(String keyword);
    boolean existsByNameAndUnit(String tenSP, String donViTinh);
    boolean existsByName(String tenSP);
    boolean existsByNameExcluding(String tenSP, int excludeMaSP);

    // ★ Phân trang + Sort server-side
    List<Product> getPagedWithStock(int offset, int pageSize, String keyword, String statusFilter,
                                    String sortColumn, String sortDirection);
    int countFiltered(String keyword, String statusFilter);

    int insert(Product product);
    boolean update(Product product);
    boolean softDelete(int maSP);

    // ★ Bulk action
    int bulkUpdateStatus(List<Integer> ids, boolean trangThai);
}
