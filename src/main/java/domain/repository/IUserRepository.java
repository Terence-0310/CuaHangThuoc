package domain.repository;

import domain.dto.UserListDTO;
import domain.entity.User;
import java.sql.SQLException;
import java.util.List;

/**
 * Repository Interface: Người dùng
 * (DIP: Service phụ thuộc interface này, không phụ thuộc impl)
 */
public interface IUserRepository {
    User findByCredentials(String tenDangNhap, String matKhau);

    /** Tìm kiếm, phân trang, sắp xếp danh sách người dùng */
    List<UserListDTO> searchUsers(String keyword, String roleFilter, String sortCol, boolean sortAsc, int offset, int pageSize) throws SQLException;
    int countUsers(String keyword, String roleFilter) throws SQLException;

    /** Kiểm tra trùng tên đăng nhập */
    boolean isDuplicateUsername(String tenDN, int excludeMaND) throws SQLException;

    /** CRUD người dùng */
    void insertUser(User user) throws SQLException;
    void updateUser(User user, boolean updatePassword) throws SQLException;
    void updateStatus(int maND, boolean newStatus) throws SQLException;
    void updatePassword(int maND, String newPassword) throws SQLException;
}
