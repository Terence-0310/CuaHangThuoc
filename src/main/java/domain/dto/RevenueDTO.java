package domain.dto;

import java.math.BigDecimal;

/**
 * DTO: Doanh thu cho Dashboard cards
 */
public class RevenueDTO {
    private BigDecimal today;
    private BigDecimal month;
    private BigDecimal quarter;

    public RevenueDTO() {}

    public RevenueDTO(BigDecimal today, BigDecimal month, BigDecimal quarter) {
        this.today = today;
        this.month = month;
        this.quarter = quarter;
    }

    public BigDecimal getToday() { return today; }
    public void setToday(BigDecimal today) { this.today = today; }

    public BigDecimal getMonth() { return month; }
    public void setMonth(BigDecimal month) { this.month = month; }

    public BigDecimal getQuarter() { return quarter; }
    public void setQuarter(BigDecimal quarter) { this.quarter = quarter; }
}
