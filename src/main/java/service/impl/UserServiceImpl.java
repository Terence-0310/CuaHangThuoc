package service.impl;

import domain.dto.PagedResult;
import domain.dto.UserListDTO;
import domain.entity.User;
import domain.repository.IUserRepository;
import service.IUserService;

import java.sql.SQLException;
import java.util.List;

public class UserServiceImpl implements IUserService {

    private final IUserRepository userRepository;

    public UserServiceImpl(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public PagedResult<UserListDTO> searchUsers(String keyword, String roleFilter, String sortCol, boolean sortAsc, int page, int pageSize) throws SQLException {
        int offset = (page - 1) * pageSize;
        List<UserListDTO> data = userRepository.searchUsers(keyword, roleFilter, sortCol, sortAsc, offset, pageSize);
        int total = userRepository.countUsers(keyword, roleFilter);
        int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        return new PagedResult<>(data, totalPages);
    }

    @Override
    public boolean isDuplicateUsername(String tenDN, int excludeMaND) throws SQLException {
        return userRepository.isDuplicateUsername(tenDN, excludeMaND);
    }

    @Override
    public void insertUser(User user) throws SQLException {
        userRepository.insertUser(user);
    }

    @Override
    public void updateUser(User user, boolean updatePassword) throws SQLException {
        userRepository.updateUser(user, updatePassword);
    }

    @Override
    public void toggleUserLock(int maND, boolean newStatus) throws SQLException {
        userRepository.updateStatus(maND, newStatus);
    }

    @Override
    public void resetPassword(int maND, String newPassword) throws SQLException {
        userRepository.updatePassword(maND, newPassword);
    }
}
