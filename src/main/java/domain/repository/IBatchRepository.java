package domain.repository;

import domain.entity.Batch;
import java.sql.Connection;
import java.util.List;

/**
 * Repository Interface: Lô hàng
 * Một số method nhận Connection để hỗ trợ transaction (FEFO checkout)
 */
public interface IBatchRepository {
    int insert(Batch batch);
    List<Batch> getByProductId(int maSP);
    List<Batch> getAllWithProduct();
    List<Batch> getFEFO(Connection conn, int maSP);
    boolean updateQuantity(Connection conn, int maLo, int newQuantity);
}
