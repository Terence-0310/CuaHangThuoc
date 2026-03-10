package dao;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import common.ConnectDB;
import entity.ChiTietPhieuNhap;

/**
 * DAO class cho ChiTietPhieuNhap
 * 
 * @author Generated
 * @version 1.0
 */
public class ChiTietPhieuNhapDao {

	/**
	 * Lấy chi tiết phiếu nhập theo mã phiếu nhập
	 */
	public List<ChiTietPhieuNhap> getByMaPhieuNhap(int maPhieuNhap) {
		List<ChiTietPhieuNhap> list = new ArrayList<>();
		try (
			var con = ConnectDB.getCon();
			var ps = con.prepareStatement(
				"SELECT ctpn.*, sp.TenSanPham " +
				"FROM ChiTietPhieuNhap ctpn " +
				"JOIN SanPham sp ON sp.MaSanPham = ctpn.MaSanPham " +
				"WHERE ctpn.MaPhieuNhap = ? " +
				"ORDER BY ctpn.MaCTPN"
			);
		) {
			ps.setInt(1, maPhieuNhap);
			var rs = ps.executeQuery();
			while (rs.next()) {
				list.add(mapResultSet(rs));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * Map ResultSet to ChiTietPhieuNhap entity
	 */
	private ChiTietPhieuNhap mapResultSet(ResultSet rs) throws Exception {
		var ctpn = new ChiTietPhieuNhap();
		ctpn.setMaCTPN(rs.getInt("MaCTPN"));
		ctpn.setMaPhieuNhap(rs.getInt("MaPhieuNhap"));
		ctpn.setMaSanPham(rs.getInt("MaSanPham"));
		ctpn.setSoLo(rs.getString("SoLo"));
		if (rs.getDate("HanSuDung") != null) {
			ctpn.setHanSuDung(rs.getDate("HanSuDung").toLocalDate());
		}
		ctpn.setGiaNhap(rs.getBigDecimal("GiaNhap"));
		ctpn.setSoLuong(rs.getInt("SoLuong"));
		ctpn.setThanhTien(rs.getBigDecimal("ThanhTien"));
		return ctpn;
	}

	/**
	 * Lấy chi tiết phiếu nhập với thông tin sản phẩm (cho hiển thị)
	 */
	public List<Object[]> getDetailForDisplay(int maPhieuNhap) {
		List<Object[]> list = new ArrayList<>();
		try (
			var con = ConnectDB.getCon();
			var ps = con.prepareStatement(
				"SELECT ctpn.MaSanPham, sp.TenSanPham, ctpn.SoLo, " +
				"ctpn.HanSuDung, ctpn.GiaNhap, ctpn.SoLuong, ctpn.ThanhTien " +
				"FROM ChiTietPhieuNhap ctpn " +
				"JOIN SanPham sp ON sp.MaSanPham = ctpn.MaSanPham " +
				"WHERE ctpn.MaPhieuNhap = ? " +
				"ORDER BY ctpn.MaCTPN"
			);
		) {
			ps.setInt(1, maPhieuNhap);
			var rs = ps.executeQuery();
			while (rs.next()) {
				list.add(new Object[]{
					rs.getInt("MaSanPham"),
					rs.getString("TenSanPham"),
					rs.getString("SoLo"),
					rs.getDate("HanSuDung").toLocalDate(),
					rs.getBigDecimal("GiaNhap"),
					rs.getInt("SoLuong"),
					rs.getBigDecimal("ThanhTien")
				});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}
}
