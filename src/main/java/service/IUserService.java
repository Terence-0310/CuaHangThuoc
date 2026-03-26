package service;

import domain.dto.PagedResult;
import domain.dto.UserListDTO;
import domain.entity.User;
import java.sql.SQLException;

public interface IUserService {
    PagedResult<UserListDTO> searchUsers(String keyword, String roleFilter, String sortCol, boolean sortAsc, int page, int pageSize) throws SQLException;
    boolean isDuplicateUsername(String tenDN, int excludeMaND) throws SQLException;
    void insertUser(User user) throws SQLException;
    void updateUser(User user, boolean updatePassword) throws SQLException;
    void toggleUserLock(int maND, boolean newStatus) throws SQLException;
    void resetPassword(int maND, String newPassword) throws SQLException;
}
