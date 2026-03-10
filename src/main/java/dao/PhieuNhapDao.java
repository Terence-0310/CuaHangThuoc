package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import common.ConnectDB;
import entity.ChiTietPhieuNhap;
import entity.PhieuNhap;

/**
 * DAO class cho PhieuNhap
 * 
 * @author Generated
 * @version 3.0
 */
public class PhieuNhapDao {

	/**
	 * Tạo phiếu nhập mới, lưu chi tiết hàng loạt và cập nhật Lô hàng trong cùng một giao dịch (Transaction)
	 * 
	 * @return ID (MaPhieuNhap) nếu thành công, trả về null nếu có lỗi và tự rollback.
	 */
	public Integer savePhieuNhapTransaction(int maNguoiDung, Integer maNCC, String ghiChu, List<ChiTietPhieuNhap> chiTietList) {
		Connection con = ConnectDB.getCon();
		if (con == null) return null;

		try {
			// [Bước 4] Bọc toàn bộ vào 1 Transaction
			con.setAutoCommit(false);

			System.out.println("\n[Transaction DAO] === BẮT ĐẦU TRANSACTION NHẬP HÀNG ===");
			
			// [Bước 1] INSERT INTO PhieuNhap (Lấy Generated ID)
			String sqlPhieuNhap = "INSERT INTO dbo.PhieuNhap (MaNguoiDung, MaNCC, GhiChu) VALUES (?, ?, ?)";
			int maPhieuNhapMoi = 0;
			try (PreparedStatement psPN = con.prepareStatement(sqlPhieuNhap, Statement.RETURN_GENERATED_KEYS)) {
				psPN.setInt(1, maNguoiDung);
				if (maNCC != null) psPN.setInt(2, maNCC);
				else psPN.setNull(2, java.sql.Types.INTEGER);
				if (ghiChu != null) psPN.setNString(3, ghiChu);
				else psPN.setNull(3, java.sql.Types.NVARCHAR);

				psPN.executeUpdate();

				try (ResultSet rsKeys = psPN.getGeneratedKeys()) {
					if (rsKeys.next()) {
						maPhieuNhapMoi = (int) rsKeys.getLong(1);
					} else {
						throw new Exception("Không thể lấy được ID IDENTITY của Phiếu Nhập!");
					}
				}
			}
			System.out.println("[Transaction DAO] -> Đã lấy ID IDENTITY: MaPhieuNhap = " + maPhieuNhapMoi);

			// Vòng lặp Chi tiết nhập kho
			String sqlChiTiet = "INSERT INTO dbo.ChiTietPhieuNhap (MaPhieuNhap, MaSanPham, SoLo, HanSuDung, GiaNhap, SoLuong) VALUES (?, ?, ?, ?, ?, ?)";
			String sqlCheckLo = "SELECT 1 FROM dbo.LoHang WHERE MaSanPham = ? AND SoLo = ?";
			String sqlUpdateLo = "UPDATE dbo.LoHang SET SoLuongTon = SoLuongTon + ?, SoLuongNhap = SoLuongNhap + ?, "
								+ "HanSuDung = CASE WHEN HanSuDung < ? THEN ? ELSE HanSuDung END, GiaNhap = ?, TrangThai = N'Đang bán' "
								+ "WHERE MaSanPham = ? AND SoLo = ?";
			String sqlInsertLo = "INSERT INTO dbo.LoHang (MaSanPham, SoLo, MaNCC, MaPhieuNhap, HanSuDung, GiaNhap, SoLuongNhap, SoLuongTon) "
								+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

			try (PreparedStatement psCT = con.prepareStatement(sqlChiTiet);
				 PreparedStatement psCheckLo = con.prepareStatement(sqlCheckLo);
				 PreparedStatement psUpdLo = con.prepareStatement(sqlUpdateLo);
				 PreparedStatement psInsLo = con.prepareStatement(sqlInsertLo)) {

				BigDecimal tongTienHoaDon = BigDecimal.ZERO;

				for (int i = 0; i < chiTietList.size(); i++) {
					ChiTietPhieuNhap ct = chiTietList.get(i);
					System.out.println("[Transaction DAO] Xử lý Dòng " + i + ": Sản phẩm ID " + ct.getMaSanPham() + ", Lô " + ct.getSoLo());

					// 1. Lưu ChiTietPhieuNhap
					// [Bước 2] Dữ liệu tiền (GiaNhap) là BigDecimal chuẩn, chống tràn số
					psCT.setInt(1, maPhieuNhapMoi);
					psCT.setInt(2, ct.getMaSanPham());
					psCT.setNString(3, ct.getSoLo());
					psCT.setDate(4, java.sql.Date.valueOf(ct.getHanSuDung()));
					psCT.setBigDecimal(5, ct.getGiaNhap());
					psCT.setInt(6, ct.getSoLuong());
					psCT.executeUpdate();

					// Cộng dồn tổng tiền hóa đơn
					BigDecimal thanhTienDong = ct.getGiaNhap().multiply(new BigDecimal(ct.getSoLuong()));
					tongTienHoaDon = tongTienHoaDon.add(thanhTienDong);

					// 2. [Bước 3] Thêm lệnh INSERT hoặc UPDATE vào bảng 'LoHang'
					boolean isExistLo = false;
					psCheckLo.setInt(1, ct.getMaSanPham());
					psCheckLo.setNString(2, ct.getSoLo());
					try (ResultSet rsLo = psCheckLo.executeQuery()) {
						if (rsLo.next()) isExistLo = true;
					}

					if (isExistLo) {
						// Đã có lô -> update cộng dồn
						psUpdLo.setInt(1, ct.getSoLuong());
						psUpdLo.setInt(2, ct.getSoLuong());
						psUpdLo.setDate(3, java.sql.Date.valueOf(ct.getHanSuDung()));
						psUpdLo.setDate(4, java.sql.Date.valueOf(ct.getHanSuDung()));
						psUpdLo.setBigDecimal(5, ct.getGiaNhap());
						psUpdLo.setInt(6, ct.getMaSanPham());
						psUpdLo.setNString(7, ct.getSoLo());
						psUpdLo.executeUpdate();
						System.out.println("[Transaction DAO] -> Cập nhật cộng tồn Lô hàng cũ: " + ct.getSoLo());
					} else {
						// Lô mới -> INSERT
						psInsLo.setInt(1, ct.getMaSanPham());
						psInsLo.setNString(2, ct.getSoLo());
						if (maNCC != null) psInsLo.setInt(3, maNCC);
						else psInsLo.setNull(3, java.sql.Types.INTEGER);
						psInsLo.setInt(4, maPhieuNhapMoi);
						psInsLo.setDate(5, java.sql.Date.valueOf(ct.getHanSuDung()));
						psInsLo.setBigDecimal(6, ct.getGiaNhap());
						psInsLo.setInt(7, ct.getSoLuong()); // SoLuongNhap
						psInsLo.setInt(8, ct.getSoLuong()); // SoLuongTon
						psInsLo.executeUpdate();
						System.out.println("[Transaction DAO] -> THÊM MỚI Lô hàng: " + ct.getSoLo());
					}
				}

				// 3. Cập nhật lại Tổng Tiền cho Phiếu Nhập
				try (PreparedStatement psTongTien = con.prepareStatement("UPDATE dbo.PhieuNhap SET TongTien = ? WHERE MaPhieuNhap = ?")) {
					psTongTien.setBigDecimal(1, tongTienHoaDon);
					psTongTien.setInt(2, maPhieuNhapMoi);
					psTongTien.executeUpdate();
				}
			}

			// NẾU TẤT CẢ OK -> COMMIT TOÀN BỘ Transaction
			con.commit();
			System.out.println("[Transaction DAO] === COMMIT TRANSACTION THÀNH CÔNG ===");
			return maPhieuNhapMoi;

		} catch (Exception e) {
			System.err.println("[Transaction DAO] === LỖI XẢY RA - ROLLBACK TOÀN BỘ DỮ LIỆU ===");
			e.printStackTrace();
			try {
				con.rollback(); // [Bước 4] Nếu line 0 lỗi -> rollback, không lưu rác!
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return null;
		} finally {
			try {
				con.setAutoCommit(true); // Trả lại autocommit
				con.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
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
