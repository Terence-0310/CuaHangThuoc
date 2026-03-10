package test;

import dao.UserDAO;

public class TestUserDAO {

	public static void main(String[] args) {

		var dao = new UserDAO();
		var user = dao.findByUsername("admin"); // đổi username có thật

		if (user != null) {
			System.out.println("✅ Tìm thấy user: " + user.getUsername());
			System.out.println("Role: " + user.getRole());
			System.out.println("Deleted: " + user.isDeleted());
		} else {
			System.out.println("❌ Không tìm thấy user");
		}
	}
}
