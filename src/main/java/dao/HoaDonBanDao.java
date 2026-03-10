package dao;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import common.ConnectDB;
import entity.HoaDonBan;

/**
 * DAO class cho HoaDonBan
 * 
 * @author Generated
 * @version 1.0
 */
public class HoaDonBanDao {

	/**
	 * Tạo hóa đơn mới - gọi stored procedure
	 */
	public Integer createHoaDon(int maNguoiDung, Integer maKhachHang, String ghiChu) {
		try (
			var con = ConnectDB.getCon();
			var cs = con.prepareCall("{call sp_HoaDonBan_Create(?, ?, ?)}");
		) {
			cs.setInt(1, maNguoiDung);
			if (maKhachHang != null) {
				cs.setInt(2, maKhachHang);
			} else {
				cs.setNull(2, java.sql.Types.INTEGER);
			}
			cs.setString(3, ghiChu);
			
			var rs = cs.executeQuery();
			if (rs.next()) {
				return rs.getInt("MaHoaDon");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Bán sản phẩm theo FEFO - gọi stored procedure
	 */
	public boolean sellProductFEFO(int maHoaDon, int maSanPham, int soLuongCanBan, BigDecimal giaBan) {
		try (
			var con = ConnectDB.getCon();
			var cs = con.prepareCall("{call sp_HoaDonBan_Sell_FEFO(?, ?, ?, ?)}");
		) {
			cs.setInt(1, maHoaDon);
			cs.setInt(2, maSanPham);
			cs.setInt(3, soLuongCanBan);
			if (giaBan != null) {
				cs.setBigDecimal(4, giaBan);
			} else {
				cs.setNull(4, java.sql.Types.DECIMAL);
			}
			cs.execute();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Lấy danh sách hóa đơn
	 */
	public List<HoaDonBan> getAll() {
		List<HoaDonBan> list = new ArrayList<>();
		try (
			var con = ConnectDB.getCon();
			var stmt = con.createStatement();
			var rs = stmt.executeQuery(
				"SELECT * FROM HoaDonBan ORDER BY NgayBan DESC"
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
	 * Lấy hóa đơn theo ID
	 */
	public HoaDonBan findById(int maHoaDon) {
		try (
			var con = ConnectDB.getCon();
			var ps = con.prepareStatement("SELECT * FROM HoaDonBan WHERE MaHoaDon = ?");
		) {
			ps.setInt(1, maHoaDon);
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
	 * Map ResultSet to HoaDonBan entity
	 */
	private HoaDonBan mapResultSet(ResultSet rs) throws Exception {
		var hdb = new HoaDonBan();
		hdb.setMaHoaDon(rs.getInt("MaHoaDon"));
		hdb.setMaNguoiDung(rs.getInt("MaNguoiDung"));
		var maKH = rs.getObject("MaKhachHang");
		if (maKH != null) {
			hdb.setMaKhachHang((Integer) maKH);
		}
		hdb.setTongTien(rs.getBigDecimal("TongTien"));
		if (rs.getTimestamp("NgayBan") != null) {
			hdb.setNgayBan(rs.getTimestamp("NgayBan").toLocalDateTime());
		}
		hdb.setGhiChu(rs.getString("GhiChu"));
		return hdb;
	}
}
