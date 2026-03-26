package presentation.view;

import domain.dto.*;

import java.util.List;

public interface IReportView {
    void displayAvailableYears(List<Integer> years);
    void displayKPIs(BusinessMetricDTO metrics);
    void displayTopProducts(List<TopSellingDTO> products);
    void displayTopCustomers(List<TopCustomerDTO> customers);
    void displayTopSuppliers(List<TopSupplierDTO> suppliers);
    void displayStockAlerts(List<StockAlertDTO> alerts);
    
    void showLoading();
    void hideLoading();
    void showError(String message);
}
