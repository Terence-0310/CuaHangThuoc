package presentation.view;

import domain.entity.Product;
import java.util.List;

/**
 * ★ View Interface (MVP Pattern)
 * 
 * Đây là "hợp đồng" giữa Presenter và View.
 * - Presenter CHỈ gọi các hàm trong interface này
 * - View (ProductPanel / ProductFxView / ...) implement interface này
 * 
 * → Đổi UI? Chỉ cần tạo class mới implement IProductView.
 *   Presenter + Service + DAO không cần sửa 1 dòng nào.
 */
public interface IProductView {

    // ============== ĐỌC DỮ LIỆU TỪ FORM ==============

    /** Lấy keyword tìm kiếm */
    String getSearchKeyword();

    /** Lấy filter trạng thái (Tất cả / Đang bán / Ngừng bán) */
    String getStatusFilter();

    /** Lấy tên SP từ form */
    String getTenSP();

    /** Lấy đơn vị tính từ form */
    String getDonViTinh();

    /** Lấy text giá bán (raw string, chưa parse) */
    String getGiaBanText();



    // ============== HIỂN THỊ DỮ LIỆU LÊN FORM ==============

    /** Hiển thị danh sách sản phẩm lên bảng */
    void displayProducts(List<Product> products, java.util.Set<Integer> checkedIds);

    /** Điền thông tin sản phẩm vào form */
    void displayFormData(int maSP, String tenSP, String donViTinh,
                         String giaBan, boolean dangBan);

    /** Xóa trắng tất cả field trong form */
    void clearForm();



    // ============== PHÂN TRANG ==============

    /** Cập nhật UI phân trang (nút, số trang, ...) */
    void updatePaginationUI(int currentPage, int totalPages);

    // ============== TRẠNG THÁI NÚT ==============

    /** Cập nhật trạng thái nút Thêm / Sửa / Xóa */
    void updateButtonState(boolean isEditMode);

    /** Ẩn/hiện panel bulk action */
    void setBulkPanelVisible(boolean visible);

    /** Đổi text + màu nút toggle (Ngừng Bán / Khôi Phục) */
    void setToggleButton(boolean dangBan);

    // ============== THÔNG BÁO ==============

    void showInfo(String msg);
    void showWarning(String msg);
    void showError(String msg);

    /**
     * Hiện confirm dialog
     * @return true nếu user chọn YES
     */
    boolean confirm(String message, String title, boolean isWarning);

    // ============== LOADING STATE ==============

    /** Bật/tắt trạng thái loading (đổi cursor, khóa header...) */
    void setLoading(boolean loading);

    // ============== TABLE CHECKBOX ==============

    /** Lấy row đang selected (cho fillForm) */
    int getSelectedTableRow();

    /** Lấy MaSP ở hàng row, cột 1 */
    int getRowMaSP(int row);

    /** Set checkbox tất cả dòng */
    void setAllCheckboxes(boolean checked);

    /** Repaint header checkbox */
    void refreshHeaderCheckbox();

    /** Lấy số dòng đang hiển thị trong bảng */
    int getTableRowCount();

    /** Kiểm tra checkbox dòng row có được tick không */
    boolean isRowChecked(int row);

    /** Focus vào ô tên SP */
    void focusTenSP();

    /** Xóa selection bảng */
    void clearTableSelection();

    /** Điều chỉnh row height theo viewport */
    void adjustRowHeight(int rowCount, int pageSize);
}
