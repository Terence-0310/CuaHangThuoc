package presentation.presenter;

import domain.entity.Supplier;
import presentation.view.ISupplierManagementView;
import service.ISupplierService;

import javax.swing.JOptionPane;
import java.util.List;

public class SupplierPresenter {
    private final ISupplierManagementView view;
    private final ISupplierService supplierService;

    public SupplierPresenter(ISupplierManagementView view, ISupplierService supplierService) {
        this.view = view;
        this.supplierService = supplierService;
    }

    public void loadSuppliers(int page, int pageSize, String keyword, String statusFilter, String sortCol, String sortDir) {
        try {
            view.showLoading();
            List<Supplier> data = supplierService.getPagedList((page - 1) * pageSize, pageSize, keyword, statusFilter, sortCol, sortDir);
            int total = supplierService.countFiltered(keyword, statusFilter);
            int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
            view.displaySuppliers(data, page, totalPages);
        } catch (Exception e) {
            e.printStackTrace();
            view.showError("Lỗi tải danh sách nhà cung cấp: " + e.getMessage());
        } finally {
            view.hideLoading();
        }
    }

    public void addSupplier(Supplier supplier) {
        try {
            view.showLoading();
            int id = supplierService.add(supplier);
            if (id > 0) {
                view.showMessage("Thêm nhà cung cấp thành công!\nMã NCC: " + id, "Thành công", JOptionPane.INFORMATION_MESSAGE);
                view.clearForm();
                view.refreshData();
            }
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage();
            if (msg != null && msg.contains("UQ_NhaCungCap_SoDT")) {
                view.showMessage("Số điện thoại này đã tồn tại trong hệ thống!", "Trùng Số điện thoại", JOptionPane.WARNING_MESSAGE);
            } else if (msg != null && msg.contains("UQ_NhaCungCap_Email")) {
                view.showMessage("Email này đã tồn tại trong hệ thống!", "Trùng Email", JOptionPane.WARNING_MESSAGE);
            } else {
                view.showError("Lỗi thêm NCC: " + msg);
            }
        } finally {
            view.hideLoading();
        }
    }

    public void updateSupplier(Supplier supplier) {
        try {
            view.showLoading();
            boolean ok = supplierService.update(supplier);
            if (ok) {
                view.showMessage("Cập nhật thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                view.refreshData();
            }
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage();
            if (msg != null && msg.contains("UQ_NhaCungCap_SoDT")) {
                view.showMessage("Số điện thoại này đã được sử dụng bởi NCC khác!", "Trùng Số điện thoại", JOptionPane.WARNING_MESSAGE);
            } else if (msg != null && msg.contains("UQ_NhaCungCap_Email")) {
                view.showMessage("Email này đã được sử dụng bởi NCC khác!", "Trùng Email", JOptionPane.WARNING_MESSAGE);
            } else {
                view.showError("Lỗi cập nhật NCC: " + msg);
            }
        } finally {
            view.hideLoading();
        }
    }

    public void deleteSupplier(int maNCC) {
        try {
            view.showLoading();
            boolean ok = supplierService.delete(maNCC);
            if (ok) {
                view.showMessage("Đã khóa nhà cung cấp thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                view.clearForm();
                view.refreshData();
            }
        } catch (Exception e) {
            e.printStackTrace();
            view.showError("Lỗi xóa/khóa NCC: " + e.getMessage());
        } finally {
            view.hideLoading();
        }
    }
}
