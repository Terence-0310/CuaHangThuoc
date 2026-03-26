package presentation.view;

import domain.entity.Customer;
import domain.dto.CartItem;
import domain.dto.HeldOrder;
import java.util.List;

public interface IPOSView {
    void displayProducts(List<Object[]> products);
    void displayCustomerInfo(Customer customer);
    
    // Cart operations
    void refreshCartTable();
    void clearCart();
    
    // UI State
    void showMessage(String message, String title, int messageType);
    void showCheckoutSuccess(int invoiceId, java.math.BigDecimal totalAmount, String payMethod, String dtKH);
    void showLoading(boolean isLoading);
    
    // Form and input
    String getSearchKeyword();
    String getCustomerPhoneInput();
    String getCustomerNameInput();
    String getPaymentMethod();
    
    // Manage state natively instead of pushing to view
    List<CartItem> getCartItems();
    List<HeldOrder> getHeldOrders();
}
