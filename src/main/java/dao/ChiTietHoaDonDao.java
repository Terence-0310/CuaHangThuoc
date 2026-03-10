package dao;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import common.ConnectDB;
import entity.ChiTietHoaDon;

/**
 * DAO class cho ChiTietHoaDon
 * 
 * @author Generated
 * @version 1.0
 */
public class ChiTietHoaDonDao {

	/**
	 * Lấy chi tiết hóa đơn theo mã hóa đơn
	 */
	public List<ChiTietHoaDon> getByMaHoaDon(int maHoaDon) {
		List<ChiTietHoaDon> list = new ArrayList<>();
		try (
			var con = ConnectDB.getCon();
			var ps = con.prepareStatement(
				"SELECT cthd.*, sp.TenSanPham, lh.SoLo " +
				"FROM ChiTietHoaDon cthd " +
				"JOIN SanPham sp ON sp.MaSanPham = cthd.MaSanPham " +
				"JOIN LoHang lh ON lh.MaLoHang = cthd.MaLoHang " +
				"WHERE cthd.MaHoaDon = ? " +
				"ORDER BY cthd.MaCTHD"
			);
		) {
			ps.setInt(1, maHoaDon);
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
	 * Map ResultSet to ChiTietHoaDon entity
	 */
	private ChiTietHoaDon mapResultSet(ResultSet rs) throws Exception {
		var cthd = new ChiTietHoaDon();
		cthd.setMaCTHD(rs.getInt("MaCTHD"));
		cthd.setMaHoaDon(rs.getInt("MaHoaDon"));
		cthd.setMaLoHang(rs.getInt("MaLoHang"));
		cthd.setMaSanPham(rs.getInt("MaSanPham"));
		cthd.setSoLuong(rs.getInt("SoLuong"));
		cthd.setGiaBan(rs.getBigDecimal("GiaBan"));
		cthd.setThanhTien(rs.getBigDecimal("ThanhTien"));
		return cthd;
	}

	/**
	 * Lấy chi tiết hóa đơn với thông tin sản phẩm và lô hàng (cho hiển thị)
	 */
	public List<Object[]> getDetailForDisplay(int maHoaDon) {
		List<Object[]> list = new ArrayList<>();
		try (
			var con = ConnectDB.getCon();
			var ps = con.prepareStatement(
				"SELECT cthd.MaSanPham, sp.TenSanPham, lh.SoLo, " +
				"cthd.SoLuong, cthd.GiaBan, cthd.ThanhTien " +
				"FROM ChiTietHoaDon cthd " +
				"JOIN SanPham sp ON sp.MaSanPham = cthd.MaSanPham " +
				"JOIN LoHang lh ON lh.MaLoHang = cthd.MaLoHang " +
				"WHERE cthd.MaHoaDon = ? " +
				"ORDER BY cthd.MaCTHD"
			);
		) {
			ps.setInt(1, maHoaDon);
			var rs = ps.executeQuery();
			while (rs.next()) {
				list.add(new Object[]{
					rs.getInt("MaSanPham"),
					rs.getString("TenSanPham"),
					rs.getString("SoLo"),
					rs.getInt("SoLuong"),
					rs.getBigDecimal("GiaBan"),
					rs.getBigDecimal("ThanhTien")
				});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}
}
