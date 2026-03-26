package domain.repository;

import domain.dto.*;
import domain.entity.Batch;
import java.util.List;

/**
 * Repository Interface: Báo cáo (ISP — tách riêng khỏi các repo khác)
 */
public interface IReportRepository {
    // Legacy (giữ cho các module cũ chưa migrate)
    RevenueDTO getRevenue();
    List<Batch> getExpiringBatches();
    List<TopSellingDTO> getTopSelling(int topN);
    List<TopCustomerDTO> getTopCustomers(int topN);

    // Mới: Báo cáo nâng cao theo năm/quý
    List<Integer> getAvailableYears();
    BusinessMetricDTO getKPIs(int year, int quarter);
    List<TopSellingDTO> getTopProducts(int year, int quarter);
    List<TopCustomerDTO> getTopCustomers(int year, int quarter);
    List<TopSupplierDTO> getTopSuppliers(int year, int quarter);
    List<StockAlertDTO> getStockAlerts();
}
