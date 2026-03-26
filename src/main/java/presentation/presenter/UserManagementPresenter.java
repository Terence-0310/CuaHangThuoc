package presentation.presenter;

import domain.dto.PagedResult;
import domain.dto.UserListDTO;
import domain.entity.User;
import presentation.view.IUserManagementView;
import service.IUserService;

import javax.swing.JOptionPane;
import java.sql.SQLException;

public class UserManagementPresenter {
    private final IUserManagementView view;
    private final IUserService userService;

    public UserManagementPresenter(IUserManagementView view, IUserService userService) {
        this.view = view;
        this.userService = userService;
    }

    public void loadUsers(String keyword, String roleFilter, String sortCol, boolean sortAsc, int page, int pageSize) {
        try {
            view.showLoading();
            PagedResult<UserListDTO> result = userService.searchUsers(keyword, roleFilter, sortCol, sortAsc, page, pageSize);
            view.displayUsers(result.getData(), page, result.getTotalPages());
        } catch (SQLException e) {
            e.printStackTrace();
            view.showError("Lỗi tải dữ liệu người dùng: " + e.getMessage());
        } finally {
            view.hideLoading();
        }
    }

    public void addUser(User user) {
        try {
            view.showLoading();
            if (userService.isDuplicateUsername(user.getTenDangNhap(), -1)) {
                view.showError("Tên đăng nhập \"" + user.getTenDangNhap() + "\" đã tồn tại!");
                return;
            }
            userService.insertUser(user);
            view.showMessage("Thêm người dùng thành công!\nMật khẩu: " + user.getMatKhau(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
            view.clearForm();
            view.refreshData();
        } catch (SQLException e) {
            e.printStackTrace();
            view.showError("Lỗi thêm người dùng: " + e.getMessage());
        } finally {
            view.hideLoading();
        }
    }

    public void updateUser(User user, boolean updatePassword) {
        try {
            view.showLoading();
            userService.updateUser(user, updatePassword);
            view.showMessage("Cập nhật thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            view.clearForm();
            view.refreshData();
        } catch (SQLException e) {
            e.printStackTrace();
            view.showError("Lỗi cập nhật: " + e.getMessage());
        } finally {
            view.hideLoading();
        }
    }

    public void toggleUserLock(int maND, boolean newStatus, String actionName) {
        try {
            view.showLoading();
            userService.toggleUserLock(maND, newStatus);
            view.showMessage(actionName + " thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            view.clearForm();
            view.refreshData();
        } catch (SQLException e) {
            e.printStackTrace();
            view.showError("Lỗi: " + e.getMessage());
        } finally {
            view.hideLoading();
        }
    }

    public void resetPassword(int maND, String defaultPassword) {
        try {
            view.showLoading();
            userService.resetPassword(maND, defaultPassword);
            view.showMessage("Reset mật khẩu thành công!\nMật khẩu mới: " + defaultPassword, "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            e.printStackTrace();
            view.showError("Lỗi reset mật khẩu: " + e.getMessage());
        } finally {
            view.hideLoading();
        }
    }
}
