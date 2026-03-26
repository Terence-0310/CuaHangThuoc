package domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class InventoryBatchDTO {
    private int maLo;
    private String soLo;
    private String tenSP;
    private String donViTinh;
    private int soLuong;
    private BigDecimal giaNhap;
    private LocalDate hanSuDung;
    private LocalDateTime ngayNhap;
    private String nguoiNhap;
    private String tenNCC;
    private String status;

    public int getMaLo() { return maLo; }
    public void setMaLo(int maLo) { this.maLo = maLo; }

    public String getSoLo() { return soLo; }
    public void setSoLo(String soLo) { this.soLo = soLo; }

    public String getTenSP() { return tenSP; }
    public void setTenSP(String tenSP) { this.tenSP = tenSP; }

    public String getDonViTinh() { return donViTinh; }
    public void setDonViTinh(String donViTinh) { this.donViTinh = donViTinh; }

    public int getSoLuong() { return soLuong; }
    public void setSoLuong(int soLuong) { this.soLuong = soLuong; }

    public BigDecimal getGiaNhap() { return giaNhap; }
    public void setGiaNhap(BigDecimal giaNhap) { this.giaNhap = giaNhap; }

    public LocalDate getHanSuDung() { return hanSuDung; }
    public void setHanSuDung(LocalDate hanSuDung) { this.hanSuDung = hanSuDung; }

    public LocalDateTime getNgayNhap() { return ngayNhap; }
    public void setNgayNhap(LocalDateTime ngayNhap) { this.ngayNhap = ngayNhap; }

    public String getNguoiNhap() { return nguoiNhap; }
    public void setNguoiNhap(String nguoiNhap) { this.nguoiNhap = nguoiNhap; }

    public String getTenNCC() { return tenNCC; }
    public void setTenNCC(String tenNCC) { this.tenNCC = tenNCC; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
