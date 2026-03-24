package domain.dto;

import java.math.BigDecimal;

/**
 * DTO: Item trong giỏ hàng (không phải entity DB)
 */
public class CartItem {
    private int maSP;
    private String tenSP;
    private String donViTinh;
    private int soLuong;
    private BigDecimal giaBan;

    public CartItem() {}

    public CartItem(int maSP, String tenSP, String donViTinh, int soLuong, BigDecimal giaBan) {
        this.maSP = maSP;
        this.tenSP = tenSP;
        this.donViTinh = donViTinh;
        this.soLuong = soLuong;
        this.giaBan = giaBan;
    }

    public BigDecimal getThanhTien() {
        return giaBan.multiply(BigDecimal.valueOf(soLuong));
    }

    // --- Getters & Setters ---
    public int getMaSP() { return maSP; }
    public void setMaSP(int maSP) { this.maSP = maSP; }

    public String getTenSP() { return tenSP; }
    public void setTenSP(String tenSP) { this.tenSP = tenSP; }

    public String getDonViTinh() { return donViTinh; }
    public void setDonViTinh(String donViTinh) { this.donViTinh = donViTinh; }

    public int getSoLuong() { return soLuong; }
    public void setSoLuong(int soLuong) { this.soLuong = soLuong; }

    public BigDecimal getGiaBan() { return giaBan; }
    public void setGiaBan(BigDecimal giaBan) { this.giaBan = giaBan; }
}
