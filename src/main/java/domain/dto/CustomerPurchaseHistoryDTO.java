package domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CustomerPurchaseHistoryDTO {
    private int maHD;
    private LocalDateTime ngayBan;
    private BigDecimal tongTien;
    private int soSP;
    private String nhanVien;

    public CustomerPurchaseHistoryDTO() {}

    public CustomerPurchaseHistoryDTO(int maHD, LocalDateTime ngayBan, BigDecimal tongTien, int soSP, String nhanVien) {
        this.maHD = maHD;
        this.ngayBan = ngayBan;
        this.tongTien = tongTien;
        this.soSP = soSP;
        this.nhanVien = nhanVien;
    }

    public int getMaHD() { return maHD; }
    public void setMaHD(int maHD) { this.maHD = maHD; }

    public LocalDateTime getNgayBan() { return ngayBan; }
    public void setNgayBan(LocalDateTime ngayBan) { this.ngayBan = ngayBan; }

    public BigDecimal getTongTien() { return tongTien; }
    public void setTongTien(BigDecimal tongTien) { this.tongTien = tongTien; }

    public int getSoSP() { return soSP; }
    public void setSoSP(int soSP) { this.soSP = soSP; }

    public String getNhanVien() { return nhanVien; }
    public void setNhanVien(String nhanVien) { this.nhanVien = nhanVien; }
}
