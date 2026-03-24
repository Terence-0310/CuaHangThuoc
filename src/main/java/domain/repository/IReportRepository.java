package domain.repository;

import domain.dto.RevenueDTO;
import domain.dto.TopCustomerDTO;
import domain.dto.TopSellingDTO;
import domain.entity.Batch;
import java.util.List;

/**
 * Repository Interface: Báo cáo (ISP — tách riêng khỏi các repo khác)
 */
public interface IReportRepository {
    RevenueDTO getRevenue();
    List<Batch> getExpiringBatches();
    List<TopSellingDTO> getTopSelling(int topN);
    List<TopCustomerDTO> getTopCustomers(int topN);
}
