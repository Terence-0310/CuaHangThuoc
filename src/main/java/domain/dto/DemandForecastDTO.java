package domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DemandForecastDTO {
    private LocalDate forecastDate;
    private int maSP;
    private int maCN;
    private int regionKey;
    private BigDecimal forecastQty;
    private BigDecimal lowerBoundQty;
    private BigDecimal upperBoundQty;
    private String modelName;
    private LocalDateTime generatedAt;

    public LocalDate getForecastDate() {
        return forecastDate;
    }

    public void setForecastDate(LocalDate forecastDate) {
        this.forecastDate = forecastDate;
    }

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

    public int getRegionKey() {
        return regionKey;
    }

    public void setRegionKey(int regionKey) {
        this.regionKey = regionKey;
    }

    public BigDecimal getForecastQty() {
        return forecastQty;
    }

    public void setForecastQty(BigDecimal forecastQty) {
        this.forecastQty = forecastQty;
    }

    public BigDecimal getLowerBoundQty() {
        return lowerBoundQty;
    }

    public void setLowerBoundQty(BigDecimal lowerBoundQty) {
        this.lowerBoundQty = lowerBoundQty;
    }

    public BigDecimal getUpperBoundQty() {
        return upperBoundQty;
    }

    public void setUpperBoundQty(BigDecimal upperBoundQty) {
        this.upperBoundQty = upperBoundQty;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
