package domain.dto;

import java.time.LocalDate;

public class DemandForecastPointDTO {
    private LocalDate date;
    private int predictedDemand;

    public DemandForecastPointDTO() {
    }

    public DemandForecastPointDTO(LocalDate date, int predictedDemand) {
        this.date = date;
        this.predictedDemand = predictedDemand;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getPredictedDemand() {
        return predictedDemand;
    }

    public void setPredictedDemand(int predictedDemand) {
        this.predictedDemand = predictedDemand;
    }
}
