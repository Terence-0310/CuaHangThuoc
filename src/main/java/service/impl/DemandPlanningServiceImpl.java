package service.impl;

import domain.dto.DemandForecastDTO;
import domain.dto.DemandPlanningEvaluationDTO;
import domain.dto.InternalTransferSuggestionDTO;
import domain.dto.ReplenishmentSuggestionDTO;
import domain.repository.IDemandPlanningRepository;
import service.IDemandPlanningService;

import java.time.LocalDate;
import java.util.List;

public class DemandPlanningServiceImpl implements IDemandPlanningService {
    private final IDemandPlanningRepository repository;

    public DemandPlanningServiceImpl(IDemandPlanningRepository repository) {
        this.repository = repository;
    }

    @Override
    public void refreshDataMart(LocalDate fromDate, LocalDate toDate) {
        repository.refreshDataMart(fromDate, toDate);
    }

    @Override
    public void generateForecast(int horizonDays, int lookbackDays) {
        repository.generateForecast(horizonDays, lookbackDays);
    }

    @Override
    public void runOptimization(int horizonDays) {
        repository.runOptimization(horizonDays);
    }

    @Override
    public DemandPlanningEvaluationDTO evaluate(int backtestDays) {
        return repository.evaluate(backtestDays);
    }

    @Override
    public List<DemandForecastDTO> getLatestForecasts(int limit) {
        return repository.getLatestForecasts(limit);
    }

    @Override
    public List<ReplenishmentSuggestionDTO> getLatestReplenishmentSuggestions() {
        return repository.getLatestReplenishmentSuggestions();
    }

    @Override
    public List<InternalTransferSuggestionDTO> getLatestTransferSuggestions() {
        return repository.getLatestTransferSuggestions();
    }
}
