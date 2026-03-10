package dao;

import common.ConnectDB;
import model.User;
import model.Role;

public class UserDAO {
	public User findByUsername(String username) {

		var sql = """
				    SELECT MaNguoiDung, TenDangNhap, MatKhau, VaiTro, DaXoa
				    FROM NguoiDung
				    WHERE TenDangNhap = ?
				""";

		try (var con = ConnectDB.getCon(); var ps = con.prepareStatement(sql)) {
			ps.setString(1, username);
			var rs = ps.executeQuery();
			if (rs.next()) {
				var user = new User();
				user.setId(rs.getInt("MaNguoiDung"));
				user.setUsername(rs.getString("TenDangNhap"));
				user.setPassword(rs.getString("MatKhau"));
				user.setRole(Role.valueOf(rs.getString("VaiTro")));
				user.setDeleted(rs.getBoolean("DaXoa")); // mapping chuẩn
				return user;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
}
