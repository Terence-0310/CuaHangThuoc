package domain.dto;

import domain.entity.User;
import java.time.LocalDate;

public class UserListDTO extends User {
    private boolean dangOnline;
    private LocalDate ngayTao;

    public UserListDTO() {}

    public UserListDTO(int maND, String tenDangNhap, String matKhau, String hoTen, String vaiTro, boolean trangThai, boolean dangOnline, LocalDate ngayTao) {
        super(maND, tenDangNhap, matKhau, hoTen, vaiTro, trangThai);
        this.dangOnline = dangOnline;
        this.ngayTao = ngayTao;
    }

    public boolean isDangOnline() {
        return dangOnline;
    }

    public void setDangOnline(boolean dangOnline) {
        this.dangOnline = dangOnline;
    }

    public LocalDate getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDate ngayTao) {
        this.ngayTao = ngayTao;
    }
}
