package domain.entity;

/**
 * Entity: Người dùng hệ thống (Admin / Nhân viên)
 */
public class User {
    private int maND;
    private String tenDangNhap;
    private String matKhau;
    private String hoTen;
    private String vaiTro;
    private boolean trangThai;

    public User() {}

    public User(int maND, String tenDangNhap, String matKhau, String hoTen, String vaiTro, boolean trangThai) {
        this.maND = maND;
        this.tenDangNhap = tenDangNhap;
        this.matKhau = matKhau;
        this.hoTen = hoTen;
        this.vaiTro = vaiTro;
        this.trangThai = trangThai;
    }

    public boolean isAdmin() {
        return "Admin".equalsIgnoreCase(vaiTro);
    }

    // --- Getters & Setters ---
    public int getMaND() { return maND; }
    public void setMaND(int maND) { this.maND = maND; }

    public String getTenDangNhap() { return tenDangNhap; }
    public void setTenDangNhap(String tenDangNhap) { this.tenDangNhap = tenDangNhap; }

    public String getMatKhau() { return matKhau; }
    public void setMatKhau(String matKhau) { this.matKhau = matKhau; }

    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }

    public String getVaiTro() { return vaiTro; }
    public void setVaiTro(String vaiTro) { this.vaiTro = vaiTro; }

    public boolean isTrangThai() { return trangThai; }
    public void setTrangThai(boolean trangThai) { this.trangThai = trangThai; }
}
