package domain.entity;

import java.math.BigDecimal;

/**
 * Entity: Sản phẩm (Thuốc)
 * ⚠️ GiaNhap đã dời sang LoHang (giá vốn theo lô, không theo sản phẩm)
 *    SanPham chỉ giữ GiaBan (giá niêm yết hiện tại)
 */
public class Product {
    private int maSP;
    private String tenSP;
    private String donViTinh;
    private BigDecimal giaBan;
    private boolean trangThai;

    public Product() {}

    public Product(int maSP, String tenSP, String donViTinh, BigDecimal giaBan, boolean trangThai) {
        this.maSP = maSP;
        this.tenSP = tenSP;
        this.donViTinh = donViTinh;
        this.giaBan = giaBan;
        this.trangThai = trangThai;
    }

    // --- Getters & Setters ---
    public int getMaSP() { return maSP; }
    public void setMaSP(int maSP) { this.maSP = maSP; }

    public String getTenSP() { return tenSP; }
    public void setTenSP(String tenSP) { this.tenSP = tenSP; }

    public String getDonViTinh() { return donViTinh; }
    public void setDonViTinh(String donViTinh) { this.donViTinh = donViTinh; }

    public BigDecimal getGiaBan() { return giaBan; }
    public void setGiaBan(BigDecimal giaBan) { this.giaBan = giaBan; }



    public boolean isTrangThai() { return trangThai; }
    public void setTrangThai(boolean trangThai) { this.trangThai = trangThai; }

    // Transient: tồn kho (lấy từ vw_TonKhoTheoSanPham)
    private int tongTonKho;
    public int getTongTonKho() { return tongTonKho; }
    public void setTongTonKho(int tongTonKho) { this.tongTonKho = tongTonKho; }
}
