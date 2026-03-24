package domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity: Phiếu nhập hàng (Import Receipt)
 * Quản lý dòng tiền chi ra cho NCC
 * 1 PhieuNhap → N LoHang
 */
public class ImportReceipt {
    private int maPN;
    private int maND;              // Người tạo phiếu
    private Integer maNCC;         // ★ Nhà cung cấp
    private LocalDateTime ngayNhap;
    private BigDecimal tongTien;
    private String ghiChu;

    // Transient
    private String tenNguoiNhap;
    private String tenNCC;         // ★ Transient: tên NCC

    public ImportReceipt() {}

    public ImportReceipt(int maND, String ghiChu) {
        this.maND = maND;
        this.tongTien = BigDecimal.ZERO;
        this.ghiChu = ghiChu;
    }

    // --- Getters & Setters ---
    public int getMaPN() { return maPN; }
    public void setMaPN(int maPN) { this.maPN = maPN; }

    public int getMaND() { return maND; }
    public void setMaND(int maND) { this.maND = maND; }

    public LocalDateTime getNgayNhap() { return ngayNhap; }
    public void setNgayNhap(LocalDateTime ngayNhap) { this.ngayNhap = ngayNhap; }

    public BigDecimal getTongTien() { return tongTien; }
    public void setTongTien(BigDecimal tongTien) { this.tongTien = tongTien; }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    public String getTenNguoiNhap() { return tenNguoiNhap; }
    public void setTenNguoiNhap(String tenNguoiNhap) { this.tenNguoiNhap = tenNguoiNhap; }

    public Integer getMaNCC() { return maNCC; }
    public void setMaNCC(Integer maNCC) { this.maNCC = maNCC; }

    public String getTenNCC() { return tenNCC; }
    public void setTenNCC(String tenNCC) { this.tenNCC = tenNCC; }
}
