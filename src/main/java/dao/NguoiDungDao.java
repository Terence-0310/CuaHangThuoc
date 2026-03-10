package dao;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import common.ConnectDB;
import entity.NguoiDung;

/**
 * DAO class cho NguoiDung
 * 
 * @author Generated
 * @version 1.0
 */
public class NguoiDungDao {

	/**
	 * Đăng nhập - gọi stored procedure sp_Login
	 */
	public NguoiDung login(String tenDangNhap, String matKhau) {
		try (
			var con = ConnectDB.getCon();
			var cs = con.prepareCall("{call sp_Login(?, ?)}");
		) {
			cs.setString(1, tenDangNhap);
			cs.setString(2, matKhau);
			var rs = cs.executeQuery();
			
			if (rs.next()) {
				var nguoiDung = new NguoiDung();
				nguoiDung.setMaNguoiDung(rs.getInt("MaNguoiDung"));
				nguoiDung.setTenDangNhap(rs.getString("TenDangNhap"));
				nguoiDung.setVaiTro(rs.getString("VaiTro"));
				nguoiDung.setHoTen(rs.getString("HoTen"));
				nguoiDung.setEmail(rs.getString("Email"));
				nguoiDung.setSoDienThoai(rs.getString("SoDienThoai"));
				return nguoiDung;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Lấy danh sách người dùng (chưa xóa)
	 */
	public List<NguoiDung> getAll() {
		List<NguoiDung> list = new ArrayList<>();
		try (
			var con = ConnectDB.getCon();
			var stmt = con.createStatement();
			var rs = stmt.executeQuery(
				"SELECT * FROM NguoiDung WHERE DaXoa = 0 ORDER BY MaNguoiDung"
			);
		) {
			while (rs.next()) {
				var nd = new NguoiDung();
				nd.setMaNguoiDung(rs.getInt("MaNguoiDung"));
				nd.setTenDangNhap(rs.getString("TenDangNhap"));
				nd.setVaiTro(rs.getString("VaiTro"));
				nd.setHoTen(rs.getString("HoTen"));
				nd.setEmail(rs.getString("Email"));
				nd.setSoDienThoai(rs.getString("SoDienThoai"));
				nd.setDaXoa(rs.getBoolean("DaXoa"));
				if (rs.getTimestamp("NgayTao") != null) {
					nd.setNgayTao(rs.getTimestamp("NgayTao").toLocalDateTime());
				}
				list.add(nd);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * Thêm người dùng mới
	 */
	public boolean insert(NguoiDung nd) {
		try (
			var con = ConnectDB.getCon();
			var ps = con.prepareStatement(
				"INSERT INTO NguoiDung(TenDangNhap, MatKhau, VaiTro, HoTen, Email, SoDienThoai) " +
				"VALUES (?, ?, ?, ?, ?, ?)"
			);
		) {
			ps.setString(1, nd.getTenDangNhap());
			ps.setString(2, nd.getMatKhau());
			ps.setString(3, nd.getVaiTro());
			ps.setString(4, nd.getHoTen());
			ps.setString(5, nd.getEmail());
			ps.setString(6, nd.getSoDienThoai());
			return ps.executeUpdate() > 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Cập nhật người dùng
	 */
	public boolean update(NguoiDung nd) {
		try (
			var con = ConnectDB.getCon();
			var ps = con.prepareStatement(
				"UPDATE NguoiDung SET TenDangNhap=?, MatKhau=?, VaiTro=?, " +
				"HoTen=?, Email=?, SoDienThoai=? WHERE MaNguoiDung=?"
			);
		) {
			ps.setString(1, nd.getTenDangNhap());
			ps.setString(2, nd.getMatKhau());
			ps.setString(3, nd.getVaiTro());
			ps.setString(4, nd.getHoTen());
			ps.setString(5, nd.getEmail());
			ps.setString(6, nd.getSoDienThoai());
			ps.setInt(7, nd.getMaNguoiDung());
			return ps.executeUpdate() > 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Xóa mềm người dùng (soft delete)
	 */
	public boolean delete(int maNguoiDung) {
		try (
			var con = ConnectDB.getCon();
			var ps = con.prepareStatement(
				"UPDATE NguoiDung SET DaXoa=1 WHERE MaNguoiDung=?"
			);
		) {
			ps.setInt(1, maNguoiDung);
			return ps.executeUpdate() > 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
