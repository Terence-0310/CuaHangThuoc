package presentation.presenter;

import common.Session;
import domain.dto.ImportCartItem;
import domain.entity.Product;
import infrastructure.repository.NhapKhoDAO;
import presentation.view.IImportView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ★ ImportPresenter — Logic + State cho module Nhập Kho (MVP)
 *
 * KHÔNG import Swing/AWT.
 * Giao tiếp UI qua IImportView.
 */
public class ImportPresenter {

    private final IImportView view;
    private final NhapKhoDAO nhapKhoDAO;
    private List<Product> productList = new ArrayList<>();
    private final List<ImportCartItem> cart = new ArrayList<>();

    public ImportPresenter(IImportView view, NhapKhoDAO nhapKhoDAO) {
        this.view = view;
        this.nhapKhoDAO = nhapKhoDAO;
    }

    /** Khoi tao: load SP + NCC */
    public void init() {
        loadProducts();
        loadSuppliers();
    }

    public void loadProducts() {
        productList = nhapKhoDAO.getAllActiveProducts();
        view.setProductList(productList);
    }

    public void loadSuppliers() {
        view.setSupplierList(nhapKhoDAO.getActiveSuppliers());
    }

    // ================================================================
    //  Auto-suggest: Khi user chọn SP có sẵn → điền form
    // ================================================================

    public void onProductSelected(Product p) {
        if (p == null) return;
        view.setDonViTinhText(p.getDonViTinh());
        view.setGiaBanText(p.getGiaBan() != null ? String.format("%,.0f", p.getGiaBan()) : "");
    }



    // ================================================================
    //  Thêm vào giỏ
    // ================================================================

    public void addToCart() {
        // --- Validate SP ---
        String tenSP = view.getTenSP().trim();
        if (tenSP.isEmpty()) {
            view.showWarning("Tên sản phẩm không được để trống!");
            view.focusTenSP();
            return;
        }
        String dvt = view.getDonViTinh().trim();
        if (dvt.isEmpty()) {
            view.showWarning("Đơn vị tính không được để trống!");
            return;
        }

        BigDecimal giaBan;
        try {
            giaBan = new BigDecimal(view.getGiaBanText().replace(",", "").trim());
            if (giaBan.signum() <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            view.showWarning("Giá bán phải là số > 0!");
            return;
        }


        // --- Validate Lô hàng ---
        String soLo = view.getSoLoText().trim();
        if (soLo.isEmpty()) {
            view.showWarning("Số lô không được để trống!");
            return;
        }
        // ★ Check trùng số lô trong giỏ
        for (ImportCartItem existing : cart) {
            if (existing.getSoLo().equalsIgnoreCase(soLo)) {
                view.showWarning("So lo \"" + soLo + "\" da co trong gio!\nMoi so lo chi duoc nhap 1 lan.");
                return;
            }
        }
        // ★ Check trùng số lô trong DB
        if (nhapKhoDAO.existsSoLo(soLo)) {
            view.showWarning("So lo \"" + soLo + "\" da ton tai trong he thong!\nVui long nhap so lo khac.");
            return;
        }

        LocalDate hsd = view.getHanSuDungDate();
        if (hsd == null) {
            view.showWarning("Vui lòng chọn hạn sử dụng!");
            return;
        }
        if (hsd.isBefore(LocalDate.now())) {
            view.showWarning("Hạn sử dụng đã quá hạn!\nKhông được nhập thuốc hết hạn.");
            return;
        }

        int soLuong;
        try {
            soLuong = Integer.parseInt(view.getSoLuongText().replace(",", "").trim());
            if (soLuong <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            view.showWarning("Số lượng phải là số nguyên > 0!");
            return;
        }

        BigDecimal giaNhap;
        try {
            giaNhap = new BigDecimal(view.getGiaNhapText().replace(",", "").trim());
            if (giaNhap.signum() <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            view.showWarning("Giá nhập phải là số > 0!");
            return;
        }

        // --- Tạo DTO ---
        ImportCartItem item = new ImportCartItem();
        item.setTenSP(tenSP);
        item.setDonViTinh(dvt);
        item.setGiaBan(giaBan);
        item.setSoLo(soLo);
        item.setHanSuDung(hsd);
        item.setSoLuong(soLuong);
        item.setGiaNhap(giaNhap);

        // Check SP có sẵn?
        Product existing = findProduct(tenSP, dvt);
        item.setNew(existing == null);
        item.setMaSP(existing != null ? existing.getMaSP() : -1);

        cart.add(item);

        // ★ Nếu SP mới → thêm vào productList local để auto-suggest lần sau
        if (existing == null) {
            Product tempP = new Product();
            tempP.setMaSP(-1); // chưa có MaSP thật, sẽ có sau khi lưu
            tempP.setTenSP(tenSP);
            tempP.setDonViTinh(dvt);
            tempP.setGiaBan(giaBan);
            tempP.setTrangThai(true);
            productList.add(tempP);
            view.setProductList(productList);
        }

        // --- Thêm vào JTable ---
        view.addCartRow(tenSP, dvt, soLo,
                hsd.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy")),
                soLuong, String.format("%,.0f", giaNhap));

        updateTotal();
        view.clearForm();
        view.focusTenSP();
    }

    // ================================================================
    //  Xóa dòng khỏi giỏ
    // ================================================================

    public void removeFromCart() {
        int row = view.getSelectedCartRow();
        if (row < 0 || row >= cart.size()) {
            view.showWarning("Vui lòng chọn dòng cần xóa!");
            return;
        }
        cart.remove(row);
        view.removeCartRow(row);
        updateTotal();
    }

    // ================================================================
    //  Lưu phiếu nhập — gọi NhapKhoDAO (Transaction)
    // ================================================================

    public void saveImportTicket() {
        if (!Session.isAdmin()) {
            view.showWarning("Bạn không có quyền nhập kho!");
            return;
        }
        if (cart.isEmpty()) {
            view.showWarning("Giỏ nhập trống!\nHãy thêm ít nhất 1 lô hàng.");
            return;
        }

        // Confirm
        int totalItems = cart.size();
        BigDecimal totalMoney = BigDecimal.ZERO;
        for (ImportCartItem item : cart) {
            totalMoney = totalMoney.add(item.getTongTienDong());
        }
        String msg = "Xác nhận lưu phiếu nhập?\n"
                + "• Số dòng: " + totalItems + "\n"
                + "• Tổng tiền: " + String.format("%,.0f", totalMoney) + " VNĐ";
        if (!view.confirm(msg, "Xác nhận nhập kho")) return;

        // Validate NCC
        int maNCC = view.getSelectedMaNCC();
        if (maNCC <= 0) {
            view.showWarning("Vui long chon nha cung cap!");
            return;
        }

        view.setLoading(true);
        try {
            int maPN = nhapKhoDAO.saveImportTicket(cart, maNCC);
            view.showInfo("Lưu thành công!\nMã phiếu nhập: PN-" + maPN
                    + "\nSố lô hàng: " + totalItems
                    + "\nTổng tiền: " + String.format("%,.0f", totalMoney) + " VNĐ");

            // Reset
            cart.clear();
            view.clearCart();
            updateTotal();
            loadProducts(); // refresh danh sách SP (có thể có SP mới)
            view.focusTenSP();

        } catch (Exception e) {
            view.showError("Lỗi lưu phiếu nhập:\n" + e.getMessage());
        } finally {
            view.setLoading(false);
        }
    }

    // ================================================================
    //  Helpers
    // ================================================================

    private Product findProduct(String tenSP, String dvt) {
        String keyName = tenSP.trim().toLowerCase();
        String keyUnit = dvt.trim().toLowerCase();
        for (Product p : productList) {
            if (p.getTenSP().trim().toLowerCase().equals(keyName)
                    && p.getDonViTinh().trim().toLowerCase().equals(keyUnit)) {
                return p;
            }
        }
        return null;
    }

    private void updateTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (ImportCartItem item : cart) {
            total = total.add(item.getTongTienDong());
        }
        view.updateTotalLabel(String.format("%,.0f VNĐ", total));
    }
}
