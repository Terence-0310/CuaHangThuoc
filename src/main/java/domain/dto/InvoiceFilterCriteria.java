package domain.dto;

import java.time.LocalDate;

public class InvoiceFilterCriteria {
    private LocalDate fromDate;
    private LocalDate toDate;
    private String searchKH;
    private String searchMaHD;
    private String searchSP;
    private int statusFilter; // 0=all, 1=Thanh cong, 2=Da huy
    private int customerTypeFilter; // 0=all, 1=registered, 2=walk-in
    private int invoiceTypeFilter; // 0=all, 1=SALE, 2=RETURN
    private String fromTime; // HH:mm
    private String toTime; // HH:mm

    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }

    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }

    public String getSearchKH() { return searchKH; }
    public void setSearchKH(String searchKH) { this.searchKH = searchKH; }

    public String getSearchMaHD() { return searchMaHD; }
    public void setSearchMaHD(String searchMaHD) { this.searchMaHD = searchMaHD; }

    public String getSearchSP() { return searchSP; }
    public void setSearchSP(String searchSP) { this.searchSP = searchSP; }

    public int getStatusFilter() { return statusFilter; }
    public void setStatusFilter(int statusFilter) { this.statusFilter = statusFilter; }

    public int getCustomerTypeFilter() { return customerTypeFilter; }
    public void setCustomerTypeFilter(int customerTypeFilter) { this.customerTypeFilter = customerTypeFilter; }

    public int getInvoiceTypeFilter() { return invoiceTypeFilter; }
    public void setInvoiceTypeFilter(int invoiceTypeFilter) { this.invoiceTypeFilter = invoiceTypeFilter; }

    public String getFromTime() { return fromTime; }
    public void setFromTime(String fromTime) { this.fromTime = fromTime; }

    public String getToTime() { return toTime; }
    public void setToTime(String toTime) { this.toTime = toTime; }
}
