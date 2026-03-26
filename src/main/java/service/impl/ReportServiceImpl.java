package service.impl;

import domain.dto.*;
import domain.repository.IReportRepository;
import service.IReportService;

import java.util.List;

public class ReportServiceImpl implements IReportService {
    private final IReportRepository repository;

    public ReportServiceImpl(IReportRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Integer> getAvailableYears() {
        return repository.getAvailableYears();
    }

    @Override
    public BusinessMetricDTO getKPIs(int year, int quarter) {
        return repository.getKPIs(year, quarter);
    }

    @Override
    public List<TopSellingDTO> getTopProducts(int year, int quarter) {
        return repository.getTopProducts(year, quarter);
    }

    @Override
    public List<TopCustomerDTO> getTopCustomers(int year, int quarter) {
        return repository.getTopCustomers(year, quarter);
    }

    @Override
    public List<TopSupplierDTO> getTopSuppliers(int year, int quarter) {
        return repository.getTopSuppliers(year, quarter);
    }

    @Override
    public List<StockAlertDTO> getStockAlerts() {
        return repository.getStockAlerts();
    }
}
