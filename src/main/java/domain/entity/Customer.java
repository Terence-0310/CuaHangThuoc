package domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity: Khách hàng
 */
public class Customer {
    private int maKH;
    private String soDT;
    private String tenKH;
    private String gioiTinh;        // Nam / Nữ / Khác
    private BigDecimal tongMua;     // Tổng tiền đã mua (computed từ HoaDon)
    private LocalDateTime ngayTao;

    public Customer() {}

    // --- Getters & Setters ---
    public int getMaKH() { return maKH; }
    public void setMaKH(int maKH) { this.maKH = maKH; }

    public String getSoDT() { return soDT; }
    public void setSoDT(String soDT) { this.soDT = soDT; }

    public String getTenKH() { return tenKH; }
    public void setTenKH(String tenKH) { this.tenKH = tenKH; }

    public String getGioiTinh() { return gioiTinh; }
    public void setGioiTinh(String gioiTinh) { this.gioiTinh = gioiTinh; }

    public BigDecimal getTongMua() { return tongMua; }
    public void setTongMua(BigDecimal tongMua) { this.tongMua = tongMua; }

    public LocalDateTime getNgayTao() { return ngayTao; }
    public void setNgayTao(LocalDateTime ngayTao) { this.ngayTao = ngayTao; }
}
