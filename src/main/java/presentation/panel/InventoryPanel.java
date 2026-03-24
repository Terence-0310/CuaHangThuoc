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
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel Quản Lý Kho — Phân trang + CRUD lô hàng
 * Layout y chang ProductPanel: JSplitPane(Form trái + Table phải) + Pagination dưới
 */
public class InventoryPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int PAGE_SIZE = 20;

    // === STATE ===
    private int currentPage = 1;
    private int totalPages = 1;
    private int selectedMaLo = -1;

    // === SORT ===
    private int sortColumnIndex = 7;   // default: Ngày Nhập
    private boolean sortAsc = false;   // default: DESC (mới nhất trước)

    // Column index → SQL expression (null = không sort được)
    private static final String[] SORT_SQL = {
        null,                                   // 0: STT
        "l.SoLo",                               // 1: Số Lô
        "sp.TenSP",                             // 2: Tên SP
        "sp.DonViTinh",                         // 3: ĐVT
        "l.SoLuong",                            // 4: SL
        "l.GiaNhap",                            // 5: Giá Nhập
        "l.HanSuDung",                          // 6: Hạn SD
        "ISNULL(p.NgayNhap, l.NgayNhap)",       // 7: Ngày Nhập
        "nd.HoTen",                             // 8: Người Nhập
        "ncc.TenNCC",                           // 9: NCC
        null                                    // 10: Trạng Thái
    };

    // === FORM FIELDS ===
    private JTextField txtMaLo, txtSoLo, txtTenSP, txtDVT;
    private JTextField txtSoLuong, txtGiaNhap, txtHSD;
    private JTextField txtNgayNhap, txtNguoiNhap, txtNCC;

    // === BUTTONS ===
    private JButton btnUpdate, btnDelete, btnClear;

    // === TABLE ===
    private DefaultTableModel tableModel;
    private JTable table;

    // === FILTER ===
    private JTextField txtSearch;
    private JComboBox<String> cboStatus;

    // === PAGINATION ===
    private JButton btnFirst, btnPrev, btnNext, btnLast;
    private JLabel lblPageInfo;
    private JPanel pageNumbersPanel;

    // === SUMMARY ===
    private JLabel lblSummary;

    public InventoryPanel() {
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
    //  LAYOUT (y chang ProductPanel)
    // ================================================================

    private void initComponents() {
        // === TOP BAR ===
        JPanel topBar = new JPanel(new BorderLayout(12, 0));
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel lblTitle = new JLabel("Quản Lý Kho");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(AppColors.TEXT_PRIMARY);
        topBar.add(lblTitle, BorderLayout.WEST);

        // Filter bar
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filterPanel.setOpaque(false);

        cboStatus = new JComboBox<>(new String[]{
                "Tất cả", "Tốt", "Cận Date", "Hết HSD", "Sắp hết", "Hết hàng"
        });
        cboStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cboStatus.setPreferredSize(new Dimension(120, 32));

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
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm số lô, tên thuốc, NCC...");

        filterPanel.add(lblFilter);
        filterPanel.add(cboStatus);
        filterPanel.add(lblSearch);
        filterPanel.add(txtSearch);
        topBar.add(filterPanel, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // === CENTER: JSplitPane ===
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(280);
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
        cboStatus.addActionListener(e -> loadPage(1));
    }

    // ================================================================
    //  FORM PANEL (Left side)
    // ================================================================

    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel lblForm = new JLabel("Thông Tin Lô Hàng");
        lblForm.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblForm.setForeground(AppColors.PRIMARY);
        lblForm.setAlignmentX(LEFT_ALIGNMENT);
        formPanel.add(lblForm);
        formPanel.add(Box.createRigidArea(new Dimension(0, 16)));

        // Read-only fields
        txtMaLo = new JTextField();
        txtMaLo.setEditable(false);
        txtMaLo.setBackground(AppColors.NEUTRAL);
        addFormRow(formPanel, "Mã Lô:", txtMaLo);

        txtTenSP = new JTextField();
        txtTenSP.setEditable(false);
        txtTenSP.setBackground(AppColors.NEUTRAL);
        addFormRow(formPanel, "Tên sản phẩm:", txtTenSP);

        txtDVT = new JTextField();
        txtDVT.setEditable(false);
        txtDVT.setBackground(AppColors.NEUTRAL);
        addFormRow(formPanel, "Đơn vị tính:", txtDVT);

        // Editable fields
        txtSoLo = new JTextField();
        addFormRow(formPanel, "Số lô:", txtSoLo);

        txtSoLuong = new JTextField();
        addFormRow(formPanel, "Số lượng:", txtSoLuong);

        txtGiaNhap = new JTextField();
        addFormRowWithSuffix(formPanel, "Giá nhập:", txtGiaNhap, "VNĐ");

        txtHSD = new JTextField();
        txtHSD.setToolTipText("dd/MM/yyyy");
        addDateAutoSlash(txtHSD);
        addFormRow(formPanel, "Hạn sử dụng:", txtHSD);

        // Read-only info
        txtNgayNhap = new JTextField();
        txtNgayNhap.setEditable(false);
        txtNgayNhap.setBackground(AppColors.NEUTRAL);
        addFormRow(formPanel, "Ngày nhập:", txtNgayNhap);

        txtNguoiNhap = new JTextField();
        txtNguoiNhap.setEditable(false);
        txtNguoiNhap.setBackground(AppColors.NEUTRAL);
        addFormRow(formPanel, "Người nhập:", txtNguoiNhap);

        txtNCC = new JTextField();
        txtNCC.setEditable(false);
        txtNCC.setBackground(AppColors.NEUTRAL);
        addFormRow(formPanel, "Nhà cung cấp:", txtNCC);

        formPanel.add(Box.createRigidArea(new Dimension(0, 16)));

        // === Buttons: 2x2 grid ===
        JPanel btnPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setAlignmentX(LEFT_ALIGNMENT);
        btnPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));

        btnUpdate = createButton("Cập Nhật", new Color(0x17, 0xA2, 0xB8));
        btnUpdate.setEnabled(false);
        btnUpdate.addActionListener(e -> doUpdate());

        btnDelete = createButton("Xóa Lô", AppColors.DANGER);
        btnDelete.setEnabled(false);
        btnDelete.addActionListener(e -> doDelete());

        btnClear = createButton("Làm Mới", AppColors.SECONDARY);
        btnClear.addActionListener(e -> clearForm());

        JButton btnEmpty = createButton("", AppColors.NEUTRAL_DARK);
        btnEmpty.setVisible(false);

        btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);
        btnPanel.add(btnClear);
        btnPanel.add(btnEmpty);
        formPanel.add(btnPanel);

        formPanel.add(Box.createVerticalGlue());

        return formPanel;
    }

    // ================================================================
    //  TABLE + PAGINATION (Right side — y chang ProductPanel)
    // ================================================================

    private JPanel createTablePanel() {
        JPanel rightWrapper = new JPanel(new BorderLayout());
        rightWrapper.setBackground(Color.WHITE);
        rightWrapper.setBorder(new EmptyBorder(0, 12, 0, 24));

        // --- Table ---
        String[] cols = {
                "STT", "Số Lô", "Tên Sản Phẩm", "ĐVT",
                "SL", "Giá Nhập", "Hạn SD",
                "Ngày Nhập", "Người Nhập", "NCC",
                "Trạng Thái"
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

        // ★ Header click → sort (y chang ProductPanel)
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
        int[] widths = {35, 100, 160, 45, 50, 85, 80, 80, 100, 120, 80};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(0).setMaxWidth(45);

        // Unified cell renderer (y chang ProductPanel)
        table.setDefaultRenderer(Object.class, createCellRenderer());

        // Row selection → fill form
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

        lblSummary = new JLabel(" ");
        lblSummary.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSummary.setForeground(AppColors.TEXT_SECONDARY);

        return bar;
    }

    // ================================================================
    //  DATA LOADING — server-side pagination
    // ================================================================

    private void loadPage(int page) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        String keyword = txtSearch.getText().trim();
        String statusFilter = (String) cboStatus.getSelectedItem();

        // Build WHERE clause
        StringBuilder where = new StringBuilder("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (!keyword.isEmpty()) {
            where.append("AND (sp.TenSP LIKE ? OR l.SoLo LIKE ? OR nd.HoTen LIKE ? OR ncc.TenNCC LIKE ?) ");
            String like = "%" + keyword + "%";
            params.add(like); params.add(like); params.add(like); params.add(like);
        }

        // Status filter requires computing status — use CASE in SQL
        if (statusFilter != null && !"Tất cả".equals(statusFilter)) {
            // We'll filter post-query since status is calculated
        }

        String fromClause =
            "FROM LoHang l " +
            "JOIN SanPham sp ON l.MaSP = sp.MaSP " +
            "LEFT JOIN PhieuNhap p ON l.MaPN = p.MaPN " +
            "LEFT JOIN NguoiDung nd ON p.MaND = nd.MaND " +
            "LEFT JOIN NhaCungCap ncc ON p.MaNCC = ncc.MaNCC ";

        // Count total
        String countSql = "SELECT COUNT(*) " + fromClause + where;
        // Data query
        String dataSql =
            "SELECT l.MaLo, l.SoLo, sp.TenSP, sp.DonViTinh, " +
            "       l.SoLuong, l.GiaNhap, l.HanSuDung, " +
            "       ISNULL(p.NgayNhap, l.NgayNhap) AS NgayNhap, " +
            "       ISNULL(nd.HoTen, N'---') AS NguoiNhap, " +
            "       ISNULL(ncc.TenNCC, N'---') AS TenNCC " +
            fromClause + where +
            "ORDER BY " + buildOrderBy() + " " +
            "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        try (Connection conn = DatabaseHelper.getConnection()) {
            // Count
            int total;
            try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setNString(i + 1, (String) params.get(i));
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
            LocalDate today = LocalDate.now();
            int stt = offset + 1;
            int countHetHSD = 0, countCanDate = 0, countHetHang = 0;

            try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                int pi = 1;
                for (Object p2 : params) {
                    ps.setNString(pi++, (String) p2);
                }
                ps.setInt(pi++, offset);
                ps.setInt(pi, PAGE_SIZE);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int soLuong = rs.getInt("SoLuong");
                        LocalDate hsd = rs.getDate("HanSuDung") != null
                                ? rs.getDate("HanSuDung").toLocalDate() : null;

                        String status = calcStatus(soLuong, hsd, today);

                        // Status filter (client-side for calculated field)
                        if (statusFilter != null && !"Tất cả".equals(statusFilter)) {
                            if (!status.equals(statusFilter)) {
                                stt++;
                                continue;
                            }
                        }

                        String ngayNhap = "---";
                        Timestamp ts = rs.getTimestamp("NgayNhap");
                        if (ts != null) {
                            ngayNhap = ts.toLocalDateTime().toLocalDate().format(DATE_FMT);
                        }

                        BigDecimal giaNhap = rs.getBigDecimal("GiaNhap");
                        String giaNhapStr = giaNhap != null
                                ? String.format("%,d", giaNhap.longValue()) : "---";

                        if ("Hết HSD".equals(status)) countHetHSD++;
                        else if ("Cận Date".equals(status)) countCanDate++;
                        else if ("Hết hàng".equals(status)) countHetHang++;

                        tableModel.addRow(new Object[]{
                                stt++,
                                rs.getNString("SoLo"),
                                rs.getNString("TenSP"),
                                rs.getNString("DonViTinh"),
                                soLuong,
                                giaNhapStr,
                                hsd != null ? hsd.format(DATE_FMT) : "---",
                                ngayNhap,
                                rs.getNString("NguoiNhap"),
                                rs.getNString("TenNCC"),
                                status
                        });
                    }
                }
            }

            // Update pagination UI
            updatePaginationUI();

            // Summary
            lblSummary.setText(String.format(
                    "  Trang %d/%d   |   Tổng: %,d lô   |   ⚠ Hết HSD: %d   |   ⚠ Cận Date: %d   |   ❌ Hết hàng: %d",
                    currentPage, totalPages, total, countHetHSD, countCanDate, countHetHang));

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi tải dữ liệu kho:\n" + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        // ★ Adjust row height to fill viewport (y chang ProductPanel)
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

        String soLo = (String) tableModel.getValueAt(row, 1);
        String tenSP = (String) tableModel.getValueAt(row, 2);
        String dvt = (String) tableModel.getValueAt(row, 3);
        Object slObj = tableModel.getValueAt(row, 4);
        String giaNhap = (String) tableModel.getValueAt(row, 5);
        String hsd = (String) tableModel.getValueAt(row, 6);
        String ngayNhap = (String) tableModel.getValueAt(row, 7);
        String nguoiNhap = (String) tableModel.getValueAt(row, 8);
        String ncc = (String) tableModel.getValueAt(row, 9);

        // Query MaLo from DB
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT l.MaLo FROM LoHang l JOIN SanPham sp ON l.MaSP = sp.MaSP " +
                     "WHERE l.SoLo = ? AND sp.TenSP = ?")) {
            ps.setNString(1, soLo);
            ps.setNString(2, tenSP);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    selectedMaLo = rs.getInt("MaLo");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        txtMaLo.setText(String.valueOf(selectedMaLo));
        txtSoLo.setText(soLo);
        txtTenSP.setText(tenSP);
        txtDVT.setText(dvt);
        txtSoLuong.setText(slObj != null ? slObj.toString() : "");
        txtGiaNhap.setText(giaNhap);
        txtHSD.setText(hsd);
        txtNgayNhap.setText(ngayNhap);
        txtNguoiNhap.setText(nguoiNhap);
        txtNCC.setText(ncc);

        boolean isAdmin = Session.isAdmin();
        btnUpdate.setEnabled(isAdmin);
        btnDelete.setEnabled(isAdmin);
    }

    // ================================================================
    //  CRUD OPERATIONS
    // ================================================================

    private void doUpdate() {
        if (!Session.isAdmin()) { showWarning("Bạn không có quyền!"); return; }
        if (selectedMaLo < 0) { showWarning("Vui lòng chọn lô hàng cần sửa!"); return; }

        String soLo = txtSoLo.getText().trim();
        if (soLo.isEmpty()) { showWarning("Số lô không được trống!"); return; }

        int soLuong;
        try {
            soLuong = Integer.parseInt(txtSoLuong.getText().trim().replace(",", ""));
            if (soLuong < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showWarning("Số lượng phải là số nguyên ≥ 0!");
            return;
        }

        BigDecimal giaNhap;
        try {
            giaNhap = new BigDecimal(txtGiaNhap.getText().trim().replace(",", ""));
            if (giaNhap.signum() < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showWarning("Giá nhập không hợp lệ!");
            return;
        }

        LocalDate hsd;
        try {
            hsd = LocalDate.parse(txtHSD.getText().trim(), DATE_FMT);
        } catch (DateTimeParseException e) {
            showWarning("Hạn sử dụng không hợp lệ! (dd/MM/yyyy)");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Cập nhật lô hàng Mã Lô = " + selectedMaLo + "?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "UPDATE LoHang SET SoLo = ?, SoLuong = ?, GiaNhap = ?, HanSuDung = ? WHERE MaLo = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, soLo);
            ps.setInt(2, soLuong);
            ps.setBigDecimal(3, giaNhap);
            ps.setDate(4, Date.valueOf(hsd));
            ps.setInt(5, selectedMaLo);
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

    private void doDelete() {
        if (!Session.isAdmin()) { showWarning("Bạn không có quyền!"); return; }
        if (selectedMaLo < 0) { showWarning("Vui lòng chọn lô hàng cần xóa!"); return; }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Xóa vĩnh viễn lô hàng Mã Lô = " + selectedMaLo + "?\n" +
                "Hành động này KHÔNG thể hoàn tác!",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DatabaseHelper.getConnection()) {
            try (PreparedStatement check = conn.prepareStatement(
                    "SELECT COUNT(*) FROM ChiTietHoaDon WHERE MaLo = ?")) {
                check.setInt(1, selectedMaLo);
                try (ResultSet rs = check.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        showWarning("Không thể xóa!\nLô hàng này đã được bán trong hóa đơn.\nHãy đặt số lượng về 0 thay vì xóa.");
                        return;
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM LoHang WHERE MaLo = ?")) {
                ps.setInt(1, selectedMaLo);
                int affected = ps.executeUpdate();
                if (affected > 0) {
                    JOptionPane.showMessageDialog(this,
                            "Xóa thành công!", "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                    clearForm();
                    loadPage(currentPage);
                }
            }
        } catch (SQLException e) {
            showError("Lỗi xóa:\n" + e.getMessage());
        }
    }

    private void clearForm() {
        selectedMaLo = -1;
        txtMaLo.setText("");
        txtSoLo.setText("");
        txtTenSP.setText("");
        txtDVT.setText("");
        txtSoLuong.setText("");
        txtGiaNhap.setText("");
        txtHSD.setText("");
        txtNgayNhap.setText("");
        txtNguoiNhap.setText("");
        txtNCC.setText("");
        table.clearSelection();
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);
    }

    // ================================================================
    //  SORT HELPERS
    // ================================================================

    private String buildOrderBy() {
        if (sortColumnIndex >= 0 && sortColumnIndex < SORT_SQL.length && SORT_SQL[sortColumnIndex] != null) {
            return SORT_SQL[sortColumnIndex] + (sortAsc ? " ASC" : " DESC");
        }
        return "ISNULL(p.NgayNhap, l.NgayNhap) DESC";
    }

    // ================================================================
    //  UNIFIED CELL RENDERER (y chang ProductPanel.createCellRenderer)
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

                // Status column (col 10) - color coded
                if (col == 10 && val != null) {
                    String status = val.toString();
                    if (!sel) {
                        switch (status) {
                            case "Hết HSD": case "Hết hàng":
                                c.setForeground(AppColors.DANGER);
                                setFont(getFont().deriveFont(Font.BOLD));
                                break;
                            case "Cận Date": case "Sắp hết":
                                c.setForeground(new Color(0x85, 0x6D, 0x04));
                                setFont(getFont().deriveFont(Font.BOLD));
                                break;
                            case "Tốt":
                                c.setForeground(AppColors.SUCCESS);
                                setFont(getFont().deriveFont(Font.PLAIN));
                                break;
                            default:
                                c.setForeground(AppColors.TEXT_PRIMARY);
                                break;
                        }
                    }
                } else if (!sel) {
                    c.setForeground(AppColors.TEXT_PRIMARY);
                }

                // Alignment
                if (col == 0 || col == 4 || col == 6 || col == 7 || col == 10) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else if (col == 5) {
                    setHorizontalAlignment(SwingConstants.RIGHT);
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
    //  STATUS LOGIC
    // ================================================================

    private String calcStatus(int soLuong, LocalDate hsd, LocalDate today) {
        if (soLuong == 0) return "Hết hàng";
        if (soLuong <= 10) {
            if (hsd != null && hsd.isBefore(today)) return "Hết HSD";
            return "Sắp hết";
        }
        if (hsd != null && hsd.isBefore(today)) return "Hết HSD";
        if (hsd != null && !hsd.isAfter(today.plusDays(30))) return "Cận Date";
        return "Tốt";
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

    private void addDateAutoSlash(JTextField field) {
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                String text = field.getText();
                if (!Character.isDigit(c) && c != '/'
                        && c != KeyEvent.VK_BACK_SPACE && c != KeyEvent.VK_DELETE) {
                    e.consume();
                    return;
                }
                if (Character.isDigit(c) && text.length() >= 10) {
                    e.consume();
                    return;
                }
                if (Character.isDigit(c)) {
                    if (text.length() == 2 || text.length() == 5) {
                        field.setText(text + "/");
                    }
                }
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
