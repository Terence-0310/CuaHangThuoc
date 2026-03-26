package domain.repository;

import domain.dto.InventoryBatchDTO;
import java.util.List;

public interface IInventoryRepository {
    List<InventoryBatchDTO> getPagedBatches(int offset, int pageSize, String keyword, String statusFilter, String sortCol, String sortDir);
    int countBatches(String keyword, String statusFilter);
    boolean updateBatch(int maLo, java.math.BigDecimal giaNhap, java.time.LocalDate hanSuDung, int newSoLuong);
    InventoryBatchDTO getBatchInfo(String soLo, String tenSP);
    boolean isDuplicateBatch(String soLo, String tenSP, int excludeMaLo);
    
    String checkForeignKeyConstraints(int maLo);
    boolean deleteBatch(int maLo);
    
    List<Object[]> getBatchHistory(int maLo);
    
    List<Object[]> getReturnHistory();
    List<Object[]> getDestroyHistory();
    
    // For hủy, trả hàng
    int getOriginalQuantity(int maLo);
    
    // Summary info
    int countHetHSD();
    int countCanDate();
    int countHetHang();
}
