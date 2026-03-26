package service;

import domain.dto.CartItem;
import java.util.List;

/**
 * Service Interface: Bán hàng (POS) — Logic FEFO
 */
public interface ISaleService {
    int checkout(List<CartItem> cart, String soDT, String tenKH, String phuongThucTT);
    List<Object[]> searchProductsForSale(String keyword);
    domain.entity.Customer findCustomerByPhone(String phone);
}
