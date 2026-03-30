package presentation.panel;

import common.AppColors;
import common.Session;
import infrastructure.database.DatabaseHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel Quản Lý Nhà Cung Cấp — Phân trang + CRUD
 * Layout y chang InventoryPanel: JSplitPane(Form trái + Table phải) + Pagination dưới
 */
public class SupplierPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int PAGE_SIZE = 20;

    // === STATE ===
    private int currentPage = 1;
    private int totalPages = 1;
    private int selectedMaNCC = -1;

    // === SORT ===
    private int sortColumnIndex = 1;   // default: MaNCC
    private boolean sortAsc = true;    // default: ASC

    // Column index → SQL column (null = không sort được)
    private static final String[] SORT_SQL = {
        null,           // 0: STT
        "MaNCC",        // 1: Mã NCC
        "TenNCC",       // 2: Tên NCC
        null,           // 3: Số ĐT
        "DiaChi",       // 4: Địa chỉ
        "Email",        // 5: Email
        "TrangThai",    // 6: Trạng thái
        "NgayTao"       // 7: Ngày tạo
    };

    // === FORM FIELDS ===
    private JTextField txtMaNCC, txtTenNCC, txtSoDT, txtDiaChi, txtEmail, txtNgayTao;
    private JComboBox<String> cboTrangThai;

    // === BUTTONS ===
    private JButton btnAdd, btnUpdate, btnDelete, btnClear, btnDetail;

    // === TABLE ===
    private DefaultTableModel tableModel;
    private JTable table;

    // === FILTER ===
    private JTextField txtSearch;
    private JComboBox<String> cboFilterStatus;

    // === PAGINATION ===
    private JButton btnFirst, btnPrev, btnNext, btnLast;
    private JLabel lblPageInfo;
    private JPanel pageNumbersPanel;

    public SupplierPanel() {
        setLayout(new BorderLayout());
        setBackground(AppColors.NEUTRAL);
        initComponents();
        loadPage(1);

        // ★ Khi panel được hiển thị (chuyển tab) → reload để adjustRowHeight tính đúng
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                loadPage(currentPage);
            }
        });
    }

    // ================================================================
    //  LAYOUT (y chang InventoryPanel)
    // ================================================================

    private void initComponents() {
        // === TOP BAR ===
        JPanel topBar = new JPanel(new BorderLayout(12, 0));
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel lblTitle = new JLabel("Quản Lý Nhà Cung Cấp");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(AppColors.TEXT_PRIMARY);
        topBar.add(lblTitle, BorderLayout.WEST);

        // Filter bar
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filterPanel.setOpaque(false);

        cboFilterStatus = new JComboBox<>(new String[]{
                "Tất cả", "Hoạt động", "Ngừng hợp tác"
        });
        cboFilterStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cboFilterStatus.setPreferredSize(new Dimension(140, 32));

        JLabel lblFilter = new JLabel("Trạng thái:");
        lblFilter.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblFilter.setForeground(AppColors.TEXT_SECONDARY);

        JLabel lblSearch = new JLabel("Tìm kiếm:");
        lblSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSearch.setForeground(AppColors.TEXT_SECONDARY);

        txtSearch = new JTextField(16);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearch.setPreferredSize(new Dimension(200, 32));
        txtSearch.setBackground(Color.WHITE);
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.PRIMARY_LIGHT, 1),
                new EmptyBorder(0, 10, 0, 10)
        ));
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm tên NCC, SĐT, địa chỉ, email...");

        filterPanel.add(lblFilter);
        filterPanel.add(cboFilterStatus);
        filterPanel.add(lblSearch);
        filterPanel.add(txtSearch);
        topBar.add(filterPanel, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // === CENTER: JSplitPane ===
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(340);
        splitPane.setDividerSize(1);
        splitPane.setBorder(null);

        // Left: Form
        JPanel formWrapper = new JPanel(new BorderLayout());
        formWrapper.setBackground(Color.WHITE);
        formWrapper.add(createFormPanel(), BorderLayout.CENTER);
        splitPane.setLeftComponent(formWrapper);

        // Right: Table + Pagination
        splitPane.setRightComponent(createTablePanel());

        add(splitPane, BorderLayout.CENTER);

        // === EVENTS ===
        javax.swing.Timer searchTimer = new javax.swing.Timer(400, e -> loadPage(1));
        searchTimer.setRepeats(false);
        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { searchTimer.restart(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { searchTimer.restart(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}
        });
        cboFilterStatus.addActionListener(e -> loadPage(1));
    }

    // ================================================================
    //  FORM PANEL (Left side)
    // ================================================================

    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel lblForm = new JLabel("Thông Tin Nhà Cung Cấp");
        lblForm.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblForm.setForeground(AppColors.PRIMARY);
        lblForm.setAlignmentX(LEFT_ALIGNMENT);
        formPanel.add(lblForm);
        formPanel.add(Box.createRigidArea(new Dimension(0, 16)));

        // Read-only: Mã NCC
        txtMaNCC = new JTextField();
        txtMaNCC.setEditable(false);
        txtMaNCC.setBackground(AppColors.NEUTRAL);
        addFormRow(formPanel, "Mã NCC:", txtMaNCC);

        // Editable: Tên NCC (required)
        txtTenNCC = new JTextField();
        addFormRow(formPanel, "Tên NCC: *", txtTenNCC);

        // Editable: Số ĐT
        txtSoDT = new JTextField();
        addFormRow(formPanel, "Số ĐT:", txtSoDT);

        // Editable: Địa chỉ
        txtDiaChi = new JTextField();
        addFormRow(formPanel, "Địa chỉ:", txtDiaChi);

        // Editable: Email
        txtEmail = new JTextField();
        addFormRow(formPanel, "Email:", txtEmail);

        // Editable: Trạng thái (ComboBox)
        cboTrangThai = new JComboBox<>(new String[]{"Hoạt động", "Ngừng hợp tác"});
        cboTrangThai.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addFormRowCombo(formPanel, "Trạng thái:", cboTrangThai);

        // Read-only: Ngày tạo
        txtNgayTao = new JTextField();
        txtNgayTao.setEditable(false);
        txtNgayTao.setBackground(AppColors.NEUTRAL);
        addFormRow(formPanel, "Ngày tạo:", txtNgayTao);

        formPanel.add(Box.createRigidArea(new Dimension(0, 16)));

        // === Buttons ===
        JPanel btnPanel = new JPanel(new GridLayout(3, 2, 8, 8));
        btnPanel.setOpaque(false);
        btnPanel.setAlignmentX(LEFT_ALIGNMENT);
        btnPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        btnAdd = createButton("Thêm NCC", AppColors.PRIMARY);
        btnAdd.addActionListener(e -> doAdd());

        btnUpdate = createButton("Cập Nhật", AppColors.PRIMARY);
        btnUpdate.setEnabled(false);
        btnUpdate.addActionListener(e -> doUpdate());

        btnDelete = createButton("Ngừng HT", AppColors.DANGER);
        btnDelete.setEnabled(false);
        btnDelete.addActionListener(e -> doSoftDelete());

        btnClear = createButton("Làm Mới", AppColors.SECONDARY);
        btnClear.addActionListener(e -> clearForm());

        btnDetail = createButton("Xem Chi Tiết", AppColors.PRIMARY);
        btnDetail.setEnabled(false);
        btnDetail.addActionListener(e -> doShowDetail());

        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);
        btnPanel.add(btnClear);
        btnPanel.add(btnDetail);
        // Empty placeholder for grid alignment
        JButton btnEmpty = createButton("", AppColors.NEUTRAL_DARK);
        btnEmpty.setVisible(false);
        btnPanel.add(btnEmpty);
        formPanel.add(btnPanel);

        formPanel.add(Box.createVerticalGlue());

        // Hide CRUD buttons for non-admin (but keep Xem Chi Tiết visible for all)
        if (!Session.isAdmin()) {
            btnAdd.setVisible(false);
            btnUpdate.setVisible(false);
            btnDelete.setVisible(false);
        }

        return formPanel;
    }

    // ================================================================
    //  TABLE + PAGINATION (Right side — y chang InventoryPanel)
    // ================================================================

    private JPanel createTablePanel() {
        JPanel rightWrapper = new JPanel(new BorderLayout());
        rightWrapper.setBackground(Color.WHITE);
        rightWrapper.setBorder(new EmptyBorder(0, 12, 0, 24));

        // --- Table ---
        String[] cols = {
                "STT", "Mã NCC", "Tên NCC", "Số ĐT",
                "Địa chỉ", "Email", "Trạng thái", "Ngày tạo"
        };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(AppColors.PRIMARY_VERY_LIGHT);
        table.setSelectionForeground(AppColors.TEXT_PRIMARY);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(false);

        JTableHeader header = table.getTableHeader();
        header.setBackground(AppColors.TABLE_HEADER_BG);
        header.setForeground(AppColors.TABLE_HEADER_FG);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setPreferredSize(new Dimension(0, 36));
        header.setReorderingAllowed(false);

        // ★ Header click → sort
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                if (col >= 0 && col < SORT_SQL.length && SORT_SQL[col] != null) {
                    if (sortColumnIndex == col) {
                        sortAsc = !sortAsc;
                    } else {
                        sortColumnIndex = col;
                        sortAsc = true;
                    }
                    currentPage = 1;
                    loadPage(1);
                }
            }
        });

        // Column widths
        int[] widths = {40, 70, 180, 100, 200, 180, 90, 90};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(0).setMaxWidth(50);

        // Unified cell renderer
        table.setDefaultRenderer(Object.class, createCellRenderer());

        // Row selection → fill form
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
        });

        // ★ Right-click → Popup menu "Xem Chi Tiết"
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem miDetail = new JMenuItem("Xem Chi Tiết");
        miDetail.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        miDetail.setIcon(UIManager.getIcon("FileView.fileIcon"));
        miDetail.addActionListener(e -> doShowDetail());
        popupMenu.add(miDetail);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { showPopup(e); }
            @Override
            public void mouseReleased(MouseEvent e) { showPopup(e); }
            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        table.setRowSelectionInterval(row, row);
                        popupMenu.show(table, e.getX(), e.getY());
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);

        rightWrapper.add(scrollPane, BorderLayout.CENTER);
        rightWrapper.add(createPaginationBar(), BorderLayout.SOUTH);

        return rightWrapper;
    }

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

    // ================================================================
    //  DATA LOADING — server-side pagination
    // ================================================================

    private void loadPage(int page) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        String keyword = txtSearch.getText().trim();
        String statusFilter = (String) cboFilterStatus.getSelectedItem();

        // Build WHERE clause
        StringBuilder where = new StringBuilder("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (!keyword.isEmpty()) {
            where.append("AND (TenNCC LIKE ? OR SoDT LIKE ? OR DiaChi LIKE ? OR Email LIKE ?) ");
            String like = "%" + keyword + "%";
            params.add(like); params.add(like); params.add(like); params.add(like);
        }

        if (statusFilter != null && !"Tất cả".equals(statusFilter)) {
            where.append("AND TrangThai = ? ");
            params.add("Hoạt động".equals(statusFilter) ? 1 : 0);
        }

        // Count total
        String countSql = "SELECT COUNT(*) FROM NhaCungCap " + where;
        // Data query
        String dataSql =
            "SELECT MaNCC, TenNCC, SoDT, DiaChi, Email, TrangThai, NgayTao " +
            "FROM NhaCungCap " + where +
            "ORDER BY " + buildOrderBy() + " " +
            "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        try (Connection conn = DatabaseHelper.getConnection()) {
            // Count
            int total;
            try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                int pi = 1;
                for (Object p : params) {
                    if (p instanceof Integer) {
                        ps.setInt(pi++, (Integer) p);
                    } else {
                        ps.setNString(pi++, (String) p);
                    }
                }
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getInt(1);
                }
            }

            totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
            currentPage = Math.min(page, totalPages);
            int offset = (currentPage - 1) * PAGE_SIZE;

            // Data
            tableModel.setRowCount(0);
            int stt = offset + 1;

            try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                int pi = 1;
                for (Object p : params) {
                    if (p instanceof Integer) {
                        ps.setInt(pi++, (Integer) p);
                    } else {
                        ps.setNString(pi++, (String) p);
                    }
                }
                ps.setInt(pi++, offset);
                ps.setInt(pi, PAGE_SIZE);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        boolean trangThai = rs.getBoolean("TrangThai");
                        String statusText = trangThai ? "Hoạt động" : "Ngừng HT";

                        String ngayTao = "---";
                        Timestamp ts = rs.getTimestamp("NgayTao");
                        if (ts != null) {
                            ngayTao = ts.toLocalDateTime().toLocalDate().format(DATE_FMT);
                        }

                        tableModel.addRow(new Object[]{
                                stt++,
                                rs.getInt("MaNCC"),
                                rs.getNString("TenNCC"),
                                rs.getString("SoDT"),
                                rs.getNString("DiaChi"),
                                rs.getString("Email"),
                                statusText,
                                ngayTao
                        });
                    }
                }
            }

            // Update pagination UI
            updatePaginationUI();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi tải dữ liệu nhà cung cấp:\n" + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        // ★ Adjust row height to fill viewport
        adjustRowHeight(tableModel.getRowCount());

        setCursor(Cursor.getDefaultCursor());
    }

    private void adjustRowHeight(int rowCount) {
        SwingUtilities.invokeLater(() -> {
            if (rowCount >= PAGE_SIZE && table.getParent() != null) {
                int viewportH = table.getParent().getHeight();
                int dynamicH = viewportH / rowCount;
                table.setRowHeight(Math.max(30, dynamicH));
            } else {
                table.setRowHeight(32);
            }
        });
    }

    // ================================================================
    //  ROW SELECTION → fill form
    // ================================================================

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;

        selectedMaNCC = (int) tableModel.getValueAt(row, 1);
        txtMaNCC.setText(String.valueOf(selectedMaNCC));
        txtTenNCC.setText(safeStr(tableModel.getValueAt(row, 2)));
        txtSoDT.setText(safeStr(tableModel.getValueAt(row, 3)));
        txtDiaChi.setText(safeStr(tableModel.getValueAt(row, 4)));
        txtEmail.setText(safeStr(tableModel.getValueAt(row, 5)));

        String statusText = safeStr(tableModel.getValueAt(row, 6));
        cboTrangThai.setSelectedIndex("Hoạt động".equals(statusText) ? 0 : 1);

        txtNgayTao.setText(safeStr(tableModel.getValueAt(row, 7)));

        btnDetail.setEnabled(true);
        if (Session.isAdmin()) {
            btnUpdate.setEnabled(true);
            btnDelete.setEnabled(true);
        }
    }

    private String safeStr(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    // ================================================================
    //  CRUD OPERATIONS
    // ================================================================

    private void doAdd() {
        if (!Session.isAdmin()) { showWarning("Bạn không có quyền!"); return; }

        String tenNCC = txtTenNCC.getText().trim();
        if (tenNCC.isEmpty()) { showWarning("Tên NCC không được trống!"); return; }

        // ★ Check trùng tên NCC
        if (isDuplicateName(tenNCC, -1)) {
            showWarning("Tên NCC \"" + tenNCC + "\" đã tồn tại!\nVui lòng nhập tên khác.");
            return;
        }

        String soDT = txtSoDT.getText().trim();
        if (!soDT.isEmpty() && !soDT.matches("[0-9 ]+")) {
            showWarning("Số ĐT chỉ được chứa số và dấu cách!");
            return;
        }

        String email = txtEmail.getText().trim();
        if (!email.isEmpty() && !email.matches("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showWarning("Email không hợp lệ!");
            return;
        }

        String diaChi = txtDiaChi.getText().trim();
        boolean trangThai = cboTrangThai.getSelectedIndex() == 0;

        String sql = "INSERT INTO NhaCungCap (TenNCC, SoDT, DiaChi, Email, TrangThai) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, tenNCC);
            ps.setString(2, soDT.isEmpty() ? null : soDT);
            ps.setNString(3, diaChi.isEmpty() ? null : diaChi);
            ps.setString(4, email.isEmpty() ? null : email);
            ps.setBoolean(5, trangThai);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this,
                        "Thêm nhà cung cấp thành công!", "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadPage(currentPage);
            }
        } catch (SQLException e) {
            showError("Lỗi thêm nhà cung cấp:\n" + e.getMessage());
        }
    }

    private void doUpdate() {
        if (!Session.isAdmin()) { showWarning("Bạn không có quyền!"); return; }
        if (selectedMaNCC < 0) { showWarning("Vui lòng chọn nhà cung cấp cần sửa!"); return; }

        String tenNCC = txtTenNCC.getText().trim();
        if (tenNCC.isEmpty()) { showWarning("Tên NCC không được trống!"); return; }

        // ★ Check trùng tên NCC (exclude current NCC)
        if (isDuplicateName(tenNCC, selectedMaNCC)) {
            showWarning("Tên NCC \"" + tenNCC + "\" đã tồn tại!\nVui lòng nhập tên khác.");
            return;
        }

        String soDT = txtSoDT.getText().trim();
        if (!soDT.isEmpty() && !soDT.matches("[0-9 ]+")) {
            showWarning("Số ĐT chỉ được chứa số và dấu cách!");
            return;
        }

        String email = txtEmail.getText().trim();
        if (!email.isEmpty() && !email.matches("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showWarning("Email không hợp lệ!");
            return;
        }

        String diaChi = txtDiaChi.getText().trim();
        boolean trangThai = cboTrangThai.getSelectedIndex() == 0;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Cập nhật nhà cung cấp Mã NCC = " + selectedMaNCC + "?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "UPDATE NhaCungCap SET TenNCC = ?, SoDT = ?, DiaChi = ?, Email = ?, TrangThai = ? WHERE MaNCC = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, tenNCC);
            ps.setString(2, soDT.isEmpty() ? null : soDT);
            ps.setNString(3, diaChi.isEmpty() ? null : diaChi);
            ps.setString(4, email.isEmpty() ? null : email);
            ps.setBoolean(5, trangThai);
            ps.setInt(6, selectedMaNCC);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this,
                        "Cập nhật thành công!", "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadPage(currentPage);
            }
        } catch (SQLException e) {
            showError("Lỗi cập nhật:\n" + e.getMessage());
        }
    }

    private void doSoftDelete() {
        if (!Session.isAdmin()) { showWarning("Bạn không có quyền!"); return; }
        if (selectedMaNCC < 0) { showWarning("Vui lòng chọn nhà cung cấp!"); return; }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Ngừng hợp tác với NCC Mã = " + selectedMaNCC + "?\n" +
                "NCC sẽ được đánh dấu \"Ngừng hợp tác\", KHÔNG xóa vật lý.",
                "Xác nhận ngừng hợp tác", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        // SOFT DELETE: SET TrangThai = 0
        String sql = "UPDATE NhaCungCap SET TrangThai = 0 WHERE MaNCC = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, selectedMaNCC);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this,
                        "Đã ngừng hợp tác với NCC!", "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadPage(currentPage);
            }
        } catch (SQLException e) {
            showError("Lỗi xóa:\n" + e.getMessage());
        }
    }

    private void clearForm() {
        selectedMaNCC = -1;
        txtMaNCC.setText("");
        txtTenNCC.setText("");
        txtSoDT.setText("");
        txtDiaChi.setText("");
        txtEmail.setText("");
        cboTrangThai.setSelectedIndex(0);
        txtNgayTao.setText("");
        table.clearSelection();
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);
        btnDetail.setEnabled(false);
    }

    // ================================================================
    //  VIEW DETAIL
    // ================================================================

    private void doShowDetail() {
        if (selectedMaNCC < 0) { showWarning("Vui lòng chọn nhà cung cấp!"); return; }
        String tenNCC = txtTenNCC.getText().trim();
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        new presentation.dialog.SupplierDetailDialog(parentFrame, selectedMaNCC, tenNCC).setVisible(true);
    }

    // ================================================================
    //  DUPLICATE NAME CHECK
    // ================================================================

    /**
     * Kiểm tra tên NCC đã tồn tại chưa (case-insensitive).
     * @param tenNCC tên cần kiểm tra
     * @param excludeMaNCC mã NCC cần loại trừ (dùng khi update), -1 nếu thêm mới
     */
    private boolean isDuplicateName(String tenNCC, int excludeMaNCC) {
        String sql = "SELECT COUNT(*) FROM NhaCungCap WHERE TenNCC = ?" +
                (excludeMaNCC > 0 ? " AND MaNCC <> ?" : "");
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, tenNCC);
            if (excludeMaNCC > 0) {
                ps.setInt(2, excludeMaNCC);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ================================================================
    //  SORT HELPERS
    // ================================================================

    private String buildOrderBy() {
        if (sortColumnIndex >= 0 && sortColumnIndex < SORT_SQL.length && SORT_SQL[sortColumnIndex] != null) {
            return SORT_SQL[sortColumnIndex] + (sortAsc ? " ASC" : " DESC");
        }
        return "MaNCC ASC";
    }

    // ================================================================
    //  UNIFIED CELL RENDERER
    // ================================================================

    private DefaultTableCellRenderer createCellRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, focus, row, col);

                // Alt-row background
                if (!sel) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);
                }

                // Status column (col 6) — color coded
                if (col == 6 && val != null) {
                    String status = val.toString();
                    if (!sel) {
                        if ("Hoạt động".equals(status)) {
                            c.setForeground(AppColors.SUCCESS);
                            setFont(getFont().deriveFont(Font.BOLD));
                        } else {
                            c.setForeground(AppColors.DANGER);
                            setFont(getFont().deriveFont(Font.BOLD));
                        }
                    }
                } else if (!sel) {
                    c.setForeground(AppColors.TEXT_PRIMARY);
                }

                // Alignment per column
                // STT(0)=center, MaNCC(1)=center, TenNCC(2)=left, SoDT(3)=center,
                // DiaChi(4)=left, Email(5)=left, TrangThai(6)=center, NgayTao(7)=center
                if (col == 0 || col == 1 || col == 3 || col == 6 || col == 7) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }

                return c;
            }
        };
    }

    // ================================================================
    //  PAGINATION UI
    // ================================================================

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
            final int pg = i;
            btn.addActionListener(e -> loadPage(pg));
            pageNumbersPanel.add(btn);
        }
        pageNumbersPanel.revalidate();
        pageNumbersPanel.repaint();

        lblPageInfo.setText("Trang " + currentPage + " / " + totalPages);
    }

    // ================================================================
    //  UI HELPERS
    // ================================================================

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

    private void addFormRowCombo(JPanel panel, String labelText, JComboBox<?> combo) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(AppColors.TEXT_SECONDARY);
        label.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));

        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        combo.setPreferredSize(new Dimension(0, 34));
        combo.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(combo);
        panel.add(Box.createRigidArea(new Dimension(0, 12)));
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

        Color hover = bg.darker();
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (btn.isEnabled()) btn.setBackground(hover); }
            public void mouseExited(MouseEvent e) { if (btn.isEnabled()) btn.setBackground(bg); }
        });
        return btn;
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

    private void showWarning(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Cảnh báo", JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
}
