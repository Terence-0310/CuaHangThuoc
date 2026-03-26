package presentation.presenter;

import domain.dto.InventoryBatchDTO;
import presentation.view.IInventoryManagementView;
import service.IInventoryService;

import javax.swing.JOptionPane;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class InventoryPresenter {
    private final IInventoryManagementView view;
    private final IInventoryService service;

    public InventoryPresenter(IInventoryManagementView view, IInventoryService service) {
        this.view = view;
        this.service = service;
    }

    public void loadBatches(int page, int pageSize, String keyword, String statusFilter, String sortCol, String sortDir) {
        try {
            view.showLoading();
            List<InventoryBatchDTO> data = service.getPagedBatches((page - 1) * pageSize, pageSize, keyword, statusFilter, sortCol, sortDir);
            int totalList = service.countBatches(keyword, statusFilter);
            int totalPages = Math.max(1, (int) Math.ceil((double) totalList / pageSize));
            
            int cHetHSD = service.countHetHSD();
            int cCanDate = service.countCanDate();
            int cHetHang = service.countHetHang();
            
            view.displayBatches(data, page, totalPages, totalList);
            view.updateSummary(totalPages, page, totalList, cHetHSD, cCanDate, cHetHang);
        } catch (Exception e) {
            e.printStackTrace();
            view.showError("Lỗi tải danh sách lô hàng: " + e.getMessage());
        } finally {
            view.hideLoading();
        }
    }

    public void updateBatch(int maLo, String soLoToUpdate, String tenSPToUpdate, BigDecimal giaNhap, LocalDate hsd, int newSoLuong, int excludeMaLo) {
        try {
            view.showLoading();
            if (service.isDuplicateBatch(soLoToUpdate, tenSPToUpdate, excludeMaLo)) {
                view.showMessage("Số lô này đã tồn tại cho sản phẩm: " + tenSPToUpdate, "Trùng Số Lô", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            boolean ok = service.updateBatch(maLo, giaNhap, hsd, newSoLuong);
            if (ok) {
                view.showMessage("Cập nhật lô hàng thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                view.refreshData();
                view.clearForm();
            }
        } catch (Exception e) {
            e.printStackTrace();
            view.showError("Lỗi cập nhật lô hàng: " + e.getMessage());
        } finally {
            view.hideLoading();
        }
    }

    public void returnOrDestroyBatch(String soLo, String tenSP, int tonKho) {
        if (tonKho <= 0) {
            view.showMessage("Lô hàng này đã hết tồn kho (SL = 0).\nKhông cần trả/hủy.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            view.showLoading();
            InventoryBatchDTO info = service.getBatchInfo(soLo, tenSP);
            if (info == null) {
                view.showError("Không tìm thấy thông tin lô hàng trong DB!");
                return;
            }

            int soLuongGoc = service.getOriginalQuantity(info.getMaLo());
            if (soLuongGoc == -1) soLuongGoc = tonKho;

            view.showReturnDestroyDialog(info.getMaLo(), soLo, tenSP, info.getGiaNhap(), info.getTenNCC(), tonKho, soLuongGoc);
        } catch (Exception e) {
            e.printStackTrace();
            view.showError("Lỗi tìm thông tin lô hàng: " + e.getMessage());
        } finally {
            view.hideLoading();
        }
    }

    public void deleteBatch(int maLo) {
        try {
            view.showLoading();
            String refTable = service.checkForeignKeyConstraints(maLo);
            if (refTable != null) {
                view.showMessage("Không thể xóa!\n" +
                                 "Lô hàng này đang được tham chiếu bởi bảng \"" + refTable + "\".\n" +
                                 "Hãy đặt số lượng về 0 thay vì xóa.", 
                                 "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            boolean ok = service.deleteBatch(maLo);
            if (ok) {
                view.showMessage("Xóa thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                view.clearForm();
                view.refreshData();
            }
        } catch (Exception e) {
            e.printStackTrace();
            view.showError("Lỗi xóa: " + e.getMessage());
        } finally {
            view.hideLoading();
        }
    }
}
