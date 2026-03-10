package dao;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import common.ConnectDB;
import entity.SanPham;

/**
 * DAO class cho SanPham
 * 
 * @author Generated
 * @version 1.0
 */
public class SanPhamDao {

	/**
	 * Lấy tất cả sản phẩm chưa xóa
	 */
	public List<SanPham> getAll() {
		List<SanPham> list = new ArrayList<>();
		try (
			var con = ConnectDB.getCon();
			var stmt = con.createStatement();
			var rs = stmt.executeQuery(
				"SELECT * FROM SanPham WHERE DaXoa = 0 ORDER BY TenSanPham"
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
	 * Tìm sản phẩm theo ID
	 */
	public SanPham findById(int maSanPham) {
		try (
			var con = ConnectDB.getCon();
			var ps = con.prepareStatement("SELECT * FROM SanPham WHERE MaSanPham = ? AND DaXoa = 0");
		) {
			ps.setInt(1, maSanPham);
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
	 * Tìm sản phẩm theo tên (tìm kiếm)
	 */
	public List<SanPham> searchByName(String keyword) {
		List<SanPham> list = new ArrayList<>();
		try (
			var con = ConnectDB.getCon();
			var ps = con.prepareStatement(
				"SELECT * FROM SanPham WHERE DaXoa = 0 AND TenSanPham LIKE ? ORDER BY TenSanPham"
			);
		) {
			ps.setString(1, "%" + keyword + "%");
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
	 * Thêm sản phẩm mới
	 */
	public boolean insert(SanPham sp) {
		try (
			var con = ConnectDB.getCon();
			var ps = con.prepareStatement(
				"INSERT INTO SanPham(TenSanPham, DonViTinh, GiaBanDeXuat, LoaiSanPham, MoTa, MucTonToiThieu) " +
				"VALUES (?, ?, ?, ?, ?, ?)"
			);
		) {
			ps.setString(1, sp.getTenSanPham());
			ps.setString(2, sp.getDonViTinh());
			ps.setBigDecimal(3, sp.getGiaBanDeXuat());
			ps.setString(4, sp.getLoaiSanPham());
			ps.setString(5, sp.getMoTa());
			ps.setInt(6, sp.getMucTonToiThieu());
			return ps.executeUpdate() > 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Cập nhật sản phẩm
	 */
	public boolean update(SanPham sp) {
		try (
			var con = ConnectDB.getCon();
			var ps = con.prepareStatement(
				"UPDATE SanPham SET TenSanPham=?, DonViTinh=?, GiaBanDeXuat=?, " +
				"LoaiSanPham=?, MoTa=?, MucTonToiThieu=? WHERE MaSanPham=?"
			);
		) {
			ps.setString(1, sp.getTenSanPham());
			ps.setString(2, sp.getDonViTinh());
			ps.setBigDecimal(3, sp.getGiaBanDeXuat());
			ps.setString(4, sp.getLoaiSanPham());
			ps.setString(5, sp.getMoTa());
			ps.setInt(6, sp.getMucTonToiThieu());
			ps.setInt(7, sp.getMaSanPham());
			return ps.executeUpdate() > 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Xóa mềm sản phẩm
	 */
	public boolean delete(int maSanPham) {
		try (
			var con = ConnectDB.getCon();
			var ps = con.prepareStatement("UPDATE SanPham SET DaXoa=1 WHERE MaSanPham=?");
		) {
			ps.setInt(1, maSanPham);
			return ps.executeUpdate() > 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Map ResultSet to SanPham entity
	 */
	private SanPham mapResultSet(ResultSet rs) throws Exception {
		var sp = new SanPham();
		sp.setMaSanPham(rs.getInt("MaSanPham"));
		sp.setTenSanPham(rs.getString("TenSanPham"));
		sp.setDonViTinh(rs.getString("DonViTinh"));
		sp.setGiaBanDeXuat(rs.getBigDecimal("GiaBanDeXuat"));
		sp.setLoaiSanPham(rs.getString("LoaiSanPham"));
		sp.setMoTa(rs.getString("MoTa"));
		sp.setMucTonToiThieu(rs.getInt("MucTonToiThieu"));
		sp.setDaXoa(rs.getBoolean("DaXoa"));
		if (rs.getTimestamp("NgayTao") != null) {
			sp.setNgayTao(rs.getTimestamp("NgayTao").toLocalDateTime());
		}
		return sp;
	}
}
