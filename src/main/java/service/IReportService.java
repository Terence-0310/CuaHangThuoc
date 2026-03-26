package service;

import domain.dto.*;
import java.util.List;

public interface IReportService {
    List<Integer> getAvailableYears();
    BusinessMetricDTO getKPIs(int year, int quarter);
    List<TopSellingDTO> getTopProducts(int year, int quarter);
    List<TopCustomerDTO> getTopCustomers(int year, int quarter);
    List<TopSupplierDTO> getTopSuppliers(int year, int quarter);
    List<StockAlertDTO> getStockAlerts();
}
