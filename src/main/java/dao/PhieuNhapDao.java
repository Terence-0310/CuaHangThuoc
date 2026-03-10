package dao;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import common.ConnectDB;
import entity.PhieuNhap;

/**
 * DAO class cho PhieuNhap
 * 
 * @author Generated
 * @version 1.0
 */
public class PhieuNhapDao {

	/**
	 * Tạo phiếu nhập mới - gọi stored procedure
	 */
	public Integer createPhieuNhap(int maNguoiDung, Integer maNCC, String ghiChu) {
		try (
			var con = ConnectDB.getCon();
			var cs = con.prepareCall("{call sp_PhieuNhap_Create(?, ?, ?)}");
		) {
			cs.setInt(1, maNguoiDung);
			if (maNCC != null) {
				cs.setInt(2, maNCC);
			} else {
				cs.setNull(2, java.sql.Types.INTEGER);
			}
			cs.setString(3, ghiChu);
			
			var rs = cs.executeQuery();
			if (rs.next()) {
				return rs.getInt("MaPhieuNhap");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Thêm item vào phiếu nhập theo lô - gọi stored procedure
	 */
	public boolean addItemBatch(int maPhieuNhap, int maSanPham, String soLo,
			LocalDate hanSuDung, BigDecimal giaNhap, int soLuong) {
		try (
			var con = ConnectDB.getCon();
			var cs = con.prepareCall("{call sp_PhieuNhap_AddItem_Batch(?, ?, ?, ?, ?, ?)}");
		) {
			cs.setInt(1, maPhieuNhap);
			cs.setInt(2, maSanPham);
			cs.setString(3, soLo);
			cs.setDate(4, java.sql.Date.valueOf(hanSuDung));
			cs.setBigDecimal(5, giaNhap);
			cs.setInt(6, soLuong);
			cs.execute();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Lấy danh sách phiếu nhập
	 */
	public List<PhieuNhap> getAll() {
		List<PhieuNhap> list = new ArrayList<>();
		try (
			var con = ConnectDB.getCon();
			var stmt = con.createStatement();
			var rs = stmt.executeQuery(
				"SELECT * FROM PhieuNhap ORDER BY NgayNhap DESC"
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
	 * Map ResultSet to PhieuNhap entity
	 */
	private PhieuNhap mapResultSet(ResultSet rs) throws Exception {
		var pn = new PhieuNhap();
		pn.setMaPhieuNhap(rs.getInt("MaPhieuNhap"));
		pn.setMaNguoiDung(rs.getInt("MaNguoiDung"));
		var maNCC = rs.getObject("MaNCC");
		if (maNCC != null) {
			pn.setMaNCC((Integer) maNCC);
		}
		pn.setTongTien(rs.getBigDecimal("TongTien"));
		if (rs.getTimestamp("NgayNhap") != null) {
			pn.setNgayNhap(rs.getTimestamp("NgayNhap").toLocalDateTime());
		}
		pn.setGhiChu(rs.getString("GhiChu"));
		return pn;
	}
}
