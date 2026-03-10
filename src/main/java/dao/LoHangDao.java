package dao;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import common.ConnectDB;
import entity.LoHang;

/**
 * DAO class cho LoHang
 * 
 * @author Generated
 * @version 1.0
 */
public class LoHangDao {

	/**
	 * Lấy tất cả lô hàng
	 */
	public List<LoHang> getAll() {
		List<LoHang> list = new ArrayList<>();
		try (
			var con = ConnectDB.getCon();
			var stmt = con.createStatement();
			var rs = stmt.executeQuery(
				"SELECT * FROM LoHang ORDER BY HanSuDung ASC, NgayNhap ASC"
			);
		) {
			while (rs.next()) {
				list.add(mapResultSet(rs));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * Lấy lô hàng theo mã sản phẩm
	 */
	public List<LoHang> getByMaSanPham(int maSanPham) {
		List<LoHang> list = new ArrayList<>();
		try (
			var con = ConnectDB.getCon();
			var ps = con.prepareStatement(
				"SELECT * FROM LoHang WHERE MaSanPham = ? ORDER BY HanSuDung ASC, NgayNhap ASC"
			);
		) {
			ps.setInt(1, maSanPham);
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
	 * Lấy lô hàng còn tồn và chưa hết hạn (cho bán hàng FEFO)
	 */
	public List<LoHang> getAvailableForSale(int maSanPham) {
		List<LoHang> list = new ArrayList<>();
		try (
			var con = ConnectDB.getCon();
			var ps = con.prepareStatement(
				"SELECT * FROM LoHang " +
				"WHERE MaSanPham = ? " +
				"AND SoLuongTon > 0 " +
				"AND TrangThai = N'Đang bán' " +
				"AND HanSuDung >= CAST(GETDATE() AS DATE) " +
				"ORDER BY HanSuDung ASC, NgayNhap ASC"
			);
		) {
			ps.setInt(1, maSanPham);
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
	 * Lấy lô hàng theo ID
	 */
	public LoHang findById(int maLoHang) {
		try (
			var con = ConnectDB.getCon();
			var ps = con.prepareStatement("SELECT * FROM LoHang WHERE MaLoHang = ?");
		) {
			ps.setInt(1, maLoHang);
			var rs = ps.executeQuery();
			if (rs.next()) {
				return mapResultSet(rs);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Cập nhật trạng thái lô hàng
	 */
	public boolean updateTrangThai(int maLoHang, String trangThai) {
		try (
			var con = ConnectDB.getCon();
			var ps = con.prepareStatement("UPDATE LoHang SET TrangThai = ? WHERE MaLoHang = ?");
		) {
			ps.setString(1, trangThai);
			ps.setInt(2, maLoHang);
			return ps.executeUpdate() > 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Map ResultSet to LoHang entity
	 */
	private LoHang mapResultSet(ResultSet rs) throws Exception {
		var lh = new LoHang();
		lh.setMaLoHang(rs.getInt("MaLoHang"));
		lh.setMaSanPham(rs.getInt("MaSanPham"));
		lh.setSoLo(rs.getString("SoLo"));
		var maNCC = rs.getObject("MaNCC");
		if (maNCC != null) {
			lh.setMaNCC((Integer) maNCC);
		}
		var maPN = rs.getObject("MaPhieuNhap");
		if (maPN != null) {
			lh.setMaPhieuNhap((Integer) maPN);
		}
		var ngaySX = rs.getDate("NgaySanXuat");
		if (ngaySX != null) {
			lh.setNgaySanXuat(ngaySX.toLocalDate());
		}
		if (rs.getDate("HanSuDung") != null) {
			lh.setHanSuDung(rs.getDate("HanSuDung").toLocalDate());
		}
		lh.setGiaNhap(rs.getBigDecimal("GiaNhap"));
		lh.setSoLuongNhap(rs.getInt("SoLuongNhap"));
		lh.setSoLuongTon(rs.getInt("SoLuongTon"));
		if (rs.getTimestamp("NgayNhap") != null) {
			lh.setNgayNhap(rs.getTimestamp("NgayNhap").toLocalDateTime());
		}
		lh.setTrangThai(rs.getString("TrangThai"));
		return lh;
	}
}
