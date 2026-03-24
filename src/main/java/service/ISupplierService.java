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
}
