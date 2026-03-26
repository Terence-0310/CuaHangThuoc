package presentation.view;

import domain.dto.CustomerPurchaseHistoryDTO;
import domain.dto.InvoiceListDTO;
import domain.entity.Customer;

import java.util.List;

public interface ICustomerManagementView {
    void displayCustomers(List<Customer> customers, int currentPage, int totalPages);
    void displayWalkInInvoices(List<InvoiceListDTO> invoices, String totalRevenueStr);
    void showPurchaseHistoryDialog(String customerName, String customerPhone, List<CustomerPurchaseHistoryDTO> history);
    void showLoading();
    void hideLoading();
    void showMessage(String message, String title, int messageType);
    void showError(String message);
    void clearForm();
    void refreshData();
}
