package service.impl;

import domain.dto.ImportCartItem;
import domain.entity.Product;
import domain.entity.Supplier;
import infrastructure.repository.NhapKhoDAO;
import service.IImportService;
import java.util.List;

/**
 * Service Impl: Nhập Kho — delegates to NhapKhoDAO (SRP + DIP)
 */
public class ImportServiceImpl implements IImportService {

    private final NhapKhoDAO nhapKhoDAO;

    public ImportServiceImpl(NhapKhoDAO nhapKhoDAO) {
        this.nhapKhoDAO = nhapKhoDAO;
    }

    @Override
    public List<Product> getAllActiveProducts() {
        return nhapKhoDAO.getAllActiveProducts();
    }

    @Override
    public List<Supplier> getActiveSuppliers() {
        return nhapKhoDAO.getActiveSuppliers();
    }

    @Override
    public boolean existsSoLo(String soLo) {
        return nhapKhoDAO.existsSoLo(soLo);
    }

    @Override
    public int saveImportTicket(List<ImportCartItem> cartItems, int maNCC) {
        return nhapKhoDAO.saveImportTicket(cartItems, maNCC);
    }
}
