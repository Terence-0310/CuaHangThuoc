package service;

import domain.entity.Batch;
import java.util.List;

/**
 * Service Interface: Quản lý lô hàng / nhập kho
 */
public interface IBatchService {
    int importBatch(Batch batch);
    List<Batch> getByProductId(int maSP);
    List<Batch> getAllWithProduct();
}
