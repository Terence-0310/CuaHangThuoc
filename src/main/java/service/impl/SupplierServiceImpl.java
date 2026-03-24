package service.impl;

import domain.entity.Supplier;
import domain.repository.ISupplierRepository;
import service.ISupplierService;
import java.util.List;

/**
 * Service Impl: Nhà cung cấp (SRP + DIP)
 */
public class SupplierServiceImpl implements ISupplierService {

    private final ISupplierRepository supplierRepo;

    public SupplierServiceImpl(ISupplierRepository supplierRepo) {
        this.supplierRepo = supplierRepo;
    }

    @Override
    public int add(Supplier supplier) {
        if (supplier.getTenNCC() == null || supplier.getTenNCC().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên NCC không được để trống");
        }
        return supplierRepo.insert(supplier);
    }

    @Override
    public boolean update(Supplier supplier) {
        if (supplier.getTenNCC() == null || supplier.getTenNCC().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên NCC không được để trống");
        }
        return supplierRepo.update(supplier);
    }

    @Override
    public boolean delete(int maNCC) {
        return supplierRepo.softDelete(maNCC);
    }

    @Override
    public Supplier getById(int maNCC) {
        return supplierRepo.getById(maNCC);
    }

    @Override
    public List<Supplier> getAll() {
        return supplierRepo.getAll();
    }

    @Override
    public List<Supplier> getActive() {
        return supplierRepo.getActive();
    }

    @Override
    public List<Supplier> search(String keyword) {
        return supplierRepo.search(keyword);
    }
}
