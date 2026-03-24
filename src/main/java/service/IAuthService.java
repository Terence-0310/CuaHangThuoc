package service;

import domain.entity.User;

/**
 * Service Interface: Đăng nhập & Phân quyền
 */
public interface IAuthService {
    User login(String tenDangNhap, String matKhau);
}
