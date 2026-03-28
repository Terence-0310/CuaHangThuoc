package domain.dto;

import java.math.BigDecimal;

/**
 * DTO: KPI tổng quan (Doanh thu, Chi phí, Lợi nhuận)
 */
public class BusinessMetricDTO {
    private BigDecimal netRevenue;
    private int totalSaleInvoices;
    private int totalProducts;
    private BigDecimal totalExpenses;   // Nhập hàng + Lương
    private BigDecimal netProfit;       // Doanh thu - Chi phí

    public BusinessMetricDTO() {}

    public BusinessMetricDTO(BigDecimal netRevenue, int totalSaleInvoices, int totalProducts,
                             BigDecimal totalExpenses, BigDecimal netProfit) {
        this.netRevenue = netRevenue;
        this.totalSaleInvoices = totalSaleInvoices;
        this.totalProducts = totalProducts;
        this.totalExpenses = totalExpenses;
        this.netProfit = netProfit;
    }

    public BigDecimal getNetRevenue() { return netRevenue; }
    public void setNetRevenue(BigDecimal netRevenue) { this.netRevenue = netRevenue; }

    public int getTotalSaleInvoices() { return totalSaleInvoices; }
    public void setTotalSaleInvoices(int totalSaleInvoices) { this.totalSaleInvoices = totalSaleInvoices; }

    public int getTotalProducts() { return totalProducts; }
    public void setTotalProducts(int totalProducts) { this.totalProducts = totalProducts; }

    public BigDecimal getTotalExpenses() { return totalExpenses; }
    public void setTotalExpenses(BigDecimal totalExpenses) { this.totalExpenses = totalExpenses; }

    public BigDecimal getNetProfit() { return netProfit; }
    public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }
}
