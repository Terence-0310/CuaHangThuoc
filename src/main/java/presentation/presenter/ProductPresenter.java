package presentation.presenter;

import common.Session;
import domain.entity.Product;
import presentation.view.IProductView;
import service.IProductService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ★ ProductPresenter (MVP Pattern)
 * 
 * Chứa TOÀN BỘ business logic + state management.
 * KHÔNG import bất kỳ thứ gì từ javax.swing hay java.awt.
 * 
 * → Đổi UI? File này KHÔNG CẦN SỬA.
 * → Test logic? Chỉ cần mock IProductView + IProductService.
 */
public class ProductPresenter {

    private final IProductView view;
    private final IProductService productService;

    // ============== STATE ==============
    public static final int PAGE_SIZE = 20;

    private int currentPage = 1;
    private int totalPages = 1;
    private int selectedMaSP = -1;
    private String sortColumn = "MaSP";
    private String sortDirection = "ASC";
    private volatile boolean isLoading = false;
    private final Set<Integer> globalSelectedIds = new HashSet<>();

    // ★ Whitelist: map UI column index → DB column name
    // index 0 = Checkbox (skip), 1 = MaSP, 2 = TenSP, ...
    private static final String[] COLUMN_DB_NAMES = {
        null, "MaSP", "TenSP", "DonViTinh", "GiaBan", "TongTonKho", "TrangThai"
    };

    // ============== CONSTRUCTOR ==============

    public ProductPresenter(IProductView view, IProductService productService) {
        this.view = view;
        this.productService = productService;
    }

    // ============== GETTERS (cho View đọc state) ==============

    public int getCurrentPage()   { return currentPage; }
    public int getTotalPages()    { return totalPages; }
    public int getSelectedMaSP()  { return selectedMaSP; }
    public String getSortColumn() { return sortColumn; }
    public String getSortDir()    { return sortDirection; }
    public Set<Integer> getGlobalSelectedIds() { return globalSelectedIds; }
    public boolean isLoading()    { return isLoading; }

    // ============== DATA LOADING ==============

    /**
     * ★ Load trang sản phẩm — gọi Service, trả kết quả về View
     * return data qua callback interface, View tự quyết hiển thị thế nào.
     */
    public void loadPage(int page) {
        if (isLoading) return;
        isLoading = true;
        view.setLoading(true);

        try {
            String kw = view.getSearchKeyword();
            String sf = view.getStatusFilter();

            int total = productService.countFiltered(kw, sf);
            totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
            currentPage = Math.min(page, totalPages);

            int offset = (currentPage - 1) * PAGE_SIZE;
            List<Product> products = productService.getPagedWithStock(
                    offset, PAGE_SIZE, kw, sf, sortColumn, sortDirection);

            view.displayProducts(products, globalSelectedIds);
            view.updatePaginationUI(currentPage, totalPages);
            view.adjustRowHeight(products.size(), PAGE_SIZE);
        } catch (Exception ex) {
            ex.printStackTrace();
            Throwable root = ex.getCause();
            while (root != null && root.getCause() != null) root = root.getCause();
            String msg = root != null ? root.getMessage() : ex.getMessage();
            view.showError("Lỗi tải dữ liệu:\n" + msg);
        } finally {
            isLoading = false;
            view.setLoading(false);
            view.updateButtonState(selectedMaSP >= 0);
        }
    }

    // ============== PAGINATION ==============

    public void goFirstPage() { loadPage(1); }
    public void goPrevPage()  { if (currentPage > 1) loadPage(currentPage - 1); }
    public void goNextPage()  { if (currentPage < totalPages) loadPage(currentPage + 1); }
    public void goLastPage()  { loadPage(totalPages); }
    public void goToPage(int page) { loadPage(page); }

    // ============== SORTING ==============

    /**
     * ★ Xử lý click header cột
     * @param colIndex index cột trong bảng (0 = checkbox, 1 = MaSP, ...)
     */
    public void onHeaderClick(int colIndex) {
        // Col 0 = checkbox → xử lý ở onSelectAllToggle()
        if (colIndex <= 0 || colIndex >= COLUMN_DB_NAMES.length) return;

        String clickedCol = COLUMN_DB_NAMES[colIndex];
        if (clickedCol == null) return;

        if (isLoading) return;

        if (clickedCol.equals(sortColumn)) {
            sortDirection = "ASC".equals(sortDirection) ? "DESC" : "ASC";
        } else {
            sortColumn = clickedCol;
            sortDirection = "ASC";
        }
        currentPage = 1;
        loadPage(1);
    }

    // ============== CHECKBOX / BULK ==============

    /** Header checkbox: toggle tất cả dòng trên trang hiện tại */
    public void onSelectAllToggle() {
        int rowCount = view.getTableRowCount();
        if (rowCount == 0) return;

        // Kiểm tra xem tất cả đã tick chưa
        boolean allSelected = true;
        for (int i = 0; i < rowCount; i++) {
            if (!view.isRowChecked(i)) {
                allSelected = false;
                break;
            }
        }

        boolean newVal = !allSelected;
        for (int i = 0; i < rowCount; i++) {
            int maSP = view.getRowMaSP(i);
            if (newVal) globalSelectedIds.add(maSP);
            else globalSelectedIds.remove(maSP);
        }
        view.setAllCheckboxes(newVal);
        updateBulkVisibility();
        view.refreshHeaderCheckbox();
    }

    /** Khi 1 checkbox thay đổi */
    public void onCheckboxChanged(int row, boolean checked) {
        int maSP = view.getRowMaSP(row);
        if (checked) {
            globalSelectedIds.add(maSP);
        } else {
            globalSelectedIds.remove(maSP);
        }
        updateBulkVisibility();
        view.refreshHeaderCheckbox();
    }

    public void updateBulkVisibility() {
        if (!Session.isAdmin()) return;
        int checkedCount = 0;
        for (int i = 0; i < view.getTableRowCount(); i++) {
            if (view.isRowChecked(i)) checkedCount++;
        }
        boolean show = checkedCount >= 2 || globalSelectedIds.size() >= 2;
        view.setBulkPanelVisible(show);
        view.updateButtonState(selectedMaSP >= 0);
    }

    // ============== ROW SELECTION ==============

    /** Khi user click 1 dòng trong bảng → điền form */
    public void onRowSelected() {
        int row = view.getSelectedTableRow();
        if (row < 0) return;

        selectedMaSP = view.getRowMaSP(row);
        Product p = productService.getById(selectedMaSP);
        if (p == null) return;

        String giaBan = p.getGiaBan() != null ? p.getGiaBan().toBigInteger().toString() : "";

        view.displayFormData(
            p.getMaSP(), p.getTenSP(), p.getDonViTinh(),
            giaBan, p.isTrangThai()
        );
        view.updateButtonState(true);
    }

    // ============== CRUD ==============

    public void doAdd() {
        if (!Session.isAdmin()) { view.showWarning("Bạn không có quyền thực hiện!"); return; }
        if (selectedMaSP >= 0) {
            view.showWarning("Bạn đang chọn sản phẩm có sẵn!\nHãy bấm 'Làm Mới' trước khi thêm mới.");
            return;
        }
        try {
            Product p = buildProductFromView();
            if (productService.existsByName(p.getTenSP())) {
                view.showWarning("Sản phẩm '" + p.getTenSP() + "' đã tồn tại!\nKhông được phép trùng tên sản phẩm.");
                return;
            }
            p.setTrangThai(true);
            int newId = productService.insert(p);
            view.showInfo("Thêm sản phẩm thành công! Mã SP: " + newId);
            doClearForm();
            loadPage(currentPage);
        } catch (IllegalArgumentException ex) {
            view.showWarning(ex.getMessage());
        } catch (Exception ex) {
            view.showError("Lỗi hệ thống: " + ex.getMessage());
        }
    }

    public void doUpdate() {
        if (!Session.isAdmin()) { view.showWarning("Bạn không có quyền!"); return; }
        if (selectedMaSP < 0) { view.showWarning("Vui lòng chọn sản phẩm cần sửa!"); return; }

        if (!view.confirm("Bạn có chắc muốn cập nhật sản phẩm này?", "Xác nhận", false)) return;

        try {
            Product p = buildProductFromView();
            p.setMaSP(selectedMaSP);
            // ★ Check trùng tên (loại trừ chính nó)
            if (productService.existsByNameExcluding(p.getTenSP(), selectedMaSP)) {
                view.showWarning("Sản phẩm '" + p.getTenSP() + "' đã tồn tại!\nKhông được phép trùng tên sản phẩm.");
                return;
            }
            p.setTrangThai(true);
            productService.update(p);
            view.showInfo("Cập nhật thành công!");
            doClearForm();
            loadPage(currentPage);
        } catch (IllegalArgumentException ex) {
            view.showWarning(ex.getMessage());
        } catch (Exception ex) {
            view.showError("Lỗi hệ thống: " + ex.getMessage());
        }
    }

    public void doToggle() {
        if (!Session.isAdmin()) { view.showWarning("Bạn không có quyền!"); return; }
        if (selectedMaSP < 0) { view.showWarning("Vui lòng chọn sản phẩm!"); return; }

        Product current = productService.getById(selectedMaSP);
        if (current == null) return;

        String action = current.isTrangThai() ? "NGỪNG BÁN" : "KHÔI PHỤC";
        if (!view.confirm(
                "Bạn có chắc muốn " + action + " sản phẩm '" + current.getTenSP() + "'?",
                "Xác nhận " + action,
                current.isTrangThai())) return;

        try {
            current.setTrangThai(!current.isTrangThai());
            productService.update(current);
            view.showInfo(action + " thành công!");
            doClearForm();
            loadPage(currentPage);
        } catch (Exception ex) {
            view.showError("Lỗi: " + ex.getMessage());
        }
    }

    public void doBulkAction(boolean newStatus) {
        if (!Session.isAdmin()) { view.showWarning("Bạn không có quyền!"); return; }

        List<Integer> selectedIds = new ArrayList<>(globalSelectedIds);
        if (selectedIds.isEmpty()) {
            view.showWarning("Vui lòng tick chọn ít nhất 1 sản phẩm!");
            return;
        }

        String action = newStatus ? "KHÔI PHỤC" : "NGỪNG BÁN";
        if (!view.confirm(
                action + " hàng loạt " + selectedIds.size() + " sản phẩm đã chọn?",
                "Xác nhận " + action,
                !newStatus)) return;

        try {
            int affected = productService.bulkUpdateStatus(selectedIds, newStatus);
            view.showInfo(action + " thành công " + affected + " sản phẩm!");
            globalSelectedIds.clear();
            loadPage(currentPage);
        } catch (Exception ex) {
            view.showError("Lỗi: " + ex.getMessage());
        }
    }

    // ============== FORM HELPERS ==============

    public void doClearForm() {
        selectedMaSP = -1;
        view.clearForm();
        view.setToggleButton(true);
        view.updateButtonState(false);
        view.focusTenSP();
    }

    /** Search thay đổi → reset page 1 */
    public void onSearchChanged() {
        currentPage = 1;
        loadPage(1);
    }

    /** Filter thay đổi → reset page 1 */
    public void onFilterChanged() {
        currentPage = 1;
        loadPage(1);
    }

    // ============== PRIVATE: Build Product từ View ==============

    private Product buildProductFromView() {
        Product p = new Product();

        String tenSP = view.getTenSP().trim();
        if (tenSP.isEmpty()) throw new IllegalArgumentException("Tên sản phẩm không được để trống!");

        String dvt = view.getDonViTinh().trim();
        if (dvt.isEmpty()) throw new IllegalArgumentException("Đơn vị tính không được để trống!");

        BigDecimal giaBan = parseMoney(view.getGiaBanText(), "Giá bán");

        p.setTenSP(tenSP);
        p.setDonViTinh(dvt);
        p.setGiaBan(giaBan);
        return p;
    }

    private BigDecimal parseMoney(String text, String fieldName) {
        try {
            String cleaned = text.trim()
                    .replace(",", "").replace(".", "")
                    .replace(" ", "").replace("VNĐ", "").replace("VND", "");
            if (cleaned.isEmpty()) throw new IllegalArgumentException(fieldName + " không được để trống!");
            BigDecimal value = new BigDecimal(cleaned);
            if (value.signum() < 0) throw new IllegalArgumentException(fieldName + " phải >= 0!");
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " không hợp lệ! Vui lòng nhập số.");
        }
    }
}
