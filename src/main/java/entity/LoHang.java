package entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity class cho bảng LoHang
 * 
 * @author Generated
 * @version 1.0
 */
public class LoHang {
	private int maLoHang;
	private int maSanPham;
	private String soLo;
	private Integer maNCC;
	private Integer maPhieuNhap;
	private LocalDate ngaySanXuat;
	private LocalDate hanSuDung;
	private BigDecimal giaNhap;
	private int soLuongNhap;
	private int soLuongTon;
	private LocalDateTime ngayNhap;
	private String trangThai; // Đang bán, Ngưng bán, Hết hàng

	public LoHang() {
	}

	public LoHang(int maLoHang, int maSanPham, String soLo,
			Integer maNCC, Integer maPhieuNhap, LocalDate ngaySanXuat,
			LocalDate hanSuDung, BigDecimal giaNhap, int soLuongNhap,
			int soLuongTon, LocalDateTime ngayNhap, String trangThai) {
		this.maLoHang = maLoHang;
		this.maSanPham = maSanPham;
		this.soLo = soLo;
		this.maNCC = maNCC;
		this.maPhieuNhap = maPhieuNhap;
		this.ngaySanXuat = ngaySanXuat;
		this.hanSuDung = hanSuDung;
		this.giaNhap = giaNhap;
		this.soLuongNhap = soLuongNhap;
		this.soLuongTon = soLuongTon;
		this.ngayNhap = ngayNhap;
		this.trangThai = trangThai;
	}

	// Getters and Setters
	public int getMaLoHang() {
		return maLoHang;
	}

	public void setMaLoHang(int maLoHang) {
		this.maLoHang = maLoHang;
	}

	public int getMaSanPham() {
		return maSanPham;
	}

	public void setMaSanPham(int maSanPham) {
		this.maSanPham = maSanPham;
	}

	public String getSoLo() {
		return soLo;
	}

	public void setSoLo(String soLo) {
		this.soLo = soLo;
	}

	public Integer getMaNCC() {
		return maNCC;
	}

	public void setMaNCC(Integer maNCC) {
		this.maNCC = maNCC;
	}

	public Integer getMaPhieuNhap() {
		return maPhieuNhap;
	}

	public void setMaPhieuNhap(Integer maPhieuNhap) {
		this.maPhieuNhap = maPhieuNhap;
	}

	public LocalDate getNgaySanXuat() {
		return ngaySanXuat;
	}

	public void setNgaySanXuat(LocalDate ngaySanXuat) {
		this.ngaySanXuat = ngaySanXuat;
	}

	public LocalDate getHanSuDung() {
		return hanSuDung;
	}

	public void setHanSuDung(LocalDate hanSuDung) {
		this.hanSuDung = hanSuDung;
	}

	public BigDecimal getGiaNhap() {
		return giaNhap;
	}

	public void setGiaNhap(BigDecimal giaNhap) {
		this.giaNhap = giaNhap;
	}

	public int getSoLuongNhap() {
		return soLuongNhap;
	}

	public void setSoLuongNhap(int soLuongNhap) {
		this.soLuongNhap = soLuongNhap;
	}

	public int getSoLuongTon() {
		return soLuongTon;
	}

	public void setSoLuongTon(int soLuongTon) {
		this.soLuongTon = soLuongTon;
	}

	public LocalDateTime getNgayNhap() {
		return ngayNhap;
	}

	public void setNgayNhap(LocalDateTime ngayNhap) {
		this.ngayNhap = ngayNhap;
	}

	public String getTrangThai() {
		return trangThai;
	}

	public void setTrangThai(String trangThai) {
		this.trangThai = trangThai;
	}

	@Override
	public String toString() {
		return "LoHang [maLoHang=" + maLoHang
			+ ", maSanPham=" + maSanPham
			+ ", soLo=" + soLo + ", hanSuDung=" + hanSuDung + "]";
	}
}
