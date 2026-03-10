package entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity class cho bảng SanPham
 * 
 * @author Generated
 * @version 1.0
 */
public class SanPham {
	private int maSanPham;
	private String tenSanPham;
	private String donViTinh;
	private BigDecimal giaBanDeXuat;
	private String loaiSanPham; // Thuoc, DuocMiPham, ThucPhamChucNang, ChamSocCaNhan, ThietBiYTe
	private String moTa;
	private int mucTonToiThieu;
	private boolean daXoa;
	private LocalDateTime ngayTao;

	public SanPham() {
	}

	public SanPham(int maSanPham, String tenSanPham, String donViTinh,
			BigDecimal giaBanDeXuat, String loaiSanPham, String moTa,
			int mucTonToiThieu, boolean daXoa, LocalDateTime ngayTao) {
		this.maSanPham = maSanPham;
		this.tenSanPham = tenSanPham;
		this.donViTinh = donViTinh;
		this.giaBanDeXuat = giaBanDeXuat;
		this.loaiSanPham = loaiSanPham;
		this.moTa = moTa;
		this.mucTonToiThieu = mucTonToiThieu;
		this.daXoa = daXoa;
		this.ngayTao = ngayTao;
	}

	// Getters and Setters
	public int getMaSanPham() {
		return maSanPham;
	}

	public void setMaSanPham(int maSanPham) {
		this.maSanPham = maSanPham;
	}

	public String getTenSanPham() {
		return tenSanPham;
	}

	public void setTenSanPham(String tenSanPham) {
		this.tenSanPham = tenSanPham;
	}

	public String getDonViTinh() {
		return donViTinh;
	}

	public void setDonViTinh(String donViTinh) {
		this.donViTinh = donViTinh;
	}

	public BigDecimal getGiaBanDeXuat() {
		return giaBanDeXuat;
	}

	public void setGiaBanDeXuat(BigDecimal giaBanDeXuat) {
		this.giaBanDeXuat = giaBanDeXuat;
	}

	public String getLoaiSanPham() {
		return loaiSanPham;
	}

	public void setLoaiSanPham(String loaiSanPham) {
		this.loaiSanPham = loaiSanPham;
	}

	public String getMoTa() {
		return moTa;
	}

	public void setMoTa(String moTa) {
		this.moTa = moTa;
	}

	public int getMucTonToiThieu() {
		return mucTonToiThieu;
	}

	public void setMucTonToiThieu(int mucTonToiThieu) {
		this.mucTonToiThieu = mucTonToiThieu;
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
		return "SanPham [maSanPham=" + maSanPham
			+ ", tenSanPham=" + tenSanPham
			+ ", loaiSanPham=" + loaiSanPham + "]";
	}
}
