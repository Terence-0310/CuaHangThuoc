package presentation.view;

import domain.dto.InvoiceListDTO;
import domain.dto.InvoiceDetailDTO;
import domain.entity.Invoice;

import java.util.List;

public interface IInvoiceView {
    // Core data display
    void displayInvoices(List<InvoiceListDTO> invoices, int currentPage, int totalPages);
    
    // Popup details
    void displayInvoiceDetails(Invoice header, List<InvoiceDetailDTO> details);
    
    // System UI feedback
    void showLoading();
    void hideLoading();
    void showMessage(String message, String title, int messageType);
    void showError(String message);
    
    // Callbacks for success actions
    void onVoidSuccess();
}
