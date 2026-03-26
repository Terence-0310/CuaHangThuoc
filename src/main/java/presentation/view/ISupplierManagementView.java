package presentation.view;

import domain.entity.Supplier;
import java.util.List;

public interface ISupplierManagementView {
    void displaySuppliers(List<Supplier> suppliers, int currentPage, int totalPages);
    void showLoading();
    void hideLoading();
    void showMessage(String message, String title, int messageType);
    void showError(String message);
    void clearForm();
    void refreshData();
}
