package panels;

import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import common.ColorScheme;
import components.ChiTietHoaDonTableModel;
import dao.ChiTietHoaDonDao;
import dao.HoaDonBanDao;
import dao.KhachHangDao;
import dao.SanPhamDao;
import entity.KhachHang;
import entity.NguoiDung;
import entity.SanPham;
import ui.StatusBadgeRenderer;

/**
 * BanHangPanel – Màn hình POS nhà thuốc (3 cột).
 *
 * Layout:
 *   LEFT  (30%) – Tìm & danh sách thuốc
 *   CENTER(45%) – Chi tiết hóa đơn
 *   RIGHT (25%) – Khách hàng + Thanh toán
 *
 * @version 5.0 – Pharmacy POS
 */
public class BanHangPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    /* ============ PALETTE ============ */
    private static final Color BG       = new Color(246, 248, 250);
    private static final Color CARD     = Color.WHITE;
    private static final Color BORDER   = new Color(226, 232, 240);
    private static final Color DIVIDER  = new Color(241, 245, 249);
    private static final Color BLUE     = new Color(37, 99, 235);
    private static final Color GREEN    = new Color(22, 163, 74);
    private static final Color RED      = new Color(220, 38, 38);
    private static final Color ORANGE   = new Color(234, 88, 12);
    private static final Color TXT1     = new Color(15, 23, 42);
    private static final Color TXT2     = new Color(100, 116, 139);
    private static final Color TXT_M    = new Color(148, 163, 184);
    private static final Color HDR_BG   = new Color(248, 250, 252);
    private static final Color ALT      = new Color(248, 250, 252);
    private static final Color HOVER    = new Color(241, 245, 249);

    /* ============ FONTS ============ */
    private static final Font F_TITLE   = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font F_SEC     = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font F_LBL     = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font F_INPUT   = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font F_TABLE   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font F_TH      = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font F_BTN     = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font F_TOTAL   = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font F_PAY_BTN = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font F_HINT    = new Font("Segoe UI", Font.ITALIC, 12);
    private static final Font F_MONEY   = new Font("Segoe UI", Font.BOLD, 15);

    private static final NumberFormat VND = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    /* ============ DAOs ============ */
    private final HoaDonBanDao hoaDonDao   = new HoaDonBanDao();
    private final ChiTietHoaDonDao cthdDao = new ChiTietHoaDonDao();
    private final SanPhamDao spDao         = new SanPhamDao();
    private final KhachHangDao khDao       = new KhachHangDao();

    /* ============ STATE ============ */
    private final NguoiDung currentUser;
    private Integer currentMaHoaDon;
    private Integer selectedMaKH;
    private List<SanPham> productCache;

    /* ============ LEFT: Product search ============ */
    private JTextField txtSearch;
    private DefaultTableModel mdlProduct;
    private JTable tblProduct;
    private JLabel lblProductCount;

    /* ============ CENTER: Invoice details ============ */
    private ChiTietHoaDonTableModel mdlInvoice;
    private JTable tblInvoice;
    private JLabel lblMaHoaDon;
    private JLabel lblTongTien;
    private JPanel emptyState;
    private JScrollPane invoiceScroll;

    /* ============ RIGHT: Payment ============ */
    private JTextField txtTimKH;
    private JLabel lblKhachHang;
    private JButton btnXoaKH;
    private JLabel lblSummaryTotal;
    private JTextField txtTienKhach;
    private JLabel lblTienThua;
    private List<KhachHang> khCache;
    private JPopupMenu popupKH;
    private DefaultListModel<String> khModel;
    private JList<String> khList;
    private javax.swing.Timer searchKHTimer;

    /* ============ CONSTRUCTOR ============ */

    public BanHangPanel(NguoiDung currentUser) {
        this.currentUser = currentUser;
        buildUI();
        loadProducts(null);
        resetForm();

        // Focus vào ô tìm kiếm khi hiện
        SwingUtilities.invokeLater(() -> txtSearch.requestFocusInWindow());
    }

    /* ================================================================
       UI CONSTRUCTION
       ================================================================ */

    private void buildUI() {
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(12, 14, 12, 14));

        // Header
        add(createHeader(), BorderLayout.NORTH);

        // 3-column body
        JPanel body = new JPanel(new BorderLayout(10, 0));
        body.setOpaque(false);

        body.add(createLeftPanel(), BorderLayout.WEST);
        body.add(createCenterPanel(), BorderLayout.CENTER);
        body.add(createRightPanel(), BorderLayout.EAST);

        add(body, BorderLayout.CENTER);

        // F9 shortcut
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F9"), "pay");
        getActionMap().put("pay", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { handleThanhToan(); }
        });
    }

    /* ======================== HEADER ======================== */

    private JPanel createHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        h.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel t = new JLabel("Bán hàng");
        t.setFont(F_TITLE);
        t.setForeground(TXT1);
        h.add(t, BorderLayout.WEST);

        JLabel info = new JLabel("Quy trình: Tìm thuốc → Thêm vào hóa đơn → Thanh toán (F9)  •  Xuất kho tự động FEFO");
        info.setFont(F_HINT);
        info.setForeground(TXT2);
        h.add(info, BorderLayout.EAST);

        return h;
    }

    /* ======================== LEFT: Product Search (30%) ======================== */

    private JPanel createLeftPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(CARD);
        p.setBorder(cardBorder());
        p.setPreferredSize(new Dimension(380, 0));

        // Top: Search + filter
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel lbl = label("Tìm thuốc", F_SEC, TXT1);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        top.add(lbl);
        top.add(Box.createVerticalStrut(6));

        txtSearch = new JTextField();
        txtSearch.setFont(F_INPUT);
        txtSearch.setPreferredSize(new Dimension(0, 38));
        txtSearch.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        txtSearch.setAlignmentX(LEFT_ALIGNMENT);
        txtSearch.putClientProperty("JTextField.placeholderText", "Mã, tên thuốc, hoạt chất...");
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(4, 10, 4, 10)
        ));

        // Debounce search
        javax.swing.Timer searchTimer = new javax.swing.Timer(250, e -> loadProducts(txtSearch.getText().trim()));
        searchTimer.setRepeats(false);
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { searchTimer.restart(); }
            public void removeUpdate(DocumentEvent e) { searchTimer.restart(); }
            public void changedUpdate(DocumentEvent e) { searchTimer.restart(); }
        });
        // Enter to add first visible product
        txtSearch.addActionListener(e -> addFirstProduct());
        top.add(txtSearch);
        top.add(Box.createVerticalStrut(6));

        // Filter chips
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        filters.setOpaque(false);
        filters.setAlignmentX(LEFT_ALIGNMENT);
        filters.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        lblProductCount = label("0 sản phẩm", F_LBL, TXT_M);
        filters.add(lblProductCount);

        top.add(filters);

        p.add(top, BorderLayout.NORTH);

        // Product table
        mdlProduct = new DefaultTableModel(
            new String[]{"Mã", "Tên thuốc", "ĐVT", "Giá bán", "Tồn", "T.Thái"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tblProduct = styledTable(mdlProduct);
        tblProduct.getColumnModel().getColumn(5).setCellRenderer(new StatusBadgeRenderer());
        setColWidths(tblProduct, 40, 140, 45, 75, 40, 60);

        // Double-click to add
        tblProduct.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) addSelectedProduct();
            }
        });
        // Enter to add
        tblProduct.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "addProd");
        tblProduct.getActionMap().put("addProd", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { addSelectedProduct(); }
        });

        JScrollPane sp = new JScrollPane(tblProduct,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));
        sp.getVerticalScrollBar().setUnitIncrement(16);
        p.add(sp, BorderLayout.CENTER);

        return p;
    }

    /* ======================== CENTER: Invoice (45%) ======================== */

    private JPanel createCenterPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setBackground(CARD);
        p.setBorder(cardBorder());

        // Header
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        hdr.setBorder(new EmptyBorder(0, 0, 8, 0));

        JPanel hdrLeft = new JPanel();
        hdrLeft.setOpaque(false);
        hdrLeft.setLayout(new BoxLayout(hdrLeft, BoxLayout.Y_AXIS));

        lblMaHoaDon = label("Chi tiết hóa đơn", F_SEC, TXT1);
        lblMaHoaDon.setAlignmentX(LEFT_ALIGNMENT);
        hdrLeft.add(lblMaHoaDon);
        JLabel fefoHint = label("Xuất kho tự động theo FEFO", F_HINT, TXT_M);
        fefoHint.setAlignmentX(LEFT_ALIGNMENT);
        hdrLeft.add(fefoHint);
        hdr.add(hdrLeft, BorderLayout.WEST);

        // Toolbar buttons
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        toolbar.setOpaque(false);
        toolbar.add(outlineBtn("Tạo mới", BLUE, e -> handleTaoMoi()));
        toolbar.add(outlineBtn("Xóa tất cả", RED, e -> handleXoaTatCa()));
        hdr.add(toolbar, BorderLayout.EAST);

        p.add(hdr, BorderLayout.NORTH);

        // Invoice table
        mdlInvoice = new ChiTietHoaDonTableModel();
        tblInvoice = styledTable(mdlInvoice);
        // Hide MaCTHD (col 0)
        tblInvoice.getColumnModel().getColumn(0).setMinWidth(0);
        tblInvoice.getColumnModel().getColumn(0).setMaxWidth(0);
        tblInvoice.getColumnModel().getColumn(0).setPreferredWidth(0);
        setColWidths(tblInvoice, 0, 50, 160, 70, 55, 80, 90);

        // Right-align money columns
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tblInvoice.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);
        tblInvoice.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tblInvoice.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        invoiceScroll = new JScrollPane(tblInvoice,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        invoiceScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        invoiceScroll.getVerticalScrollBar().setUnitIncrement(16);

        // Empty state
        emptyState = new JPanel(new GridBagLayout());
        emptyState.setBackground(CARD);
        JPanel emptyContent = new JPanel();
        emptyContent.setOpaque(false);
        emptyContent.setLayout(new BoxLayout(emptyContent, BoxLayout.Y_AXIS));
        JLabel ico = label("🛒", new Font("Segoe UI Emoji", Font.PLAIN, 40), TXT_M);
        ico.setAlignmentX(CENTER_ALIGNMENT);
        emptyContent.add(ico);
        emptyContent.add(Box.createVerticalStrut(8));
        JLabel et1 = label("Chưa có sản phẩm", F_SEC, TXT_M);
        et1.setAlignmentX(CENTER_ALIGNMENT);
        emptyContent.add(et1);
        JLabel et2 = label("Double-click hoặc Enter từ danh sách thuốc bên trái", F_HINT, TXT_M);
        et2.setAlignmentX(CENTER_ALIGNMENT);
        emptyContent.add(et2);
        emptyState.add(emptyContent);

        // Card layout switch
        JPanel tableArea = new JPanel(new CardLayout());
        tableArea.add(emptyState, "empty");
        tableArea.add(invoiceScroll, "table");
        p.add(tableArea, BorderLayout.CENTER);

        // Bottom: edit toolbar + total
        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(8, 0, 0, 0));

        // Edit toolbar
        JPanel editBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        editBar.setOpaque(false);

        JButton btnSuaSL = outlineBtn("✏ Sửa SL", ORANGE, e -> handleSuaSoLuong());
        JButton btnXoaDong = outlineBtn("🗑 Xóa dòng", RED, e -> handleXoaDong());
        btnSuaSL.setEnabled(false);
        btnXoaDong.setEnabled(false);
        editBar.add(btnSuaSL);
        editBar.add(btnXoaDong);
        editBar.add(label("← Chọn dòng để thao tác", F_HINT, TXT_M));

        tblInvoice.getSelectionModel().addListSelectionListener(e -> {
            boolean sel = tblInvoice.getSelectedRow() >= 0;
            btnSuaSL.setEnabled(sel);
            btnXoaDong.setEnabled(sel);
        });

        bottom.add(editBar, BorderLayout.NORTH);

        // Total ribbon
        JPanel totalBar = new JPanel(new BorderLayout());
        totalBar.setBackground(new Color(239, 246, 255));
        totalBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(191, 219, 254)),
            new EmptyBorder(12, 16, 12, 16)
        ));

        JLabel ltl = label("TỔNG TIỀN:", new Font("Segoe UI", Font.BOLD, 16), BLUE);
        totalBar.add(ltl, BorderLayout.WEST);

        lblTongTien = new JLabel("0 ₫");
        lblTongTien.setFont(F_TOTAL);
        lblTongTien.setForeground(RED);
        totalBar.add(lblTongTien, BorderLayout.EAST);

        bottom.add(totalBar, BorderLayout.SOUTH);

        p.add(bottom, BorderLayout.SOUTH);

        return p;
    }

    /* ======================== RIGHT: Payment (25%) ======================== */

    private JPanel createRightPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(CARD);
        p.setBorder(cardBorder());
        p.setPreferredSize(new Dimension(280, 0));

        // === A. Khách hàng ===
        p.add(sectionLabel("Khách hàng"));
        p.add(Box.createVerticalStrut(6));

        // Selected customer
        JPanel selPanel = new JPanel(new BorderLayout(4, 0));
        selPanel.setOpaque(true);
        selPanel.setBackground(new Color(248, 250, 252));
        selPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(6, 10, 6, 6)
        ));
        selPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        selPanel.setAlignmentX(LEFT_ALIGNMENT);

        lblKhachHang = new JLabel("Khách lẻ");
        lblKhachHang.setFont(F_LBL);
        lblKhachHang.setForeground(TXT_M);
        selPanel.add(lblKhachHang, BorderLayout.CENTER);

        btnXoaKH = new JButton("✕");
        btnXoaKH.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btnXoaKH.setForeground(RED);
        btnXoaKH.setContentAreaFilled(false);
        btnXoaKH.setBorderPainted(false);
        btnXoaKH.setFocusPainted(false);
        btnXoaKH.setPreferredSize(new Dimension(24, 24));
        btnXoaKH.setVisible(false);
        btnXoaKH.addActionListener(e -> clearKhachHang());
        selPanel.add(btnXoaKH, BorderLayout.EAST);

        p.add(selPanel);
        p.add(Box.createVerticalStrut(6));

        // Search KH
        txtTimKH = new JTextField();
        txtTimKH.setFont(F_INPUT);
        txtTimKH.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        txtTimKH.setAlignmentX(LEFT_ALIGNMENT);
        txtTimKH.putClientProperty("JTextField.placeholderText", "Tìm tên/SĐT khách hàng...");
        txtTimKH.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(4, 8, 4, 8)
        ));
        p.add(txtTimKH);
        setupKHAutocomplete();

        p.add(Box.createVerticalStrut(4));

        // Quick buttons
        JPanel khBtns = new JPanel(new GridLayout(1, 2, 4, 0));
        khBtns.setOpaque(false);
        khBtns.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        khBtns.setAlignmentX(LEFT_ALIGNMENT);
        JButton btnKhachLe = smallBtn("Khách lẻ", TXT2);
        btnKhachLe.addActionListener(e -> clearKhachHang());
        khBtns.add(btnKhachLe);
        JButton btnThemKH = smallBtn("+ Thêm KH", BLUE);
        btnThemKH.addActionListener(e -> handleThemKH());
        khBtns.add(btnThemKH);
        p.add(khBtns);

        // === Divider ===
        p.add(Box.createVerticalStrut(12));
        p.add(divider());
        p.add(Box.createVerticalStrut(12));

        // === B. Tóm tắt thanh toán ===
        p.add(sectionLabel("Thanh toán"));
        p.add(Box.createVerticalStrut(8));

        lblSummaryTotal = new JLabel("0 ₫");
        p.add(summaryRow("Tổng tiền hàng:", lblSummaryTotal, F_MONEY, RED));
        p.add(Box.createVerticalStrut(10));

        // Tiền khách đưa
        JLabel lk = label("Tiền khách đưa:", F_LBL, TXT1);
        lk.setAlignmentX(LEFT_ALIGNMENT);
        p.add(lk);
        p.add(Box.createVerticalStrut(3));

        txtTienKhach = new JTextField();
        txtTienKhach.setFont(F_INPUT);
        txtTienKhach.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        txtTienKhach.setAlignmentX(LEFT_ALIGNMENT);
        txtTienKhach.putClientProperty("JTextField.placeholderText", "Nhập số tiền...");
        txtTienKhach.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(4, 8, 4, 8)
        ));
        txtTienKhach.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { calcTienThua(); }
            public void removeUpdate(DocumentEvent e) { calcTienThua(); }
            public void changedUpdate(DocumentEvent e) { calcTienThua(); }
        });
        p.add(txtTienKhach);
        p.add(Box.createVerticalStrut(6));

        lblTienThua = new JLabel("0 ₫");
        p.add(summaryRow("Tiền thừa:", lblTienThua, F_MONEY, GREEN));

        // === Divider ===
        p.add(Box.createVerticalStrut(12));
        p.add(divider());
        p.add(Box.createVerticalStrut(12));

        // === C. Phương thức ===
        p.add(sectionLabel("Phương thức thanh toán"));
        p.add(Box.createVerticalStrut(6));
        JPanel ptPanel = new JPanel(new GridLayout(1, 2, 4, 0));
        ptPanel.setOpaque(false);
        ptPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        ptPanel.setAlignmentX(LEFT_ALIGNMENT);
        // TODO: toggle group to select payment method
        ptPanel.add(smallBtn("Tiền mặt ✓", GREEN));
        ptPanel.add(smallBtn("Chuyển khoản", TXT2));
        p.add(ptPanel);

        // Spacer
        p.add(Box.createVerticalGlue());

        // === D. Action buttons ===
        p.add(Box.createVerticalStrut(10));

        JButton btnPay = new JButton("THANH TOÁN  (F9)");
        btnPay.setFont(F_PAY_BTN);
        btnPay.setBackground(GREEN);
        btnPay.setForeground(Color.WHITE);
        btnPay.setFocusPainted(false);
        btnPay.setBorderPainted(false);
        btnPay.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        btnPay.setAlignmentX(LEFT_ALIGNMENT);
        btnPay.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPay.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btnPay.setBackground(GREEN.darker()); }
            public void mouseExited(java.awt.event.MouseEvent e) { btnPay.setBackground(GREEN); }
        });
        btnPay.addActionListener(e -> handleThanhToan());
        p.add(btnPay);
        p.add(Box.createVerticalStrut(6));

        JPanel subBtns = new JPanel(new GridLayout(1, 2, 4, 0));
        subBtns.setOpaque(false);
        subBtns.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        subBtns.setAlignmentX(LEFT_ALIGNMENT);
        subBtns.add(outlineBtn("Hủy hóa đơn", RED, e -> handleHuy()));
        subBtns.add(outlineBtn("In hóa đơn", BLUE, e -> { /* TODO */ }));
        p.add(subBtns);

        return p;
    }

    /* ================================================================
       WIDGET HELPERS
       ================================================================ */

    private JLabel label(String t, Font f, Color c) {
        JLabel l = new JLabel(t);
        l.setFont(f);
        l.setForeground(c);
        return l;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = label(text, F_SEC, TXT1);
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(0, 0, 0, 0));
        return l;
    }

    private JSeparator divider() {
        JSeparator s = new JSeparator();
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        s.setForeground(DIVIDER);
        return s;
    }

    private JPanel summaryRow(String label, JLabel valueLabel, Font vFont, Color vColor) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.add(label(label, F_LBL, TXT1), BorderLayout.WEST);
        valueLabel.setFont(vFont);
        valueLabel.setForeground(vColor);
        row.add(valueLabel, BorderLayout.EAST);
        return row;
    }

    private javax.swing.border.Border cardBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER), new EmptyBorder(14, 14, 14, 14));
    }

    private JButton outlineBtn(String text, Color color, java.awt.event.ActionListener action) {
        JButton b = new JButton(text);
        b.setFont(F_BTN);
        b.setForeground(color);
        b.setBackground(CARD);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color),
            new EmptyBorder(4, 10, 4, 10)
        ));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(action);
        return b;
    }

    private JButton smallBtn(String text, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 11));
        b.setForeground(fg);
        b.setBackground(CARD);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(3, 6, 3, 6)
        ));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JTable styledTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setRowHeight(36);
        t.setFont(F_TABLE);
        t.setShowGrid(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(DIVIDER);
        t.setSelectionBackground(new Color(219, 234, 254));
        t.setSelectionForeground(TXT1);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setFillsViewportHeight(true);
        t.getTableHeader().setReorderingAllowed(false);

        JTableHeader hdr = t.getTableHeader();
        hdr.setFont(F_TH);
        hdr.setBackground(HDR_BG);
        hdr.setForeground(TXT2);
        hdr.setBorder(new MatteBorder(0, 0, 2, 0, BORDER));
        hdr.setPreferredSize(new Dimension(0, 38));

        // Hover + alternating renderer
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private int hRow = -1;
            private boolean attached;
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val,
                    boolean isSel, boolean focus, int row, int col) {
                if (!attached) {
                    attached = true;
                    tbl.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                        @Override public void mouseMoved(java.awt.event.MouseEvent e) {
                            int r = tbl.rowAtPoint(e.getPoint());
                            if (r != hRow) { hRow = r; tbl.repaint(); }
                        }
                    });
                    tbl.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override public void mouseExited(java.awt.event.MouseEvent e) { hRow = -1; tbl.repaint(); }
                    });
                }
                Component c = super.getTableCellRendererComponent(tbl, val, isSel, focus, row, col);
                if (!isSel) c.setBackground(row == hRow ? HOVER : (row % 2 == 0 ? CARD : ALT));
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return c;
            }
        });
        return t;
    }

    private void setColWidths(JTable t, int... w) {
        var cm = t.getColumnModel();
        for (int i = 0; i < w.length && i < cm.getColumnCount(); i++)
            cm.getColumn(i).setPreferredWidth(w[i]);
    }

    /* ================================================================
       CUSTOMER AUTOCOMPLETE
       ================================================================ */

    private void setupKHAutocomplete() {
        khModel = new DefaultListModel<>();
        khList = new JList<>(khModel);
        khList.setFont(F_INPUT);
        khList.setFixedCellHeight(34);
        khList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        popupKH = new JPopupMenu();
        popupKH.setBorder(BorderFactory.createLineBorder(BORDER));
        JScrollPane sp = new JScrollPane(khList);
        sp.setBorder(null);
        sp.setPreferredSize(new Dimension(260, 170));
        popupKH.add(sp);

        khList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && khList.getSelectedIndex() >= 0) {
                int idx = khList.getSelectedIndex();
                if (khCache != null && idx < khCache.size()) {
                    selectKH(khCache.get(idx));
                    popupKH.setVisible(false);
                }
            }
        });

        searchKHTimer = new javax.swing.Timer(300, e -> doSearchKH());
        searchKHTimer.setRepeats(false);

        txtTimKH.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { searchKHTimer.restart(); }
            public void removeUpdate(DocumentEvent e) { searchKHTimer.restart(); }
            public void changedUpdate(DocumentEvent e) { searchKHTimer.restart(); }
        });
    }

    private void doSearchKH() {
        String kw = txtTimKH.getText().trim();
        if (kw.length() < 1) { popupKH.setVisible(false); return; }
        khCache = khDao.searchByNameOrPhone(kw);
        khModel.clear();
        if (khCache.isEmpty()) {
            khModel.addElement("Không tìm thấy \"" + kw + "\"");
        } else {
            for (var kh : khCache) {
                khModel.addElement(kh.getHoTen() + "  |  " +
                    (kh.getSoDienThoai() != null ? kh.getSoDienThoai() : "--") +
                    "  (Mã: " + kh.getMaKhachHang() + ")");
            }
        }
        if (!popupKH.isVisible()) popupKH.show(txtTimKH, 0, txtTimKH.getHeight());
        popupKH.setPopupSize(txtTimKH.getWidth(), Math.min(khCache.size() * 36 + 10, 200));
    }

    private void selectKH(KhachHang kh) {
        selectedMaKH = kh.getMaKhachHang();
        lblKhachHang.setText("✓ " + kh.getHoTen() +
            (kh.getSoDienThoai() != null ? "  •  " + kh.getSoDienThoai() : ""));
        lblKhachHang.setForeground(GREEN);
        btnXoaKH.setVisible(true);
        txtTimKH.setText("");
    }

    private void clearKhachHang() {
        selectedMaKH = null;
        lblKhachHang.setText("Khách lẻ");
        lblKhachHang.setForeground(TXT_M);
        btnXoaKH.setVisible(false);
    }

    private void handleThemKH() {
        JTextField fName = new JTextField();
        JTextField fPhone = new JTextField();
        Object[] msg = {"Họ tên:", fName, "Số điện thoại:", fPhone};
        int opt = JOptionPane.showConfirmDialog(this, msg, "Thêm khách hàng mới", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            String name = fName.getText().trim();
            String phone = fPhone.getText().trim();
            if (name.isEmpty() || phone.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đủ họ tên và SĐT!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
                return;
            }
            var kh = new KhachHang();
            kh.setHoTen(name);
            kh.setSoDienThoai(phone);
            Integer id = khDao.insertAndGetId(kh);
            if (id != null) {
                kh.setMaKhachHang(id);
                selectKH(kh);
                JOptionPane.showMessageDialog(this, "Thêm KH thành công! Mã: " + id, "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Thêm KH thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /* ================================================================
       PRODUCT LOADING
       ================================================================ */

    private void loadProducts(String keyword) {
        SwingUtilities.invokeLater(() -> {
            productCache = spDao.searchForSale(keyword);
            mdlProduct.setRowCount(0);
            for (SanPham sp : productCache) {
                String status;
                if (sp.getTongTon() <= 0) status = "Hết hàng";
                else if (sp.getTongTon() <= sp.getMucTonToiThieu()) status = "Sắp hết";
                else status = "Còn hàng";

                mdlProduct.addRow(new Object[]{
                    sp.getMaSanPham(),
                    sp.getTenSanPham(),
                    sp.getDonViTinh(),
                    VND.format(sp.getGiaBanDeXuat() != null ? sp.getGiaBanDeXuat() : BigDecimal.ZERO),
                    sp.getTongTon(),
                    status
                });
            }
            lblProductCount.setText(productCache.size() + " sản phẩm");
        });
    }

    /* ================================================================
       ADD PRODUCT TO INVOICE
       ================================================================ */

    private void addFirstProduct() {
        if (mdlProduct.getRowCount() > 0) {
            tblProduct.setRowSelectionInterval(0, 0);
            addSelectedProduct();
        }
    }

    private void addSelectedProduct() {
        int row = tblProduct.getSelectedRow();
        if (row < 0) return;

        int maSP = (int) mdlProduct.getValueAt(row, 0);
        String tenSP = (String) mdlProduct.getValueAt(row, 1);
        int ton = (int) mdlProduct.getValueAt(row, 4);
        String status = (String) mdlProduct.getValueAt(row, 5);

        if ("Hết hàng".equals(status) || ton <= 0) {
            JOptionPane.showMessageDialog(this,
                "\"" + tenSP + "\" đã hết hàng!", "Không thể thêm", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Auto-create invoice if needed
        if (currentMaHoaDon == null) {
            Integer id = hoaDonDao.createHoaDon(currentUser.getMaNguoiDung(), selectedMaKH, "");
            if (id == null) {
                JOptionPane.showMessageDialog(this,
                    "Không thể tạo hóa đơn!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            currentMaHoaDon = id;
            lblMaHoaDon.setText("Chi tiết hóa đơn  #HD" + String.format("%04d", id));
        }

        // Sell using FEFO (1 unit default)
        if (hoaDonDao.sellProductFEFO(currentMaHoaDon, maSP, 1, null)) {
            loadInvoiceDetails();
            // Refresh product stock
            loadProducts(txtSearch.getText().trim());
            txtSearch.requestFocusInWindow();
        } else {
            JOptionPane.showMessageDialog(this,
                "Thêm \"" + tenSP + "\" thất bại!\nKiểm tra tồn kho.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /* ================================================================
       INVOICE MANAGEMENT
       ================================================================ */

    private void loadInvoiceDetails() {
        if (currentMaHoaDon == null) return;
        mdlInvoice.setRowCount(0);
        var list = cthdDao.getDetailForDisplay(currentMaHoaDon);
        for (var row : list) mdlInvoice.addRow(row);
        updateTongTien();
        updateTableVisibility();
    }

    private void updateTongTien() {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < mdlInvoice.getRowCount(); i++) {
            BigDecimal tt = (BigDecimal) mdlInvoice.getValueAt(i, 6);
            if (tt != null) total = total.add(tt);
        }
        lblTongTien.setText(VND.format(total));
        lblSummaryTotal.setText(VND.format(total));
        calcTienThua();
    }

    private void calcTienThua() {
        try {
            BigDecimal total = BigDecimal.ZERO;
            for (int i = 0; i < mdlInvoice.getRowCount(); i++) {
                BigDecimal tt = (BigDecimal) mdlInvoice.getValueAt(i, 6);
                if (tt != null) total = total.add(tt);
            }
            String txt = txtTienKhach.getText().trim().replaceAll("[^\\d]", "");
            if (!txt.isEmpty()) {
                BigDecimal tienKhach = new BigDecimal(txt);
                BigDecimal thua = tienKhach.subtract(total);
                lblTienThua.setText(VND.format(thua.max(BigDecimal.ZERO)));
                lblTienThua.setForeground(thua.signum() >= 0 ? GREEN : RED);
            } else {
                lblTienThua.setText("0 ₫");
                lblTienThua.setForeground(TXT_M);
            }
        } catch (NumberFormatException ex) {
            lblTienThua.setText("–");
            lblTienThua.setForeground(TXT_M);
        }
    }

    private void updateTableVisibility() {
        JPanel area = (JPanel) invoiceScroll.getParent();
        CardLayout cl = (CardLayout) area.getLayout();
        cl.show(area, mdlInvoice.getRowCount() == 0 ? "empty" : "table");
    }

    /* ================================================================
       BUTTON HANDLERS
       ================================================================ */

    private void handleTaoMoi() {
        if (currentMaHoaDon != null && mdlInvoice.getRowCount() > 0) {
            int opt = JOptionPane.showConfirmDialog(this,
                "Hóa đơn hiện tại sẽ bị hủy. Tiếp tục?", "Tạo mới", JOptionPane.YES_NO_OPTION);
            if (opt != JOptionPane.YES_OPTION) return;
        }
        resetForm();
    }

    private void handleXoaTatCa() {
        if (mdlInvoice.getRowCount() == 0) return;
        int opt = JOptionPane.showConfirmDialog(this,
            "Xóa tất cả sản phẩm khỏi hóa đơn?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            // Delete all invoice items
            for (int i = mdlInvoice.getRowCount() - 1; i >= 0; i--) {
                int maCTHD = (int) mdlInvoice.getValueAt(i, 0);
                cthdDao.deleteChiTiet(maCTHD);
            }
            loadInvoiceDetails();
            loadProducts(txtSearch.getText().trim());
        }
    }

    private void handleSuaSoLuong() {
        int row = tblInvoice.getSelectedRow();
        if (row < 0) return;
        int maCTHD = (int) mdlInvoice.getValueAt(row, 0);
        String tenSP = (String) mdlInvoice.getValueAt(row, 2);
        int slCu = (int) mdlInvoice.getValueAt(row, 4);

        String input = JOptionPane.showInputDialog(this,
            "Sản phẩm: " + tenSP + "\nSố lượng hiện tại: " + slCu + "\n\nNhập số lượng mới:",
            "Sửa số lượng", JOptionPane.PLAIN_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;

        int slMoi;
        try {
            slMoi = Integer.parseInt(input.trim());
            if (slMoi <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "Số lượng phải là số nguyên dương!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (slMoi == slCu) return;

        if (cthdDao.updateSoLuong(maCTHD, slMoi)) {
            loadInvoiceDetails();
            loadProducts(txtSearch.getText().trim());
        } else {
            JOptionPane.showMessageDialog(this,
                "Cập nhật thất bại! Có thể không đủ tồn kho.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleXoaDong() {
        int row = tblInvoice.getSelectedRow();
        if (row < 0) return;
        int maCTHD = (int) mdlInvoice.getValueAt(row, 0);
        String tenSP = (String) mdlInvoice.getValueAt(row, 2);
        int sl = (int) mdlInvoice.getValueAt(row, 4);

        int opt = JOptionPane.showConfirmDialog(this,
            "Xóa: " + tenSP + " (SL: " + sl + ")?\nTồn kho sẽ được hoàn trả.",
            "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            if (cthdDao.deleteChiTiet(maCTHD)) {
                loadInvoiceDetails();
                loadProducts(txtSearch.getText().trim());
            }
        }
    }

    private void handleThanhToan() {
        if (currentMaHoaDon == null) {
            JOptionPane.showMessageDialog(this,
                "Chưa có hóa đơn!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (mdlInvoice.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                "Hóa đơn chưa có sản phẩm!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Calculate total
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < mdlInvoice.getRowCount(); i++) {
            BigDecimal tt = (BigDecimal) mdlInvoice.getValueAt(i, 6);
            if (tt != null) total = total.add(tt);
        }

        // Validate tiền khách nếu có nhập
        String tienStr = txtTienKhach.getText().trim().replaceAll("[^\\d]", "");
        if (!tienStr.isEmpty()) {
            BigDecimal tienKhach = new BigDecimal(tienStr);
            if (tienKhach.compareTo(total) < 0) {
                JOptionPane.showMessageDialog(this,
                    "Tiền khách đưa chưa đủ!\nCần: " + VND.format(total) + "\nĐã đưa: " + VND.format(tienKhach),
                    "Thiếu tiền", JOptionPane.WARNING_MESSAGE);
                txtTienKhach.requestFocus();
                return;
            }
        }

        int opt = JOptionPane.showConfirmDialog(this,
            "Xác nhận thanh toán hóa đơn #HD" + String.format("%04d", currentMaHoaDon) +
            "?\n\nTổng tiền: " + VND.format(total),
            "Thanh toán", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(this,
                "✅ Thanh toán thành công!\nMã HĐ: HD" + String.format("%04d", currentMaHoaDon) +
                "\nTổng: " + VND.format(total),
                "Thành công", JOptionPane.INFORMATION_MESSAGE);
            resetForm();
        }
    }

    private void handleHuy() {
        if (currentMaHoaDon == null) return;
        int opt = JOptionPane.showConfirmDialog(this,
            "Hủy hóa đơn #HD" + String.format("%04d", currentMaHoaDon) + "?",
            "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            // Delete all items to restore stock
            for (int i = mdlInvoice.getRowCount() - 1; i >= 0; i--) {
                int maCTHD = (int) mdlInvoice.getValueAt(i, 0);
                cthdDao.deleteChiTiet(maCTHD);
            }
            resetForm();
        }
    }

    private void resetForm() {
        currentMaHoaDon = null;
        selectedMaKH = null;
        lblMaHoaDon.setText("Chi tiết hóa đơn");
        clearKhachHang();
        txtTimKH.setText("");
        txtTienKhach.setText("");
        lblTienThua.setText("0 ₫");
        mdlInvoice.setRowCount(0);
        updateTongTien();
        updateTableVisibility();
        loadProducts(null);
        SwingUtilities.invokeLater(() -> txtSearch.requestFocusInWindow());
    }
}
