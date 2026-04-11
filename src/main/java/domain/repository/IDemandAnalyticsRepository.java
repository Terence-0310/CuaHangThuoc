package domain.repository;

import domain.dto.DemandHistoryPointDTO;

import java.time.LocalDate;
import java.util.List;

public interface IDemandAnalyticsRepository {
    List<DemandHistoryPointDTO> getDailyDemandHistory(int maSP, int maCN, LocalDate fromDate, LocalDate toDate);
    boolean runDailyEtl(LocalDate fromDate, LocalDate toDate);
}
