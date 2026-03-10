package service;

import dao.UserDAO;
import model.User;

public class AuthService {

	private UserDAO userDAO = new UserDAO();

	public User login(String username, String password) throws Exception {

		var user = userDAO.findByUsername(username);

		// 1. Không tồn tại tài khoản
		if (user == null) {
			throw new Exception("Tài khoản không tồn tại");
		}
		// 2. Tài khoản bị vô hiệu hóa (DaXoa = 1)
		if (user.isDeleted()) {
			throw new Exception("Tài khoản đã bị vô hiệu hóa");
		}
		// 3. Sai mật khẩu
		if (!user.getPassword().trim().equals(password.trim())) {
			throw new Exception("Sai mật khẩu");
		}
		// 4. Login thành công
		return user;
	}
}
