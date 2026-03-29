package service;

import domain.entity.Product;
import domain.entity.Batch;
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
    boolean existsByName(String tenSP);
    boolean existsByNameExcluding(String tenSP, int excludeMaSP);
    List<Product> getPagedWithStock(int offset, int pageSize, String keyword, String statusFilter,
                                    String sortColumn, String sortDirection);
    int countFiltered(String keyword, String statusFilter);
    int insert(Product product);
    boolean update(Product product);
    boolean softDelete(int maSP);
    int bulkUpdateStatus(List<Integer> ids, boolean trangThai);

    // Clean Arch: Data access cho dialog ProductDetail & ProductPanel.showSalesHistory
    List<Batch> getBatchesByProduct(int maSP);
    List<Object[]> getSalesHistoryByProduct(int maSP);
}
