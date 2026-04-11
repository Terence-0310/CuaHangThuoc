package service.impl;

import domain.dto.DemandForecastPointDTO;
import domain.dto.DemandHistoryPointDTO;
import domain.repository.IDemandAnalyticsRepository;
import infrastructure.repository.DemandAnalyticsRepositoryImpl;
import service.IDemandForecastService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DemandForecastServiceImpl implements IDemandForecastService {
    private final IDemandAnalyticsRepository demandAnalyticsRepository;

    public DemandForecastServiceImpl() {
        this.demandAnalyticsRepository = new DemandAnalyticsRepositoryImpl();
    }

    public DemandForecastServiceImpl(IDemandAnalyticsRepository demandAnalyticsRepository) {
        this.demandAnalyticsRepository = demandAnalyticsRepository;
    }

    @Override
    public boolean refreshEtl(LocalDate fromDate, LocalDate toDate) {
        return demandAnalyticsRepository.runDailyEtl(fromDate, toDate);
    }

    @Override
    public List<DemandHistoryPointDTO> getHistory(int maSP, int maCN, LocalDate fromDate, LocalDate toDate) {
        return demandAnalyticsRepository.getDailyDemandHistory(maSP, maCN, fromDate, toDate);
    }

    @Override
    public List<DemandForecastPointDTO> forecastNextDays(int maSP, int maCN, int lookbackDays, int horizonDays) {
        LocalDate endDate = LocalDate.now();
        LocalDate fromDate = endDate.minusDays(Math.max(lookbackDays, 7L));
        List<DemandHistoryPointDTO> history = getHistory(maSP, maCN, fromDate, endDate);

        int window = Math.min(7, history.size());
        if (window <= 0) {
            window = 1;
        }

        int sum = 0;
        for (int i = Math.max(0, history.size() - window); i < history.size(); i++) {
            sum += Math.max(0, history.get(i).getDemand());
        }
        int forecastDaily = Math.max(0, Math.round((float) sum / window));

        List<DemandForecastPointDTO> forecast = new ArrayList<>();
        for (int i = 1; i <= horizonDays; i++) {
            forecast.add(new DemandForecastPointDTO(endDate.plusDays(i), forecastDaily));
        }
        return forecast;
    }
}
