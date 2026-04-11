package domain.dto;

public class ReplenishmentSuggestionDTO {
    private int maSP;
    private int maCN;
    private int forecastWindowDays;
    private int currentStock;
    private int requiredQty;
    private int suggestedQty;

    public int getMaSP() {
        return maSP;
    }

    public void setMaSP(int maSP) {
        this.maSP = maSP;
    }

    public int getMaCN() {
        return maCN;
    }

    public void setMaCN(int maCN) {
        this.maCN = maCN;
    }

    public int getForecastWindowDays() {
        return forecastWindowDays;
    }

    public void setForecastWindowDays(int forecastWindowDays) {
        this.forecastWindowDays = forecastWindowDays;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(int currentStock) {
        this.currentStock = currentStock;
    }

    public int getRequiredQty() {
        return requiredQty;
    }

    public void setRequiredQty(int requiredQty) {
        this.requiredQty = requiredQty;
    }

    public int getSuggestedQty() {
        return suggestedQty;
    }

    public void setSuggestedQty(int suggestedQty) {
        this.suggestedQty = suggestedQty;
    }
}
