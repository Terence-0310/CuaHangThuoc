package domain.dto;

import java.math.BigDecimal;

/**
 * DTO: Top sản phẩm bán chạy
 */
public class TopSellingDTO {
    private String tenSP;
    private String donViTinh;
    private int tongSoLuongBan;
    private BigDecimal tongDoanhThu;

    public TopSellingDTO() {}

    public TopSellingDTO(String tenSP, String donViTinh, int tongSoLuongBan, BigDecimal tongDoanhThu) {
        this.tenSP = tenSP;
        this.donViTinh = donViTinh;
        this.tongSoLuongBan = tongSoLuongBan;
        this.tongDoanhThu = tongDoanhThu;
    }

    public String getTenSP() { return tenSP; }
    public void setTenSP(String tenSP) { this.tenSP = tenSP; }

    public String getDonViTinh() { return donViTinh; }
    public void setDonViTinh(String donViTinh) { this.donViTinh = donViTinh; }

    public int getTongSoLuongBan() { return tongSoLuongBan; }
    public void setTongSoLuongBan(int tongSoLuongBan) { this.tongSoLuongBan = tongSoLuongBan; }

    public BigDecimal getTongDoanhThu() { return tongDoanhThu; }
    public void setTongDoanhThu(BigDecimal tongDoanhThu) { this.tongDoanhThu = tongDoanhThu; }
}
