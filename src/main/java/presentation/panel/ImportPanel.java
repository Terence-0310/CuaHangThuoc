package presentation.panel;

import common.AppColors;
import domain.entity.Product;
import infrastructure.repository.NhapKhoDAO;
import presentation.presenter.ImportPresenter;
import presentation.view.IImportView;

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
import java.util.ArrayList;
import java.util.List;

/**
 * ★ ImportPanel — Pure UI (MVP View) cho module Nhập Kho
 *
 * 3 sections:
 *   Section 1: Thông tin SP (TenSP auto-suggest, DVT, GiaBan, %Si, GiaBanSi)
 *   Section 2: Thông tin Lô (SoLo, HSD, SoLuong, GiaNhap)
 *   Section 3: Giỏ nhập (JTable cart + buttons)
 *
 * KHÔNG có logic. Delegate tất cả cho ImportPresenter.
 */
public class ImportPanel extends JPanel implements IImportView {

    private final ImportPresenter presenter;

    // Nha cung cap
    private JComboBox<SupplierItem> cboNCC;


    // Section 1: Sản phẩm
    private JTextField txtTenSP, txtDVT, txtGiaBan;

    // Section 2: Lô hàng
    private JTextField txtSoLo, txtHSD, txtSoLuong, txtGiaNhap;

    // Section 3: Giỏ
    private JTable cartTable;
    private DefaultTableModel cartModel;
    private JLabel lblTotal;

    // Auto-suggest
    private List<Product> productList = new ArrayList<>();
    private JPopupMenu suggestPopup;
    private boolean suppressSuggest = false;
    private javax.swing.Timer suggestTimer;

    public ImportPanel() {
        presenter = new ImportPresenter(this, new NhapKhoDAO());
        setLayout(new BorderLayout(0, 0));
        setBackground(AppColors.NEUTRAL);
        initComponents();
        presenter.init();

        // ★ Khi chuyển sang tab Nhập Kho → refresh danh sách NCC (có thể vừa thêm mới)
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                presenter.loadSuppliers();
            }
        });
    }

    // ================================================================
    //  LAYOUT — Chỉ Swing, không logic
    // ================================================================

    private void initComponents() {
        // === HEADER ===
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(14, 24, 14, 24));

        JLabel lblTitle = new JLabel("Nhập Kho");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(AppColors.PRIMARY);
        header.add(lblTitle, BorderLayout.WEST);

        JLabel lblDesc = new JLabel("Nhập thông tin sản phẩm, lô hàng, xác nhận và lưu phiếu");
        lblDesc.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblDesc.setForeground(AppColors.TEXT_SECONDARY);
        header.add(lblDesc, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // === CENTER: Form + Cart ===
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setBackground(AppColors.NEUTRAL);
        center.setBorder(new EmptyBorder(10, 16, 10, 16));

        // --- TOP: 2 Sections form ---
        JPanel formRow = new JPanel(new GridLayout(1, 2, 12, 0));
        formRow.setOpaque(false);
        formRow.add(createProductSection());
        formRow.add(createBatchSection());
        center.add(formRow, BorderLayout.NORTH);

        // --- CENTER: Cart ---
        center.add(createCartSection(), BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);
    }

    // ==================== SECTION 1: SẢN PHẨM ====================

    private JPanel createProductSection() {
        JPanel panel = createSectionPanel("Thông Tin Sản Phẩm");

        txtTenSP = new JTextField();
        txtTenSP.setToolTipText("Gõ tên SP để gợi ý, hoặc nhập tên mới");
        addFormField(panel, "Tên sản phẩm:", txtTenSP);

        // Auto-suggest: Timer debounce 250ms
        suggestPopup = new JPopupMenu();
        suggestPopup.setFocusable(false);
        suggestTimer = new javax.swing.Timer(250, e -> doShowSuggestions());
        suggestTimer.setRepeats(false);

        txtTenSP.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                int code = e.getKeyCode();
                // Ignore navigation/control keys
                if (code == KeyEvent.VK_ESCAPE) {
                    suggestPopup.setVisible(false);
                    return;
                }
                if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_UP
                        || code == KeyEvent.VK_DOWN || code == KeyEvent.VK_TAB) {
                    return;
                }
                if (!suppressSuggest) {
                    suggestTimer.restart();
                }
            }
        });

        txtDVT = new JTextField();
        txtDVT.setToolTipText("VD: Viên, Hộp, Chai, Tuýp");
        addFormField(panel, "Đơn vị tính:", txtDVT);

        txtGiaBan = new JTextField();
        addMoneyField(txtGiaBan);
        addFormFieldWithSuffix(panel, "Giá bán lẻ:", txtGiaBan, "VNĐ");

        // Row: Ghi chú sau giá bán



        panel.add(Box.createVerticalGlue());
        return panel;
    }

    // ==================== SECTION 2: LÔ HÀNG ====================

    private JPanel createBatchSection() {
        JPanel panel = createSectionPanel("Thông Tin Lô Hàng");

        txtSoLo = new JTextField();
        txtSoLo.setToolTipText("Số lô do NCC cung cấp (VD: LOT-2026-001)");
        addFormField(panel, "Số lô:", txtSoLo);

        // ★ Nhà cung cấp (ComboBox) — ngay dưới số lô
        JLabel lblNCC = new JLabel("Nhà cung cấp:");
        lblNCC.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblNCC.setForeground(AppColors.TEXT_SECONDARY);
        lblNCC.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(lblNCC);
        panel.add(Box.createRigidArea(new Dimension(0, 3)));

        cboNCC = new JComboBox<>();
        cboNCC.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cboNCC.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        cboNCC.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(cboNCC);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        txtHSD = new JTextField();
        txtHSD.setToolTipText("dd/MM/yyyy (VD: 31/12/2027)");
        addDateAutoSlash(txtHSD);
        addFormField(panel, "Hạn sử dụng:", txtHSD);

        txtSoLuong = new JTextField();
        addNumericFilter(txtSoLuong);
        addFormField(panel, "Số lượng:", txtSoLuong);

        txtGiaNhap = new JTextField();
        addMoneyField(txtGiaNhap);
        addFormFieldWithSuffix(panel, "Giá nhập:", txtGiaNhap, "VNĐ");

        panel.add(Box.createRigidArea(new Dimension(0, 12)));

        // Button "Xác nhận"
        JButton btnAddToCart = createButton("Xác Nhận", AppColors.SUCCESS);
        btnAddToCart.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnAddToCart.setAlignmentX(LEFT_ALIGNMENT);
        btnAddToCart.addActionListener(e -> presenter.addToCart());
        panel.add(btnAddToCart);

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    // ==================== SECTION 3: GIỎ NHẬP ====================

    private JPanel createCartSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AppColors.NEUTRAL_DARK, 1),
                new EmptyBorder(12, 16, 12, 16)
        ));

        // --- Header bar ---
        JPanel barPanel = new JPanel(new BorderLayout());
        barPanel.setOpaque(false);

        JLabel lblCart = new JLabel("Giỏ Nhập Kho");
        lblCart.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblCart.setForeground(AppColors.PRIMARY);
        barPanel.add(lblCart, BorderLayout.WEST);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);

        JButton btnRemove = createButton("Xóa Dòng", AppColors.DANGER);
        btnRemove.addActionListener(e -> presenter.removeFromCart());
        btnPanel.add(btnRemove);

        JButton btnSave = createButton("XÁC NHẬN NHẬP HÀNG", AppColors.PRIMARY);
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSave.setPreferredSize(new Dimension(180, 36));
        btnSave.addActionListener(e -> presenter.saveImportTicket());
        btnPanel.add(btnSave);

        barPanel.add(btnPanel, BorderLayout.EAST);
        panel.add(barPanel, BorderLayout.NORTH);

        // --- Table ---
        String[] cols = {"#", "Tên SP", "ĐVT", "Mã Lô", "Hạn SD", "SL", "Giá Nhập (VNĐ)"};
        cartModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        cartTable = new JTable(cartModel);
        cartTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cartTable.setRowHeight(32);
        cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cartTable.setShowGrid(true);
        cartTable.setGridColor(AppColors.NEUTRAL_DARK);
        cartTable.setFillsViewportHeight(true);

        JTableHeader th = cartTable.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 12));
        th.setBackground(AppColors.TABLE_HEADER_BG);
        th.setForeground(AppColors.TABLE_HEADER_FG);
        th.setPreferredSize(new Dimension(0, 34));
        th.setReorderingAllowed(false);

        // Column widths
        cartTable.getColumnModel().getColumn(0).setPreferredWidth(35);
        cartTable.getColumnModel().getColumn(0).setMaxWidth(45);
        cartTable.getColumnModel().getColumn(1).setPreferredWidth(220);
        cartTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        cartTable.getColumnModel().getColumn(2).setMaxWidth(80);
        cartTable.getColumnModel().getColumn(3).setPreferredWidth(110);
        cartTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        cartTable.getColumnModel().getColumn(5).setPreferredWidth(60);
        cartTable.getColumnModel().getColumn(5).setMaxWidth(75);
        cartTable.getColumnModel().getColumn(6).setPreferredWidth(140);

        // Cell renderers
        DefaultTableCellRenderer centerR = new DefaultTableCellRenderer();
        centerR.setHorizontalAlignment(SwingConstants.CENTER);

        cartTable.getColumnModel().getColumn(0).setCellRenderer(centerR);
        cartTable.getColumnModel().getColumn(2).setCellRenderer(centerR);
        cartTable.getColumnModel().getColumn(4).setCellRenderer(centerR);
        cartTable.getColumnModel().getColumn(5).setCellRenderer(centerR);

        // Giá Nhập: right-align, commas, alt row color
        cartTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            { setHorizontalAlignment(SwingConstants.RIGHT); }
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) c.setBackground(row % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);
                return c;
            }
        });

        // Alt row colors for other cols
        cartTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) c.setBackground(row % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(cartTable);
        scroll.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1));
        panel.add(scroll, BorderLayout.CENTER);

        // --- Footer: Tổng tiền ---
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppColors.NEUTRAL_DARK));

        JLabel lblTotalLabel = new JLabel("TỔNG TIỀN:");
        lblTotalLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTotalLabel.setForeground(AppColors.TEXT_PRIMARY);
        footer.add(lblTotalLabel);

        lblTotal = new JLabel("0 VNĐ");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTotal.setForeground(AppColors.PRIMARY);
        footer.add(lblTotal);

        panel.add(footer, BorderLayout.SOUTH);

        return panel;
    }

    // ================================================================
    //  Auto-Suggest Popup
    // ================================================================

    private void doShowSuggestions() {
        String text = txtTenSP.getText().trim().toLowerCase();
        suggestPopup.setVisible(false);
        suggestPopup.removeAll();

        if (text.length() < 2) {
            return;
        }

        List<Product> matches = new ArrayList<>();
        for (Product p : productList) {
            if (p.getTenSP().toLowerCase().contains(text)) {
                matches.add(p);
                if (matches.size() >= 10) break;
            }
        }

        // Nếu chỉ 1 kết quả và trùng chính xác → đã chọn rồi
        if (matches.size() == 1 && matches.get(0).getTenSP().equalsIgnoreCase(text)) {
            return;
        }

        if (matches.isEmpty()) {
            JMenuItem noMatch = new JMenuItem("[Moi] " + txtTenSP.getText().trim());
            noMatch.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            noMatch.setForeground(AppColors.SUCCESS);
            noMatch.setEnabled(false);
            suggestPopup.add(noMatch);
        } else {
            for (Product p : matches) {
                String label = p.getTenSP() + "  (" + p.getDonViTinh() + ")  |  Ma: " + p.getMaSP();
                JMenuItem item = new JMenuItem(label);
                item.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                item.addActionListener(e -> {
                    suppressSuggest = true;
                    txtTenSP.setText(p.getTenSP());
                    suggestPopup.setVisible(false);
                    presenter.onProductSelected(p);
                    // Reset flag after a short delay to avoid re-trigger
                    javax.swing.Timer resetFlag = new javax.swing.Timer(100, ev -> suppressSuggest = false);
                    resetFlag.setRepeats(false);
                    resetFlag.start();
                });
                suggestPopup.add(item);
            }
        }

        if (txtTenSP.isShowing() && suggestPopup.getComponentCount() > 0) {
            suggestPopup.show(txtTenSP, 0, txtTenSP.getHeight());
            txtTenSP.requestFocusInWindow();
        }
    }

    // ================================================================
    //  IImportView IMPLEMENTATION
    // ================================================================

    @Override
    public String getTenSP() { return txtTenSP.getText(); }
    @Override
    public String getDonViTinh() { return txtDVT.getText(); }
    @Override
    public String getGiaBanText() { return txtGiaBan.getText(); }
    @Override
    public String getSoLuongText() { return txtSoLuong.getText(); }
    @Override
    public String getSoLoText() { return txtSoLo.getText(); }
    @Override
    public String getHanSuDungText() { return txtHSD.getText(); }
    @Override
    public String getGiaNhapText() { return txtGiaNhap.getText(); }

    @Override
    public void addCartRow(String tenSP, String dvt, String soLo, String hsd,
                           int soLuong, String giaNhap) {
        int rowNum = cartModel.getRowCount() + 1;
        cartModel.addRow(new Object[]{rowNum, tenSP, dvt, soLo, hsd,
                soLuong, giaNhap});
    }

    @Override
    public void removeCartRow(int row) {
        cartModel.removeRow(row);
        // Re-number
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            cartModel.setValueAt(i + 1, i, 0);
        }
    }

    @Override
    public int getCartRowCount() { return cartModel.getRowCount(); }

    @Override
    public int getSelectedCartRow() { return cartTable.getSelectedRow(); }

    @Override
    public void clearCart() {
        cartModel.setRowCount(0);
    }

    @Override
    public void clearForm() {
        txtTenSP.setText("");
        txtDVT.setText("");
        txtGiaBan.setText("");
        txtSoLo.setText("");
        txtHSD.setText("");
        txtSoLuong.setText("");
        txtGiaNhap.setText("");
    }

    @Override
    public void updateTotalLabel(String text) { lblTotal.setText(text); }

    @Override
    public void setDonViTinhText(String text) { txtDVT.setText(text); }
    @Override
    public void setGiaBanText(String text) { txtGiaBan.setText(text); }

    @Override
    public void setProductList(List<Product> products) {
        this.productList = products;
    }

    @Override
    public void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }
    @Override
    public void showWarning(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Cảnh báo", JOptionPane.WARNING_MESSAGE);
    }
    @Override
    public void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
    @Override
    public boolean confirm(String message, String title) {
        return JOptionPane.showConfirmDialog(this, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }
    @Override
    public void setLoading(boolean loading) {
        setCursor(loading ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }
    @Override
    public void focusTenSP() { txtTenSP.requestFocusInWindow(); }

    @Override
    public int getSelectedMaNCC() {
        SupplierItem item = (SupplierItem) cboNCC.getSelectedItem();
        return item != null ? item.maNCC : -1;
    }

    @Override
    public void setSupplierList(java.util.List<domain.entity.Supplier> suppliers) {
        cboNCC.removeAllItems();
        cboNCC.addItem(new SupplierItem(-1, "-- Chon nha cung cap --"));
        for (domain.entity.Supplier s : suppliers) {
            cboNCC.addItem(new SupplierItem(s.getMaNCC(), s.getTenNCC()));
        }
    }

    /** Wrapper cho JComboBox — hien ten, giu ma */
    private static class SupplierItem {
        final int maNCC;
        final String tenNCC;
        SupplierItem(int maNCC, String tenNCC) {
            this.maNCC = maNCC;
            this.tenNCC = tenNCC;
        }
        @Override
        public String toString() { return tenNCC; }
    }

    // ================================================================
    //  UI HELPERS — Pure Swing
    // ================================================================

    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AppColors.NEUTRAL_DARK, 1),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setForeground(AppColors.PRIMARY);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(lbl);
        panel.add(Box.createRigidArea(new Dimension(0, 12)));

        return panel;
    }

    private void addFormField(JPanel panel, String label, JTextField field) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(AppColors.TEXT_SECONDARY);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(lbl);
        panel.add(Box.createRigidArea(new Dimension(0, 3)));

        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        field.setPreferredSize(new Dimension(0, 34));
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AppColors.NEUTRAL_DARKER, 1),
                new EmptyBorder(0, 10, 0, 10)
        ));
        field.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(field);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
    }

    private void addFormFieldWithSuffix(JPanel panel, String label, JTextField field, String suffix) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(AppColors.TEXT_SECONDARY);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(lbl);
        panel.add(Box.createRigidArea(new Dimension(0, 3)));

        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel lblSuffix = new JLabel("  " + suffix + "  ");
        lblSuffix.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSuffix.setForeground(AppColors.TEXT_SECONDARY);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        wrapper.setPreferredSize(new Dimension(0, 34));
        wrapper.setBorder(new LineBorder(AppColors.NEUTRAL_DARKER, 1));
        wrapper.setBackground(Color.WHITE);
        wrapper.setAlignmentX(LEFT_ALIGNMENT);

        field.setBorder(new EmptyBorder(0, 10, 0, 4));
        wrapper.add(field, BorderLayout.CENTER);
        wrapper.add(lblSuffix, BorderLayout.EAST);

        panel.add(wrapper);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
    }

    private JPanel createLabeledField(String label, JTextField field) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(AppColors.TEXT_SECONDARY);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        p.add(lbl);
        p.add(Box.createRigidArea(new Dimension(0, 3)));

        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        field.setPreferredSize(new Dimension(0, 34));
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AppColors.NEUTRAL_DARKER, 1),
                new EmptyBorder(0, 10, 0, 10)
        ));
        field.setAlignmentX(LEFT_ALIGNMENT);
        p.add(field);

        return p;
    }

    private void addNumericFilter(JTextField field) {
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c) && c != '.' && c != ','
                        && c != KeyEvent.VK_BACK_SPACE && c != KeyEvent.VK_DELETE) {
                    e.consume();
                }
            }
        });
    }

    /** Auto-insert '/' cho dd/MM/yyyy khi user go so */
    private void addDateAutoSlash(JTextField field) {
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                String text = field.getText();
                // Chi cho phep so va /
                if (!Character.isDigit(c) && c != '/'
                        && c != KeyEvent.VK_BACK_SPACE && c != KeyEvent.VK_DELETE) {
                    e.consume();
                    return;
                }
                // Max 10 ky tu (dd/MM/yyyy)
                if (Character.isDigit(c) && text.length() >= 10) {
                    e.consume();
                    return;
                }
                // Auto add '/' sau vi tri 2 va 5
                if (Character.isDigit(c)) {
                    if (text.length() == 2 || text.length() == 5) {
                        field.setText(text + "/");
                    }
                }
            }
        });
    }

    /** Chỉ cho nhập số + tự thêm dấu phẩy phân cách hàng nghìn khi gõ */
    private void addMoneyField(JTextField field) {
        addNumericFilter(field);
        field.getDocument().addDocumentListener(new DocumentListener() {
            private boolean updating = false;
            public void insertUpdate(DocumentEvent e) { formatMoney(); }
            public void removeUpdate(DocumentEvent e) { formatMoney(); }
            public void changedUpdate(DocumentEvent e) {}
            private void formatMoney() {
                if (updating) return;
                updating = true;
                SwingUtilities.invokeLater(() -> {
                    try {
                        String raw = field.getText().replace(",", "").replace(".", "").trim();
                        if (raw.isEmpty()) { updating = false; return; }
                        long val = Long.parseLong(raw);
                        int pos = field.getCaretPosition();
                        int oldLen = field.getText().length();
                        field.setText(String.format("%,d", val));
                        int newLen = field.getText().length();
                        int newPos = Math.max(0, Math.min(pos + (newLen - oldLen), newLen));
                        field.setCaretPosition(newPos);
                    } catch (NumberFormatException ignored) {
                    } finally {
                        updating = false;
                    }
                });
            }
        });
    }

    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 16, 34));

        Color hover = bg.darker();
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (btn.isEnabled()) btn.setBackground(hover); }
            public void mouseExited(MouseEvent e) { if (btn.isEnabled()) btn.setBackground(bg); }
        });
        return btn;
    }
}
