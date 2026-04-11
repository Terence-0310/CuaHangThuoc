package domain.dto;

public class ReorderSuggestionDTO {
    private int maCN;
    private int maSP;
    private int tonHienTai;
    private int demandDailyForecast;
    private int safetyStock;
    private int reorderPoint;
    private int suggestedOrderQty;

    public int getMaCN() {
        return maCN;
    }

    public void setMaCN(int maCN) {
        this.maCN = maCN;
    }

    public int getMaSP() {
        return maSP;
    }

    public void setMaSP(int maSP) {
        this.maSP = maSP;
    }

    public int getTonHienTai() {
        return tonHienTai;
    }

    public void setTonHienTai(int tonHienTai) {
        this.tonHienTai = tonHienTai;
    }

    public int getDemandDailyForecast() {
        return demandDailyForecast;
    }

    public void setDemandDailyForecast(int demandDailyForecast) {
        this.demandDailyForecast = demandDailyForecast;
    }

    public int getSafetyStock() {
        return safetyStock;
    }

    public void setSafetyStock(int safetyStock) {
        this.safetyStock = safetyStock;
    }

    public int getReorderPoint() {
        return reorderPoint;
    }

    public void setReorderPoint(int reorderPoint) {
        this.reorderPoint = reorderPoint;
    }

    public int getSuggestedOrderQty() {
        return suggestedOrderQty;
    }

    public void setSuggestedOrderQty(int suggestedOrderQty) {
        this.suggestedOrderQty = suggestedOrderQty;
    }
}
