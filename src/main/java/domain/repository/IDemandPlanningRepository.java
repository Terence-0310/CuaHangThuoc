package domain.repository;

import domain.dto.DemandForecastDTO;
import domain.dto.DemandPlanningEvaluationDTO;
import domain.dto.InternalTransferSuggestionDTO;
import domain.dto.ReplenishmentSuggestionDTO;

import java.time.LocalDate;
import java.util.List;

public interface IDemandPlanningRepository {
    void refreshDataMart(LocalDate fromDate, LocalDate toDate);
    void generateForecast(int horizonDays, int lookbackDays);
    void runOptimization(int horizonDays);
    DemandPlanningEvaluationDTO evaluate(int backtestDays);
    List<DemandForecastDTO> getLatestForecasts(int limit);
    List<ReplenishmentSuggestionDTO> getLatestReplenishmentSuggestions();
    List<InternalTransferSuggestionDTO> getLatestTransferSuggestions();
}
