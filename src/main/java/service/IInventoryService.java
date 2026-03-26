package service;

import domain.dto.InventoryBatchDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface IInventoryService {
    List<InventoryBatchDTO> getPagedBatches(int offset, int pageSize, String keyword, String statusFilter, String sortCol, String sortDir);
    int countBatches(String keyword, String statusFilter);
    boolean updateBatch(int maLo, BigDecimal giaNhap, LocalDate hanSuDung, int newSoLuong);
    InventoryBatchDTO getBatchInfo(String soLo, String tenSP);
    boolean isDuplicateBatch(String soLo, String tenSP, int excludeMaLo);
    
    String checkForeignKeyConstraints(int maLo);
    boolean deleteBatch(int maLo);
    
    List<Object[]> getBatchHistory(int maLo);
    
    List<Object[]> getReturnHistory();
    List<Object[]> getDestroyHistory();
    
    int getOriginalQuantity(int maLo);
    
    int countHetHSD();
    int countCanDate();
    int countHetHang();
}
