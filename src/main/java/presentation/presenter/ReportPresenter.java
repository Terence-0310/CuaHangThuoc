package presentation.presenter;

import domain.dto.*;
import presentation.view.IReportView;
import service.IReportService;

import java.util.List;

public class ReportPresenter {
    private final IReportView view;
    private final IReportService reportService;

    public ReportPresenter(IReportView view, IReportService reportService) {
        this.view = view;
        this.reportService = reportService;
    }

    public void loadAvailableYears() {
        try {
            view.showLoading();
            List<Integer> years = reportService.getAvailableYears();
            view.displayAvailableYears(years);
        } catch (Exception e) {
            e.printStackTrace();
            view.showError("Lỗi khi tải danh sách năm: " + e.getMessage());
        } finally {
            view.hideLoading();
        }
    }

    public void loadDashboardData(int year, int quarter) {
        try {
            view.showLoading();

            BusinessMetricDTO metrics = reportService.getKPIs(year, quarter);
            view.displayKPIs(metrics);

            List<TopSellingDTO> products = reportService.getTopProducts(year, quarter);
            view.displayTopProducts(products);

            List<TopCustomerDTO> customers = reportService.getTopCustomers(year, quarter);
            view.displayTopCustomers(customers);

            List<TopSupplierDTO> suppliers = reportService.getTopSuppliers(year, quarter);
            view.displayTopSuppliers(suppliers);

            List<StockAlertDTO> alerts = reportService.getStockAlerts();
            view.displayStockAlerts(alerts);

        } catch (Exception e) {
            e.printStackTrace();
            view.showError("Lỗi khi tải dữ liệu báo cáo: " + e.getMessage());
        } finally {
            view.hideLoading();
        }
    }
}
