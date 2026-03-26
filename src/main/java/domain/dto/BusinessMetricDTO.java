package domain.dto;

import java.math.BigDecimal;

/**
 * DTO: KPI tổng quan
 */
public class BusinessMetricDTO {
    private BigDecimal netRevenue;
    private int totalSaleInvoices;
    private int totalProducts;

    public BusinessMetricDTO() {}

    public BusinessMetricDTO(BigDecimal netRevenue, int totalSaleInvoices, int totalProducts) {
        this.netRevenue = netRevenue;
        this.totalSaleInvoices = totalSaleInvoices;
        this.totalProducts = totalProducts;
    }

    public BigDecimal getNetRevenue() { return netRevenue; }
    public void setNetRevenue(BigDecimal netRevenue) { this.netRevenue = netRevenue; }

    public int getTotalSaleInvoices() { return totalSaleInvoices; }
    public void setTotalSaleInvoices(int totalSaleInvoices) { this.totalSaleInvoices = totalSaleInvoices; }

    public int getTotalProducts() { return totalProducts; }
    public void setTotalProducts(int totalProducts) { this.totalProducts = totalProducts; }
}
