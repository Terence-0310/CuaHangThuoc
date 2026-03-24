package service;

import domain.dto.CartItem;
import java.util.List;

/**
 * Service Interface: Bán hàng (POS) — Logic FEFO
 */
public interface ISaleService {
    boolean checkout(List<CartItem> cart, String soDT, String tenKH, String phuongThucTT);
}
