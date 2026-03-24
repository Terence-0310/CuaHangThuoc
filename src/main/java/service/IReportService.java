package service;

import domain.dto.RevenueDTO;
import domain.dto.TopCustomerDTO;
import domain.dto.TopSellingDTO;
import domain.entity.Batch;
import java.util.List;

/**
 * Service Interface: Dashboard & Báo cáo
 */
public interface IReportService {
    RevenueDTO getRevenue();
    List<Batch> getExpiringBatches();
    List<TopSellingDTO> getTopSelling(int topN);
    List<TopCustomerDTO> getTopCustomers(int topN);
}
