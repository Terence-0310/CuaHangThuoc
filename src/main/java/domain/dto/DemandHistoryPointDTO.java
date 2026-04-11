package domain.dto;

import java.time.LocalDate;

public class DemandHistoryPointDTO {
    private LocalDate date;
    private int demand;

    public DemandHistoryPointDTO() {
    }

    public DemandHistoryPointDTO(LocalDate date, int demand) {
        this.date = date;
        this.demand = demand;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getDemand() {
        return demand;
    }

    public void setDemand(int demand) {
        this.demand = demand;
    }
}
