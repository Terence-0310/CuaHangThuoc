package domain.repository;

import domain.entity.User;

/**
 * Repository Interface: Người dùng
 * (DIP: Service phụ thuộc interface này, không phụ thuộc impl)
 */
public interface IUserRepository {
    User findByCredentials(String tenDangNhap, String matKhau);
}
