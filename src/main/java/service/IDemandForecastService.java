package service;

import domain.dto.DemandForecastPointDTO;
import domain.dto.DemandHistoryPointDTO;

import java.time.LocalDate;
import java.util.List;

public interface IDemandForecastService {
    boolean refreshEtl(LocalDate fromDate, LocalDate toDate);
    List<DemandHistoryPointDTO> getHistory(int maSP, int maCN, LocalDate fromDate, LocalDate toDate);
    List<DemandForecastPointDTO> forecastNextDays(int maSP, int maCN, int lookbackDays, int horizonDays);
}
