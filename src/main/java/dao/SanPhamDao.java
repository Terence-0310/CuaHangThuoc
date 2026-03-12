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
		return getByPage(page, size, null, "MaSanPham", "ASC", "Tất cả", "Tất cả loại");
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
			String sortColumn, String sortOrder, String loaiHinhBan, String loaiSP) {
		List<SanPham> list = new ArrayList<>();
		int totalRows = 0;
		int totalPages = 0;

		String whereClause = "WHERE sp.DaXoa = 0 ";
		if (keyword != null && !keyword.trim().isEmpty()) {
			whereClause += " AND sp.TenSanPham LIKE ? ";
		}
		if (loaiHinhBan != null && !loaiHinhBan.trim().isEmpty() && !loaiHinhBan.equals("Tất cả")) {
			whereClause += " AND EXISTS (SELECT 1 FROM LoHang lh WHERE lh.MaSanPham = sp.MaSanPham AND lh.LoaiHinhBan = ? AND lh.SoLuongTon > 0) ";
		}
		if (loaiSP != null && !loaiSP.trim().isEmpty() && !loaiSP.equals("Tất cả loại")) {
			whereClause += " AND sp.LoaiSanPham = ? ";
		}

		String countQuery = "SELECT COUNT(*) FROM SanPham sp " + whereClause;

		String sortExp = "sp.MaSanPham";
		if ("TenSanPham".equals(sortColumn)) sortExp = "sp.TenSanPham";
		else if ("GiaBanDeXuat".equals(sortColumn)) sortExp = "sp.GiaBanDeXuat";
		else if ("MoTa".equals(sortColumn)) sortExp = "sp.MoTa";
		else if ("DonViTinh".equals(sortColumn)) sortExp = "sp.DonViTinh";
		else if ("LoaiSanPham".equals(sortColumn)) sortExp = "sp.LoaiSanPham";
		else if ("MucTonToiThieu".equals(sortColumn)) sortExp = "sp.MucTonToiThieu";

		String sql = "SELECT sp.*, "
				+ "ISNULL((SELECT SUM(SoLuongTon) FROM LoHang tk WHERE tk.MaSanPham = sp.MaSanPham), 0) AS TongTon, "
				+ "(SELECT MIN(HanSuDung) FROM LoHang tk2 WHERE tk2.MaSanPham = sp.MaSanPham AND tk2.SoLuongTon > 0) AS HanSuDungGanNhat "
				+ " FROM SanPham sp "
				+ whereClause
				+ " ORDER BY " + sortExp + " " + (sortOrder != null && sortOrder.equalsIgnoreCase("DESC") ? "DESC" : "ASC")
				+ " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

		try (var con = ConnectDB.getCon()) {
			// Count total rows
			try (var psCount = con.prepareStatement(countQuery)) {
				int paramIndex = 1;
				if (keyword != null && !keyword.trim().isEmpty()) {
					psCount.setString(paramIndex++, "%" + keyword.trim() + "%");
				}
				if (loaiHinhBan != null && !loaiHinhBan.trim().isEmpty() && !loaiHinhBan.equals("Tất cả")) {
					psCount.setString(paramIndex++, loaiHinhBan);
				}
				if (loaiSP != null && !loaiSP.trim().isEmpty() && !loaiSP.equals("Tất cả loại")) {
					psCount.setString(paramIndex++, loaiSP);
				}
				var rsCount = psCount.executeQuery();
				if (rsCount.next()) totalRows = rsCount.getInt(1);
			}

			// Fetch items
			try (var ps = con.prepareStatement(sql)) {
				int paramIndex = 1;
				if (keyword != null && !keyword.trim().isEmpty()) {
					ps.setString(paramIndex++, "%" + keyword.trim() + "%");
				}
				if (loaiHinhBan != null && !loaiHinhBan.trim().isEmpty() && !loaiHinhBan.equals("Tất cả")) {
					ps.setString(paramIndex++, loaiHinhBan);
				}
				if (loaiSP != null && !loaiSP.trim().isEmpty() && !loaiSP.equals("Tất cả loại")) {
					ps.setString(paramIndex++, loaiSP);
				}
				ps.setInt(paramIndex++, (page - 1) * size);
				ps.setInt(paramIndex++, size);

				var rs = ps.executeQuery();
				while (rs.next()) {
					var sp = new SanPham();
					sp.setMaSanPham(rs.getInt("MaSanPham"));
					sp.setTenSanPham(rs.getString("TenSanPham"));
					sp.setDonViTinh(rs.getString("DonViTinh"));
					sp.setGiaBanDeXuat(rs.getBigDecimal("GiaBanDeXuat"));
					sp.setLoaiSanPham(rs.getString("LoaiSanPham"));
					sp.setMoTa(rs.getString("MoTa"));
					sp.setMucTonToiThieu(rs.getInt("MucTonToiThieu"));
					sp.setTongTon(rs.getInt("TongTon")); 
					if (rs.getDate("HanSuDungGanNhat") != null) {
						sp.setHanSuDungGanNhat(rs.getDate("HanSuDungGanNhat").toLocalDate());
					}
					list.add(sp);
				}
			}
			totalPages = (int) Math.ceil((double) totalRows / size);
			if (totalPages == 0) totalPages = 1;

		} catch (Exception e) {
			e.printStackTrace();
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
				var sp = mapResultSet(rs);
				// Lấy thêm tổng tồn do hàm findById select thẳng từ SanPham không qua view
				try (var psTon = con.prepareStatement("SELECT ISNULL(SUM(SoLuongTon), 0) FROM LoHang WHERE MaSanPham = ?")) {
					psTon.setInt(1, maSanPham);
					var rsTon = psTon.executeQuery();
					if (rsTon.next()) {
						sp.setTongTon(rsTon.getInt(1));
					}
				}
				return sp;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Lấy thông tin chi tiết đầy đủ mở rộng (Dùng cho xem chi tiết)
	 */
	public SanPham getFullDetailByMaSP(int maSanPham) {
		String sql = "SELECT sp.*, ISNULL(tk.TongTon, 0) AS TongTon, "
				+ "(SELECT MIN(HanSuDung) FROM LoHang lh WHERE lh.MaSanPham = sp.MaSanPham AND lh.SoLuongTon > 0) AS HanSuDungGanNhat "
				+ "FROM SanPham sp "
				+ "LEFT JOIN v_TonKhoSanPham tk ON sp.MaSanPham = tk.MaSanPham "
				+ "WHERE sp.MaSanPham = ?";
		try (
				var con = ConnectDB.getCon();
				var ps = con.prepareStatement(sql)) {
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
						"SELECT * FROM SanPham WHERE DaXoa = 0 AND TenSanPham LIKE ? ORDER BY TenSanPham ASC");) {
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
	 * Tìm sản phẩm khớp chính xác theo tên (Dùng cho NhapHangPanel)
	 */
	public SanPham findByNameExact(String exactName) {
		try (
				var con = ConnectDB.getCon();
				var ps = con.prepareStatement("SELECT * FROM SanPham WHERE DaXoa = 0 AND TenSanPham = ? ORDER BY TenSanPham ASC")) {
			ps.setNString(1, exactName);
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
	 * Thêm sản phẩm mới
	 */
	public boolean insert(SanPham sp) {
		try (
				var con = ConnectDB.getCon();
				var ps = con.prepareStatement(
						"INSERT INTO SanPham(TenSanPham, LoaiSanPham, DonViTinh, GiaBanDeXuat) " +
								"VALUES (?, ?, ?, ?)");) {
			ps.setNString(1, sp.getTenSanPham());
			ps.setNString(2, "thuoc"); // Luôn lưu là 'thuoc' không dấu
			ps.setNString(3, sp.getDonViTinh());
			ps.setBigDecimal(4, sp.getGiaBanDeXuat());
			return ps.executeUpdate() > 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Thêm sản phẩm mới và trả về MaSanPham (Generated Key)
	 * Dùng cho flow Nhập hàng khi tạo SP mới inline
	 */
	public int insertAndGetId(SanPham sp) {
		try (
				var con = ConnectDB.getCon();
				var ps = con.prepareStatement(
						"INSERT INTO SanPham(TenSanPham, LoaiSanPham, DonViTinh, GiaBanDeXuat) " +
								"VALUES (?, ?, ?, ?)",
						java.sql.Statement.RETURN_GENERATED_KEYS);) {
			ps.setNString(1, sp.getTenSanPham());
			ps.setNString(2, "thuoc"); // Luôn lưu là 'thuoc' không dấu
			ps.setNString(3, sp.getDonViTinh());
			ps.setBigDecimal(4, sp.getGiaBanDeXuat());
			ps.executeUpdate();
			var rs = ps.getGeneratedKeys();
			if (rs.next()) {
				return rs.getInt(1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
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
			ps.setNString(1, sp.getTenSanPham());
			ps.setNString(2, sp.getDonViTinh());
			ps.setBigDecimal(3, sp.getGiaBanDeXuat());
			ps.setNString(4, "thuoc"); // Luôn lưu là 'thuoc' không dấu
			ps.setNString(5, sp.getMoTa());
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
	 * Tìm sản phẩm cho màn hình bán hàng (kèm tồn kho)
	 * Dùng cho BanHangPanel - tìm theo mã, tên, từ khóa
	 */
	public List<SanPham> searchForSale(String keyword) {
		List<SanPham> list = new ArrayList<>();
		String sql = "SELECT sp.*, " +
				"ISNULL((SELECT SUM(SoLuongTon) FROM LoHang lh WHERE lh.MaSanPham = sp.MaSanPham), 0) AS TongTon " +
				"FROM SanPham sp WHERE sp.DaXoa = 0 ";
		if (keyword != null && !keyword.trim().isEmpty()) {
			sql += "AND (sp.TenSanPham LIKE ? OR CAST(sp.MaSanPham AS NVARCHAR) LIKE ?) ";
		}
		sql += "ORDER BY sp.TenSanPham ASC";

		try (var con = ConnectDB.getCon();
			 var ps = con.prepareStatement(sql)) {
			if (keyword != null && !keyword.trim().isEmpty()) {
				String pattern = "%" + keyword.trim() + "%";
				ps.setNString(1, pattern);
				ps.setNString(2, pattern);
			}
			var rs = ps.executeQuery();
			while (rs.next()) {
				var sp = mapResultSet(rs);
				list.add(sp);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
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
		
		// Map thêm các trường ảo (nếu có trong câu query)
		try {
			sp.setTongTon(rs.getInt("TongTon"));
		} catch (Exception ignore) {}

		try {
			if (rs.getDate("HanSuDungGanNhat") != null) {
				sp.setHanSuDungGanNhat(rs.getDate("HanSuDungGanNhat").toLocalDate());
			}
		} catch (Exception ignore) {}
		
		return sp;
	}
}
