package domain.repository;

import domain.entity.Supplier;
import java.util.List;

/**
 * Repository Interface: Nhà cung cấp
 */
public interface ISupplierRepository {
    int insert(Supplier supplier);
    boolean update(Supplier supplier);
    boolean softDelete(int maNCC);
    Supplier getById(int maNCC);
    List<Supplier> getAll();
    List<Supplier> getActive();
    List<Supplier> search(String keyword);

    /** Phân trang với filter trạng thái */
    List<Supplier> getPagedList(int offset, int pageSize, String keyword, String statusFilter, String sortCol, String sortDir);
    int countFiltered(String keyword, String statusFilter);
}
