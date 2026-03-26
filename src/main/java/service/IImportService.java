package service;

import domain.dto.ImportCartItem;
import domain.entity.Product;
import domain.entity.Supplier;
import java.util.List;

/**
 * Service Interface: Nhập Kho (Clean Architecture — thay thế NhapKhoDAO trong Presentation)
 */
public interface IImportService {
    List<Product> getAllActiveProducts();
    List<Supplier> getActiveSuppliers();
    boolean existsSoLo(String soLo);
    int saveImportTicket(List<ImportCartItem> cartItems, int maNCC);
}
