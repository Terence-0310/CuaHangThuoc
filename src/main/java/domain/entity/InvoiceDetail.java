package domain.entity;

import java.math.BigDecimal;

/**
 * Entity: Chi tiết hóa đơn
 */
public class InvoiceDetail {
    private int maCTHD;
    private int maHD;
    private int maLo;
    private int maSP;
    private int soLuong;
    private BigDecimal donGia;
    private BigDecimal giaVon;       // ★ Snapshot giá vốn tại thời điểm bán
    private BigDecimal thanhTien;

    // Transient
    private String tenSP;
    private String donViTinh;
    private String soLo;

    public InvoiceDetail() {}

    public InvoiceDetail(int maHD, int maLo, int maSP, int soLuong, BigDecimal donGia, BigDecimal giaVon) {
        this.maHD = maHD;
        this.maLo = maLo;
        this.maSP = maSP;
        this.soLuong = soLuong;
        this.donGia = donGia;
        this.giaVon = giaVon;
        this.thanhTien = donGia.multiply(BigDecimal.valueOf(soLuong));
    }

    // --- Getters & Setters ---
    public int getMaCTHD() { return maCTHD; }
    public void setMaCTHD(int maCTHD) { this.maCTHD = maCTHD; }

    public int getMaHD() { return maHD; }
    public void setMaHD(int maHD) { this.maHD = maHD; }

    public int getMaLo() { return maLo; }
    public void setMaLo(int maLo) { this.maLo = maLo; }

    public int getMaSP() { return maSP; }
    public void setMaSP(int maSP) { this.maSP = maSP; }

    public int getSoLuong() { return soLuong; }
    public void setSoLuong(int soLuong) { this.soLuong = soLuong; }

    public BigDecimal getDonGia() { return donGia; }
    public void setDonGia(BigDecimal donGia) { this.donGia = donGia; }

    public BigDecimal getGiaVon() { return giaVon; }
    public void setGiaVon(BigDecimal giaVon) { this.giaVon = giaVon; }

    public BigDecimal getThanhTien() { return thanhTien; }
    public void setThanhTien(BigDecimal thanhTien) { this.thanhTien = thanhTien; }

    public String getTenSP() { return tenSP; }
    public void setTenSP(String tenSP) { this.tenSP = tenSP; }

    public String getDonViTinh() { return donViTinh; }
    public void setDonViTinh(String donViTinh) { this.donViTinh = donViTinh; }

    public String getSoLo() { return soLo; }
    public void setSoLo(String soLo) { this.soLo = soLo; }
}
