package test;

import service.AuthService;

public class TestLogin {
	public static void main(String[] args) {
		var auth = new AuthService();
		try {
			var user = auth.login("admin", "123456"); // đúng data DB
			System.out.println("✅ Login thành công: " + user.getUsername());
		} catch (Exception e) {
			System.out.println("❌ Login thất bại: " + e.getMessage());
		}
	}
}
