package presentation.presenter;

import domain.dto.InvoiceFilterCriteria;
import domain.dto.InvoiceListDTO;
import domain.dto.InvoiceDetailDTO;
import domain.dto.PagedResult;
import domain.entity.Invoice;
import presentation.view.IInvoiceView;
import service.IInvoiceService;

import javax.swing.JOptionPane;
import java.sql.SQLException;
import java.util.List;

public class InvoicePresenter {
    private final IInvoiceView view;
    private final IInvoiceService invoiceService;

    public InvoicePresenter(IInvoiceView view, IInvoiceService invoiceService) {
        this.view = view;
        this.invoiceService = invoiceService;
    }

    public void loadInvoices(InvoiceFilterCriteria criteria, int page, int pageSize) {
        try {
            view.showLoading();
            PagedResult<InvoiceListDTO> result = invoiceService.searchInvoices(criteria, page, pageSize);
            view.displayInvoices(result.getData(), page, result.getTotalPages());
        } catch (Exception e) {
            e.printStackTrace();
            view.showError("Lỗi tải danh sách hóa đơn: " + e.getMessage());
        } finally {
            view.hideLoading();
        }
    }

    public void voidInvoice(int maHD, String reason) {
        try {
            view.showLoading();
            invoiceService.voidInvoice(maHD, reason);
            view.showMessage("Đã hủy hóa đơn thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            view.onVoidSuccess();
        } catch (SQLException e) {
            e.printStackTrace();
            view.showError("Không thể hủy hóa đơn: " + e.getMessage());
        } finally {
            view.hideLoading();
        }
    }

    public void showInvoiceDetails(int maHD) {
        try {
            view.showLoading();
            Invoice header = invoiceService.getInvoiceHeader(maHD);
            if (header != null) {
                List<InvoiceDetailDTO> details = invoiceService.getInvoiceDetails(maHD);
                view.displayInvoiceDetails(header, details);
            } else {
                view.showError("Không tìm thấy thông tin hóa đơn #" + maHD);
            }
        } catch (Exception e) {
            e.printStackTrace();
            view.showError("Lỗi tải chi tiết hóa đơn: " + e.getMessage());
        } finally {
            view.hideLoading();
        }
    }
}
