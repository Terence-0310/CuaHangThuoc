package domain.dto;

/**
 * DTO: Cảnh báo tồn kho
 */
public class StockAlertDTO {
    private int maSP;
    private String tenSP;
    private int tongTon;
    private int soLo;
    private String trangThai;

    public StockAlertDTO() {}

    public StockAlertDTO(int maSP, String tenSP, int tongTon, int soLo, String trangThai) {
        this.maSP = maSP;
        this.tenSP = tenSP;
        this.tongTon = tongTon;
        this.soLo = soLo;
        this.trangThai = trangThai;
    }

    public int getMaSP() { return maSP; }
    public void setMaSP(int maSP) { this.maSP = maSP; }

    public String getTenSP() { return tenSP; }
    public void setTenSP(String tenSP) { this.tenSP = tenSP; }

    public int getTongTon() { return tongTon; }
    public void setTongTon(int tongTon) { this.tongTon = tongTon; }

    public int getSoLo() { return soLo; }
    public void setSoLo(int soLo) { this.soLo = soLo; }

    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
}
