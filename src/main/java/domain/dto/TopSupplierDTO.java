package domain.dto;

import java.math.BigDecimal;

/**
 * DTO: Top nhà cung cấp
 */
public class TopSupplierDTO {
    private String tenNCC;
    private String soDT;
    private int soPhieuNhap;
    private BigDecimal tongTienNhap;

    public TopSupplierDTO() {}

    public TopSupplierDTO(String tenNCC, String soDT, int soPhieuNhap, BigDecimal tongTienNhap) {
        this.tenNCC = tenNCC;
        this.soDT = soDT;
        this.soPhieuNhap = soPhieuNhap;
        this.tongTienNhap = tongTienNhap;
    }

    public String getTenNCC() { return tenNCC; }
    public void setTenNCC(String tenNCC) { this.tenNCC = tenNCC; }

    public String getSoDT() { return soDT; }
    public void setSoDT(String soDT) { this.soDT = soDT; }

    public int getSoPhieuNhap() { return soPhieuNhap; }
    public void setSoPhieuNhap(int soPhieuNhap) { this.soPhieuNhap = soPhieuNhap; }

    public BigDecimal getTongTienNhap() { return tongTienNhap; }
    public void setTongTienNhap(BigDecimal tongTienNhap) { this.tongTienNhap = tongTienNhap; }
}
