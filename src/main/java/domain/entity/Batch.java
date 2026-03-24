package domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity: Lô hàng (Batch) — quản lý theo HSD
 * ⚠️ GiaNhap nằm ở đây (giá vốn theo từng lô nhập)
 */
public class Batch {
    private int maLo;
    private int maSP;
    private String soLo;
    private LocalDate hanSuDung;
    private int soLuong;
    private BigDecimal giaNhap;      // ★ Giá vốn theo lô
    private LocalDateTime ngayNhap;
    private Integer maPN;            // ★ FK → PhieuNhap

    // Transient field (không lưu DB, chỉ dùng khi JOIN)
    private String tenSP;
    private String donViTinh;
    private String tenNguoiNhap;     // Transient: từ PhieuNhap→NguoiDung

    public Batch() {}

    public Batch(int maLo, int maSP, String soLo, LocalDate hanSuDung, int soLuong, BigDecimal giaNhap) {
        this.maLo = maLo;
        this.maSP = maSP;
        this.soLo = soLo;
        this.hanSuDung = hanSuDung;
        this.soLuong = soLuong;
        this.giaNhap = giaNhap;
    }

    // --- Getters & Setters ---
    public int getMaLo() { return maLo; }
    public void setMaLo(int maLo) { this.maLo = maLo; }

    public int getMaSP() { return maSP; }
    public void setMaSP(int maSP) { this.maSP = maSP; }

    public String getSoLo() { return soLo; }
    public void setSoLo(String soLo) { this.soLo = soLo; }

    public LocalDate getHanSuDung() { return hanSuDung; }
    public void setHanSuDung(LocalDate hanSuDung) { this.hanSuDung = hanSuDung; }

    public int getSoLuong() { return soLuong; }
    public void setSoLuong(int soLuong) { this.soLuong = soLuong; }

    public BigDecimal getGiaNhap() { return giaNhap; }
    public void setGiaNhap(BigDecimal giaNhap) { this.giaNhap = giaNhap; }

    public LocalDateTime getNgayNhap() { return ngayNhap; }
    public void setNgayNhap(LocalDateTime ngayNhap) { this.ngayNhap = ngayNhap; }

    public String getTenSP() { return tenSP; }
    public void setTenSP(String tenSP) { this.tenSP = tenSP; }

    public String getDonViTinh() { return donViTinh; }
    public void setDonViTinh(String donViTinh) { this.donViTinh = donViTinh; }

    public Integer getMaPN() { return maPN; }
    public void setMaPN(Integer maPN) { this.maPN = maPN; }

    public String getTenNguoiNhap() { return tenNguoiNhap; }
    public void setTenNguoiNhap(String tenNguoiNhap) { this.tenNguoiNhap = tenNguoiNhap; }

    // Transient: ten nha cung cap (tu PhieuNhap JOIN NhaCungCap)
    private String tenNCC;
    public String getTenNCC() { return tenNCC; }
    public void setTenNCC(String tenNCC) { this.tenNCC = tenNCC; }
}
