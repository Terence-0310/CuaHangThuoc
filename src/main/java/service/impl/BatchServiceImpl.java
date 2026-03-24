package service.impl;

import domain.entity.Batch;
import domain.repository.IBatchRepository;
import domain.repository.IProductRepository;
import service.IBatchService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service Impl: Nhập kho / Quản lý lô (SRP + DIP)
 * ★ Mỗi lần nhập = 1 dòng LoHang mới (thuộc 1 PhieuNhap)
 *   FEFO tự biết cách trừ dần, không lo trùng lô
 */
public class BatchServiceImpl implements IBatchService {

    private final IBatchRepository batchRepo;
    private final IProductRepository productRepo;

    public BatchServiceImpl(IBatchRepository batchRepo, IProductRepository productRepo) {
        this.batchRepo = batchRepo;
        this.productRepo = productRepo;
    }

    @Override
    public int importBatch(Batch batch) {
        // === VALIDATE ===
        if (batch.getMaSP() <= 0) {
            throw new IllegalArgumentException("Vui lòng chọn sản phẩm");
        }
        if (productRepo.getById(batch.getMaSP()) == null) {
            throw new IllegalArgumentException("Sản phẩm không tồn tại");
        }
        if (batch.getSoLo() == null || batch.getSoLo().trim().isEmpty()) {
            throw new IllegalArgumentException("Số lô không được để trống");
        }
        if (batch.getHanSuDung() == null || !batch.getHanSuDung().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Hạn sử dụng phải sau ngày hôm nay");
        }
        if (batch.getSoLuong() <= 0) {
            throw new IllegalArgumentException("Số lượng phải > 0");
        }
        if (batch.getGiaNhap() == null || batch.getGiaNhap().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá nhập phải >= 0");
        }

        // ★ Luôn INSERT mới — mỗi lần nhập = 1 dòng riêng (thuộc PhieuNhap khác nhau)
        return batchRepo.insert(batch);
    }

    @Override
    public List<Batch> getByProductId(int maSP) {
        return batchRepo.getByProductId(maSP);
    }

    @Override
    public List<Batch> getAllWithProduct() {
        return batchRepo.getAllWithProduct();
    }
}
