package presentation;

import common.AppColors;
import common.ServiceFactory;
import common.Session;
import domain.entity.Product;
import presentation.presenter.ProductPresenter;
import presentation.view.IProductView;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;

/**
 * ★ ProductPanel — Pure UI (MVP View)
 * 
 * CHỈ chứa Swing layout + wiring events → delegate cho Presenter.
 * KHÔNG có business logic, KHÔNG có state, KHÔNG gọi Service trực tiếp.
 * 
 * → Đổi UI? Sửa file NÀY.
 * → Logic không đổi? ProductPresenter giữ nguyên.
 */
public class ProductPanel extends JPanel implements IProductView {

    private final ProductPresenter presenter;
    private final DecimalFormat moneyFormat = new DecimalFormat("#,### VNĐ");

    // --- Form fields ---
    private JTextField txtMaSP, txtTenSP, txtDonViTinh, txtGiaBan;
    private JTextField txtSearch;
    private JComboBox<String> cboFilter;

    // --- Buttons ---
    private JButton btnAdd, btnUpdate, btnToggle, btnClear;
    private JButton btnBulkStop, btnBulkRestore;
    private JPanel bulkPanel;

    // --- Table ---
    private JTable table;
    private DefaultTableModel tableModel;

    // --- Pagination ---
    private JButton btnFirst, btnPrev, btnNext, btnLast;
    private JLabel lblPageInfo;
    private JPanel pageNumbersPanel;

    // --- Debounce ---
    private Timer searchDebounce;

    // --- Internal UI state (suppress loop) ---
    private boolean suppressModelListener = false;

    public ProductPanel() {
        presenter = new ProductPresenter(this, ServiceFactory.getProductService());

        setLayout(new BorderLayout(0, 0));
        setBackground(AppColors.NEUTRAL);
        initComponents();
        presenter.loadPage(1);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                presenter.loadPage(presenter.getCurrentPage());
            }
        });
    }

    // ================================================================
    //  PHẦN DƯỚI ĐÂY **CHỈ CÓ** SWING LAYOUT + WIRING
    //  Không có logic, không có state, không gọi Service.
    //  → KHI ĐỔI UI: sửa các hàm bên dưới.
    // ================================================================

    private void initComponents() {
        // === TOP BAR ===
        JPanel topBar = new JPanel(new BorderLayout(12, 0));
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel lblTitle = new JLabel("Quản Lý Sản Phẩm");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(AppColors.TEXT_PRIMARY);
        topBar.add(lblTitle, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        if (Session.isAdmin()) {
            cboFilter = new JComboBox<>(new String[]{"Tất cả", "Đang bán", "Ngừng bán"});
            cboFilter.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            cboFilter.setPreferredSize(new Dimension(120, 32));
            cboFilter.addActionListener(e -> presenter.onFilterChanged());
            rightPanel.add(cboFilter);
        }

        JLabel lblSearch = new JLabel("Tìm kiếm:");
        lblSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSearch.setForeground(AppColors.TEXT_SECONDARY);
        rightPanel.add(lblSearch);

        txtSearch = new JTextField(16);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearch.setPreferredSize(new Dimension(200, 32));
        txtSearch.setBackground(Color.WHITE);
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.PRIMARY_LIGHT, 1),
                new EmptyBorder(0, 10, 0, 10)
        ));
        txtSearch.putClientProperty("JTextField.placeholderText", "Nhập tên thuốc...");

        searchDebounce = new Timer(400, e -> presenter.onSearchChanged());
        searchDebounce.setRepeats(false);
        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { searchDebounce.restart(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { searchDebounce.restart(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { searchDebounce.restart(); }
        });
        rightPanel.add(txtSearch);

        topBar.add(rightPanel, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // === CENTER: Split ===
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(340);
        splitPane.setDividerSize(1);
        splitPane.setBorder(null);

        // --- LEFT: Form ---
        JPanel formWrapper = new JPanel(new BorderLayout());
        formWrapper.setBackground(Color.WHITE);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel lblForm = new JLabel("Thông Tin Sản Phẩm");
        lblForm.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblForm.setForeground(AppColors.PRIMARY);
        lblForm.setAlignmentX(LEFT_ALIGNMENT);
        formPanel.add(lblForm);
        formPanel.add(Box.createRigidArea(new Dimension(0, 16)));

        txtMaSP = new JTextField();
        txtMaSP.setEditable(false);
        txtMaSP.setBackground(AppColors.NEUTRAL);
        addFormRow(formPanel, "Mã SP:", txtMaSP);

        txtTenSP = new JTextField();
        addFormRow(formPanel, "Tên sản phẩm:", txtTenSP);

        txtDonViTinh = new JTextField();
        addFormRow(formPanel, "Đơn vị tính:", txtDonViTinh);

        txtGiaBan = new JTextField();
        addNumericFilter(txtGiaBan);
        addFormRowWithSuffix(formPanel, "Giá bán:", txtGiaBan, "VNĐ");



        formPanel.add(Box.createRigidArea(new Dimension(0, 16)));

        // --- Buttons ---
        JPanel btnPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setAlignmentX(LEFT_ALIGNMENT);
        btnPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));

        btnAdd = createButton("Thêm Mới", AppColors.PRIMARY);
        btnUpdate = createButton("Cập Nhật", AppColors.PRIMARY);
        btnToggle = createButton("Ngừng Bán", AppColors.DANGER);
        btnClear = createButton("Làm Mới", AppColors.SECONDARY);

        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnToggle);
        btnPanel.add(btnClear);
        formPanel.add(btnPanel);

        // --- Bulk Action (HIDDEN by default) ---
        formPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        bulkPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        bulkPanel.setBackground(Color.WHITE);
        bulkPanel.setAlignmentX(LEFT_ALIGNMENT);
        bulkPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        bulkPanel.setVisible(false);

        btnBulkStop = createButton("Ngừng Hàng Loạt", AppColors.DANGER);
        btnBulkRestore = createButton("Khôi Phục Hàng Loạt", AppColors.PRIMARY);
        bulkPanel.add(btnBulkStop);
        bulkPanel.add(btnBulkRestore);
        formPanel.add(bulkPanel);

        formPanel.add(Box.createVerticalGlue());

        JLabel lblTip = new JLabel("<html><i>* Giá bán sỉ thường = 85-95% giá lẻ</i></html>");
        lblTip.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblTip.setForeground(AppColors.TEXT_SECONDARY);
        lblTip.setAlignmentX(LEFT_ALIGNMENT);
        formPanel.add(lblTip);

        formWrapper.add(formPanel, BorderLayout.CENTER);
        splitPane.setLeftComponent(formWrapper);

        // --- RIGHT: Table + Pagination ---
        JPanel rightWrapper = new JPanel(new BorderLayout());
        rightWrapper.setBackground(Color.WHITE);
        rightWrapper.setBorder(new EmptyBorder(0, 12, 0, 24));

        String[] columns = {"", "Mã SP", "Tên Sản Phẩm", "ĐVT", "Giá Bán", "Số lượng", "Trạng Thái"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
            @Override
            public Class<?> getColumnClass(int col) {
                switch (col) {
                    case 0: return Boolean.class;
                    case 1: return Integer.class;
                    case 4: return BigDecimal.class;
                    case 5: return Integer.class;
                    default: return String.class;
                }
            }
        };

        table = new JTable(tableModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(AppColors.PRIMARY_VERY_LIGHT);
        table.setSelectionForeground(AppColors.TEXT_PRIMARY);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(false);

        JTableHeader header = table.getTableHeader();
        header.setBackground(AppColors.TABLE_HEADER_BG);
        header.setForeground(AppColors.TABLE_HEADER_FG);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setPreferredSize(new Dimension(0, 36));

        // ★ Header click → delegate to presenter
        header.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int colIndex = table.columnAtPoint(e.getPoint());
                if (colIndex == 0) {
                    presenter.onSelectAllToggle();
                } else {
                    presenter.onHeaderClick(colIndex);
                }
            }
        });

        // ★ Gmail header checkbox renderer
        table.getColumnModel().getColumn(0).setHeaderRenderer(new TableCellRenderer() {
            private final JCheckBox chk = new JCheckBox();
            {
                chk.setHorizontalAlignment(SwingConstants.CENTER);
                chk.setOpaque(true);
            }
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                chk.setBackground(AppColors.TABLE_HEADER_BG);
                int rowCount = tableModel.getRowCount();
                int checkedCount = 0;
                for (int i = 0; i < rowCount; i++) {
                    if (Boolean.TRUE.equals(tableModel.getValueAt(i, 0))) checkedCount++;
                }
                chk.setSelected(rowCount > 0 && checkedCount == rowCount);
                return chk;
            }
        });

        // ★ TableModelListener → delegate checkbox changes to presenter
        tableModel.addTableModelListener(e -> {
            if (suppressModelListener) return;
            if (e.getColumn() == 0 && e.getType() == TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                if (row >= 0 && row < tableModel.getRowCount()) {
                    boolean checked = Boolean.TRUE.equals(tableModel.getValueAt(row, 0));
                    presenter.onCheckboxChanged(row, checked);
                }
            }
        });

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(0).setMaxWidth(35);
        table.getColumnModel().getColumn(1).setPreferredWidth(55);
        table.getColumnModel().getColumn(1).setMaxWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(250);
        table.getColumnModel().getColumn(3).setPreferredWidth(50);
        table.getColumnModel().getColumn(3).setMaxWidth(70);
        table.getColumnModel().getColumn(4).setPreferredWidth(120);
        table.getColumnModel().getColumn(5).setPreferredWidth(80);
        table.getColumnModel().getColumn(5).setMaxWidth(100);
        table.getColumnModel().getColumn(6).setPreferredWidth(90);
        table.getColumnModel().getColumn(6).setMaxWidth(110);

        table.setDefaultRenderer(Object.class, createCellRenderer());
        table.setDefaultRenderer(Integer.class, createCellRenderer());
        table.setDefaultRenderer(BigDecimal.class, createCellRenderer());

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);

        rightWrapper.add(scrollPane, BorderLayout.CENTER);
        rightWrapper.add(createPaginationBar(), BorderLayout.SOUTH);

        splitPane.setRightComponent(rightWrapper);
        add(splitPane, BorderLayout.CENTER);

        // === EVENTS → DELEGATE TO PRESENTER ===
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) presenter.onRowSelected();
        });

        // ★ Right-click context menu → Xem chi tiet lo hang
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem menuViewDetail = new JMenuItem("Xem chi ti\u1ebft l\u00f4 h\u00e0ng");
        menuViewDetail.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        menuViewDetail.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            int maSP = (int) tableModel.getValueAt(row, 1);
            Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
            new presentation.dialog.ProductDetailDialog(parentFrame, maSP).setVisible(true);
        });
        contextMenu.add(menuViewDetail);

        // ★ Menu: Xem lịch sử bán hàng
        JMenuItem menuSalesHistory = new JMenuItem("Xem lịch sử bán hàng");
        menuSalesHistory.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        menuSalesHistory.setForeground(new Color(0x17, 0xA2, 0xB8));
        menuSalesHistory.addActionListener(e -> showSalesHistory());
        contextMenu.addSeparator();
        contextMenu.add(menuSalesHistory);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) { showContextMenu(e); }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) { showContextMenu(e); }
            private void showContextMenu(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        table.setRowSelectionInterval(row, row);
                        contextMenu.show(table, e.getX(), e.getY());
                    }
                }
            }
        });

        btnAdd.addActionListener(e -> presenter.doAdd());
        btnUpdate.addActionListener(e -> presenter.doUpdate());
        btnToggle.addActionListener(e -> presenter.doToggle());
        btnClear.addActionListener(e -> presenter.doClearForm());
        btnBulkStop.addActionListener(e -> presenter.doBulkAction(false));
        btnBulkRestore.addActionListener(e -> presenter.doBulkAction(true));

        // === PERMISSIONS (pure UI) ===
        if (!Session.isAdmin()) {
            // NV: ẩn toàn bộ form bên trái, chỉ hiện table
            splitPane.setLeftComponent(null);
            splitPane.setDividerSize(0);
            splitPane.setDividerLocation(0);
        } else {
            updateButtonState(false);
        }
    }

    // ================================================================
    //  IProductView IMPLEMENTATION — Presenter gọi các hàm này
    // ================================================================

    @Override
    public String getSearchKeyword() { return txtSearch.getText().trim(); }

    @Override
    public String getStatusFilter() {
        if (cboFilter == null) return "Đang bán";
        return (String) cboFilter.getSelectedItem();
    }

    @Override
    public String getTenSP() { return txtTenSP.getText(); }

    @Override
    public String getDonViTinh() { return txtDonViTinh.getText(); }

    @Override
    public String getGiaBanText() { return txtGiaBan.getText(); }

    @Override
    public void displayProducts(List<Product> products, Set<Integer> checkedIds) {
        suppressModelListener = true;
        tableModel.setRowCount(0);
        for (Product p : products) {
            tableModel.addRow(new Object[]{
                    checkedIds.contains(p.getMaSP()),
                    p.getMaSP(),
                    p.getTenSP(),
                    p.getDonViTinh(),
                    p.getGiaBan(),
                    p.getTongTonKho(),
                    p.isTrangThai() ? "Đang bán" : "Ngừng bán"
            });
        }
        suppressModelListener = false;
    }

    @Override
    public void displayFormData(int maSP, String tenSP, String donViTinh,
                                String giaBan, boolean dangBan) {
        txtMaSP.setText(String.valueOf(maSP));
        txtTenSP.setText(tenSP);
        txtDonViTinh.setText(donViTinh);
        txtGiaBan.setText(giaBan);
        setToggleButton(dangBan);
    }

    @Override
    public void clearForm() {
        txtMaSP.setText("");
        txtTenSP.setText("");
        txtDonViTinh.setText("");
        txtGiaBan.setText("");
        table.clearSelection();
    }

    @Override
    public void updatePaginationUI(int currentPage, int totalPages) {
        btnFirst.setEnabled(currentPage > 1);
        btnPrev.setEnabled(currentPage > 1);
        btnNext.setEnabled(currentPage < totalPages);
        btnLast.setEnabled(currentPage < totalPages);

        pageNumbersPanel.removeAll();
        int start = Math.max(1, currentPage - 2);
        int end = Math.min(totalPages, currentPage + 2);

        for (int i = start; i <= end; i++) {
            JButton btn = createPageNumButton(String.valueOf(i));
            if (i == currentPage) {
                btn.setBackground(AppColors.PRIMARY);
                btn.setForeground(Color.WHITE);
                btn.setBorder(BorderFactory.createLineBorder(AppColors.PRIMARY, 1));
            }
            final int page = i;
            btn.addActionListener(e -> presenter.goToPage(page));
            pageNumbersPanel.add(btn);
        }
        pageNumbersPanel.revalidate();
        pageNumbersPanel.repaint();

        lblPageInfo.setText("Trang " + currentPage + " / " + totalPages);
    }

    @Override
    public void updateButtonState(boolean isEditMode) {
        if (!Session.isAdmin()) return;
        
        boolean hasBulkSelection = presenter != null && !presenter.getGlobalSelectedIds().isEmpty();
        
        if (hasBulkSelection) {
            btnAdd.setEnabled(false);
            btnUpdate.setEnabled(false);
            btnToggle.setEnabled(false);
        } else {
            btnAdd.setEnabled(!isEditMode);
            btnUpdate.setEnabled(isEditMode);
            btnToggle.setEnabled(isEditMode);
        }
    }

    @Override
    public void setBulkPanelVisible(boolean visible) {
        bulkPanel.setVisible(visible);
        bulkPanel.getParent().revalidate();
    }

    @Override
    public void setToggleButton(boolean dangBan) {
        btnToggle.setText(dangBan ? "Ngừng Bán" : "Khôi Phục");
        btnToggle.setBackground(dangBan ? AppColors.DANGER : AppColors.SUCCESS);
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
    public boolean confirm(String message, String title, boolean isWarning) {
        int result = JOptionPane.showConfirmDialog(this, message, title,
                JOptionPane.YES_NO_OPTION,
                isWarning ? JOptionPane.WARNING_MESSAGE : JOptionPane.QUESTION_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }

    @Override
    public void setLoading(boolean loading) {
        setCursor(loading
                ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                : Cursor.getDefaultCursor());
        table.getTableHeader().setEnabled(!loading);
    }

    @Override
    public int getSelectedTableRow() { return table.getSelectedRow(); }

    @Override
    public int getRowMaSP(int row) { return (int) tableModel.getValueAt(row, 1); }

    @Override
    public void setAllCheckboxes(boolean checked) {
        suppressModelListener = true;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt(checked, i, 0);
        }
        suppressModelListener = false;
    }

    @Override
    public void refreshHeaderCheckbox() { table.getTableHeader().repaint(); }

    @Override
    public int getTableRowCount() { return tableModel.getRowCount(); }

    @Override
    public boolean isRowChecked(int row) {
        return Boolean.TRUE.equals(tableModel.getValueAt(row, 0));
    }

    @Override
    public void focusTenSP() { txtTenSP.requestFocusInWindow(); }

    @Override
    public void clearTableSelection() { table.clearSelection(); }

    @Override
    public void adjustRowHeight(int rowCount, int pageSize) {
        SwingUtilities.invokeLater(() -> {
            if (rowCount >= pageSize && table.getParent() != null) {
                int viewportH = table.getParent().getHeight();
                int dynamicH = viewportH / rowCount;
                table.setRowHeight(Math.max(32, dynamicH));
            } else {
                table.setRowHeight(32);
            }
        });
    }

    // ================================================================
    //  UI HELPER METHODS (Pure Swing — không có logic)
    // ================================================================

    private JPanel createPaginationBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 8));
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppColors.NEUTRAL_DARK));

        btnFirst = createPageNavButton("|< Đầu");
        btnPrev  = createPageNavButton("< Trước");
        pageNumbersPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        pageNumbersPanel.setOpaque(false);
        btnNext  = createPageNavButton("Sau >");
        btnLast  = createPageNavButton("Cuối >|");

        lblPageInfo = new JLabel();
        lblPageInfo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPageInfo.setForeground(AppColors.TEXT_PRIMARY);

        btnFirst.addActionListener(e -> presenter.goFirstPage());
        btnPrev.addActionListener(e -> presenter.goPrevPage());
        btnNext.addActionListener(e -> presenter.goNextPage());
        btnLast.addActionListener(e -> presenter.goLastPage());

        bar.add(btnFirst);
        bar.add(btnPrev);
        bar.add(Box.createHorizontalStrut(8));
        bar.add(pageNumbersPanel);
        bar.add(Box.createHorizontalStrut(8));
        bar.add(btnNext);
        bar.add(btnLast);
        bar.add(Box.createHorizontalStrut(16));
        bar.add(lblPageInfo);

        return bar;
    }

    private JButton createPageNavButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setPreferredSize(new Dimension(70, 28));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBackground(Color.WHITE);
        btn.setForeground(AppColors.PRIMARY);
        btn.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARKER, 1));
        return btn;
    }

    private JButton createPageNumButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setPreferredSize(new Dimension(32, 28));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBackground(Color.WHITE);
        btn.setForeground(AppColors.PRIMARY);
        btn.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARKER, 1));
        return btn;
    }

    private void addFormRow(JPanel panel, String labelText, JTextField field) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(AppColors.TEXT_SECONDARY);
        label.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));

        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        field.setPreferredSize(new Dimension(0, 34));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.NEUTRAL_DARKER, 1),
                new EmptyBorder(0, 10, 0, 10)
        ));
        field.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(field);
        panel.add(Box.createRigidArea(new Dimension(0, 12)));
    }

    private void addFormRowWithSuffix(JPanel panel, String labelText, JTextField field, String suffix) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(AppColors.TEXT_SECONDARY);
        label.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));

        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel lblSuffix = new JLabel(suffix + "  ");
        lblSuffix.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSuffix.setForeground(AppColors.TEXT_SECONDARY);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        wrapper.setPreferredSize(new Dimension(0, 34));
        wrapper.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARKER, 1));
        wrapper.setBackground(field.isEditable() ? Color.WHITE : AppColors.NEUTRAL);
        wrapper.setAlignmentX(LEFT_ALIGNMENT);

        field.setBorder(new EmptyBorder(0, 10, 0, 4));
        field.setBackground(field.isEditable() ? Color.WHITE : AppColors.NEUTRAL);

        wrapper.add(field, BorderLayout.CENTER);
        wrapper.add(lblSuffix, BorderLayout.EAST);

        panel.add(wrapper);
        panel.add(Box.createRigidArea(new Dimension(0, 12)));
    }

    private void addNumericFilter(JTextField field) {
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c) && c != KeyEvent.VK_BACK_SPACE && c != KeyEvent.VK_DELETE && c != ',') {
                    e.consume();
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_RIGHT) return;
                String text = field.getText().replace(",", "").trim();
                if (!text.isEmpty()) {
                    try {
                        long val = Long.parseLong(text);
                        String formatted = String.format("%,d", val);
                        if (!field.getText().equals(formatted)) {
                            field.setText(formatted);
                        }
                    } catch (NumberFormatException ex) {}
                }
            }
        });
    }

    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 38));

        Color hoverColor = bg.darker();
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(hoverColor);
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(bg);
            }
        });
        return btn;
    }

    private DefaultTableCellRenderer createCellRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                String display;
                if (col == 4 && val instanceof BigDecimal) {
                    display = moneyFormat.format(val);
                } else if (col == 4 && val == null) {
                    display = "-";
                } else {
                    display = val != null ? val.toString() : "";
                }

                Component c = super.getTableCellRendererComponent(t, display, sel, focus, row, col);

                if (!sel) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);
                }

                if (col == 6 && val != null) {
                    c.setForeground(val.toString().contains("Ngừng") ? AppColors.DANGER : AppColors.SUCCESS);
                } else if (col == 5 && val instanceof Integer && (int) val == 0) {
                    c.setForeground(AppColors.DANGER);
                } else if (!sel) {
                    c.setForeground(AppColors.TEXT_PRIMARY);
                }

                if (col == 1 || col == 3 || col == 5 || col == 6) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else if (col == 4) {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }
                return c;
            }
        };
    }

    // ================================================================
    //  ★ Xem lịch sử bán hàng của sản phẩm
    // ================================================================

    private void showSalesHistory() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int maSP = (int) tableModel.getValueAt(row, 1);
        String tenSP = tableModel.getValueAt(row, 2).toString();

        String sql =
            "SELECT hd.MaHD, hd.NgayBan, l.SoLo, ct.SoLuong AS SLBan, " +
            "(ct.SoLuong * ct.DonGia) AS ThanhTien, " +
            "ISNULL(kh.TenKH, N'Khách vãng lai') AS TenKH, " +
            "ISNULL(kh.SoDT, N'---') AS SDT " +
            "FROM ChiTietHoaDon ct " +
            "JOIN HoaDon hd ON ct.MaHD = hd.MaHD " +
            "LEFT JOIN KhachHang kh ON hd.MaKH = kh.MaKH " +
            "JOIN LoHang l ON ct.MaLo = l.MaLo " +
            "WHERE ct.MaSP = ? " +
            "ORDER BY hd.NgayBan DESC";

        String[] cols = {"Mã HĐ", "Ngày giờ bán", "Số Lô", "SL bán", "Thành tiền", "Khách hàng", "SĐT"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        try (Connection conn = infrastructure.database.DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maSP);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ngay = rs.getTimestamp("NgayBan");
                    String ngayStr = ngay != null
                            ? ngay.toLocalDateTime().format(
                                java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"))
                            : "---";
                    java.math.BigDecimal thanhTien = rs.getBigDecimal("ThanhTien");
                    String thanhTienStr = thanhTien != null
                            ? String.format("%,.0f VNĐ", thanhTien) : "---";
                    model.addRow(new Object[]{
                        rs.getInt("MaHD"),
                        ngayStr,
                        rs.getNString("SoLo"),
                        rs.getInt("SLBan"),
                        thanhTienStr,
                        rs.getNString("TenKH"),
                        rs.getString("SDT")
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(this,
                "Lỗi truy vấn lịch sử bán: " + e.getMessage(),
                "SQL Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }

        // === Dialog ===
        JDialog dlg = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Lịch sử bán hàng — " + tenSP,
                true);
        dlg.setSize(950, 500);
        dlg.setLocationRelativeTo(this);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.WHITE);

        // === Header ===
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(new Color(0x17, 0xA2, 0xB8));
        hdr.setBorder(new javax.swing.border.EmptyBorder(14, 20, 14, 20));
        JLabel lblH = new JLabel("Lịch sử bán — " + tenSP);
        lblH.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblH.setForeground(Color.WHITE);
        hdr.add(lblH, BorderLayout.WEST);
        JLabel lblCount = new JLabel(model.getRowCount() + " hóa đơn");
        lblCount.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblCount.setForeground(new Color(0xE0, 0xF7, 0xFA));
        hdr.add(lblCount, BorderLayout.EAST);
        content.add(hdr, BorderLayout.NORTH);

        if (model.getRowCount() == 0) {
            JLabel emptyLbl = new JLabel("Sản phẩm này chưa bán cho khách hàng nào.", SwingConstants.CENTER);
            emptyLbl.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            emptyLbl.setForeground(AppColors.TEXT_SECONDARY);
            content.add(emptyLbl, BorderLayout.CENTER);
        } else {
            // === Top wrapper: header + search ===
            JPanel topWrapper = new JPanel(new BorderLayout());
            topWrapper.setBackground(Color.WHITE);

            topWrapper.add(hdr, BorderLayout.NORTH);
            content.remove(hdr); // remove from content NORTH, put into topWrapper

            // === Search bar for batch number ===
            JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
            searchPanel.setBackground(Color.WHITE);
            searchPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.NEUTRAL_DARK));

            JLabel lblSearchLo = new JLabel("Lọc theo mã lô:");
            lblSearchLo.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblSearchLo.setForeground(AppColors.TEXT_PRIMARY);
            searchPanel.add(lblSearchLo);

            final String PLACEHOLDER = "Nhập mã lô để lọc...";
            JTextField txtSearchLo = new JTextField(22);
            txtSearchLo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            txtSearchLo.setForeground(AppColors.TEXT_SECONDARY);
            txtSearchLo.setText(PLACEHOLDER);
            txtSearchLo.setBorder(BorderFactory.createCompoundBorder(
                    new javax.swing.border.LineBorder(AppColors.NEUTRAL_DARK, 1),
                    new javax.swing.border.EmptyBorder(6, 10, 6, 10)));

            // Placeholder behavior
            txtSearchLo.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    if (txtSearchLo.getText().equals(PLACEHOLDER)) {
                        txtSearchLo.setText("");
                        txtSearchLo.setForeground(AppColors.TEXT_PRIMARY);
                    }
                }
                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    if (txtSearchLo.getText().trim().isEmpty()) {
                        txtSearchLo.setText(PLACEHOLDER);
                        txtSearchLo.setForeground(AppColors.TEXT_SECONDARY);
                    }
                }
            });
            searchPanel.add(txtSearchLo);

            topWrapper.add(searchPanel, BorderLayout.SOUTH);
            content.add(topWrapper, BorderLayout.NORTH);

            // === Table ===
            JTable salesTable = new JTable(model);
            salesTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            salesTable.setRowHeight(30);
            salesTable.setShowGrid(false);
            salesTable.setFillsViewportHeight(true);
            salesTable.setIntercellSpacing(new Dimension(0, 0));
            salesTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
            salesTable.getTableHeader().setBackground(AppColors.TABLE_HEADER_BG);
            salesTable.getTableHeader().setForeground(AppColors.TABLE_HEADER_FG);
            salesTable.getTableHeader().setPreferredSize(new Dimension(0, 34));

            // Column widths
            salesTable.getColumnModel().getColumn(0).setPreferredWidth(55);
            salesTable.getColumnModel().getColumn(0).setMaxWidth(70);
            salesTable.getColumnModel().getColumn(1).setPreferredWidth(130);
            salesTable.getColumnModel().getColumn(2).setPreferredWidth(120);
            salesTable.getColumnModel().getColumn(3).setPreferredWidth(55);
            salesTable.getColumnModel().getColumn(3).setMaxWidth(65);
            salesTable.getColumnModel().getColumn(4).setPreferredWidth(110);
            salesTable.getColumnModel().getColumn(5).setPreferredWidth(150);
            salesTable.getColumnModel().getColumn(6).setPreferredWidth(100);

            // Renderer
            salesTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable t, Object v,
                        boolean sel, boolean foc, int r, int c) {
                    Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                    if (!sel) comp.setBackground(r % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);

                    if (c == 0 || c == 3) {
                        setHorizontalAlignment(SwingConstants.CENTER);
                        if (!sel) comp.setForeground(AppColors.TEXT_PRIMARY);
                    } else if (c == 4) {
                        setHorizontalAlignment(SwingConstants.RIGHT);
                        comp.setForeground(AppColors.SUCCESS);
                        setFont(new Font("Segoe UI", Font.BOLD, 12));
                    } else if (c == 2) {
                        setHorizontalAlignment(SwingConstants.LEFT);
                        comp.setForeground(AppColors.PRIMARY);
                        setFont(new Font("Segoe UI", Font.BOLD, 12));
                    } else if (c == 6) {
                        setHorizontalAlignment(SwingConstants.LEFT);
                        comp.setForeground(new Color(0x17, 0xA2, 0xB8));
                        setFont(new Font("Segoe UI", Font.BOLD, 12));
                    } else {
                        setHorizontalAlignment(SwingConstants.LEFT);
                        if (!sel) comp.setForeground(AppColors.TEXT_PRIMARY);
                    }
                    return comp;
                }
            });

            // RowSorter for filtering
            javax.swing.table.TableRowSorter<DefaultTableModel> sorter =
                    new javax.swing.table.TableRowSorter<>(model);
            salesTable.setRowSorter(sorter);

            // Search listener — filter by column 2 (So Lo)
            txtSearchLo.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                private void doFilter() {
                    String text = txtSearchLo.getText().trim();
                    if (text.isEmpty() || text.equals(PLACEHOLDER)) {
                        sorter.setRowFilter(null);
                        lblCount.setText(model.getRowCount() + " hóa đơn");
                    } else {
                        sorter.setRowFilter(javax.swing.RowFilter.regexFilter(
                                "(?i)" + java.util.regex.Pattern.quote(text), 2));
                        lblCount.setText(salesTable.getRowCount() + " / " + model.getRowCount() + " hóa đơn");
                    }
                }
                @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { doFilter(); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { doFilter(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { doFilter(); }
            });

            JScrollPane sp = new JScrollPane(salesTable);
            sp.setBorder(BorderFactory.createEmptyBorder());
            sp.getViewport().setBackground(Color.WHITE);
            content.add(sp, BorderLayout.CENTER);
        }

        dlg.setContentPane(content);
        dlg.setVisible(true);
    }
}
