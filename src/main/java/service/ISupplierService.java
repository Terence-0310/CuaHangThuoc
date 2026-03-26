package service;

import domain.entity.Supplier;
import java.util.List;

/**
 * Service Interface: Nhà cung cấp
 */
public interface ISupplierService {
    int add(Supplier supplier);
    boolean update(Supplier supplier);
    boolean delete(int maNCC);
    Supplier getById(int maNCC);
    List<Supplier> getAll();
    List<Supplier> getActive();
    List<Supplier> search(String keyword);
    List<Supplier> getPagedList(int offset, int pageSize, String keyword, String statusFilter, String sortCol, String sortDir);
    int countFiltered(String keyword, String statusFilter);

    // Clean Arch: Data access cho SupplierDetailDialog
    List<Object[]> getSupplierDetail(int maNCC);
    List<Object[]> getImportTickets(int maNCC);
    List<Object[]> getSupplierBatches(int maNCC);
}
