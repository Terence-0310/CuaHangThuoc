package service.impl;

import domain.dto.DemandHistoryPointDTO;
import domain.repository.IDemandAnalyticsRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

class DemandForecastServiceImplTest {

    @Test
    void forecastNextDays_shouldReturnHorizonPointsUsingMovingAverage() {
        IDemandAnalyticsRepository fakeRepo = new IDemandAnalyticsRepository() {
            @Override
            public List<DemandHistoryPointDTO> getDailyDemandHistory(int maSP, int maCN, LocalDate fromDate, LocalDate toDate) {
                List<DemandHistoryPointDTO> list = new ArrayList<>();
                LocalDate cursor = toDate.minusDays(6);
                for (int i = 0; i < 7; i++) {
                    list.add(new DemandHistoryPointDTO(cursor.plusDays(i), 10));
                }
                return list;
            }

            @Override
            public boolean runDailyEtl(LocalDate fromDate, LocalDate toDate) {
                return true;
            }
        };

        DemandForecastServiceImpl service = new DemandForecastServiceImpl(fakeRepo);
        var result = service.forecastNextDays(1, 1, 30, 5);

        Assertions.assertEquals(5, result.size());
        Assertions.assertTrue(result.stream().allMatch(p -> p.getPredictedDemand() == 10));
    }
}
