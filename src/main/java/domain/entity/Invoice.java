package domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity: Hóa đơn bán hàng
 */
public class Invoice {
    private int maHD;
    private Integer maKH;       // Nullable — khách vãng lai
    private int maND;
    private LocalDateTime ngayBan;
    private BigDecimal tongTien;
    private String phuongThucTT;    // TienMat / QR
    private String trangThai;       // Thanh cong / Da huy
    private String lyDoHuy;

    // Transient
    private String tenKH;
    private String soDT;
    private String tenNhanVien;

    public Invoice() {}

    public Invoice(Integer maKH, int maND, String phuongThucTT) {
        this.maKH = maKH;
        this.maND = maND;
        this.tongTien = BigDecimal.ZERO;
        this.phuongThucTT = phuongThucTT != null ? phuongThucTT : "TienMat";
    }

    // --- Getters & Setters ---
    public int getMaHD() { return maHD; }
    public void setMaHD(int maHD) { this.maHD = maHD; }

    public Integer getMaKH() { return maKH; }
    public void setMaKH(Integer maKH) { this.maKH = maKH; }

    public int getMaND() { return maND; }
    public void setMaND(int maND) { this.maND = maND; }

    public LocalDateTime getNgayBan() { return ngayBan; }
    public void setNgayBan(LocalDateTime ngayBan) { this.ngayBan = ngayBan; }

    public BigDecimal getTongTien() { return tongTien; }
    public void setTongTien(BigDecimal tongTien) { this.tongTien = tongTien; }

    public String getPhuongThucTT() { return phuongThucTT; }
    public void setPhuongThucTT(String phuongThucTT) { this.phuongThucTT = phuongThucTT; }

    public String getTenKH() { return tenKH; }
    public void setTenKH(String tenKH) { this.tenKH = tenKH; }

    public String getSoDT() { return soDT; }
    public void setSoDT(String soDT) { this.soDT = soDT; }

    public String getTenNhanVien() { return tenNhanVien; }
    public void setTenNhanVien(String tenNhanVien) { this.tenNhanVien = tenNhanVien; }

    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }

    public String getLyDoHuy() { return lyDoHuy; }
    public void setLyDoHuy(String lyDoHuy) { this.lyDoHuy = lyDoHuy; }
}
