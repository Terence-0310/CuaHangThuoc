package entity;

import java.time.LocalDateTime;

/**
 * Entity class cho bảng NguoiDung
 * 
 * @author Generated
 * @version 1.0
 */
public class NguoiDung {
	private int maNguoiDung;
	private String tenDangNhap;
	private String matKhau;
	private String vaiTro; // Admin, QuanLy, NhanVien
	private String hoTen;
	private String email;
	private String soDienThoai;
	private boolean daXoa;
	private LocalDateTime ngayTao;

	public NguoiDung() {
	}

	public NguoiDung(int maNguoiDung, String tenDangNhap, String matKhau,
			String vaiTro, String hoTen, String email, String soDienThoai,
			boolean daXoa, LocalDateTime ngayTao) {
		this.maNguoiDung = maNguoiDung;
		this.tenDangNhap = tenDangNhap;
		this.matKhau = matKhau;
		this.vaiTro = vaiTro;
		this.hoTen = hoTen;
		this.email = email;
		this.soDienThoai = soDienThoai;
		this.daXoa = daXoa;
		this.ngayTao = ngayTao;
	}

	// Getters and Setters
	public int getMaNguoiDung() {
		return maNguoiDung;
	}

	public void setMaNguoiDung(int maNguoiDung) {
		this.maNguoiDung = maNguoiDung;
	}

	public String getTenDangNhap() {
		return tenDangNhap;
	}

	public void setTenDangNhap(String tenDangNhap) {
		this.tenDangNhap = tenDangNhap;
	}

	public String getMatKhau() {
		return matKhau;
	}

	public void setMatKhau(String matKhau) {
		this.matKhau = matKhau;
	}

	public String getVaiTro() {
		return vaiTro;
	}

	public void setVaiTro(String vaiTro) {
		this.vaiTro = vaiTro;
	}

	public String getHoTen() {
		return hoTen;
	}

	public void setHoTen(String hoTen) {
		this.hoTen = hoTen;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getSoDienThoai() {
		return soDienThoai;
	}

	public void setSoDienThoai(String soDienThoai) {
		this.soDienThoai = soDienThoai;
	}

	public boolean isDaXoa() {
		return daXoa;
	}

	public void setDaXoa(boolean daXoa) {
		this.daXoa = daXoa;
	}

	public LocalDateTime getNgayTao() {
		return ngayTao;
	}

	public void setNgayTao(LocalDateTime ngayTao) {
		this.ngayTao = ngayTao;
	}

	@Override
	public String toString() {
		return "NguoiDung [maNguoiDung=" + maNguoiDung
			+ ", tenDangNhap=" + tenDangNhap
			+ ", vaiTro=" + vaiTro + ", hoTen=" + hoTen + "]";
	}
}
