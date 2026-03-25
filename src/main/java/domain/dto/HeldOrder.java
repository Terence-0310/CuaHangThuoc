package domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO: Đơn hàng tạm giữ (Hold Order)
 * Lưu trạng thái giỏ hàng + thông tin khách để phục hồi sau.
 */
public class HeldOrder {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final List<CartItem> items;
    private final String customerPhone;
    private final String customerName;
    private final String gioiTinh;
    private final String payMethod;
    private final LocalDateTime heldAt;
    private final String note;

    public HeldOrder(List<CartItem> items, String customerPhone, String customerName,
                     String gioiTinh, String payMethod, String note) {
        this.items = new ArrayList<>(items); // deep copy
        this.customerPhone = customerPhone;
        this.customerName = customerName;
        this.gioiTinh = gioiTinh;
        this.payMethod = payMethod;
        this.heldAt = LocalDateTime.now();
        this.note = note;
    }

    public BigDecimal getTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : items) total = total.add(item.getThanhTien());
        return total;
    }

    public int getItemCount() {
        return items.stream().mapToInt(CartItem::getSoLuong).sum();
    }

    public String getSummary() {
        String time = heldAt.format(TIME_FMT);
        String customer = (customerName != null && !customerName.isEmpty())
                ? customerName : (customerPhone != null && !customerPhone.isEmpty()
                ? customerPhone : "Khách vãng lai");
        return String.format("[%s] %s — %d SP — %,.0f VNĐ%s",
                time, customer, getItemCount(), getTotal(),
                (note != null && !note.isEmpty()) ? " (" + note + ")" : "");
    }

    // Getters
    public List<CartItem> getItems() { return items; }
    public String getCustomerPhone() { return customerPhone; }
    public String getCustomerName() { return customerName; }
    public String getGioiTinh() { return gioiTinh; }
    public String getPayMethod() { return payMethod; }
    public LocalDateTime getHeldAt() { return heldAt; }
    public String getNote() { return note; }
}
