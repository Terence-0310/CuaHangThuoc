package domain.dto;

/**
 * DTO: Cảnh báo tồn kho — THEO LÔ (batch-level)
 * Mỗi dòng là 1 lô cần cảnh báo (hết hàng, sắp hết, cận date)
 */
public class StockAlertDTO {
    private int maSP;
    private String tenSP;
    private int tongTon;       // Tồn kho TOÀN SP (tổng tất cả lô)
    private int soLo;          // Mã lô cụ thể (0 nếu level=SP)
    private String trangThai;  // "Hết hàng", "Sắp hết", "Cận date"
    private String soLoStr;    // Số lô (chuỗi hiển thị — VD: "LO-00123")
    private int soLuongLo;     // Số lượng CÒN LẠI trong lô này
    private String hanSuDung;  // Hạn sử dụng (dd/MM/yyyy)

    public StockAlertDTO() {}

    // Constructor cũ (backward compat)
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

    public String getSoLoStr() { return soLoStr; }
    public void setSoLoStr(String soLoStr) { this.soLoStr = soLoStr; }

    public int getSoLuongLo() { return soLuongLo; }
    public void setSoLuongLo(int soLuongLo) { this.soLuongLo = soLuongLo; }

    public String getHanSuDung() { return hanSuDung; }
    public void setHanSuDung(String hanSuDung) { this.hanSuDung = hanSuDung; }
}
