package entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity class cho bảng PhieuNhap
 * 
 * @author Generated
 * @version 1.0
 */
public class PhieuNhap {
	private int maPhieuNhap;
	private int maNguoiDung;
	private Integer maNCC;
	private BigDecimal tongTien;
	private LocalDateTime ngayNhap;
	private String ghiChu;

	public PhieuNhap() {
	}

	public PhieuNhap(int maPhieuNhap, int maNguoiDung, Integer maNCC,
			BigDecimal tongTien, LocalDateTime ngayNhap, String ghiChu) {
		this.maPhieuNhap = maPhieuNhap;
		this.maNguoiDung = maNguoiDung;
		this.maNCC = maNCC;
		this.tongTien = tongTien;
		this.ngayNhap = ngayNhap;
		this.ghiChu = ghiChu;
	}

	// Getters and Setters
	public int getMaPhieuNhap() {
		return maPhieuNhap;
	}

	public void setMaPhieuNhap(int maPhieuNhap) {
		this.maPhieuNhap = maPhieuNhap;
	}

	public int getMaNguoiDung() {
		return maNguoiDung;
	}

	public void setMaNguoiDung(int maNguoiDung) {
		this.maNguoiDung = maNguoiDung;
	}

	public Integer getMaNCC() {
		return maNCC;
	}

	public void setMaNCC(Integer maNCC) {
		this.maNCC = maNCC;
	}

	public BigDecimal getTongTien() {
		return tongTien;
	}

	public void setTongTien(BigDecimal tongTien) {
		this.tongTien = tongTien;
	}

	public LocalDateTime getNgayNhap() {
		return ngayNhap;
	}

	public void setNgayNhap(LocalDateTime ngayNhap) {
		this.ngayNhap = ngayNhap;
	}

	public String getGhiChu() {
		return ghiChu;
	}

	public void setGhiChu(String ghiChu) {
		this.ghiChu = ghiChu;
	}

	@Override
	public String toString() {
		return "PhieuNhap [maPhieuNhap=" + maPhieuNhap
			+ ", tongTien=" + tongTien + ", ngayNhap=" + ngayNhap + "]";
	}
}
