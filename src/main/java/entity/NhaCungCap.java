package entity;

import java.time.LocalDateTime;

/**
 * Entity class cho bảng NhaCungCap
 * 
 * @author Generated
 * @version 1.0
 */
public class NhaCungCap {
	private int maNCC;
	private String tenNCC;
	private String soDienThoai;
	private String email;
	private String diaChi;
	private boolean daXoa;
	private LocalDateTime ngayTao;

	public NhaCungCap() {
	}

	public NhaCungCap(int maNCC, String tenNCC, String soDienThoai,
			String email, String diaChi, boolean daXoa, LocalDateTime ngayTao) {
		this.maNCC = maNCC;
		this.tenNCC = tenNCC;
		this.soDienThoai = soDienThoai;
		this.email = email;
		this.diaChi = diaChi;
		this.daXoa = daXoa;
		this.ngayTao = ngayTao;
	}

	// Getters and Setters
	public int getMaNCC() {
		return maNCC;
	}

	public void setMaNCC(int maNCC) {
		this.maNCC = maNCC;
	}

	public String getTenNCC() {
		return tenNCC;
	}

	public void setTenNCC(String tenNCC) {
		this.tenNCC = tenNCC;
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
		return "NhaCungCap [maNCC=" + maNCC
			+ ", tenNCC=" + tenNCC + ", soDienThoai=" + soDienThoai + "]";
	}
}
