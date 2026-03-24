package domain.dto;

import java.math.BigDecimal;

/**
 * DTO: Top khách hàng VIP
 */
public class TopCustomerDTO {
    private String tenKH;
    private String soDT;
    private int soLanMua;
    private BigDecimal tongTienMua;

    public TopCustomerDTO() {}

    public TopCustomerDTO(String tenKH, String soDT, int soLanMua, BigDecimal tongTienMua) {
        this.tenKH = tenKH;
        this.soDT = soDT;
        this.soLanMua = soLanMua;
        this.tongTienMua = tongTienMua;
    }

    public String getTenKH() { return tenKH; }
    public void setTenKH(String tenKH) { this.tenKH = tenKH; }

    public String getSoDT() { return soDT; }
    public void setSoDT(String soDT) { this.soDT = soDT; }

    public int getSoLanMua() { return soLanMua; }
    public void setSoLanMua(int soLanMua) { this.soLanMua = soLanMua; }

    public BigDecimal getTongTienMua() { return tongTienMua; }
    public void setTongTienMua(BigDecimal tongTienMua) { this.tongTienMua = tongTienMua; }
}
