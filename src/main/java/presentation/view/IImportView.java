package presentation.view;

import domain.entity.Product;
import domain.entity.Supplier;
import java.util.List;

/**
 * ★ IImportView — Giao kèo View-Presenter cho module Nhập Kho
 *
 * Presenter gọi các hàm này để ra lệnh UI.
 * ImportPanel implements interface này.
 */
public interface IImportView {

    // === Đọc dữ liệu từ form ===
    String getTenSP();
    String getDonViTinh();
    String getGiaBanText();

    String getSoLuongText();
    String getSoLoText();
    String getHanSuDungText();
    String getGiaNhapText();

    // === Hiển thị ===
    void addCartRow(String tenSP, String dvt, String soLo, String hsd,
                    int soLuong, String giaNhap);
    void removeCartRow(int row);
    int getCartRowCount();
    int getSelectedCartRow();
    void clearCart();
    void clearForm();
    void updateTotalLabel(String text);
    void setDonViTinhText(String text);
    void setGiaBanText(String text);

    // === Auto-suggest: cập nhật danh sách SP ===
    void setProductList(List<Product> products);

    // === Nha cung cap ===
    int getSelectedMaNCC();
    void setSupplierList(List<Supplier> suppliers);

    // === Feedback ===
    void showInfo(String msg);
    void showWarning(String msg);
    void showError(String msg);
    boolean confirm(String message, String title);
    void setLoading(boolean loading);
    void focusTenSP();
}
