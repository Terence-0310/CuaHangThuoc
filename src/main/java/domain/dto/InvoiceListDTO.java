package domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO đại diện cho một Hóa đơn hiển thị trên bảng
 */
public class InvoiceListDTO {
    private int maHD;
    private LocalDateTime ngayBan;
    private String tenKH;
    private String soDT;
    private String tenNV;
    private String phuongThucTT;
    private BigDecimal tongTien;
    private String loaiHD;
    private String trangThai;

    public InvoiceListDTO() {}

    public InvoiceListDTO(int maHD, LocalDateTime ngayBan, String tenKH, String soDT, 
                          String tenNV, String phuongThucTT, BigDecimal tongTien, 
                          String loaiHD, String trangThai) {
        this.maHD = maHD;
        this.ngayBan = ngayBan;
        this.tenKH = tenKH;
        this.soDT = soDT;
        this.tenNV = tenNV;
        this.phuongThucTT = phuongThucTT;
        this.tongTien = tongTien;
        this.loaiHD = loaiHD;
        this.trangThai = trangThai;
    }

    public int getMaHD() { return maHD; }
    public void setMaHD(int maHD) { this.maHD = maHD; }

    public LocalDateTime getNgayBan() { return ngayBan; }
    public void setNgayBan(LocalDateTime ngayBan) { this.ngayBan = ngayBan; }

    public String getTenKH() { return tenKH; }
    public void setTenKH(String tenKH) { this.tenKH = tenKH; }

    public String getSoDT() { return soDT; }
    public void setSoDT(String soDT) { this.soDT = soDT; }

    public String getTenNV() { return tenNV; }
    public void setTenNV(String tenNV) { this.tenNV = tenNV; }

    public String getPhuongThucTT() { return phuongThucTT; }
    public void setPhuongThucTT(String phuongThucTT) { this.phuongThucTT = phuongThucTT; }

    public BigDecimal getTongTien() { return tongTien; }
    public void setTongTien(BigDecimal tongTien) { this.tongTien = tongTien; }

    public String getLoaiHD() { return loaiHD; }
    public void setLoaiHD(String loaiHD) { this.loaiHD = loaiHD; }

    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
}
