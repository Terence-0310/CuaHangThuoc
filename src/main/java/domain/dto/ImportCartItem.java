package domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO: Một dòng trong giỏ nhập kho (Cart)
 * Chứa đủ thông tin SP + Lô để xử lý khi lưu phiếu nhập.
 */
public class ImportCartItem {
    private String tenSP;
    private String donViTinh;
    private BigDecimal giaBan;
    private BigDecimal giaBanSi;
    private int soLuong;
    private String soLo;
    private LocalDate hanSuDung;
    private BigDecimal giaNhap;

    // Resolved after save
    private int maSP;       // -1 nếu chưa có trong DB
    private boolean isNew;  // true nếu SP mới

    public ImportCartItem() { this.maSP = -1; }

    // --- Getters & Setters ---
    public String getTenSP() { return tenSP; }
    public void setTenSP(String tenSP) { this.tenSP = tenSP; }

    public String getDonViTinh() { return donViTinh; }
    public void setDonViTinh(String donViTinh) { this.donViTinh = donViTinh; }

    public BigDecimal getGiaBan() { return giaBan; }
    public void setGiaBan(BigDecimal giaBan) { this.giaBan = giaBan; }

    public BigDecimal getGiaBanSi() { return giaBanSi; }
    public void setGiaBanSi(BigDecimal giaBanSi) { this.giaBanSi = giaBanSi; }

    public int getSoLuong() { return soLuong; }
    public void setSoLuong(int soLuong) { this.soLuong = soLuong; }

    public String getSoLo() { return soLo; }
    public void setSoLo(String soLo) { this.soLo = soLo; }

    public LocalDate getHanSuDung() { return hanSuDung; }
    public void setHanSuDung(LocalDate hanSuDung) { this.hanSuDung = hanSuDung; }

    public BigDecimal getGiaNhap() { return giaNhap; }
    public void setGiaNhap(BigDecimal giaNhap) { this.giaNhap = giaNhap; }

    public int getMaSP() { return maSP; }
    public void setMaSP(int maSP) { this.maSP = maSP; }

    public boolean isNew() { return isNew; }
    public void setNew(boolean isNew) { this.isNew = isNew; }

    /** Giá nhập = tổng tiền cho dòng này (KHÔNG nhân SL) */
    public BigDecimal getTongTienDong() {
        return giaNhap != null ? giaNhap : BigDecimal.ZERO;
    }
}
