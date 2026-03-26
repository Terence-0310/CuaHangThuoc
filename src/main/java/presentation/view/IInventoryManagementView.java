package presentation.view;

import domain.dto.InventoryBatchDTO;
import java.util.List;

public interface IInventoryManagementView {
    void displayBatches(List<InventoryBatchDTO> batches, int currentPage, int totalPages, int totalItems);
    void updateSummary(int totalPages, int currentPage, int totalItems, int countHetHSD, int countCanDate, int countHetHang);
    
    void showLoading();
    void hideLoading();
    void showMessage(String message, String title, int messageType);
    void showError(String message);
    
    void clearForm();
    void refreshData();
    void showReturnDestroyDialog(int maLo, String soLo, String tenSP, java.math.BigDecimal giaNhap, String tenNCC, int tonKho, int soLuongGoc);
}
