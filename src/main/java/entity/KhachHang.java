package entity;

import java.time.LocalDateTime;

/**
 * Entity class cho bảng KhachHang
 * 
 * @author Generated
 * @version 1.0
 */
public class KhachHang {
	private int maKhachHang;
	private String hoTen;
	private String soDienThoai;
	private String email;
	private String diaChi;
	private String hoSoBenhAn;
	private boolean daXoa;
	private LocalDateTime ngayTao;

	public KhachHang() {
	}

	public KhachHang(int maKhachHang, String hoTen, String soDienThoai,
			String email, String diaChi, String hoSoBenhAn,
			boolean daXoa, LocalDateTime ngayTao) {
		this.maKhachHang = maKhachHang;
		this.hoTen = hoTen;
		this.soDienThoai = soDienThoai;
		this.email = email;
		this.diaChi = diaChi;
		this.hoSoBenhAn = hoSoBenhAn;
		this.daXoa = daXoa;
		this.ngayTao = ngayTao;
	}

	// Getters and Setters
	public int getMaKhachHang() {
		return maKhachHang;
	}

	public void setMaKhachHang(int maKhachHang) {
		this.maKhachHang = maKhachHang;
	}

	public String getHoTen() {
		return hoTen;
	}

	public void setHoTen(String hoTen) {
		this.hoTen = hoTen;
	}

	public String getSoDienThoai() {
		return soDienThoai;
	}

	public void setSoDienThoai(String soDienThoai) {
		this.soDienThoai = soDienThoai;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getDiaChi() {
		return diaChi;
	}

	public void setDiaChi(String diaChi) {
		this.diaChi = diaChi;
	}

	public String getHoSoBenhAn() {
		return hoSoBenhAn;
	}

	public void setHoSoBenhAn(String hoSoBenhAn) {
		this.hoSoBenhAn = hoSoBenhAn;
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
		return "KhachHang [maKhachHang=" + maKhachHang
			+ ", hoTen=" + hoTen + ", soDienThoai=" + soDienThoai + "]";
	}
}
