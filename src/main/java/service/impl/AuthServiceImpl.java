package service.impl;

import domain.entity.User;
import domain.repository.IUserRepository;
import service.IAuthService;

/**
 * Service Impl: Đăng nhập (SRP + DIP)
 */
public class AuthServiceImpl implements IAuthService {

    private final IUserRepository userRepo;

    public AuthServiceImpl(IUserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public User login(String tenDangNhap, String matKhau) {
        if (tenDangNhap == null || tenDangNhap.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống");
        }
        if (matKhau == null || matKhau.trim().isEmpty()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống");
        }
        User user = userRepo.findByCredentials(tenDangNhap.trim(), matKhau.trim());
        if (user == null) {
            throw new IllegalArgumentException("Sai tên đăng nhập hoặc mật khẩu");
        }
        if (!user.isTrangThai()) {
            throw new IllegalArgumentException("Tài khoản đã bị khóa");
        }
        return user;
    }
}
