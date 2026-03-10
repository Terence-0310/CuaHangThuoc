package dao;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import common.ConnectDB;
import entity.PageResult;
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
						"SELECT * FROM SanPham WHERE DaXoa = 0 ORDER BY TenSanPham");) {
			while (rs.next()) {
				list.add(mapResultSet(rs));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * Lấy sản phẩm theo trang (overload đơn giản)
	 */
	public PageResult<SanPham> getByPage(int page, int size) {
		return getByPage(page, size, null, "MaSanPham", "ASC");
	}

	/**
	 * Lấy sản phẩm theo trang - gọi sp_SanPham_GetPage (v2)
	 * Hỗ trợ tìm kiếm + sắp xếp động
	 * 
	 * @param page       số trang (bắt đầu từ 1)
	 * @param size       số dòng mỗi trang
	 * @param keyword    từ khóa tìm kiếm (null = không lọc)
	 * @param sortColumn tên cột sắp xếp
	 * @param sortOrder  ASC hoặc DESC
	 * @return PageResult chứa danh sách sản phẩm + metadata phân trang
	 */
	public PageResult<SanPham> getByPage(int page, int size, String keyword,
			String sortColumn, String sortOrder) {
		List<SanPham> list = new ArrayList<>();
		int totalRows = 0;
		int totalPages = 0;

		try (
				var con = ConnectDB.getCon();
				var cs = con.prepareCall("{CALL sp_SanPham_GetPage(?, ?, ?, ?, ?)}");) {
			cs.setInt(1, page);
			cs.setInt(2, size);
			if (keyword != null && !keyword.trim().isEmpty()) {
				cs.setString(3, keyword.trim());
			} else {
				cs.setNull(3, java.sql.Types.NVARCHAR);
			}
			cs.setString(4, sortColumn != null ? sortColumn : "MaSanPham");
			cs.setString(5, sortOrder != null ? sortOrder : "ASC");
			var rs = cs.executeQuery();

			while (rs.next()) {
				var sp = new SanPham();
				sp.setMaSanPham(rs.getInt("MaSanPham"));
				sp.setTenSanPham(rs.getString("TenSanPham"));
				sp.setDonViTinh(rs.getString("DonViTinh"));
				sp.setGiaBanDeXuat(rs.getBigDecimal("GiaBanDeXuat"));
				sp.setLoaiSanPham(rs.getString("LoaiSanPham"));
				sp.setMoTa(rs.getString("MoTa"));
				sp.setMucTonToiThieu(rs.getInt("MucTonToiThieu"));
				list.add(sp);

				// Lấy metadata phân trang từ dòng đầu tiên
				if (totalRows == 0) {
					totalRows = rs.getInt("TotalRows");
					totalPages = rs.getInt("TotalPages");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Đảm bảo totalPages tối thiểu = 1
		if (totalPages == 0) {
			totalPages = 1;
		}

		return new PageResult<>(list, totalRows, totalPages, page, size);
	}

	/**
	 * Tìm sản phẩm theo ID
	 */
	public SanPham findById(int maSanPham) {
		try (
				var con = ConnectDB.getCon();
				var ps = con.prepareStatement("SELECT * FROM SanPham WHERE MaSanPham = ? AND DaXoa = 0");) {
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
						"SELECT * FROM SanPham WHERE DaXoa = 0 AND TenSanPham LIKE ? ORDER BY TenSanPham");) {
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
								"VALUES (?, ?, ?, ?, ?, ?)");) {
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
								"LoaiSanPham=?, MoTa=?, MucTonToiThieu=? WHERE MaSanPham=?");) {
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
				var ps = con.prepareStatement("UPDATE SanPham SET DaXoa=1 WHERE MaSanPham=?");) {
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
