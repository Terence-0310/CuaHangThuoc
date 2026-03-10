package entity;

import java.math.BigDecimal;

/**
 * Entity class cho bảng ChiTietHoaDon
 * 
 * @author Generated
 * @version 1.0
 */
public class ChiTietHoaDon {
	private int maCTHD;
	private int maHoaDon;
	private int maLoHang;
	private int maSanPham;
	private int soLuong;
	private BigDecimal giaBan;
	private BigDecimal thanhTien;

	public ChiTietHoaDon() {
	}

	public ChiTietHoaDon(int maCTHD, int maHoaDon, int maLoHang,
			int maSanPham, int soLuong, BigDecimal giaBan, BigDecimal thanhTien) {
		this.maCTHD = maCTHD;
		this.maHoaDon = maHoaDon;
		this.maLoHang = maLoHang;
		this.maSanPham = maSanPham;
		this.soLuong = soLuong;
		this.giaBan = giaBan;
		this.thanhTien = thanhTien;
	}

	// Getters and Setters
	public int getMaCTHD() {
		return maCTHD;
	}

	public void setMaCTHD(int maCTHD) {
		this.maCTHD = maCTHD;
	}

	public int getMaHoaDon() {
		return maHoaDon;
	}

	public void setMaHoaDon(int maHoaDon) {
		this.maHoaDon = maHoaDon;
	}

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

	public int getSoLuong() {
		return soLuong;
	}

	public void setSoLuong(int soLuong) {
		this.soLuong = soLuong;
	}

	public BigDecimal getGiaBan() {
		return giaBan;
	}

	public void setGiaBan(BigDecimal giaBan) {
		this.giaBan = giaBan;
	}

	public BigDecimal getThanhTien() {
		return thanhTien;
	}

	public void setThanhTien(BigDecimal thanhTien) {
		this.thanhTien = thanhTien;
	}

	@Override
	public String toString() {
		return "ChiTietHoaDon [maCTHD=" + maCTHD
			+ ", maHoaDon=" + maHoaDon
			+ ", maSanPham=" + maSanPham + ", soLuong=" + soLuong + "]";
	}
}
