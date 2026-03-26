package presentation.view;

import domain.dto.UserListDTO;
import java.util.List;

public interface IUserManagementView {
    void displayUsers(List<UserListDTO> users, int currentPage, int totalPages);
    void showLoading();
    void hideLoading();
    void showMessage(String message, String title, int messageType);
    void showError(String message);
    void clearForm();
    void refreshData();
}
