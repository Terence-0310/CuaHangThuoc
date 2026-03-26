package presentation.presenter;

import domain.dto.CustomerPurchaseHistoryDTO;
import domain.dto.InvoiceFilterCriteria;
import domain.dto.InvoiceListDTO;
import domain.dto.PagedResult;
import domain.entity.Customer;
import presentation.view.ICustomerManagementView;
import service.ICustomerService;
import service.IInvoiceService;

import javax.swing.JOptionPane;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;

public class CustomerPresenter {
    private final ICustomerManagementView view;
    private final ICustomerService customerService;
    private final IInvoiceService invoiceService;
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,###");

    public CustomerPresenter(ICustomerManagementView view, ICustomerService customerService, IInvoiceService invoiceService) {
        this.view = view;
        this.customerService = customerService;
        this.invoiceService = invoiceService;
    }

    public void loadCustomers(int page, int pageSize, String keyword, String sortCol, String sortDir) {
        try {
            view.showLoading();
            List<Customer> data = customerService.getPagedList((page - 1) * pageSize, pageSize, keyword, sortCol, sortDir);
            int total = customerService.countFiltered(keyword);
            int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
            view.displayCustomers(data, page, totalPages);
        } catch (Exception e) {
            e.printStackTrace();
            view.showError("Lỗi tải danh sách khách hàng: " + e.getMessage());
        } finally {
            view.hideLoading();
        }
    }

    public void loadWalkInInvoices() {
        try {
            InvoiceFilterCriteria criteria = new InvoiceFilterCriteria();
            criteria.setCustomerTypeFilter(2); // 2 = Walk-in
            
            // Get all walk in invoices, safely use a larger max bounds or just the default backend limit.
            // Using a large number to emulate existing no pagination rule.
            PagedResult<InvoiceListDTO> result = invoiceService.searchInvoices(criteria, 1, 1000);
            
            BigDecimal revenue = BigDecimal.ZERO;
            for (InvoiceListDTO dto : result.getData()) {
                if (dto.getTongTien() != null && dto.getTongTien().signum() > 0) {
                    revenue = revenue.add(dto.getTongTien());
                }
            }
            
            view.displayWalkInInvoices(result.getData(), MONEY_FMT.format(revenue) + " VNĐ");
        } catch (Exception e) {
            e.printStackTrace();
            view.showError("Lỗi tải danh sách khách vãng lai: " + e.getMessage());
        }
    }

    public void addCustomer(Customer customer) {
        try {
            view.showLoading();
            int id = customerService.insert(customer);
            if (id > 0) {
                view.showMessage("Thêm khách hàng thành công!\nMã KH: " + id, "Thành công", JOptionPane.INFORMATION_MESSAGE);
                view.clearForm();
                view.refreshData();
            }
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage();
            if (msg != null && msg.contains("UQ_KhachHang_SoDT")) {
                view.showMessage("Số điện thoại này đã tồn tại trong hệ thống!", "Trùng SĐT", JOptionPane.WARNING_MESSAGE);
            } else {
                view.showError("Lỗi: " + msg);
            }
        } finally {
            view.hideLoading();
        }
    }

    public void updateCustomer(Customer customer) {
        try {
            view.showLoading();
            boolean ok = customerService.update(customer);
            if (ok) {
                view.showMessage("Cập nhật thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                view.refreshData();
            }
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage();
            if (msg != null && msg.contains("UQ_KhachHang_SoDT")) {
                view.showMessage("Số điện thoại này đã được sử dụng bởi khách hàng khác!", "Trùng SĐT", JOptionPane.WARNING_MESSAGE);
            } else {
                view.showError("Lỗi: " + msg);
            }
        } finally {
            view.hideLoading();
        }
    }

    public void deleteCustomer(int maKH, String tenKH) {
        try {
            view.showLoading();
            boolean ok = customerService.delete(maKH);
            if (ok) {
                view.showMessage("Đã xóa khách hàng thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                view.clearForm();
                view.refreshData();
            }
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage();
            if (msg != null && msg.contains("KHÁCH CÓ HOÁ ĐƠN")) {
                view.showMessage("Không thể xóa!\n\nKhách hàng này đã có hóa đơn trong hệ thống.\nXóa sẽ vi phạm toàn vẹn dữ liệu.", "Không thể xóa", JOptionPane.ERROR_MESSAGE);
            } else {
                view.showError("Lỗi: " + msg);
            }
        } finally {
            view.hideLoading();
        }
    }

    public void loadPurchaseHistory(int maKH, String customerName, String customerPhone) {
        try {
            view.showLoading();
            List<CustomerPurchaseHistoryDTO> history = customerService.getCustomerPurchaseHistory(maKH);
            view.showPurchaseHistoryDialog(customerName, customerPhone, history);
        } catch (Exception e) {
            e.printStackTrace();
            view.showError("Lỗi tải lịch sử mua hàng: " + e.getMessage());
        } finally {
            view.hideLoading();
        }
    }
}
