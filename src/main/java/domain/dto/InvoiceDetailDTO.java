package domain.dto;

import java.math.BigDecimal;

/**
 * DTO đại diện cho một chi tiết hóa đơn (kèm Tên SP, Lô hàng) để hiển thị
 */
public class InvoiceDetailDTO {
    private String tenSP;
    private String soLo;
    private int soLuong;
    private BigDecimal donGia;
    private BigDecimal thanhTien;

    public InvoiceDetailDTO() {}

    public InvoiceDetailDTO(String tenSP, String soLo, int soLuong, BigDecimal donGia, BigDecimal thanhTien) {
        this.tenSP = tenSP;
        this.soLo = soLo;
        this.soLuong = soLuong;
        this.donGia = donGia;
        this.thanhTien = thanhTien;
    }

    public String getTenSP() { return tenSP; }
    public void setTenSP(String tenSP) { this.tenSP = tenSP; }

    public String getSoLo() { return soLo; }
    public void setSoLo(String soLo) { this.soLo = soLo; }

    public int getSoLuong() { return soLuong; }
    public void setSoLuong(int soLuong) { this.soLuong = soLuong; }

    public BigDecimal getDonGia() { return donGia; }
    public void setDonGia(BigDecimal donGia) { this.donGia = donGia; }

    public BigDecimal getThanhTien() { return thanhTien; }
    public void setThanhTien(BigDecimal thanhTien) { this.thanhTien = thanhTien; }
}
