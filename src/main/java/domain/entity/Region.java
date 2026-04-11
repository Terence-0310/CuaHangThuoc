package domain.entity;

import java.time.LocalDateTime;

public class Region {
    private int maKhuVuc;
    private String maKhuVucCode;
    private String tenKhuVuc;
    private String tinhThanh;
    private boolean trangThai;
    private LocalDateTime ngayTao;

    public int getMaKhuVuc() {
        return maKhuVuc;
    }

    public void setMaKhuVuc(int maKhuVuc) {
        this.maKhuVuc = maKhuVuc;
    }

    public String getMaKhuVucCode() {
        return maKhuVucCode;
    }

    public void setMaKhuVucCode(String maKhuVucCode) {
        this.maKhuVucCode = maKhuVucCode;
    }

    public String getTenKhuVuc() {
        return tenKhuVuc;
    }

    public void setTenKhuVuc(String tenKhuVuc) {
        this.tenKhuVuc = tenKhuVuc;
    }

    public String getTinhThanh() {
        return tinhThanh;
    }

    public void setTinhThanh(String tinhThanh) {
        this.tinhThanh = tinhThanh;
    }

    public boolean isTrangThai() {
        return trangThai;
    }

    public void setTrangThai(boolean trangThai) {
        this.trangThai = trangThai;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }
}
