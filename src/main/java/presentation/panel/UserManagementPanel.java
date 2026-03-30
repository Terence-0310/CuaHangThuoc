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
 * Panel Quản Lý Người Dùng & Phân Quyền
 * Layout: JSplitPane(Form trái + Table phải) + Pagination dưới
 * CRUD: Thêm / Sửa / Khóa tài khoản / Reset mật khẩu
 * Phân quyền: Admin / NhanVien
 */
public class UserManagementPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int PAGE_SIZE = 20;
    private static final String DEFAULT_PASSWORD = "123456";

    // === STATE ===
    private int currentPage = 1;
    private int totalPages = 1;
    private int selectedMaND = -1;

    // === SORT ===
    private int sortColumnIndex = 1;
    private boolean sortAsc = true;
    private static final String[] SORT_SQL = {
        null, "MaND", "TenDangNhap", "MatKhau", "HoTen", "VaiTro", "TrangThai", "DangOnline", "NgayTao"
    };

    // === FORM ===
    private JTextField txtMaND, txtTenDangNhap, txtHoTen, txtNgayTao;
    private JPasswordField txtMatKhau;
    private JComboBox<String> cboVaiTro, cboTrangThai;

    // === BUTTONS ===
    private JButton btnAdd, btnUpdate, btnLock, btnResetPwd, btnClear;

    // === TABLE ===
    private DefaultTableModel tableModel;
    private JTable table;

    // === FILTER ===
    private JTextField txtSearch;
    private JComboBox<String> cboFilterRole;

    // === PAGINATION ===
    private JButton btnFirst, btnPrev, btnNext, btnLast;
    private JLabel lblPageInfo;
    private JPanel pageNumbersPanel;

    public UserManagementPanel() {
        setLayout(new BorderLayout());
        setBackground(AppColors.NEUTRAL);
        initComponents();
        loadPage(1);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                loadPage(currentPage);
            }
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

        JLabel lblTitle = new JLabel("Quản Lý Người Dùng & Phân Quyền");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(AppColors.TEXT_PRIMARY);
        topBar.add(lblTitle, BorderLayout.WEST);

        // Filter
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filterPanel.setOpaque(false);

        cboFilterRole = new JComboBox<>(new String[]{"Tất cả", "Admin", "Nhân viên"});
        cboFilterRole.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cboFilterRole.setPreferredSize(new Dimension(120, 32));

        JLabel lblFilter = new JLabel("Vai trò:");
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

        filterPanel.add(lblFilter);
        filterPanel.add(cboFilterRole);
        filterPanel.add(lblSearch);
        filterPanel.add(txtSearch);
        topBar.add(filterPanel, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // === CENTER: JSplitPane ===
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(280);
        splitPane.setDividerSize(1);
        splitPane.setBorder(null);

        JPanel formWrapper = new JPanel(new BorderLayout());
        formWrapper.setBackground(Color.WHITE);
        formWrapper.add(createFormPanel(), BorderLayout.CENTER);
        splitPane.setLeftComponent(formWrapper);
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
        cboFilterRole.addActionListener(e -> loadPage(1));
    }

    // ================================================================
    //  FORM PANEL
    // ================================================================

    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel lblForm = new JLabel("Thông Tin Người Dùng");
        lblForm.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblForm.setForeground(AppColors.PRIMARY);
        lblForm.setAlignmentX(LEFT_ALIGNMENT);
        formPanel.add(lblForm);
        formPanel.add(Box.createRigidArea(new Dimension(0, 16)));

        // Read-only
        txtMaND = new JTextField();
        txtMaND.setEditable(false);
        txtMaND.setBackground(AppColors.NEUTRAL);
        addFormRow(formPanel, "Mã ND:", txtMaND);

        // Editable
        txtTenDangNhap = new JTextField();
        addFormRow(formPanel, "Tên đăng nhập: *", txtTenDangNhap);

        txtMatKhau = new JPasswordField();
        txtMatKhau.setToolTipText("Mặc định: " + DEFAULT_PASSWORD);
        addFormRow(formPanel, "Mật khẩu: *", txtMatKhau);

        txtHoTen = new JTextField();
        addFormRow(formPanel, "Họ tên: *", txtHoTen);

        cboVaiTro = new JComboBox<>(new String[]{"NhanVien", "Admin"});
        cboVaiTro.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addFormRowCombo(formPanel, "Vai trò:", cboVaiTro);

        cboTrangThai = new JComboBox<>(new String[]{"Đang hoạt động", "Đã khóa"});
        cboTrangThai.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addFormRowCombo(formPanel, "Trạng thái:", cboTrangThai);

        txtNgayTao = new JTextField();
        txtNgayTao.setEditable(false);
        txtNgayTao.setBackground(AppColors.NEUTRAL);
        addFormRow(formPanel, "Ngày tạo:", txtNgayTao);

        formPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        // === Buttons: 3x2 grid ===
        JPanel btnPanel = new JPanel(new GridLayout(3, 2, 8, 8));
        btnPanel.setOpaque(false);
        btnPanel.setAlignmentX(LEFT_ALIGNMENT);
        btnPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        btnAdd = createButton("Thêm User", AppColors.PRIMARY);
        btnAdd.addActionListener(e -> doAdd());

        btnUpdate = createButton("Cập Nhật", AppColors.PRIMARY);
        btnUpdate.setEnabled(false);
        btnUpdate.addActionListener(e -> doUpdate());

        btnLock = createButton("Khóa TK", AppColors.DANGER);
        btnLock.setEnabled(false);
        btnLock.addActionListener(e -> doToggleLock());

        btnClear = createButton("Làm Mới", AppColors.SECONDARY);
        btnClear.addActionListener(e -> clearForm());

        btnResetPwd = createButton("Reset MK", AppColors.PRIMARY);
        btnResetPwd.setEnabled(false);
        btnResetPwd.addActionListener(e -> doResetPassword());

        JButton btnEmpty = createButton("", AppColors.NEUTRAL_DARK);
        btnEmpty.setVisible(false);

        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnLock);
        btnPanel.add(btnClear);
        btnPanel.add(btnResetPwd);
        btnPanel.add(btnEmpty);
        formPanel.add(btnPanel);

        formPanel.add(Box.createVerticalGlue());

        // Only Admin can manage users
        if (!Session.isAdmin()) {
            btnAdd.setVisible(false);
            btnUpdate.setVisible(false);
            btnLock.setVisible(false);
            btnResetPwd.setVisible(false);
        }

        return formPanel;
    }

    // ================================================================
    //  TABLE + PAGINATION
    // ================================================================

    private JPanel createTablePanel() {
        JPanel rightWrapper = new JPanel(new BorderLayout());
        rightWrapper.setBackground(Color.WHITE);
        rightWrapper.setBorder(new EmptyBorder(0, 12, 0, 24));

        String[] cols = {"STT", "Mã ND", "Tên đăng nhập", "Mật khẩu", "Họ tên", "Vai trò", "Tài khoản", "Trạng thái", "Ngày tạo"};
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

        JTableHeader header = table.getTableHeader();
        header.setBackground(AppColors.TABLE_HEADER_BG);
        header.setForeground(AppColors.TABLE_HEADER_FG);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setPreferredSize(new Dimension(0, 36));
        header.setReorderingAllowed(false);

        // Header click → sort
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
                    loadPage(1);
                }
            }
        });

        int[] widths = {35, 50, 120, 100, 160, 80, 85, 90, 85};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(0).setMaxWidth(42);

        table.setDefaultRenderer(Object.class, createCellRenderer());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
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
        btnPrev = createPageNavButton("< Trước");
        pageNumbersPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        pageNumbersPanel.setOpaque(false);
        btnNext = createPageNavButton("Sau >");
        btnLast = createPageNavButton("Cuối >|");

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
    //  DATA LOADING
    // ================================================================

    private void loadPage(int page) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        String keyword = txtSearch.getText().trim();
        String roleFilter = (String) cboFilterRole.getSelectedItem();

        StringBuilder where = new StringBuilder("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (!keyword.isEmpty()) {
            where.append("AND (TenDangNhap LIKE ? OR HoTen LIKE ?) ");
            String like = "%" + keyword + "%";
            params.add(like);
            params.add(like);
        }

        if (roleFilter != null && !"Tất cả".equals(roleFilter)) {
            where.append("AND VaiTro = ? ");
            params.add("Admin".equals(roleFilter) ? "Admin" : "NhanVien");
        }

        String countSql = "SELECT COUNT(*) FROM NguoiDung " + where;
        String dataSql =
            "SELECT MaND, TenDangNhap, MatKhau, HoTen, VaiTro, TrangThai, " +
            "ISNULL(DangOnline, 0) AS DangOnline, NgayTao " +
            "FROM NguoiDung " + where +
            "ORDER BY " + buildOrderBy() + " " +
            "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        try (Connection conn = DatabaseHelper.getConnection()) {
            int total;
            try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                int pi = 1;
                for (Object p : params) ps.setNString(pi++, (String) p);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getInt(1);
                }
            }

            totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
            currentPage = Math.min(page, totalPages);
            int offset = (currentPage - 1) * PAGE_SIZE;

            tableModel.setRowCount(0);
            int stt = offset + 1;

            try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                int pi = 1;
                for (Object p : params) ps.setNString(pi++, (String) p);
                ps.setInt(pi++, offset);
                ps.setInt(pi, PAGE_SIZE);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        boolean tt = rs.getBoolean("TrangThai");
                        boolean online = rs.getBoolean("DangOnline");
                        String vaiTro = rs.getNString("VaiTro");
                        String vaiTroDisplay = "Admin".equalsIgnoreCase(vaiTro) ? "Admin" : "Nhân viên";
                        String accountStatus = tt ? "Hoạt động" : "Đã khóa";
                        String onlineStatus = online ? "● Online" : "○ Offline";

                        String ngayTao = "---";
                        Timestamp ts = rs.getTimestamp("NgayTao");
                        if (ts != null) {
                            ngayTao = ts.toLocalDateTime().toLocalDate().format(DATE_FMT);
                        }

                        tableModel.addRow(new Object[]{
                                stt++,
                                rs.getInt("MaND"),
                                rs.getNString("TenDangNhap"),
                                rs.getNString("MatKhau"),
                                rs.getNString("HoTen"),
                                vaiTroDisplay,
                                accountStatus,
                                onlineStatus,
                                ngayTao
                        });
                    }
                }
            }

            updatePaginationUI();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi tải dữ liệu:\n" + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }

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
    //  ROW SELECTION
    // ================================================================

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;

        selectedMaND = (int) tableModel.getValueAt(row, 1);
        txtMaND.setText(String.valueOf(selectedMaND));
        txtTenDangNhap.setText(safeStr(tableModel.getValueAt(row, 2)));
        txtHoTen.setText(safeStr(tableModel.getValueAt(row, 4)));
        txtMatKhau.setText(""); // Never fill password field

        String vaiTro = safeStr(tableModel.getValueAt(row, 5));
        cboVaiTro.setSelectedIndex("Admin".equals(vaiTro) ? 1 : 0);

        String accountStatus = safeStr(tableModel.getValueAt(row, 6));
        cboTrangThai.setSelectedIndex("Hoạt động".equals(accountStatus) ? 0 : 1);

        txtNgayTao.setText(safeStr(tableModel.getValueAt(row, 8)));

        // Disable editing username for existing user
        txtTenDangNhap.setEditable(false);
        txtTenDangNhap.setBackground(AppColors.NEUTRAL);

        if (Session.isAdmin()) {
            btnUpdate.setEnabled(true);
            btnLock.setEnabled(true);
            btnResetPwd.setEnabled(true);

            // Update lock button text
            boolean isActive = "Hoạt động".equals(accountStatus);
            btnLock.setText(isActive ? "Khóa TK" : "Mở Khóa");
        }
    }

    private String safeStr(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    // ================================================================
    //  CRUD
    // ================================================================

    private void doAdd() {
        if (!Session.isAdmin()) { showWarning("Bạn không có quyền!"); return; }

        String tenDN = txtTenDangNhap.getText().trim();
        if (tenDN.isEmpty()) { showWarning("Tên đăng nhập không được trống!"); return; }
        if (tenDN.length() < 3) { showWarning("Tên đăng nhập phải >= 3 ký tự!"); return; }
        if (!tenDN.matches("[a-zA-Z0-9._]+")) {
            showWarning("Tên đăng nhập chỉ được chứa chữ, số, dấu chấm và gạch dưới!");
            return;
        }

        String matKhau = new String(txtMatKhau.getPassword()).trim();
        if (matKhau.isEmpty()) matKhau = DEFAULT_PASSWORD;
        if (matKhau.length() < 4) { showWarning("Mật khẩu phải >= 4 ký tự!"); return; }

        String hoTen = txtHoTen.getText().trim();
        if (hoTen.isEmpty()) { showWarning("Họ tên không được trống!"); return; }

        String vaiTro = (String) cboVaiTro.getSelectedItem();
        boolean trangThai = cboTrangThai.getSelectedIndex() == 0;

        // Check duplicate username
        if (isDuplicateUsername(tenDN, -1)) {
            showWarning("Tên đăng nhập \"" + tenDN + "\" đã tồn tại!");
            return;
        }

        String sql = "INSERT INTO NguoiDung (TenDangNhap, MatKhau, HoTen, VaiTro, TrangThai) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, tenDN);
            ps.setNString(2, matKhau); // In production: hash password!
            ps.setNString(3, hoTen);
            ps.setNString(4, vaiTro);
            ps.setBoolean(5, trangThai);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this,
                        "Thêm người dùng thành công!\nMật khẩu: " + matKhau,
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadPage(currentPage);
            }
        } catch (SQLException e) {
            showError("Lỗi thêm người dùng:\n" + e.getMessage());
        }
    }

    private void doUpdate() {
        if (!Session.isAdmin()) { showWarning("Bạn không có quyền!"); return; }
        if (selectedMaND < 0) { showWarning("Vui lòng chọn người dùng!"); return; }

        // Prevent editing own role
        if (selectedMaND == Session.getCurrentUser().getMaND()) {
            // Allow updating name but not role
        }

        String hoTen = txtHoTen.getText().trim();
        if (hoTen.isEmpty()) { showWarning("Họ tên không được trống!"); return; }

        String vaiTro = (String) cboVaiTro.getSelectedItem();
        boolean trangThai = cboTrangThai.getSelectedIndex() == 0;

        // Prevent self-demotion
        if (selectedMaND == Session.getCurrentUser().getMaND() && !"Admin".equals(vaiTro)) {
            showWarning("Không thể hạ quyền chính mình!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Cập nhật người dùng Mã ND = " + selectedMaND + "?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        // Update password only if field is not empty
        String newPwd = new String(txtMatKhau.getPassword()).trim();
        boolean updatePwd = !newPwd.isEmpty();

        String sql = updatePwd
                ? "UPDATE NguoiDung SET HoTen = ?, VaiTro = ?, TrangThai = ?, MatKhau = ? WHERE MaND = ?"
                : "UPDATE NguoiDung SET HoTen = ?, VaiTro = ?, TrangThai = ? WHERE MaND = ?";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, hoTen);
            ps.setNString(2, vaiTro);
            ps.setBoolean(3, trangThai);
            if (updatePwd) {
                ps.setNString(4, newPwd);
                ps.setInt(5, selectedMaND);
            } else {
                ps.setInt(4, selectedMaND);
            }

            int affected = ps.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this,
                        "Cập nhật thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadPage(currentPage);
            }
        } catch (SQLException e) {
            showError("Lỗi cập nhật:\n" + e.getMessage());
        }
    }

    private void doToggleLock() {
        if (!Session.isAdmin()) { showWarning("Bạn không có quyền!"); return; }
        if (selectedMaND < 0) { showWarning("Vui lòng chọn người dùng!"); return; }

        // Prevent self-lock
        if (selectedMaND == Session.getCurrentUser().getMaND()) {
            showWarning("Không thể khóa tài khoản chính mình!");
            return;
        }

        // Check current status
        boolean currentlyActive = cboTrangThai.getSelectedIndex() == 0;
        String action = currentlyActive ? "KHÓA" : "MỞ KHÓA";

        int confirm = JOptionPane.showConfirmDialog(this,
                action + " tài khoản Mã ND = " + selectedMaND + "?",
                "Xác nhận", JOptionPane.YES_NO_OPTION,
                currentlyActive ? JOptionPane.WARNING_MESSAGE : JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "UPDATE NguoiDung SET TrangThai = ? WHERE MaND = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, !currentlyActive);
            ps.setInt(2, selectedMaND);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this,
                        action + " thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadPage(currentPage);
            }
        } catch (SQLException e) {
            showError("Lỗi:\n" + e.getMessage());
        }
    }

    private void doResetPassword() {
        if (!Session.isAdmin()) { showWarning("Bạn không có quyền!"); return; }
        if (selectedMaND < 0) { showWarning("Vui lòng chọn người dùng!"); return; }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Reset mật khẩu về \"" + DEFAULT_PASSWORD + "\" cho Mã ND = " + selectedMaND + "?",
                "Xác nhận Reset MK", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "UPDATE NguoiDung SET MatKhau = ? WHERE MaND = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, DEFAULT_PASSWORD);
            ps.setInt(2, selectedMaND);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this,
                        "Reset mật khẩu thành công!\nMật khẩu mới: " + DEFAULT_PASSWORD,
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException e) {
            showError("Lỗi reset mật khẩu:\n" + e.getMessage());
        }
    }

    private void clearForm() {
        selectedMaND = -1;
        txtMaND.setText("");
        txtTenDangNhap.setText("");
        txtTenDangNhap.setEditable(true);
        txtTenDangNhap.setBackground(Color.WHITE);
        txtMatKhau.setText("");
        txtHoTen.setText("");
        cboVaiTro.setSelectedIndex(0);
        cboTrangThai.setSelectedIndex(0);
        txtNgayTao.setText("");
        table.clearSelection();
        btnUpdate.setEnabled(false);
        btnLock.setEnabled(false);
        btnResetPwd.setEnabled(false);
        btnLock.setText("Khóa TK");
    }

    // ================================================================
    //  HELPERS
    // ================================================================

    private boolean isDuplicateUsername(String tenDN, int excludeMaND) {
        String sql = "SELECT COUNT(*) FROM NguoiDung WHERE TenDangNhap = ?" +
                (excludeMaND > 0 ? " AND MaND <> ?" : "");
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, tenDN);
            if (excludeMaND > 0) ps.setInt(2, excludeMaND);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private String buildOrderBy() {
        if (sortColumnIndex >= 0 && sortColumnIndex < SORT_SQL.length && SORT_SQL[sortColumnIndex] != null) {
            return SORT_SQL[sortColumnIndex] + (sortAsc ? " ASC" : " DESC");
        }
        return "MaND ASC";
    }

    private DefaultTableCellRenderer createCellRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);
                }
                // Vai trò column (5) — color coded
                if (col == 5 && val != null && !sel) {
                    c.setForeground("Admin".equals(val.toString()) ? AppColors.PRIMARY : AppColors.TEXT_PRIMARY);
                    setFont(getFont().deriveFont("Admin".equals(val.toString()) ? Font.BOLD : Font.PLAIN));
                }
                // Tài khoản column (6) — account active/locked
                else if (col == 6 && val != null && !sel) {
                    boolean active = "Hoạt động".equals(val.toString());
                    c.setForeground(active ? AppColors.SUCCESS : AppColors.DANGER);
                    setFont(getFont().deriveFont(Font.BOLD));
                }
                // ★ Trạng thái Online column (7) — Facebook-style
                else if (col == 7 && val != null && !sel) {
                    boolean online = val.toString().contains("Online");
                    if (online) {
                        c.setForeground(new Color(0x28, 0xA7, 0x45)); // Green
                        c.setBackground(new Color(0xD4, 0xED, 0xDA)); // Light green BG
                    } else {
                        c.setForeground(AppColors.TEXT_SECONDARY);    // Gray
                    }
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if (!sel) {
                    c.setForeground(AppColors.TEXT_PRIMARY);
                }
                // Alignment
                if (col == 0 || col == 1 || col == 5 || col == 6 || col == 7 || col == 8) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }
                return c;
            }
        };
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
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
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
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
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
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btn.setForeground(AppColors.TEXT_PRIMARY);
        btn.setBackground(Color.WHITE);
        btn.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(65, 28));
        return btn;
    }

    private JButton createPageNumButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setForeground(AppColors.TEXT_PRIMARY);
        btn.setBackground(Color.WHITE);
        btn.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(32, 28));
        return btn;
    }

    private void showWarning(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Cảnh báo", JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
}
