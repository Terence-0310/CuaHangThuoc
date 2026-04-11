package tools;

import common.ServiceFactory;
import domain.dto.DemandForecastDTO;
import domain.dto.DemandPlanningEvaluationDTO;
import service.IDemandPlanningService;

import java.time.LocalDate;
import java.util.List;

public class DemandPlanningRunner {
    public static void main(String[] args) {
        IDemandPlanningService service = ServiceFactory.getDemandPlanningService();

        LocalDate toDate = LocalDate.now();
        LocalDate fromDate = toDate.minusDays(120);

        service.refreshDataMart(fromDate, toDate);
        service.generateForecast(14, 30);
        service.runOptimization(14);
        DemandPlanningEvaluationDTO eval = service.evaluate(30);

        System.out.println("=== DEMAND PLANNING RUN COMPLETE ===");
        System.out.println("Sample size: " + eval.getSampleSize());
        System.out.println("MAE: " + eval.getMae());
        System.out.println("RMSE: " + eval.getRmse());
        System.out.println("MAPE: " + eval.getMape());

        List<DemandForecastDTO> top = service.getLatestForecasts(5);
        for (DemandForecastDTO row : top) {
            System.out.printf("Forecast | date=%s sp=%d cn=%d qty=%s model=%s%n",
                    row.getForecastDate(), row.getMaSP(), row.getMaCN(),
                    row.getForecastQty(), row.getModelName());
        }
    }
}
