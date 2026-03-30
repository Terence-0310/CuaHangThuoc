package presentation.presenter;

import domain.entity.Customer;
import domain.dto.CartItem;
import presentation.view.IPOSView;
import service.ISaleService;
import javax.swing.SwingWorker;
import java.math.BigDecimal;
import java.util.List;

public class POSPresenter {
    private final IPOSView view;
    private final ISaleService saleService;

    public POSPresenter(IPOSView view, ISaleService saleService) {
        this.view = view;
        this.saleService = saleService;
    }

    public void searchProducts(String keyword) {
        view.showLoading(true);
        SwingWorker<List<Object[]>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Object[]> doInBackground() {
                return saleService.searchProductsForSale(keyword);
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> products = get();
                    view.displayProducts(products);
                } catch (Exception e) {
                    view.showMessage("Lỗi tải sản phẩm: " + e.getMessage(), "Lỗi", javax.swing.JOptionPane.ERROR_MESSAGE);
                } finally {
                    view.showLoading(false);
                }
            }
        };
        worker.execute();
    }

    public void lookupCustomerByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            view.displayCustomerInfo(null);
            return;
        }

        SwingWorker<Customer, Void> worker = new SwingWorker<>() {
            @Override
            protected Customer doInBackground() {
                return saleService.findCustomerByPhone(phone.trim());
            }

            @Override
            protected void done() {
                try {
                    Customer c = get();
                    view.displayCustomerInfo(c);
                } catch (Exception e) {
                    view.displayCustomerInfo(null);
                }
            }
        };
        worker.execute();
    }

    public void processCheckout(List<CartItem> cartSnapshot, String phone, String name, String gioiTinh, String payMethod) {
        view.showLoading(true);

        SwingWorker<Integer, Void> worker = new SwingWorker<>() {
            String errorMsg = null;
            @Override
            protected Integer doInBackground() {
                try {
                    return saleService.checkout(cartSnapshot,
                            phone.isEmpty() ? null : phone,
                            name.isEmpty() ? null : name,
                            gioiTinh,
                            payMethod);
                } catch (Exception e) {
                    errorMsg = e.getMessage();
                    return -1;
                }
            }

            @Override
            protected void done() {
                view.showLoading(false);
                try {
                    int maHD = get();
                    if (maHD > 0) {
                        BigDecimal total = BigDecimal.ZERO;
                        for (CartItem i : cartSnapshot) total = total.add(i.getThanhTien());
                        view.showCheckoutSuccess(maHD, total, payMethod, phone);
                    } else {
                        view.showMessage("Lỗi thanh toán:\n" + (errorMsg != null ? errorMsg : "Không xác định"),
                                "Lỗi", javax.swing.JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    view.showMessage("Lỗi: " + e.getMessage(), "Lỗi", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}
