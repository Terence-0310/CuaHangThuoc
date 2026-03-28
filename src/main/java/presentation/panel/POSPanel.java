package presentation.panel;

import common.AppColors;
import common.ServiceFactory;
import common.Session;
import domain.dto.CartItem;
import infrastructure.database.DatabaseHelper;
import service.ISaleService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * ★★★ POS Panel — Bán Hàng (Core Module) ★★★
 *
 * Layout: Split trái (Tìm SP + Giỏ hàng) | phải (Thanh toán)
 *
 * FEFO Logic:  Xử lý bên SaleServiceImpl (đã có sẵn)
 * Race Cond:   UPDLOCK trong BatchRepositoryImpl.getFEFO()
 * Transaction: SaleServiceImpl.checkout() dùng setAutoCommit(false) + rollback
 * UI Freeze:   SwingWorker cho nút Thanh Toán
 */
public class POSPanel extends JPanel {

    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,###");

    // === Service ===
    private final ISaleService saleService = ServiceFactory.getSaleService();

    // === Cart State ===
    private final List<CartItem> cart = new ArrayList<>();

    // === Left: Product search ===
    private JTextField txtSearchProduct;
    private DefaultTableModel productModel;
    private JTable productTable;

    // === Left: Cart ===
    private DefaultTableModel cartModel;
    private JTable cartTable;
    private JLabel lblCartTotal;

    // === Right: Checkout ===
    private JTextField txtCustomerPhone, txtCustomerName;
    private JComboBox<String> cboGioiTinh, cboPayMethod;
    private JTextField txtTienKhachDua;
    private JLabel lblTongTien, lblTienThoi, lblPhoneSuggest;
    private JPanel cashFieldsPanel;
    private JButton btnCheckout, btnClearCart, btnHoldOrder, btnViewHeld;
    private JLabel lblHeldBadge;
    private Timer searchTimer;
    private Timer phoneTimer;

    // === Held Orders ===
    private final List<domain.dto.HeldOrder> heldOrders = new ArrayList<>();

    public POSPanel() {
        setLayout(new BorderLayout());
        setBackground(AppColors.NEUTRAL);
        initComponents();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) { loadProducts(null); }
        });
    }

    // ================================================================
    //  LAYOUT
    // ================================================================

    private void initComponents() {
        // === TOP BAR ===
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(AppColors.PRIMARY_DARK);
        topBar.setBorder(new EmptyBorder(12, 24, 12, 24));

        JLabel lblTitle = new JLabel("BÁN HÀNG  —  POS");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);
        topBar.add(lblTitle, BorderLayout.WEST);

        JLabel lblUser = new JLabel("Nhân viên: " + Session.getCurrentUser().getHoTen() + "  ");
        lblUser.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblUser.setForeground(new Color(0xBB, 0xDE, 0xFB));
        topBar.add(lblUser, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // === MAIN SPLIT ===
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerLocation(700);
        mainSplit.setDividerSize(4);
        mainSplit.setBorder(null);
        mainSplit.setResizeWeight(0.65);

        mainSplit.setLeftComponent(createLeftPanel());
        mainSplit.setRightComponent(createRightPanel());

        add(mainSplit, BorderLayout.CENTER);
    }

    // ================================================================
    //  LEFT: Product Search + Cart
    // ================================================================

    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(Color.WHITE);

        // --- Search bar ---
        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setBackground(Color.WHITE);
        searchBar.setBorder(new EmptyBorder(14, 16, 10, 16));

        JLabel lblSearch = new JLabel("Tim:");
        lblSearch.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        searchBar.add(lblSearch, BorderLayout.WEST);

        txtSearchProduct = new JTextField();
        txtSearchProduct.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtSearchProduct.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AppColors.NEUTRAL_DARK, 1),
                new EmptyBorder(8, 12, 8, 12)));
        txtSearchProduct.setToolTipText("Tìm theo tên sản phẩm...");
        txtSearchProduct.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onProductSearch(); }
            @Override public void removeUpdate(DocumentEvent e) { onProductSearch(); }
            @Override public void changedUpdate(DocumentEvent e) { onProductSearch(); }
        });
        searchBar.add(txtSearchProduct, BorderLayout.CENTER);
        panel.add(searchBar, BorderLayout.NORTH);

        // --- VSplit: product list (top) + cart (bottom) ---
        JSplitPane vSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        vSplit.setDividerLocation(250);
        vSplit.setDividerSize(4);
        vSplit.setBorder(null);
        vSplit.setResizeWeight(0.45);

        vSplit.setTopComponent(createProductListPanel());
        vSplit.setBottomComponent(createCartPanel());

        panel.add(vSplit, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createProductListPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(new EmptyBorder(0, 16, 4, 16));

        JLabel lbl = new JLabel("  Danh sách sản phẩm (còn hàng & chưa hết hạn)");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(AppColors.TEXT_SECONDARY);
        wrapper.add(lbl, BorderLayout.NORTH);

        // ★ Risk 2 Prevention: Chỉ hiện SP còn tồn kho & chưa hết hạn
        String[] cols = {"Mã SP", "Tên Sản Phẩm", "ĐVT", "Giá Bán", "Tồn Kho"};
        productModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        productTable = new JTable(productModel);
        productTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        productTable.setRowHeight(30);
        productTable.setShowGrid(false);
        productTable.setFillsViewportHeight(true);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader h = productTable.getTableHeader();
        h.setBackground(AppColors.PRIMARY_LIGHT);
        h.setForeground(Color.WHITE);
        h.setFont(new Font("Segoe UI", Font.BOLD, 11));
        h.setPreferredSize(new Dimension(0, 30));
        h.setReorderingAllowed(false);

        productTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        productTable.getColumnModel().getColumn(0).setMaxWidth(65);
        productTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        productTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        productTable.getColumnModel().getColumn(2).setMaxWidth(75);
        productTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        productTable.getColumnModel().getColumn(4).setPreferredWidth(65);
        productTable.getColumnModel().getColumn(4).setMaxWidth(80);

        productTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) c.setBackground(row % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);
                if (col == 0 || col == 2 || col == 4) setHorizontalAlignment(SwingConstants.CENTER);
                else if (col == 3) setHorizontalAlignment(SwingConstants.RIGHT);
                else setHorizontalAlignment(SwingConstants.LEFT);
                if (col == 4 && v instanceof Integer && (int) v <= 10) {
                    c.setForeground(AppColors.DANGER);
                    setFont(new Font("Segoe UI", Font.BOLD, 13));
                } else if (!sel) {
                    c.setForeground(AppColors.TEXT_PRIMARY);
                }
                return c;
            }
        });

        // Double-click product → add to cart
        productTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) addSelectedProductToCart();
            }
        });

        // Enter key → add to cart
        productTable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "addToCart");
        productTable.getActionMap().put("addToCart", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { addSelectedProductToCart(); }
        });

        JScrollPane scroll = new JScrollPane(productTable);
        scroll.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1));
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createCartPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(new EmptyBorder(8, 16, 8, 16));

        // Header
        JPanel cartHeader = new JPanel(new BorderLayout());
        cartHeader.setOpaque(false);
        JLabel lblCart = new JLabel("  Giỏ Hàng");
        lblCart.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblCart.setForeground(AppColors.PRIMARY);
        cartHeader.add(lblCart, BorderLayout.WEST);

        lblCartTotal = new JLabel("Tổng: 0 VNĐ  ");
        lblCartTotal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblCartTotal.setForeground(AppColors.SUCCESS);
        cartHeader.add(lblCartTotal, BorderLayout.EAST);
        wrapper.add(cartHeader, BorderLayout.NORTH);

        // Table
        String[] cols = {"STT", "Tên SP", "ĐVT", "SL", "Đơn Giá", "Thành Tiền"};
        cartModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 3; } // SL editable
        };
        cartTable = new JTable(cartModel);
        cartTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cartTable.setRowHeight(30);
        cartTable.setShowGrid(false);
        cartTable.setFillsViewportHeight(true);
        cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader ch = cartTable.getTableHeader();
        ch.setBackground(AppColors.SUCCESS);
        ch.setForeground(Color.WHITE);
        ch.setFont(new Font("Segoe UI", Font.BOLD, 11));
        ch.setPreferredSize(new Dimension(0, 30));

        cartTable.getColumnModel().getColumn(0).setPreferredWidth(35);
        cartTable.getColumnModel().getColumn(0).setMaxWidth(45);
        cartTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        cartTable.getColumnModel().getColumn(2).setPreferredWidth(55);
        cartTable.getColumnModel().getColumn(2).setMaxWidth(65);
        cartTable.getColumnModel().getColumn(3).setPreferredWidth(45);
        cartTable.getColumnModel().getColumn(3).setMaxWidth(55);
        cartTable.getColumnModel().getColumn(4).setPreferredWidth(90);
        cartTable.getColumnModel().getColumn(5).setPreferredWidth(100);

        cartTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xE8, 0xF5, 0xE9));
                if (col == 0 || col == 2 || col == 3) setHorizontalAlignment(SwingConstants.CENTER);
                else if (col == 4 || col == 5) setHorizontalAlignment(SwingConstants.RIGHT);
                else setHorizontalAlignment(SwingConstants.LEFT);
                if (col == 5) {
                    c.setForeground(AppColors.SUCCESS);
                    setFont(new Font("Segoe UI", Font.BOLD, 13));
                } else if (!sel) c.setForeground(AppColors.TEXT_PRIMARY);
                return c;
            }
        });

        // Khi edit SL → cập nhật cart
        cartModel.addTableModelListener(e -> {
            if (e.getColumn() == 3 && e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                onCartQuantityChanged(row);
            }
        });

        // Delete key → xóa dòng
        cartTable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "removeItem");
        cartTable.getActionMap().put("removeItem", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { removeSelectedCartItem(); }
        });

        // Right-click xóa
        JPopupMenu cartPopup = new JPopupMenu();
        JMenuItem miRemove = new JMenuItem("Xóa khỏi giỏ");
        miRemove.addActionListener(e -> removeSelectedCartItem());
        cartPopup.add(miRemove);
        JMenuItem miClear = new JMenuItem("Xóa toàn bộ giỏ");
        miClear.setForeground(AppColors.DANGER);
        miClear.addActionListener(e -> clearCart());
        cartPopup.add(miClear);
        cartTable.setComponentPopupMenu(cartPopup);

        JScrollPane scroll = new JScrollPane(cartTable);
        scroll.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1));
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    // ================================================================
    //  RIGHT: Checkout Panel
    // ================================================================

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(0xF0, 0xF4, 0xF8));
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        // === Header ===
        JLabel lblCheckout = new JLabel("THANH TOÁN");
        lblCheckout.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblCheckout.setForeground(AppColors.PRIMARY);
        lblCheckout.setAlignmentX(LEFT_ALIGNMENT);
        form.add(lblCheckout);
        form.add(Box.createRigidArea(new Dimension(0, 20)));

        // --- Khách hàng ---
        addSectionLabel(form, "THÔNG TIN KHÁCH HÀNG");

        txtCustomerPhone = new JTextField();
        txtCustomerPhone.setToolTipText("Để trống = Khách vãng lai");
        // Numeric only
        ((javax.swing.text.AbstractDocument) txtCustomerPhone.getDocument()).setDocumentFilter(
            new javax.swing.text.DocumentFilter() {
                @Override
                public void insertString(FilterBypass fb, int offset, String s,
                        javax.swing.text.AttributeSet a) throws javax.swing.text.BadLocationException {
                    if (s != null) super.insertString(fb, offset, s.replaceAll("[^0-9]", ""), a);
                }
                @Override
                public void replace(FilterBypass fb, int offset, int len, String s,
                        javax.swing.text.AttributeSet a) throws javax.swing.text.BadLocationException {
                    if (s != null) super.replace(fb, offset, len, s.replaceAll("[^0-9]", ""), a);
                }
            });
        // Auto-suggest: khi nhập đủ 10 số → lookup DB
        txtCustomerPhone.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onPhoneChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { onPhoneChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { onPhoneChanged(); }
        });
        addFormField(form, "SĐT (để trống = vãng lai):", txtCustomerPhone);

        // Hint label: trạng thái suggest
        lblPhoneSuggest = new JLabel(" ");
        lblPhoneSuggest.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblPhoneSuggest.setForeground(AppColors.TEXT_SECONDARY);
        lblPhoneSuggest.setAlignmentX(LEFT_ALIGNMENT);
        form.add(lblPhoneSuggest);
        form.add(Box.createRigidArea(new Dimension(0, 4)));

        txtCustomerName = new JTextField();
        addFormField(form, "Tên khách:", txtCustomerName);

        cboGioiTinh = new JComboBox<>(new String[]{"Nam", "Nữ", "Khác"});
        cboGioiTinh.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cboGioiTinh.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        cboGioiTinh.setAlignmentX(LEFT_ALIGNMENT);
        JLabel lblGT = new JLabel("Giới tính:");
        lblGT.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblGT.setForeground(AppColors.TEXT_SECONDARY);
        lblGT.setAlignmentX(LEFT_ALIGNMENT);
        form.add(lblGT);
        form.add(Box.createRigidArea(new Dimension(0, 3)));
        form.add(cboGioiTinh);

        form.add(Box.createRigidArea(new Dimension(0, 16)));

        // --- Phương thức ---
        addSectionLabel(form, "PHƯƠNG THỨC THANH TOÁN");
        cboPayMethod = new JComboBox<>(new String[]{"Tiền mặt", "QR Code"});
        cboPayMethod.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cboPayMethod.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        cboPayMethod.setAlignmentX(LEFT_ALIGNMENT);
        form.add(cboPayMethod);
        form.add(Box.createRigidArea(new Dimension(0, 16)));

        // --- Tiền ---
        addSectionLabel(form, "TÍNH TIỀN");

        // Tổng tiền
        lblTongTien = new JLabel("0 VNĐ");
        lblTongTien.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTongTien.setForeground(AppColors.PRIMARY);
        lblTongTien.setAlignmentX(LEFT_ALIGNMENT);
        form.add(lblTongTien);
        form.add(Box.createRigidArea(new Dimension(0, 10)));

        // Cash fields panel (ẩn khi chọn QR)
        cashFieldsPanel = new JPanel();
        cashFieldsPanel.setLayout(new BoxLayout(cashFieldsPanel, BoxLayout.Y_AXIS));
        cashFieldsPanel.setBackground(Color.WHITE);
        cashFieldsPanel.setAlignmentX(LEFT_ALIGNMENT);
        cashFieldsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        txtTienKhachDua = new JTextField();
        txtTienKhachDua.setFont(new Font("Segoe UI", Font.BOLD, 16));
        txtTienKhachDua.setForeground(AppColors.TEXT_PRIMARY);
        // Chỉ cho nhập số
        ((javax.swing.text.AbstractDocument) txtTienKhachDua.getDocument()).setDocumentFilter(
            new javax.swing.text.DocumentFilter() {
                @Override
                public void insertString(FilterBypass fb, int offset, String text, javax.swing.text.AttributeSet attr) throws javax.swing.text.BadLocationException {
                    if (text.matches("[0-9]*")) super.insertString(fb, offset, text, attr);
                }
                @Override
                public void replace(FilterBypass fb, int offset, int length, String text, javax.swing.text.AttributeSet attr) throws javax.swing.text.BadLocationException {
                    if (text.matches("[0-9]*")) super.replace(fb, offset, length, text, attr);
                }
            }
        );
        // auto-calc tiền thối
        txtTienKhachDua.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { calcChange(); }
            @Override public void removeUpdate(DocumentEvent e) { calcChange(); }
            @Override public void changedUpdate(DocumentEvent e) { calcChange(); }
        });
        addFormField(cashFieldsPanel, "Tiền khách đưa:", txtTienKhachDua);

        lblTienThoi = new JLabel("Tiền thừa: ---");
        lblTienThoi.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTienThoi.setForeground(AppColors.SUCCESS);
        lblTienThoi.setAlignmentX(LEFT_ALIGNMENT);
        cashFieldsPanel.add(lblTienThoi);

        form.add(cashFieldsPanel);

        // Listener: show/hide cash fields khi đổi phương thức
        cboPayMethod.addItemListener(e -> {
            if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                boolean isCash = "Tiền mặt".equals(cboPayMethod.getSelectedItem());
                cashFieldsPanel.setVisible(isCash);
                if (!isCash) {
                    txtTienKhachDua.setText("");
                    lblTienThoi.setText("Tiền thừa: ---");
                }
            }
        });

        // === Buttons ===
        btnCheckout = new JButton("THANH TOÁN");
        btnCheckout.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnCheckout.setBackground(AppColors.SUCCESS);
        btnCheckout.setForeground(Color.WHITE);
        btnCheckout.setFocusPainted(false);
        btnCheckout.setBorderPainted(false);
        btnCheckout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCheckout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        btnCheckout.setAlignmentX(LEFT_ALIGNMENT);
        btnCheckout.addActionListener(e -> doCheckout());
        form.add(btnCheckout);

        form.add(Box.createRigidArea(new Dimension(0, 10)));

        btnClearCart = new JButton("Xóa Giỏ Hàng");
        btnClearCart.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnClearCart.setBackground(AppColors.DANGER);
        btnClearCart.setForeground(Color.WHITE);
        btnClearCart.setFocusPainted(false);
        btnClearCart.setBorderPainted(false);
        btnClearCart.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClearCart.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnClearCart.setAlignmentX(LEFT_ALIGNMENT);
        btnClearCart.addActionListener(e -> clearCart());
        form.add(btnClearCart);

        form.add(Box.createRigidArea(new Dimension(0, 8)));

        // Hold Order buttons
        JPanel holdPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        holdPanel.setBackground(Color.WHITE);
        holdPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        holdPanel.setAlignmentX(LEFT_ALIGNMENT);

        btnHoldOrder = new JButton("Giữ đơn");
        btnHoldOrder.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnHoldOrder.setBackground(new Color(0xFF, 0x98, 0x00)); // orange
        btnHoldOrder.setForeground(Color.WHITE);
        btnHoldOrder.setFocusPainted(false);
        btnHoldOrder.setBorderPainted(false);
        btnHoldOrder.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnHoldOrder.addActionListener(e -> doHoldOrder());
        holdPanel.add(btnHoldOrder);

        // View held with badge
        JPanel heldBtnWrapper = new JPanel(new BorderLayout());
        heldBtnWrapper.setBackground(Color.WHITE);

        btnViewHeld = new JButton("Đơn chờ");
        btnViewHeld.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnViewHeld.setBackground(new Color(0x17, 0xA2, 0xB8));
        btnViewHeld.setForeground(Color.WHITE);
        btnViewHeld.setFocusPainted(false);
        btnViewHeld.setBorderPainted(false);
        btnViewHeld.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnViewHeld.addActionListener(e -> showHeldOrdersDialog());
        heldBtnWrapper.add(btnViewHeld, BorderLayout.CENTER);

        lblHeldBadge = new JLabel("");
        lblHeldBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblHeldBadge.setForeground(Color.WHITE);
        lblHeldBadge.setBackground(AppColors.DANGER);
        lblHeldBadge.setOpaque(true);
        lblHeldBadge.setHorizontalAlignment(SwingConstants.CENTER);
        lblHeldBadge.setPreferredSize(new Dimension(22, 18));
        lblHeldBadge.setVisible(false);
        JPanel badgeWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        badgeWrapper.setOpaque(false);
        badgeWrapper.add(lblHeldBadge);
        heldBtnWrapper.add(badgeWrapper, BorderLayout.EAST);

        holdPanel.add(heldBtnWrapper);
        form.add(holdPanel);

        form.add(Box.createVerticalGlue());

        panel.add(new JScrollPane(form, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        return panel;
    }

    // ================================================================
    //  PRODUCT SEARCH (★ Risk 2: Loại bỏ SP hết hạn / hết tồn)
    // ================================================================

    private void onProductSearch() {
        if (searchTimer != null) searchTimer.stop();
        searchTimer = new Timer(250, e -> {
            String kw = txtSearchProduct.getText().trim();
            loadProducts(kw.isEmpty() ? null : kw);
        });
        searchTimer.setRepeats(false);
        searchTimer.start();
    }

    // ================================================================
    //  CUSTOMER AUTO-SUGGEST (Nhập SĐT → tự điền tên + giới tính)
    // ================================================================

    private void onPhoneChanged() {
        if (phoneTimer != null) phoneTimer.stop();
        phoneTimer = new Timer(300, e -> {
            String phone = txtCustomerPhone.getText().trim();
            if (phone.length() < 10) {
                // Chưa đủ 10 số → reset
                lblPhoneSuggest.setText(phone.isEmpty() ? " " : "Nhập đủ 10 số...");
                lblPhoneSuggest.setForeground(AppColors.TEXT_SECONDARY);
                txtCustomerName.setEditable(true);
                cboGioiTinh.setEnabled(true);
                return;
            }
            if (phone.length() == 10) {
                // Lookup DB
                try {
                    domain.entity.Customer c =
                            new infrastructure.repository.CustomerRepositoryImpl().findByPhone(phone);
                    if (c != null) {
                        // Khách cũ → tự điền
                        txtCustomerName.setText(c.getTenKH() != null ? c.getTenKH() : "");
                        if (c.getGioiTinh() != null) {
                            cboGioiTinh.setSelectedItem(c.getGioiTinh());
                        }
                        lblPhoneSuggest.setText("Khách cũ: " + (c.getTenKH() != null ? c.getTenKH() : "---"));
                        lblPhoneSuggest.setForeground(AppColors.SUCCESS);
                        txtCustomerName.setEditable(false);
                        cboGioiTinh.setEnabled(false);
                    } else {
                        // Khách mới
                        txtCustomerName.setText("");
                        txtCustomerName.setEditable(true);
                        cboGioiTinh.setEnabled(true);
                        lblPhoneSuggest.setText("Khách mới - vui lòng nhập tên");
                        lblPhoneSuggest.setForeground(new Color(0xFF, 0x98, 0x00)); // orange
                        txtCustomerName.requestFocusInWindow();
                    }
                } catch (Exception ex) {
                    lblPhoneSuggest.setText(" ");
                }
            }
        });
        phoneTimer.setRepeats(false);
        phoneTimer.start();
    }

    /**
     * ★ Risk 2 Prevention: Query chỉ select SP có lô chưa hết hạn & tồn > 0
     */
    private void loadProducts(String keyword) {
        // ★ HARD-CODE điều kiện: HanSuDung > GETDATE() AND SoLuong > 0
        StringBuilder sql = new StringBuilder(
            "SELECT sp.MaSP, sp.TenSP, sp.DonViTinh, sp.GiaBan, " +
            "ISNULL(tk.TongTon, 0) AS TonKho " +
            "FROM SanPham sp " +
            "LEFT JOIN (SELECT MaSP, SUM(SoLuong) AS TongTon FROM LoHang " +
            "           WHERE SoLuong > 0 AND HanSuDung > GETDATE() GROUP BY MaSP) tk " +
            "ON sp.MaSP = tk.MaSP " +
            "WHERE sp.TrangThai = 1 AND ISNULL(tk.TongTon, 0) > 0 ");
        if (keyword != null) {
            sql.append("AND sp.TenSP LIKE ? ");
        }
        sql.append("ORDER BY sp.TenSP");

        productModel.setRowCount(0);
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (keyword != null) {
                ps.setNString(1, "%" + keyword + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal gia = rs.getBigDecimal("GiaBan");
                    productModel.addRow(new Object[]{
                        rs.getInt("MaSP"),
                        rs.getNString("TenSP"),
                        rs.getNString("DonViTinh"),
                        gia != null ? MONEY_FMT.format(gia) + " ₫" : "---",
                        rs.getInt("TonKho")
                    });
                }
            }
        } catch (SQLException ignored) {}
    }

    // ================================================================
    //  CART LOGIC
    // ================================================================

    private void addSelectedProductToCart() {
        int row = productTable.getSelectedRow();
        if (row < 0) return;

        int maSP = (int) productModel.getValueAt(row, 0);
        String tenSP = productModel.getValueAt(row, 1).toString();
        String dvt = productModel.getValueAt(row, 2).toString();
        int tonKho = (int) productModel.getValueAt(row, 4);
        String giaStr = productModel.getValueAt(row, 3).toString()
                .replace(" ₫", "").replace(",", "").trim();
        BigDecimal giaBan;
        try { giaBan = new BigDecimal(giaStr); } catch (NumberFormatException e) { return; }

        // Kiểm tra đã có trong giỏ chưa
        for (int i = 0; i < cart.size(); i++) {
            if (cart.get(i).getMaSP() == maSP) {
                int newQty = cart.get(i).getSoLuong() + 1;
                if (newQty > tonKho) {
                    JOptionPane.showMessageDialog(this,
                            "Tồn kho chỉ còn " + tonKho + " " + dvt + "!",
                            "Vượt tồn kho", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                cart.get(i).setSoLuong(newQty);
                refreshCartTable();
                return;
            }
        }

        // Thêm mới
        cart.add(new CartItem(maSP, tenSP, dvt, 1, giaBan));
        refreshCartTable();
    }

    private void removeSelectedCartItem() {
        int row = cartTable.getSelectedRow();
        if (row < 0 || row >= cart.size()) return;
        cart.remove(row);
        refreshCartTable();
    }

    private void clearCart() {
        cart.clear();
        refreshCartTable();
        txtCustomerPhone.setText("");
        txtCustomerName.setText("");
        cboGioiTinh.setSelectedIndex(0);
        txtTienKhachDua.setText("");
        lblTienThoi.setText("Tiền thừa: ---");
        lblPhoneSuggest.setText(" ");
        txtCustomerName.setEditable(true);
        cboGioiTinh.setEnabled(true);
    }

    // ================================================================
    //  HOLD ORDER — Giữ đơn chờ xử lý
    // ================================================================

    private void doHoldOrder() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Giỏ hàng trống, không có gì để giữ!",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String note = JOptionPane.showInputDialog(this,
                "Ghi chú cho đơn chờ (có thể bỏ trống):",
                "Giữ đơn", JOptionPane.PLAIN_MESSAGE);
        if (note == null) return; // cancelled

        domain.dto.HeldOrder held = new domain.dto.HeldOrder(
                new ArrayList<>(cart),
                txtCustomerPhone.getText().trim(),
                txtCustomerName.getText().trim(),
                (String) cboGioiTinh.getSelectedItem(),
                (String) cboPayMethod.getSelectedItem(),
                note.trim()
        );

        heldOrders.add(held);
        updateHeldBadge();
        clearCart();

        JOptionPane.showMessageDialog(this,
                "Đã giữ đơn thành công!\nTổng đơn chờ: " + heldOrders.size(),
                "Giữ đơn", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showHeldOrdersDialog() {
        if (heldOrders.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có đơn chờ nào.",
                    "Đơn chờ", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dlg = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Đơn hàng chờ xử lý (" + heldOrders.size() + ")", true);
        dlg.setSize(600, 400);
        dlg.setLocationRelativeTo(this);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.WHITE);

        // Header
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(new Color(0xFF, 0x98, 0x00));
        hdr.setBorder(new javax.swing.border.EmptyBorder(12, 20, 12, 20));
        JLabel lblH = new JLabel("Đơn hàng đang chờ — " + heldOrders.size() + " đơn");
        lblH.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblH.setForeground(Color.WHITE);
        hdr.add(lblH);
        content.add(hdr, BorderLayout.NORTH);

        // List
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (int i = 0; i < heldOrders.size(); i++) {
            listModel.addElement((i + 1) + ". " + heldOrders.get(i).getSummary());
        }

        JList<String> list = new JList<>(listModel);
        list.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        list.setFixedCellHeight(36);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);

        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(new javax.swing.border.EmptyBorder(10, 20, 10, 20));
        content.add(sp, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppColors.NEUTRAL_DARK));

        JButton btnRecall = new JButton("Lấy lại đơn");
        btnRecall.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnRecall.setBackground(AppColors.SUCCESS);
        btnRecall.setForeground(Color.WHITE);
        btnRecall.setFocusPainted(false);
        btnRecall.setBorderPainted(false);
        btnRecall.setPreferredSize(new Dimension(140, 36));
        btnRecall.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRecall.addActionListener(e -> {
            int idx = list.getSelectedIndex();
            if (idx >= 0) {
                recallHeldOrder(idx);
                dlg.dispose();
            }
        });

        JButton btnDelete = new JButton("Xóa đơn");
        btnDelete.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnDelete.setBackground(AppColors.DANGER);
        btnDelete.setForeground(Color.WHITE);
        btnDelete.setFocusPainted(false);
        btnDelete.setBorderPainted(false);
        btnDelete.setPreferredSize(new Dimension(120, 36));
        btnDelete.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnDelete.addActionListener(e -> {
            int idx = list.getSelectedIndex();
            if (idx >= 0) {
                int confirm = JOptionPane.showConfirmDialog(dlg,
                        "Xóa đơn chờ này?", "Xác nhận",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    heldOrders.remove(idx);
                    updateHeldBadge();
                    listModel.remove(idx);
                    lblH.setText("Đơn hàng đang chờ — " + heldOrders.size() + " đơn");
                    dlg.setTitle("Đơn hàng chờ xử lý (" + heldOrders.size() + ")");
                    if (heldOrders.isEmpty()) dlg.dispose();
                    else if (idx >= listModel.size()) list.setSelectedIndex(listModel.size() - 1);
                }
            }
        });

        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnClose.setPreferredSize(new Dimension(100, 36));
        btnClose.addActionListener(e -> dlg.dispose());

        btnPanel.add(btnRecall);
        btnPanel.add(btnDelete);
        btnPanel.add(btnClose);
        content.add(btnPanel, BorderLayout.SOUTH);

        dlg.setContentPane(content);
        dlg.setVisible(true);
    }

    private void recallHeldOrder(int index) {
        if (index < 0 || index >= heldOrders.size()) return;

        // Warn if current cart not empty
        if (!cart.isEmpty()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Giỏ hàng hiện tại có " + cart.size() + " sản phẩm.\n" +
                    "Bạn muốn giữ giỏ hiện tại trước khi lấy đơn chờ?",
                    "Giỏ hàng không trống",
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                // Hold current cart first
                domain.dto.HeldOrder currentHeld = new domain.dto.HeldOrder(
                        new ArrayList<>(cart),
                        txtCustomerPhone.getText().trim(),
                        txtCustomerName.getText().trim(),
                        (String) cboGioiTinh.getSelectedItem(),
                        (String) cboPayMethod.getSelectedItem(),
                        "Tự động giữ"
                );
                heldOrders.add(currentHeld);
            } else if (confirm == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }

        domain.dto.HeldOrder held = heldOrders.remove(index);
        updateHeldBadge();

        // Restore state
        clearCart();
        cart.addAll(held.getItems());
        refreshCartTable();

        if (held.getCustomerPhone() != null && !held.getCustomerPhone().isEmpty()) {
            txtCustomerPhone.setText(held.getCustomerPhone());
        }
        if (held.getCustomerName() != null && !held.getCustomerName().isEmpty()) {
            txtCustomerName.setText(held.getCustomerName());
        }
        if (held.getGioiTinh() != null) {
            cboGioiTinh.setSelectedItem(held.getGioiTinh());
        }
        if (held.getPayMethod() != null) {
            cboPayMethod.setSelectedItem(held.getPayMethod());
        }
    }

    private void updateHeldBadge() {
        if (heldOrders.isEmpty()) {
            lblHeldBadge.setVisible(false);
        } else {
            lblHeldBadge.setText(String.valueOf(heldOrders.size()));
            lblHeldBadge.setVisible(true);
        }
    }

    private void onCartQuantityChanged(int row) {
        if (row < 0 || row >= cart.size()) return;
        try {
            int newQty = Integer.parseInt(cartModel.getValueAt(row, 3).toString());
            if (newQty <= 0) {
                cart.remove(row);
            } else {
                cart.get(row).setSoLuong(newQty);
            }
        } catch (NumberFormatException e) {
            // Revert
        }
        refreshCartTable();
    }

    private void refreshCartTable() {
        cartModel.setRowCount(0);
        BigDecimal total = BigDecimal.ZERO;
        int stt = 1;
        for (CartItem item : cart) {
            BigDecimal thanhTien = item.getThanhTien();
            total = total.add(thanhTien);
            cartModel.addRow(new Object[]{
                stt++,
                item.getTenSP(),
                item.getDonViTinh(),
                item.getSoLuong(),
                MONEY_FMT.format(item.getGiaBan()),
                MONEY_FMT.format(thanhTien)
            });
        }
        lblCartTotal.setText("Tổng: " + MONEY_FMT.format(total) + " VNĐ  ");
        lblTongTien.setText(MONEY_FMT.format(total) + " VNĐ");
        calcChange();
    }

    // ================================================================
    //  CHANGE CALCULATION
    // ================================================================

    private BigDecimal getCartTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cart) total = total.add(item.getThanhTien());
        return total;
    }

    private void calcChange() {
        try {
            String raw = txtTienKhachDua.getText().replace(",", "").replace(".", "").trim();
            if (raw.isEmpty()) {
                lblTienThoi.setText("Tiền thừa: ---");
                lblTienThoi.setForeground(AppColors.TEXT_SECONDARY);
                return;
            }
            BigDecimal khachDua = new BigDecimal(raw);
            BigDecimal tongTien = getCartTotal();
            BigDecimal thoi = khachDua.subtract(tongTien);

            if (thoi.signum() < 0) {
                lblTienThoi.setText("Thiếu: " + MONEY_FMT.format(thoi.abs()) + " VNĐ");
                lblTienThoi.setForeground(AppColors.DANGER);
            } else {
                lblTienThoi.setText("Tiền thừa: " + MONEY_FMT.format(thoi) + " VNĐ");
                lblTienThoi.setForeground(AppColors.SUCCESS);
            }
        } catch (NumberFormatException e) {
            lblTienThoi.setText("Tiền thừa: ---");
            lblTienThoi.setForeground(AppColors.TEXT_SECONDARY);
        }
    }

    // ================================================================
    //  ★ CHECKOUT (Risk 3 + Risk 4 Prevention)
    //
    //  Risk 3: Transaction xử lý bên SaleServiceImpl.checkout()
    //          setAutoCommit(false) → INSERT Invoice → FEFO loop → commit/rollback
    //
    //  Risk 4: SwingWorker tách DB processing khỏi EDT
    // ================================================================

    private void doCheckout() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Giỏ hàng trống!",
                    "Chưa có sản phẩm", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validate tiền khách đưa (nếu tiền mặt)
        String payMethod = mapPayMethod((String) cboPayMethod.getSelectedItem());
        if ("TienMat".equals(payMethod)) {
            String raw = txtTienKhachDua.getText().replace(",", "").replace(".", "").trim();
            if (raw.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập tiền khách đưa!",
                        "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
                txtTienKhachDua.requestFocusInWindow();
                return;
            }
            try {
                BigDecimal khachDua = new BigDecimal(raw);
                if (khachDua.compareTo(getCartTotal()) < 0) {
                    JOptionPane.showMessageDialog(this,
                            "Tiền khách đưa không đủ!\nCần: " + MONEY_FMT.format(getCartTotal()) + " VNĐ",
                            "Thiếu tiền", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Số tiền không hợp lệ!",
                        "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        // === QR Code: show QR dialog trước khi checkout ===
        if ("QR".equals(payMethod)) {
            boolean confirmed = showQRPaymentDialog(getCartTotal());
            if (!confirmed) return; // user hủy
        }

        // Validate SĐT
        String soDT = txtCustomerPhone.getText().trim();
        if (!soDT.isEmpty() && !soDT.matches("\\d{10}")) {
            JOptionPane.showMessageDialog(this, "SĐT phải đúng 10 chữ số! (hoặc để trống = vãng lai)",
                    "SĐT không hợp lệ", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnCheckout.setEnabled(false);
        btnCheckout.setText("Đang xử lý...");

        String tenKH_val = txtCustomerName.getText().trim();
        List<CartItem> cartSnapshot = new ArrayList<>(cart);

        SwingWorker<Integer, Void> worker = new SwingWorker<>() {
            String errorMsg = null;

            @Override
            protected Integer doInBackground() {
                try {
                    return saleService.checkout(cartSnapshot,
                            soDT.isEmpty() ? null : soDT,
                            tenKH_val.isEmpty() ? null : tenKH_val,
                            payMethod);
                } catch (Exception e) {
                    errorMsg = e.getMessage();
                    return -1;
                }
            }

            @Override
            protected void done() {
                btnCheckout.setEnabled(true);
                btnCheckout.setText("THANH TOÁN");
                try {
                    int maHD = get();
                    if (maHD > 0) {
                        BigDecimal total = BigDecimal.ZERO;
                        for (CartItem i : cartSnapshot) total = total.add(i.getThanhTien());

                        String msg = "Thanh toán thành công!\n\n" +
                                "Mã hóa đơn: #" + maHD + "\n" +
                                "Tổng tiền: " + MONEY_FMT.format(total) + " VNĐ\n" +
                                "Phương thức: " + cboPayMethod.getSelectedItem() + "\n" +
                                (soDT.isEmpty() ? "Khách vãng lai" : "Khách: " + soDT);

                        int opt = JOptionPane.showOptionDialog(POSPanel.this,
                                msg + "\n\nBạn có muốn xuất hóa đơn PDF?",
                                "Thanh toán thành công",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.INFORMATION_MESSAGE,
                                null,
                                new String[]{"Xuất PDF", "Đóng"},
                                "Xuất PDF");

                        if (opt == 0) {
                            exportInvoicePdf(maHD);
                        }

                        clearCart();
                        loadProducts(null);
                    } else {
                        JOptionPane.showMessageDialog(POSPanel.this,
                                "Lỗi thanh toán:\n" + (errorMsg != null ? errorMsg : "Không xác định"),
                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(POSPanel.this,
                            "Lỗi: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void exportInvoicePdf(int maHD) {
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setDialogTitle("Lưu hóa đơn PDF");
        fc.setSelectedFile(new java.io.File("HoaDon_" + maHD + ".pdf"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Files", "pdf"));

        int result = fc.showSaveDialog(this);
        if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getAbsolutePath();
            if (!path.toLowerCase().endsWith(".pdf")) path += ".pdf";

            try {
                service.InvoicePdfService.generate(maHD, path);
                JOptionPane.showMessageDialog(this,
                        "Xuất hóa đơn thành công!\n" + path,
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                try { java.awt.Desktop.getDesktop().open(new java.io.File(path)); }
                catch (Exception ignored) {}
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Lỗi xuất PDF: " + e.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ================================================================
    //  QR CODE PAYMENT DIALOG
    // ================================================================
    private boolean showQRPaymentDialog(BigDecimal amount) {
        // VietQR API — Techcombank
        String bankBin = "970407";
        String accountNo = "19072888896017";
        String accountName = "VUONG NGOC GIA BAO";
        long amountLong = amount.longValue();

        String qrUrl = "https://img.vietqr.io/image/" + bankBin + "-" + accountNo
                + "-compact2.png?amount=" + amountLong
                + "&addInfo=" + java.net.URLEncoder.encode("Thanh toan hoa don", java.nio.charset.StandardCharsets.UTF_8)
                + "&accountName=" + java.net.URLEncoder.encode(accountName, java.nio.charset.StandardCharsets.UTF_8);

        JDialog dlg = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Thanh toán QR Code", true);
        dlg.setSize(480, 680);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.WHITE);

        // Header
        JPanel hdr = new JPanel();
        hdr.setBackground(new Color(0x17, 0xA2, 0xB8));
        hdr.setBorder(new javax.swing.border.EmptyBorder(14, 20, 14, 20));
        hdr.setLayout(new BoxLayout(hdr, BoxLayout.Y_AXIS));

        JLabel lblH = new JLabel("Thanh toán bằng QR Code");
        lblH.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblH.setForeground(Color.WHITE);
        lblH.setAlignmentX(CENTER_ALIGNMENT);
        hdr.add(lblH);

        JLabel lblAmount = new JLabel("Số tiền: " + MONEY_FMT.format(amount) + " VNĐ");
        lblAmount.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblAmount.setForeground(new Color(0xFF, 0xEB, 0x3B));
        lblAmount.setAlignmentX(CENTER_ALIGNMENT);
        hdr.add(Box.createRigidArea(new Dimension(0, 4)));
        hdr.add(lblAmount);

        content.add(hdr, BorderLayout.NORTH);

        // Center: QR Image
        JPanel center = new JPanel();
        center.setBackground(Color.WHITE);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(new javax.swing.border.EmptyBorder(16, 20, 10, 20));

        // Bank info
        JLabel lblBank = new JLabel("Techcombank — " + accountName);
        lblBank.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblBank.setForeground(AppColors.TEXT_PRIMARY);
        lblBank.setAlignmentX(CENTER_ALIGNMENT);
        center.add(lblBank);

        JLabel lblAcct = new JLabel("STK: " + accountNo);
        lblAcct.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblAcct.setForeground(AppColors.TEXT_SECONDARY);
        lblAcct.setAlignmentX(CENTER_ALIGNMENT);
        center.add(lblAcct);
        center.add(Box.createRigidArea(new Dimension(0, 12)));

        // QR Image — load from VietQR API
        JLabel lblQR = new JLabel("Đang tải mã QR...");
        lblQR.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblQR.setForeground(AppColors.TEXT_SECONDARY);
        lblQR.setHorizontalAlignment(SwingConstants.CENTER);
        lblQR.setAlignmentX(CENTER_ALIGNMENT);
        lblQR.setPreferredSize(new Dimension(350, 350));
        center.add(lblQR);

        center.add(Box.createRigidArea(new Dimension(0, 10)));

        JLabel lblGuide = new JLabel("<html><center>Quét mã QR bằng app ngân hàng<br>để thanh toán đúng số tiền trên</center></html>");
        lblGuide.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblGuide.setForeground(AppColors.TEXT_SECONDARY);
        lblGuide.setAlignmentX(CENTER_ALIGNMENT);
        center.add(lblGuide);

        content.add(center, BorderLayout.CENTER);

        // Bottom: warning + checkbox + buttons
        final boolean[] confirmed = {false};

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppColors.NEUTRAL_DARK));

        // Warning
        JLabel lblWarn = new JLabel("<html><center>Vui lòng kiểm tra app ngân hàng xác nhận<br><b>đã nhận đủ tiền</b> trước khi ấn xác nhận!</center></html>");
        lblWarn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblWarn.setForeground(AppColors.DANGER);
        lblWarn.setAlignmentX(CENTER_ALIGNMENT);
        lblWarn.setBorder(new javax.swing.border.EmptyBorder(10, 20, 6, 20));
        bottomPanel.add(lblWarn);

        // Checkbox xác nhận
        JCheckBox chkConfirm = new JCheckBox("Tôi đã xác nhận tiền đã về tài khoản");
        chkConfirm.setFont(new Font("Segoe UI", Font.BOLD, 12));
        chkConfirm.setForeground(AppColors.TEXT_PRIMARY);
        chkConfirm.setBackground(Color.WHITE);
        chkConfirm.setAlignmentX(CENTER_ALIGNMENT);
        chkConfirm.setBorder(new javax.swing.border.EmptyBorder(4, 20, 8, 20));
        bottomPanel.add(chkConfirm);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        btnPanel.setBackground(Color.WHITE);

        JButton btnPrintQR = new JButton("In QR PDF");
        btnPrintQR.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnPrintQR.setBackground(new Color(0x17, 0xA2, 0xB8));
        btnPrintQR.setForeground(Color.WHITE);
        btnPrintQR.setFocusPainted(false);
        btnPrintQR.setBorderPainted(false);
        btnPrintQR.setPreferredSize(new Dimension(120, 40));
        btnPrintQR.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPrintQR.addActionListener(e -> exportQrPdf(qrUrl, amount, accountName, accountNo));

        JButton btnConfirm = new JButton("Xác nhận đã thanh toán");
        btnConfirm.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnConfirm.setBackground(Color.LIGHT_GRAY);
        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.setFocusPainted(false);
        btnConfirm.setBorderPainted(false);
        btnConfirm.setPreferredSize(new Dimension(220, 40));
        btnConfirm.setEnabled(false);
        btnConfirm.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnConfirm.addActionListener(e -> { confirmed[0] = true; dlg.dispose(); });

        chkConfirm.addActionListener(e -> {
            boolean ticked = chkConfirm.isSelected();
            btnConfirm.setEnabled(ticked);
            btnConfirm.setBackground(ticked ? AppColors.SUCCESS : Color.LIGHT_GRAY);
        });

        JButton btnCancel = new JButton("Hủy");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnCancel.setPreferredSize(new Dimension(80, 40));
        btnCancel.addActionListener(e -> dlg.dispose());

        btnPanel.add(btnPrintQR);
        btnPanel.add(btnConfirm);
        btnPanel.add(btnCancel);
        bottomPanel.add(btnPanel);

        content.add(bottomPanel, BorderLayout.SOUTH);

        dlg.setContentPane(content);

        // Load QR async
        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() {
                try {
                    java.net.URL url = new java.net.URL(qrUrl);
                    java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(url);
                    if (img != null) {
                        java.awt.Image scaled = img.getScaledInstance(320, 320, java.awt.Image.SCALE_SMOOTH);
                        return new ImageIcon(scaled);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        lblQR.setText("");
                        lblQR.setIcon(icon);
                    } else {
                        lblQR.setText("Không tải được mã QR. Vui lòng thử lại.");
                    }
                } catch (Exception e) {
                    lblQR.setText("Lỗi tải QR: " + e.getMessage());
                }
            }
        }.execute();

        dlg.setVisible(true);
        return confirmed[0];
    }

    private void exportQrPdf(String qrUrl, BigDecimal amount, String accountName, String accountNo) {
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setDialogTitle("Lưu mã QR thanh toán");
        fc.setSelectedFile(new java.io.File("QR_ThanhToan_" + amount.longValue() + ".pdf"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Files", "pdf"));

        int result = fc.showSaveDialog(this);
        if (result != javax.swing.JFileChooser.APPROVE_OPTION) return;

        String path = fc.getSelectedFile().getAbsolutePath();
        if (!path.toLowerCase().endsWith(".pdf")) path += ".pdf";

        try {
            // Download QR image
            java.awt.image.BufferedImage qrImg = javax.imageio.ImageIO.read(new java.net.URL(qrUrl));

            // Create PDF (A5)
            com.lowagie.text.Document doc = new com.lowagie.text.Document(com.lowagie.text.PageSize.A5);
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, new java.io.FileOutputStream(path));
            doc.open();

            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font normalFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12);
            com.lowagie.text.Font amountFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 16, com.lowagie.text.Font.BOLD, java.awt.Color.RED);

            // Store name
            com.lowagie.text.Paragraph pTitle = new com.lowagie.text.Paragraph("APOTHECARY PRO", titleFont);
            pTitle.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            doc.add(pTitle);

            doc.add(new com.lowagie.text.Paragraph("Quet ma QR de thanh toan", normalFont));
            doc.add(new com.lowagie.text.Paragraph(" "));

            // Bank info
            doc.add(new com.lowagie.text.Paragraph("Ngan hang: Techcombank", normalFont));
            doc.add(new com.lowagie.text.Paragraph("Chu TK: " + accountName, normalFont));
            doc.add(new com.lowagie.text.Paragraph("STK: " + accountNo, normalFont));

            com.lowagie.text.Paragraph pAmount = new com.lowagie.text.Paragraph(
                    "So tien: " + MONEY_FMT.format(amount) + " VND", amountFont);
            pAmount.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            pAmount.setSpacingBefore(10);
            doc.add(pAmount);
            doc.add(new com.lowagie.text.Paragraph(" "));

            // QR Image
            if (qrImg != null) {
                com.lowagie.text.Image img = com.lowagie.text.Image.getInstance(qrImg, null);
                img.scaleToFit(250, 250);
                img.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                doc.add(img);
            }

            doc.add(new com.lowagie.text.Paragraph(" "));
            com.lowagie.text.Paragraph pNote = new com.lowagie.text.Paragraph(
                    "So tien se tu dong dien khi quet ma QR", normalFont);
            pNote.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            doc.add(pNote);

            doc.close();

            JOptionPane.showMessageDialog(this,
                    "Xuất QR PDF thành công!\n" + path,
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
            try { java.awt.Desktop.getDesktop().open(new java.io.File(path)); }
            catch (Exception ignored) {}

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Lỗi xuất QR PDF: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String mapPayMethod(String display) {
        if (display == null) return "TienMat";
        switch (display) {
            case "Chuyển khoản": return "ChuyenKhoan";
            case "QR Code":      return "QR";
            default:             return "TienMat";
        }
    }

    // ================================================================
    //  UI HELPERS
    // ================================================================

    private void addSectionLabel(JPanel panel, String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(AppColors.TEXT_SECONDARY);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(lbl);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));
    }

    private void addFormField(JPanel panel, String label, JTextField field) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(AppColors.TEXT_SECONDARY);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(lbl);
        panel.add(Box.createRigidArea(new Dimension(0, 3)));

        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AppColors.NEUTRAL_DARK, 1),
                new EmptyBorder(6, 10, 6, 10)));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        field.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(field);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
    }
}
