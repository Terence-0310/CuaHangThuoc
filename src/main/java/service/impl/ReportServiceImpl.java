package service.impl;

import domain.dto.RevenueDTO;
import domain.dto.TopCustomerDTO;
import domain.dto.TopSellingDTO;
import domain.entity.Batch;
import domain.repository.IReportRepository;
import service.IReportService;
import java.util.List;

/**
 * Service Impl: Dashboard & Báo cáo (SRP + DIP)
 */
public class ReportServiceImpl implements IReportService {

    private final IReportRepository reportRepo;

    public ReportServiceImpl(IReportRepository reportRepo) {
        this.reportRepo = reportRepo;
    }

    @Override
    public RevenueDTO getRevenue() {
        return reportRepo.getRevenue();
    }

    @Override
    public List<Batch> getExpiringBatches() {
        return reportRepo.getExpiringBatches();
    }

    @Override
    public List<TopSellingDTO> getTopSelling(int topN) {
        return reportRepo.getTopSelling(topN);
    }

    @Override
    public List<TopCustomerDTO> getTopCustomers(int topN) {
        return reportRepo.getTopCustomers(topN);
    }
}
