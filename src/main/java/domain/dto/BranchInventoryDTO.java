package domain.dto;

public class BranchInventoryDTO {
    private int maCN;
    private int maSP;
    private int tonHienTai;
    private int mucTonToiThieu;
    private int mucTonMucTieu;
    private int soNgayLeadTime;
    private int soNgayTonAnToan;
    private String tenSanPham;
    private String tenChiNhanh;

    public int getMaCN() {
        return maCN;
    }

    public void setMaCN(int maCN) {
        this.maCN = maCN;
    }

    public int getMaSP() {
        return maSP;
    }

    public void setMaSP(int maSP) {
        this.maSP = maSP;
    }

    public int getTonHienTai() {
        return tonHienTai;
    }

    public void setTonHienTai(int tonHienTai) {
        this.tonHienTai = tonHienTai;
    }

    public int getMucTonToiThieu() {
        return mucTonToiThieu;
    }

    public void setMucTonToiThieu(int mucTonToiThieu) {
        this.mucTonToiThieu = mucTonToiThieu;
    }

    public int getMucTonMucTieu() {
        return mucTonMucTieu;
    }

    public void setMucTonMucTieu(int mucTonMucTieu) {
        this.mucTonMucTieu = mucTonMucTieu;
    }

    public int getSoNgayLeadTime() {
        return soNgayLeadTime;
    }

    public void setSoNgayLeadTime(int soNgayLeadTime) {
        this.soNgayLeadTime = soNgayLeadTime;
    }

    public int getSoNgayTonAnToan() {
        return soNgayTonAnToan;
    }

    public void setSoNgayTonAnToan(int soNgayTonAnToan) {
        this.soNgayTonAnToan = soNgayTonAnToan;
    }

    public String getTenSanPham() {
        return tenSanPham;
    }

    public void setTenSanPham(String tenSanPham) {
        this.tenSanPham = tenSanPham;
    }

    public String getTenChiNhanh() {
        return tenChiNhanh;
    }

    public void setTenChiNhanh(String tenChiNhanh) {
        this.tenChiNhanh = tenChiNhanh;
    }
}
