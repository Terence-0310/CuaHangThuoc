package entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity class cho bảng HoaDonBan
 * 
 * @author Generated
 * @version 1.0
 */
public class HoaDonBan {
	private int maHoaDon;
	private int maNguoiDung;
	private Integer maKhachHang;
	private BigDecimal tongTien;
	private LocalDateTime ngayBan;
	private String ghiChu;

	public HoaDonBan() {
	}

	public HoaDonBan(int maHoaDon, int maNguoiDung, Integer maKhachHang,
			BigDecimal tongTien, LocalDateTime ngayBan, String ghiChu) {
		this.maHoaDon = maHoaDon;
		this.maNguoiDung = maNguoiDung;
		this.maKhachHang = maKhachHang;
		this.tongTien = tongTien;
		this.ngayBan = ngayBan;
		this.ghiChu = ghiChu;
	}

	// Getters and Setters
	public int getMaHoaDon() {
		return maHoaDon;
	}

	public void setMaHoaDon(int maHoaDon) {
		this.maHoaDon = maHoaDon;
	}

	public int getMaNguoiDung() {
		return maNguoiDung;
	}

	public void setMaNguoiDung(int maNguoiDung) {
		this.maNguoiDung = maNguoiDung;
	}

	public Integer getMaKhachHang() {
		return maKhachHang;
	}

	public void setMaKhachHang(Integer maKhachHang) {
		this.maKhachHang = maKhachHang;
	}

	public BigDecimal getTongTien() {
		return tongTien;
	}

	public void setTongTien(BigDecimal tongTien) {
		this.tongTien = tongTien;
	}

	public LocalDateTime getNgayBan() {
		return ngayBan;
	}

	public void setNgayBan(LocalDateTime ngayBan) {
		this.ngayBan = ngayBan;
	}

	public String getGhiChu() {
		return ghiChu;
	}

	public void setGhiChu(String ghiChu) {
		this.ghiChu = ghiChu;
	}

	@Override
	public String toString() {
		return "HoaDonBan [maHoaDon=" + maHoaDon
			+ ", tongTien=" + tongTien + ", ngayBan=" + ngayBan + "]";
	}
}
