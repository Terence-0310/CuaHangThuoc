package presentation.panel;

import common.AppColors;
import common.Session;
import domain.entity.Customer;
import infrastructure.database.DatabaseHelper;
import infrastructure.repository.CustomerRepositoryImpl;
import service.ICustomerService;
import service.impl.CustomerServiceImpl;

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
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel Quản Lý Khách Hàng — Phân trang, CRUD, Search realtime, Lịch sử mua
 */
public class CustomerPanel extends JPanel {

    private static final int PAGE_SIZE = 15;
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,###");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // === Service ===
    private final ICustomerService customerService;

    // === State ===
    private int currentPage = 1;
    private int totalPages = 1;
    private int selectedMaKH = -1;
    private int sortColumnIndex = 1;
    private boolean sortAsc = true;

    // STT(0), MaKH(1), TenKH(2), SoDT(3), GioiTinh(4), TongMua(5), NgayTao(6)
    private static final String[] SORT_SQL = {
        null, "MaKH", "TenKH", "SoDT", "GioiTinh", "TongMua", "NgayTao"
    };

    // === Form Fields ===
    private JTextField txtMaKH, txtTenKH, txtSoDT, txtTongMua;
    private JComboBox<String> cboGioiTinh;

    // === Buttons ===
    private JButton btnAdd, btnUpdate, btnDelete, btnClear;

    // === Table ===
    private DefaultTableModel tableModel;
    private JTable table;

    // === Filter ===
    private JTextField txtSearch;

    // === Pagination ===
    private JButton btnFirst, btnPrev, btnNext, btnLast;
    private JLabel lblPageInfo;
    private JPanel pageNumbersPanel;

    public CustomerPanel() {
        customerService = new CustomerServiceImpl(new CustomerRepositoryImpl());
        setLayout(new BorderLayout());
        setBackground(AppColors.NEUTRAL);
        initComponents();

        SwingUtilities.invokeLater(() -> loadPage(1));

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) { loadPage(currentPage); }
        });
    }

    // ================================================================
    //  LAYOUT
    // ================================================================

    private void initComponents() {
        // === TOP BAR ===
        JPanel topBar = new JPanel(new BorderLayout(12, 0));
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel lblTitle = new JLabel("Quản Lý Khách Hàng");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(AppColors.TEXT_PRIMARY);
        topBar.add(lblTitle, BorderLayout.WEST);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filterPanel.setOpaque(false);

        txtSearch = new JTextField(20);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearch.setPreferredSize(new Dimension(220, 34));
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AppColors.NEUTRAL_DARK, 1),
                new EmptyBorder(4, 10, 4, 10)));
        txtSearch.setToolTipText("Tìm theo tên hoặc SĐT...");
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onSearch(); }
            @Override public void removeUpdate(DocumentEvent e) { onSearch(); }
            @Override public void changedUpdate(DocumentEvent e) { onSearch(); }
        });
        filterPanel.add(new JLabel("Tìm kiếm:"));
        filterPanel.add(txtSearch);
        topBar.add(filterPanel, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // === TABBED PANE ===
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.setBackground(Color.WHITE);

        // Tab 1: Khách đã đăng ký (main)
        JPanel mainTab = new JPanel(new BorderLayout());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(340);
        splitPane.setDividerSize(1);
        splitPane.setBorder(null);

        splitPane.setLeftComponent(createFormPanel());
        splitPane.setRightComponent(createTablePanel());

        if (!Session.isAdmin()) {
            splitPane.setLeftComponent(null);
            splitPane.setDividerSize(0);
            splitPane.setDividerLocation(0);
        }

        mainTab.add(splitPane, BorderLayout.CENTER);
        mainTab.add(createPaginationPanel(), BorderLayout.SOUTH);

        tabbedPane.addTab("Khách đã đăng ký", mainTab);

        // Tab 2: Khách vãng lai (invoices without customer)
        tabbedPane.addTab("Khách vãng lai", createWalkInTab());

        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 1) {
                loadWalkInInvoices();
            }
        });

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ================================================================
    //  FORM (Left Panel)
    // ================================================================

    private JPanel createFormPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);

        JLabel lblForm = new JLabel("Thông Tin Khách Hàng");
        lblForm.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblForm.setForeground(AppColors.PRIMARY);
        lblForm.setAlignmentX(LEFT_ALIGNMENT);
        formPanel.add(lblForm);
        formPanel.add(Box.createRigidArea(new Dimension(0, 16)));

        // Mã KH (readonly)
        txtMaKH = new JTextField();
        txtMaKH.setEditable(false);
        txtMaKH.setBackground(AppColors.NEUTRAL);
        addFormRow(formPanel, "Mã KH:", txtMaKH);

        // Tên KH
        txtTenKH = new JTextField();
        addFormRow(formPanel, "Tên khách hàng: *", txtTenKH);

        // SĐT
        txtSoDT = new JTextField();
        txtSoDT.setToolTipText("Nhập đúng 10 số");
        ((javax.swing.text.AbstractDocument) txtSoDT.getDocument()).setDocumentFilter(
            new javax.swing.text.DocumentFilter() {
                @Override
                public void insertString(FilterBypass fb, int offset, String string,
                        javax.swing.text.AttributeSet attr) throws javax.swing.text.BadLocationException {
                    if (string != null) super.insertString(fb, offset, string.replaceAll("[^0-9]", ""), attr);
                }
                @Override
                public void replace(FilterBypass fb, int offset, int length, String text,
                        javax.swing.text.AttributeSet attrs) throws javax.swing.text.BadLocationException {
                    if (text != null) super.replace(fb, offset, length, text.replaceAll("[^0-9]", ""), attrs);
                }
            });
        addFormRow(formPanel, "Số điện thoại: *", txtSoDT);

        // Giới tính
        cboGioiTinh = new JComboBox<>(new String[]{"Nam", "Nữ", "Khác"});
        cboGioiTinh.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addFormRow(formPanel, "Giới tính:", cboGioiTinh);

        // Tổng mua (readonly)
        txtTongMua = new JTextField("---");
        txtTongMua.setEditable(false);
        txtTongMua.setBackground(AppColors.NEUTRAL);
        txtTongMua.setFont(new Font("Segoe UI", Font.BOLD, 13));
        txtTongMua.setForeground(AppColors.SUCCESS);
        addFormRow(formPanel, "Tổng chi tiêu:", txtTongMua);

        formPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // === Buttons ===
        JPanel btnPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        btnPanel.setOpaque(false);
        btnPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));
        btnPanel.setAlignmentX(LEFT_ALIGNMENT);

        btnAdd = createActionButton("Thêm Mới", AppColors.PRIMARY);
        btnUpdate = createActionButton("Cập Nhật", AppColors.PRIMARY);
        btnClear = createActionButton("Làm Mới", AppColors.SECONDARY);
        btnDelete = createActionButton("Xóa", AppColors.DANGER);

        btnAdd.addActionListener(e -> doInsert());
        btnUpdate.addActionListener(e -> doUpdate());
        btnClear.addActionListener(e -> clearForm());
        btnDelete.addActionListener(e -> doDelete());

        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);

        // Chỉ Admin mới thấy nút Xóa
        btnDelete.setVisible(Session.isAdmin());

        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnClear);
        btnPanel.add(btnDelete);
        formPanel.add(btnPanel);

        formPanel.add(Box.createVerticalGlue());

        JScrollPane formScroll = new JScrollPane(formPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        wrapper.add(formScroll, BorderLayout.CENTER);
        return wrapper;
    }

    // ================================================================
    //  TABLE (Right Panel)
    // ================================================================

    private JPanel createTablePanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(new EmptyBorder(16, 12, 16, 24));

        String[] columns = {"STT", "Mã KH", "Tên Khách Hàng", "SĐT", "Giới Tính", "Tổng Mua", "Ngày Tạo"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader header = table.getTableHeader();
        header.setBackground(AppColors.TABLE_HEADER_BG);
        header.setForeground(AppColors.TABLE_HEADER_FG);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setPreferredSize(new Dimension(0, 36));
        header.setReorderingAllowed(false);
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(55);
        table.getColumnModel().getColumn(1).setMaxWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setPreferredWidth(75);
        table.getColumnModel().getColumn(4).setMaxWidth(90);
        table.getColumnModel().getColumn(5).setPreferredWidth(130);
        table.getColumnModel().getColumn(6).setPreferredWidth(130);

        // Cell renderer
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) c.setBackground(row % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);

                if (col == 0 || col == 1 || col == 4) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else if (col == 5) {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }

                // Tổng Mua → xanh lá + bold
                if (col == 5 && val != null && !val.toString().equals("---")) {
                    c.setForeground(AppColors.SUCCESS);
                    setFont(new Font("Segoe UI", Font.BOLD, 13));
                } else if (!sel) {
                    c.setForeground(AppColors.TEXT_PRIMARY);
                }
                return c;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);

        // Sort on header click
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                if (col <= 0 || col >= SORT_SQL.length || SORT_SQL[col] == null) return;
                if (sortColumnIndex == col) {
                    sortAsc = !sortAsc;
                } else {
                    sortColumnIndex = col;
                    sortAsc = true;
                }
                currentPage = 1;
                loadPage(currentPage);
            }
        });

        // Click row → fill form
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.getSelectedRow();
                if (row < 0) return;
                if (e.getClickCount() == 1) {
                    fillFormFromRow(row);
                } else if (e.getClickCount() == 2) {
                    showPurchaseHistory();
                }
            }
        });

        // Right-click menu
        JPopupMenu popup = new JPopupMenu();
        JMenuItem miHistory = new JMenuItem("Xem lịch sử mua hàng");
        miHistory.addActionListener(e -> showPurchaseHistory());
        popup.add(miHistory);

        if (Session.isAdmin()) {
            popup.addSeparator();
            JMenuItem miDelete = new JMenuItem("Xóa khách hàng");
            miDelete.setForeground(AppColors.DANGER);
            miDelete.addActionListener(e -> doDelete());
            popup.add(miDelete);
        }

        table.setComponentPopupMenu(popup);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int r = table.rowAtPoint(e.getPoint());
                    if (r >= 0) {
                        table.setRowSelectionInterval(r, r);
                        fillFormFromRow(r);
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1));
        scroll.getViewport().setBackground(Color.WHITE);
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    // ================================================================
    //  PAGINATION
    // ================================================================

    private JPanel createPaginationPanel() {
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

        btnFirst.addActionListener(e -> loadPage(1));
        btnPrev.addActionListener(e -> { if (currentPage > 1) loadPage(currentPage - 1); });
        btnNext.addActionListener(e -> { if (currentPage < totalPages) loadPage(currentPage + 1); });
        btnLast.addActionListener(e -> loadPage(totalPages));

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
        btn.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1));
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
        btn.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1));
        return btn;
    }

    // ================================================================
    //  DATA LOADING
    // ================================================================

    private void loadPage(int page) {
        String keyword = txtSearch.getText().trim();
        String sortCol = sortColumnIndex > 0 && sortColumnIndex < SORT_SQL.length ? SORT_SQL[sortColumnIndex] : null;
        String sortDir = sortAsc ? "ASC" : "DESC";

        int total = customerService.countFiltered(keyword.isEmpty() ? null : keyword);
        totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        if (page > totalPages) page = totalPages;
        currentPage = page;

        int offset = (currentPage - 1) * PAGE_SIZE;
        List<Customer> customers = customerService.getPagedList(offset, PAGE_SIZE,
                keyword.isEmpty() ? null : keyword, sortCol, sortDir);

        tableModel.setRowCount(0);
        int stt = offset + 1;
        for (Customer c : customers) {
            String tongMuaStr = formatTongMua(c.getTongMua());
            String ngayTaoStr = c.getNgayTao() != null ? c.getNgayTao().format(DT_FMT) : "---";
            tableModel.addRow(new Object[]{
                stt++,
                c.getMaKH(),
                c.getTenKH() != null ? c.getTenKH() : "---",
                c.getSoDT(),
                c.getGioiTinh() != null ? c.getGioiTinh() : "---",
                tongMuaStr,
                ngayTaoStr
            });
        }

        updatePaginationUI();
    }

    /** Tổng mua: không hiện 0, chỉ hiện khi > 0 */
    private String formatTongMua(BigDecimal tongMua) {
        if (tongMua == null || tongMua.signum() <= 0) return "---";
        return MONEY_FMT.format(tongMua) + " VNĐ";
    }

    private void updatePaginationUI() {
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
            int pg = i;
            btn.addActionListener(e -> loadPage(pg));
            pageNumbersPanel.add(btn);
        }
        pageNumbersPanel.revalidate();
        pageNumbersPanel.repaint();

        lblPageInfo.setText("Trang " + currentPage + " / " + totalPages);
    }

    // ================================================================
    //  SEARCH
    // ================================================================

    private Timer searchTimer;
    private void onSearch() {
        if (searchTimer != null) searchTimer.stop();
        searchTimer = new Timer(300, e -> {
            currentPage = 1;
            loadPage(1);
        });
        searchTimer.setRepeats(false);
        searchTimer.start();
    }

    // ================================================================
    //  WALK-IN CUSTOMER TAB (Khách vãng lai)
    // ================================================================

    private DefaultTableModel walkInModel;
    private JTable walkInTable;
    private JLabel lblWalkInInfo;

    private JPanel createWalkInTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        // Header
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(Color.WHITE);
        hdr.setBorder(new EmptyBorder(12, 20, 8, 20));

        JLabel lblTitle2 = new JLabel("Danh sách hóa đơn khách vãng lai (không có SĐT/tên)");
        lblTitle2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle2.setForeground(AppColors.TEXT_SECONDARY);
        hdr.add(lblTitle2, BorderLayout.WEST);

        lblWalkInInfo = new JLabel("");
        lblWalkInInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblWalkInInfo.setForeground(AppColors.TEXT_SECONDARY);
        hdr.add(lblWalkInInfo, BorderLayout.EAST);

        panel.add(hdr, BorderLayout.NORTH);

        // Table
        String[] cols = {"STT", "Mã HĐ", "Ngày bán", "Nhân viên", "Phương thức TT", "Tổng tiền", "Trạng thái"};
        walkInModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        walkInTable = new JTable(walkInModel);
        walkInTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        walkInTable.setRowHeight(34);
        walkInTable.setShowGrid(false);
        walkInTable.setFillsViewportHeight(true);
        walkInTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        walkInTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        walkInTable.getTableHeader().setBackground(AppColors.TABLE_HEADER_BG);
        walkInTable.getTableHeader().setForeground(AppColors.TABLE_HEADER_FG);
        walkInTable.getTableHeader().setPreferredSize(new Dimension(0, 36));

        walkInTable.getColumnModel().getColumn(0).setMaxWidth(50);
        walkInTable.getColumnModel().getColumn(1).setMaxWidth(70);

        walkInTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int r, int c) {
                Object dv = (c == 0) ? (r + 1) : v;
                Component comp = super.getTableCellRendererComponent(t, dv, sel, foc, r, c);
                if (!sel) {
                    comp.setBackground(r % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);
                    comp.setForeground(AppColors.TEXT_PRIMARY);
                }
                setFont(new Font("Segoe UI", Font.PLAIN, 12));
                if (c == 5) {
                    comp.setForeground(sel ? Color.WHITE : AppColors.SUCCESS);
                    setFont(new Font("Segoe UI", Font.BOLD, 12));
                    setHorizontalAlignment(SwingConstants.RIGHT);
                } else if (c == 6) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                    String val = dv != null ? dv.toString() : "";
                    if (val.contains("hủy") || val.contains("Hủy")) {
                        comp.setForeground(sel ? Color.WHITE : AppColors.DANGER);
                    } else {
                        comp.setForeground(sel ? Color.WHITE : AppColors.SUCCESS);
                    }
                    setFont(new Font("Segoe UI", Font.BOLD, 12));
                } else if (c == 0 || c == 1) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }
                return comp;
            }
        });

        JScrollPane sp = new JScrollPane(walkInTable);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Color.WHITE);
        panel.add(sp, BorderLayout.CENTER);

        return panel;
    }

    private void loadWalkInInvoices() {
        if (walkInModel == null) return;
        walkInModel.setRowCount(0);

        String sql = "SELECT hd.MaHD, hd.NgayBan, nd.HoTen AS TenNV, " +
                "hd.PhuongThucTT, hd.TongTien, hd.TrangThai " +
                "FROM HoaDon hd " +
                "JOIN NguoiDung nd ON hd.MaND = nd.MaND " +
                "WHERE hd.MaKH IS NULL " +
                "ORDER BY hd.NgayBan DESC";

        try (java.sql.Connection conn = DatabaseHelper.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {

            int stt = 0;
            java.math.BigDecimal totalRevenue = java.math.BigDecimal.ZERO;
            while (rs.next()) {
                stt++;
                java.sql.Timestamp ts = rs.getTimestamp("NgayBan");
                String ngay = ts != null ? ts.toLocalDateTime().format(DT_FMT) : "---";
                java.math.BigDecimal tien = rs.getBigDecimal("TongTien");
                String tienStr = tien != null ? String.format("%,.0f VNĐ", tien) : "0";
                if (tien != null) totalRevenue = totalRevenue.add(tien);

                String tt = rs.getNString("TrangThai");
                String displayTT = "Thành công";
                if (tt != null && tt.contains("huy")) displayTT = "Đã hủy";

                walkInModel.addRow(new Object[]{
                    stt, rs.getInt("MaHD"), ngay,
                    rs.getNString("TenNV"), rs.getNString("PhuongThucTT"),
                    tienStr, displayTT
                });
            }

            lblWalkInInfo.setText("Tổng: " + stt + " hóa đơn — " +
                    MONEY_FMT.format(totalRevenue) + " VNĐ");

        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    // ================================================================
    //  FORM LOGIC
    // ================================================================

    private void fillFormFromRow(int row) {
        int maKH = (int) tableModel.getValueAt(row, 1);
        selectedMaKH = maKH;

        Customer c = customerService.getById(maKH);
        if (c == null) return;

        txtMaKH.setText(String.valueOf(c.getMaKH()));
        txtTenKH.setText(c.getTenKH() != null ? c.getTenKH() : "");
        txtSoDT.setText(c.getSoDT() != null ? c.getSoDT() : "");
        cboGioiTinh.setSelectedItem(c.getGioiTinh() != null ? c.getGioiTinh() : "Khác");
        txtTongMua.setText(formatTongMua(c.getTongMua()));

        btnAdd.setEnabled(false);
        btnUpdate.setEnabled(true);
        btnDelete.setEnabled(Session.isAdmin());
    }

    private void clearForm() {
        selectedMaKH = -1;
        txtMaKH.setText("");
        txtTenKH.setText("");
        txtSoDT.setText("");
        cboGioiTinh.setSelectedIndex(0);
        txtTongMua.setText("---");
        table.clearSelection();
        btnAdd.setEnabled(true);
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);
        txtTenKH.requestFocusInWindow();
    }

    private boolean validateForm() {
        String ten = txtTenKH.getText().trim();
        if (ten.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tên khách hàng không được để trống!",
                    "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            txtTenKH.requestFocusInWindow();
            return false;
        }
        String sdt = txtSoDT.getText().trim();
        if (sdt.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Số điện thoại không được để trống!",
                    "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            txtSoDT.requestFocusInWindow();
            return false;
        }
        if (!sdt.matches("\\d{10}")) {
            JOptionPane.showMessageDialog(this, "Số điện thoại phải đúng 10 chữ số!",
                    "SĐT không hợp lệ", JOptionPane.WARNING_MESSAGE);
            txtSoDT.requestFocusInWindow();
            return false;
        }
        return true;
    }

    private Customer buildCustomerFromForm() {
        Customer c = new Customer();
        c.setMaKH(selectedMaKH);
        c.setTenKH(txtTenKH.getText().trim());
        c.setSoDT(txtSoDT.getText().trim());
        c.setGioiTinh((String) cboGioiTinh.getSelectedItem());
        return c;
    }

    private void doInsert() {
        if (!validateForm()) return;
        try {
            Customer c = buildCustomerFromForm();
            int id = customerService.insert(c);
            if (id > 0) {
                JOptionPane.showMessageDialog(this,
                        "Thêm khách hàng thành công!\nMã KH: " + id,
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadPage(1);
            }
        } catch (Exception ex) {
            String msg = ex.getMessage();
            if (msg != null && msg.contains("UQ_KhachHang_SoDT")) {
                JOptionPane.showMessageDialog(this,
                        "Số điện thoại này đã tồn tại trong hệ thống!",
                        "Trùng SĐT", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Lỗi: " + msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void doUpdate() {
        if (selectedMaKH < 0) return;
        if (!validateForm()) return;
        try {
            Customer c = buildCustomerFromForm();
            boolean ok = customerService.update(c);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Cập nhật thành công!",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                loadPage(currentPage);
                // Giữ selection
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    if ((int) tableModel.getValueAt(i, 1) == selectedMaKH) {
                        table.setRowSelectionInterval(i, i);
                        fillFormFromRow(i);
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            String msg = ex.getMessage();
            if (msg != null && msg.contains("UQ_KhachHang_SoDT")) {
                JOptionPane.showMessageDialog(this,
                        "Số điện thoại này đã được sử dụng bởi khách hàng khác!",
                        "Trùng SĐT", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Lỗi: " + msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void doDelete() {
        if (selectedMaKH < 0) return;
        int row = table.getSelectedRow();
        if (row < 0) return;
        String tenKH = tableModel.getValueAt(row, 2).toString();

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa khách hàng:\n\n  \"" + tenKH + "\"  (Mã: " + selectedMaKH + ")?\n\n" +
                "Khách đã có hóa đơn sẽ KHÔNG thể xóa.",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            boolean ok = customerService.delete(selectedMaKH);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Đã xóa khách hàng thành công!",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadPage(currentPage);
            }
        } catch (Exception ex) {
            String msg = ex.getMessage();
            if (msg != null && msg.contains("KHÁCH CÓ HOÁ ĐƠN")) {
                JOptionPane.showMessageDialog(this,
                        "Không thể xóa!\n\nKhách hàng này đã có hóa đơn trong hệ thống.\n" +
                        "Xóa sẽ vi phạm toàn vẹn dữ liệu.",
                        "Không thể xóa", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Lỗi: " + msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ================================================================
    //  PURCHASE HISTORY DIALOG
    // ================================================================

    private void showPurchaseHistory() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int maKH = (int) tableModel.getValueAt(row, 1);
        String tenKH = tableModel.getValueAt(row, 2).toString();
        String soDT = tableModel.getValueAt(row, 3).toString();

        String sql =
            "SELECT hd.MaHD, hd.NgayBan, hd.TongTien, nd.HoTen AS NhanVien, " +
            "(SELECT COUNT(*) FROM ChiTietHoaDon ct WHERE ct.MaHD = hd.MaHD) AS SoSP " +
            "FROM HoaDon hd " +
            "LEFT JOIN NguoiDung nd ON hd.MaND = nd.MaND " +
            "WHERE hd.MaKH = ? " +
            "ORDER BY hd.NgayBan DESC";

        String[] cols = {"Mã HĐ", "Ngày mua", "Tổng tiền", "Số SP", "Nhân viên bán"};
        DefaultTableModel histModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maKH);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("NgayBan");
                    String ngayStr = ts != null ? ts.toLocalDateTime().format(DT_FMT) : "---";
                    BigDecimal tong = rs.getBigDecimal("TongTien");
                    String tongStr = tong != null && tong.signum() > 0
                            ? MONEY_FMT.format(tong) + " VNĐ" : "---";
                    histModel.addRow(new Object[]{
                        "HĐ-" + rs.getInt("MaHD"),
                        ngayStr,
                        tongStr,
                        rs.getInt("SoSP"),
                        rs.getNString("NhanVien") != null ? rs.getNString("NhanVien") : "---"
                    });
                }
            }
        } catch (SQLException ignored) {}

        // === Build dialog ===
        JDialog dlg = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Lịch sử mua hàng", true);
        dlg.setSize(720, 460);
        dlg.setLocationRelativeTo(this);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.WHITE);

        // Header
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(new Color(0x17, 0xA2, 0xB8));
        hdr.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel lblName = new JLabel(tenKH + "  —  " + soDT);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblName.setForeground(Color.WHITE);
        hdr.add(lblName, BorderLayout.WEST);

        JLabel lblCount = new JLabel(histModel.getRowCount() + " hóa đơn");
        lblCount.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblCount.setForeground(new Color(0xE0, 0xF7, 0xFA));
        hdr.add(lblCount, BorderLayout.EAST);
        content.add(hdr, BorderLayout.NORTH);

        if (histModel.getRowCount() == 0) {
            JLabel emptyLbl = new JLabel("Khách hàng chưa mua hàng lần nào.", SwingConstants.CENTER);
            emptyLbl.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            emptyLbl.setForeground(AppColors.TEXT_SECONDARY);
            content.add(emptyLbl, BorderLayout.CENTER);
        } else {
            JTable histTable = new JTable(histModel);
            histTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            histTable.setRowHeight(30);
            histTable.setShowGrid(false);
            histTable.setFillsViewportHeight(true);
            histTable.setAutoCreateRowSorter(true);
            histTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
            histTable.getTableHeader().setBackground(AppColors.TABLE_HEADER_BG);
            histTable.getTableHeader().setForeground(AppColors.TABLE_HEADER_FG);

            histTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable t, Object v,
                        boolean sel, boolean foc, int r, int c) {
                    Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                    if (!sel) comp.setBackground(r % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);
                    if (c == 2) {
                        setHorizontalAlignment(SwingConstants.RIGHT);
                        comp.setForeground(AppColors.SUCCESS);
                        setFont(new Font("Segoe UI", Font.BOLD, 13));
                    } else if (c == 3) {
                        setHorizontalAlignment(SwingConstants.CENTER);
                        if (!sel) comp.setForeground(AppColors.TEXT_PRIMARY);
                    } else {
                        setHorizontalAlignment(SwingConstants.LEFT);
                        if (!sel) comp.setForeground(AppColors.TEXT_PRIMARY);
                    }
                    return comp;
                }
            });

            JScrollPane scroll = new JScrollPane(histTable);
            scroll.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1));
            content.add(scroll, BorderLayout.CENTER);
        }

        // Close button
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        bottom.setOpaque(false);
        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClose.setBackground(AppColors.PRIMARY);
        btnClose.setForeground(Color.WHITE);
        btnClose.setFocusPainted(false);
        btnClose.setBorderPainted(false);
        btnClose.setPreferredSize(new Dimension(90, 34));
        btnClose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dlg.dispose());
        bottom.add(btnClose);
        content.add(bottom, BorderLayout.SOUTH);

        dlg.setContentPane(content);
        dlg.setVisible(true);
    }

    // ================================================================
    //  UI HELPERS
    // ================================================================

    private void addFormRow(JPanel panel, String label, JComponent field) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(label.contains("*") ? AppColors.TEXT_PRIMARY : AppColors.TEXT_SECONDARY);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(lbl);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));

        if (field instanceof JTextField) {
            JTextField tf = (JTextField) field;
            tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1),
                    new EmptyBorder(0, 10, 0, 10)));
        }
        if (field instanceof JComboBox) {
            field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        }
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        field.setPreferredSize(new Dimension(0, 34));
        field.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(field);
        panel.add(Box.createRigidArea(new Dimension(0, 12)));
    }

    private JButton createActionButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 38));
        return btn;
    }
}
