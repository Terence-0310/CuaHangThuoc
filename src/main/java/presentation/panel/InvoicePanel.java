package presentation.panel;

import common.AppColors;
import domain.entity.Invoice;
import infrastructure.database.DatabaseHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * InvoicePanel — Quản Lý Hóa Đơn (Admin only)
 * - View invoices with filters
 * - Double-click to view detail
 * - Void invoice (hoàn kho)
 * NO Edit / NO Delete
 */
public class InvoicePanel extends JPanel {

    private JTextField txtSearchKH, txtSearchMaHD;
    private JTextField txtFromDate, txtToDate;
    private JComboBox<String> cboTrangThai;
    private DefaultTableModel tableModel;
    private JTable table;

    // Pagination
    private int currentPage = 1;
    private int totalPages = 1;
    private static final int PAGE_SIZE = 15;
    private JLabel lblPageInfo;

    // Customer filter: 0 = all, 1 = registered, 2 = walk-in
    private int customerTypeFilter = 0;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public InvoicePanel() {
        setLayout(new BorderLayout());
        setBackground(AppColors.NEUTRAL);

        // Top: title + filters + tab buttons
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.setBackground(Color.WHITE);
        northPanel.add(createTopBar());
        northPanel.add(createFilterTabs());
        add(northPanel, BorderLayout.NORTH);

        add(createTablePanel(), BorderLayout.CENTER);
        add(createPaginationPanel(), BorderLayout.SOUTH);

        SwingUtilities.invokeLater(() -> loadInvoices());

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                loadInvoices();
            }
        });
    }

    private JPanel createFilterTabs() {
        JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabPanel.setBackground(Color.WHITE);
        tabPanel.setBorder(new EmptyBorder(0, 24, 8, 24));

        String[] displayLabels = {"Tất cả", "Khách đã đăng ký", "Khách vãng lai"};
        JLabel[] tabLabels = new JLabel[displayLabels.length];

        for (int i = 0; i < displayLabels.length; i++) {
            JLabel lbl = new JLabel(displayLabels[i], SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lbl.setOpaque(true);
            lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lbl.setPreferredSize(new Dimension(lbl.getPreferredSize().width + 32, 32));

            tabLabels[i] = lbl;
            int idx = i;

            lbl.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    customerTypeFilter = idx;
                    currentPage = 1;
                    styleFilterTabs(tabLabels, idx);
                    loadInvoices();
                }
            });
            tabPanel.add(lbl);
        }

        styleFilterTabs(tabLabels, 0);
        return tabPanel;
    }

    private void styleFilterTabs(JLabel[] tabs, int activeIdx) {
        for (int i = 0; i < tabs.length; i++) {
            if (i == activeIdx) {
                tabs[i].setBackground(AppColors.PRIMARY);
                tabs[i].setForeground(Color.WHITE);
                tabs[i].setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 3, 0, AppColors.PRIMARY),
                        new EmptyBorder(6, 16, 3, 16)));
            } else {
                tabs[i].setBackground(new Color(0xF0, 0xF0, 0xF0));
                tabs[i].setForeground(new Color(0x33, 0x33, 0x33));
                tabs[i].setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.NEUTRAL_DARK),
                        new EmptyBorder(6, 16, 5, 16)));
            }
        }
    }

    // ================================================================
    //  TOP BAR: Title + Filters
    // ================================================================
    private JPanel createTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.setBorder(new EmptyBorder(16, 24, 12, 24));

        JLabel lblTitle = new JLabel("Quản Lý Hóa Đơn");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(AppColors.TEXT_PRIMARY);
        top.add(lblTitle, BorderLayout.NORTH);

        // Filter row
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        filterPanel.setBackground(Color.WHITE);

        filterPanel.add(makeFilterLabel("Từ ngày:"));
        txtFromDate = makeFilterField(10);
        txtFromDate.setToolTipText("dd/MM/yyyy");
        filterPanel.add(txtFromDate);

        filterPanel.add(makeFilterLabel("Đến ngày:"));
        txtToDate = makeFilterField(10);
        txtToDate.setToolTipText("dd/MM/yyyy");
        filterPanel.add(txtToDate);

        filterPanel.add(makeFilterLabel("Khách hàng:"));
        txtSearchKH = makeFilterField(12);
        filterPanel.add(txtSearchKH);

        filterPanel.add(makeFilterLabel("Mã HĐ:"));
        txtSearchMaHD = makeFilterField(6);
        filterPanel.add(txtSearchMaHD);

        filterPanel.add(makeFilterLabel("Trạng thái:"));
        cboTrangThai = new JComboBox<>(new String[]{"Tất cả", "Thành công", "Đã hủy"});
        cboTrangThai.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterPanel.add(cboTrangThai);

        JButton btnSearch = new JButton("Tìm kiếm");
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSearch.setBackground(AppColors.PRIMARY);
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFocusPainted(false);
        btnSearch.setBorderPainted(false);
        btnSearch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSearch.addActionListener(e -> { currentPage = 1; loadInvoices(); });
        filterPanel.add(btnSearch);

        top.add(filterPanel, BorderLayout.CENTER);
        return top;
    }

    private JLabel makeFilterLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(AppColors.TEXT_SECONDARY);
        return lbl;
    }

    private JTextField makeFilterField(int cols) {
        JTextField tf = new JTextField(cols);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tf.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(AppColors.NEUTRAL_DARK, 1),
                new EmptyBorder(4, 8, 4, 8)));
        return tf;
    }

    // ================================================================
    //  TABLE
    // ================================================================
    private JScrollPane createTablePanel() {
        String[] cols = {"STT", "Mã HĐ", "Ngày bán", "Khách hàng", "SĐT", "Nhân viên", "Phương thức", "Tổng tiền", "Trạng thái"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(34);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(AppColors.TABLE_HEADER_BG);
        table.getTableHeader().setForeground(AppColors.TABLE_HEADER_FG);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setMaxWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(130);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(130);
        table.getColumnModel().getColumn(6).setPreferredWidth(90);
        table.getColumnModel().getColumn(7).setPreferredWidth(110);
        table.getColumnModel().getColumn(8).setPreferredWidth(90);

        // Renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int r, int c) {
                // STT column: show visual row number
                Object displayVal = (c == 0) ? (r + 1) : v;
                Component comp = super.getTableCellRendererComponent(t, displayVal, sel, foc, r, c);
                if (!sel) comp.setBackground(r % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);
                setFont(new Font("Segoe UI", Font.PLAIN, 12));
                if (!sel) comp.setForeground(AppColors.TEXT_PRIMARY);

                if (c == 0 || c == 1) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else if (c == 7) {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                    comp.setForeground(sel ? Color.WHITE : AppColors.SUCCESS);
                    setFont(new Font("Segoe UI", Font.BOLD, 12));
                } else if (c == 8) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                    String val = displayVal != null ? displayVal.toString() : "";
                    if (val.contains("hủy") || val.contains("Hủy")) {
                        comp.setForeground(AppColors.DANGER);
                        setFont(new Font("Segoe UI", Font.BOLD, 12));
                    } else {
                        comp.setForeground(sel ? Color.WHITE : AppColors.SUCCESS);
                        setFont(new Font("Segoe UI", Font.BOLD, 12));
                    }
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }
                return comp;
            }
        });

        // Double-click → detail dialog
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    int maHD = (int) tableModel.getValueAt(table.getSelectedRow(), 1);
                    showDetailDialog(maHD);
                }
            }
        });

        // Right-click → Void
        JPopupMenu popup = new JPopupMenu();
        JMenuItem menuDetail = new JMenuItem("Xem chi tiết");
        menuDetail.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        menuDetail.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r >= 0) showDetailDialog((int) tableModel.getValueAt(r, 1));
        });
        popup.add(menuDetail);

        popup.addSeparator();

        JMenuItem menuVoid = new JMenuItem("Hủy hóa đơn");
        menuVoid.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        menuVoid.setForeground(AppColors.DANGER);
        menuVoid.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r >= 0) doVoidInvoice((int) tableModel.getValueAt(r, 1));
        });
        popup.add(menuVoid);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) { tryPopup(e); }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) { tryPopup(e); }
            private void tryPopup(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        table.setRowSelectionInterval(row, row);
                        // Only show void option if not already voided
                        String status = tableModel.getValueAt(row, 8).toString();
                        menuVoid.setEnabled(!status.contains("hủy") && !status.contains("Hủy"));
                        popup.show(table, e.getX(), e.getY());
                    }
                }
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    // ================================================================
    //  PAGINATION
    // ================================================================
    private JPanel pageNumbersPanel;

    private JPanel createPaginationPanel() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 8));
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppColors.NEUTRAL_DARK));

        JButton btnFirst = createPageNavButton("|< Đầu");
        JButton btnPrev  = createPageNavButton("< Trước");
        pageNumbersPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        pageNumbersPanel.setOpaque(false);
        JButton btnNext  = createPageNavButton("Sau >");
        JButton btnLast  = createPageNavButton("Cuối >|");

        lblPageInfo = new JLabel();
        lblPageInfo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPageInfo.setForeground(AppColors.TEXT_PRIMARY);

        btnFirst.addActionListener(e -> { currentPage = 1; loadInvoices(); });
        btnPrev.addActionListener(e -> { if (currentPage > 1) { currentPage--; loadInvoices(); } });
        btnNext.addActionListener(e -> { if (currentPage < totalPages) { currentPage++; loadInvoices(); } });
        btnLast.addActionListener(e -> { currentPage = totalPages; loadInvoices(); });

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
    //  LOAD INVOICES (with filters + pagination)
    // ================================================================
    private void loadInvoices() {
        tableModel.setRowCount(0);

        StringBuilder where = new StringBuilder("WHERE 1=1 ");
        java.util.List<Object> params = new java.util.ArrayList<>();

        // Date filter
        String fromStr = txtFromDate.getText().trim();
        if (!fromStr.isEmpty()) {
            try {
                LocalDate from = LocalDate.parse(fromStr, DATE_FMT);
                where.append("AND hd.NgayBan >= ? ");
                params.add(Timestamp.valueOf(from.atStartOfDay()));
            } catch (Exception ignored) {}
        }
        String toStr = txtToDate.getText().trim();
        if (!toStr.isEmpty()) {
            try {
                LocalDate to = LocalDate.parse(toStr, DATE_FMT);
                where.append("AND hd.NgayBan < ? ");
                params.add(Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            } catch (Exception ignored) {}
        }

        // Customer name filter
        String khFilter = txtSearchKH.getText().trim();
        if (!khFilter.isEmpty()) {
            where.append("AND (kh.TenKH LIKE ? OR kh.SoDT LIKE ?) ");
            params.add("%" + khFilter + "%");
            params.add("%" + khFilter + "%");
        }

        // Invoice ID filter
        String maHDFilter = txtSearchMaHD.getText().trim();
        if (!maHDFilter.isEmpty()) {
            try {
                where.append("AND hd.MaHD = ? ");
                params.add(Integer.parseInt(maHDFilter));
            } catch (NumberFormatException ignored) {}
        }

        // Status filter
        int statusIdx = cboTrangThai.getSelectedIndex();
        if (statusIdx == 1) {
            where.append("AND hd.TrangThai = N'Thanh cong' ");
        } else if (statusIdx == 2) {
            where.append("AND hd.TrangThai = N'Da huy' ");
        }

        // Customer type filter (from tabs)
        if (customerTypeFilter == 1) {
            where.append("AND hd.MaKH IS NOT NULL ");
        } else if (customerTypeFilter == 2) {
            where.append("AND hd.MaKH IS NULL ");
        }

        String baseQuery =
            "FROM HoaDon hd " +
            "LEFT JOIN KhachHang kh ON hd.MaKH = kh.MaKH " +
            "JOIN NguoiDung nd ON hd.MaND = nd.MaND " +
            where;

        // Count
        String countSql = "SELECT COUNT(*) " + baseQuery;

        // Data
        String dataSql =
            "SELECT hd.MaHD, hd.NgayBan, " +
            "ISNULL(kh.TenKH, N'Khách vãng lai') AS TenKH, " +
            "ISNULL(kh.SoDT, '---') AS SoDT, " +
            "nd.HoTen AS TenNV, hd.PhuongThucTT, hd.TongTien, hd.TrangThai " +
            baseQuery +
            "ORDER BY hd.NgayBan DESC " +
            "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        try (Connection conn = DatabaseHelper.getConnection()) {
            // Count total
            try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                int idx = 1;
                for (Object p : params) {
                    if (p instanceof Timestamp) ps.setTimestamp(idx++, (Timestamp) p);
                    else if (p instanceof Integer) ps.setInt(idx++, (Integer) p);
                    else ps.setNString(idx++, p.toString());
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int total = rs.getInt(1);
                        totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
                        if (currentPage > totalPages) currentPage = totalPages;
                    }
                }
            }

            // Fetch page
            try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                int idx = 1;
                for (Object p : params) {
                    if (p instanceof Timestamp) ps.setTimestamp(idx++, (Timestamp) p);
                    else if (p instanceof Integer) ps.setInt(idx++, (Integer) p);
                    else ps.setNString(idx++, p.toString());
                }
                ps.setInt(idx++, (currentPage - 1) * PAGE_SIZE);
                ps.setInt(idx, PAGE_SIZE);

                int stt = (currentPage - 1) * PAGE_SIZE;
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        stt++;
                        Timestamp ts = rs.getTimestamp("NgayBan");
                        String ngayStr = ts != null
                                ? ts.toLocalDateTime().format(DATETIME_FMT)
                                : "---";
                        BigDecimal tien = rs.getBigDecimal("TongTien");
                        String tienStr = tien != null ? String.format("%,.0f VNĐ", tien) : "0";
                        String trangThai = rs.getNString("TrangThai");
                        String displayTT = "Thành công";
                        if (trangThai != null && trangThai.contains("huy")) displayTT = "Đã hủy";

                        tableModel.addRow(new Object[]{
                            stt,
                            rs.getInt("MaHD"),
                            ngayStr,
                            rs.getNString("TenKH"),
                            rs.getString("SoDT"),
                            rs.getNString("TenNV"),
                            rs.getNString("PhuongThucTT"),
                            tienStr,
                            displayTT
                        });
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi tải hóa đơn: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
        // Update pagination page numbers
        pageNumbersPanel.removeAll();
        int start = Math.max(1, currentPage - 2);
        int end = Math.min(totalPages, currentPage + 2);
        for (int p = start; p <= end; p++) {
            JButton pBtn = createPageNumButton(String.valueOf(p));
            if (p == currentPage) {
                pBtn.setBackground(AppColors.PRIMARY);
                pBtn.setForeground(Color.WHITE);
                pBtn.setBorder(BorderFactory.createLineBorder(AppColors.PRIMARY, 1));
            }
            int pg = p;
            pBtn.addActionListener(ev -> { currentPage = pg; loadInvoices(); });
            pageNumbersPanel.add(pBtn);
        }
        pageNumbersPanel.revalidate();
        pageNumbersPanel.repaint();

        lblPageInfo.setText("Trang " + currentPage + " / " + totalPages);
    }

    // ================================================================
    //  DETAIL DIALOG
    // ================================================================
    private void showDetailDialog(int maHD) {
        String sqlHeader =
            "SELECT hd.*, " +
            "ISNULL(kh.TenKH, N'Khách vãng lai') AS TenKH, " +
            "ISNULL(kh.SoDT, '---') AS SoDT, " +
            "nd.HoTen AS TenNV " +
            "FROM HoaDon hd " +
            "LEFT JOIN KhachHang kh ON hd.MaKH = kh.MaKH " +
            "JOIN NguoiDung nd ON hd.MaND = nd.MaND " +
            "WHERE hd.MaHD = ?";

        String sqlDetails =
            "SELECT ct.MaCTHD, sp.TenSP, l.SoLo, ct.SoLuong, ct.DonGia, ct.ThanhTien " +
            "FROM ChiTietHoaDon ct " +
            "JOIN SanPham sp ON ct.MaSP = sp.MaSP " +
            "JOIN LoHang l ON ct.MaLo = l.MaLo " +
            "WHERE ct.MaHD = ?";

        try (Connection conn = DatabaseHelper.getConnection()) {
            // Header info
            Invoice inv = null;
            try (PreparedStatement ps = conn.prepareStatement(sqlHeader)) {
                ps.setInt(1, maHD);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        inv = new Invoice();
                        inv.setMaHD(rs.getInt("MaHD"));
                        Timestamp ts = rs.getTimestamp("NgayBan");
                        if (ts != null) inv.setNgayBan(ts.toLocalDateTime());
                        inv.setTongTien(rs.getBigDecimal("TongTien"));
                        inv.setPhuongThucTT(rs.getNString("PhuongThucTT"));
                        inv.setTrangThai(rs.getNString("TrangThai"));
                        inv.setLyDoHuy(rs.getNString("LyDoHuy"));
                        inv.setTenKH(rs.getNString("TenKH"));
                        inv.setSoDT(rs.getString("SoDT"));
                        inv.setTenNhanVien(rs.getNString("TenNV"));
                    }
                }
            }
            if (inv == null) return;

            // Detail items — NO Giá vốn (internal data)
            String[] detailCols = {"Tên sản phẩm", "Số Lô", "Số lượng", "Đơn giá", "Thành tiền"};
            DefaultTableModel detailModel = new DefaultTableModel(detailCols, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            try (PreparedStatement ps = conn.prepareStatement(sqlDetails)) {
                ps.setInt(1, maHD);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        BigDecimal donGia = rs.getBigDecimal("DonGia");
                        BigDecimal thanhTien = rs.getBigDecimal("ThanhTien");
                        detailModel.addRow(new Object[]{
                            rs.getNString("TenSP"),
                            rs.getNString("SoLo"),
                            rs.getInt("SoLuong"),
                            donGia != null ? String.format("%,.0f VNĐ", donGia) : "---",
                            thanhTien != null ? String.format("%,.0f VNĐ", thanhTien) : "---"
                        });
                    }
                }
            }

            // Build dialog
            JDialog dlg = new JDialog(
                    (Frame) SwingUtilities.getWindowAncestor(this),
                    "Chi tiết hóa đơn #" + maHD, true);
            dlg.setSize(800, 500);
            dlg.setLocationRelativeTo(this);

            JPanel content = new JPanel(new BorderLayout());
            content.setBackground(Color.WHITE);

            // Header
            boolean isVoided = inv.getTrangThai() != null && inv.getTrangThai().contains("huy");
            JPanel hdr = new JPanel(new BorderLayout());
            hdr.setBackground(isVoided ? AppColors.DANGER : new Color(0x17, 0xA2, 0xB8));
            hdr.setBorder(new EmptyBorder(14, 20, 14, 20));

            JLabel lblH = new JLabel("Hóa đơn #" + maHD + (isVoided ? " — ĐÃ HỦY" : ""));
            lblH.setFont(new Font("Segoe UI", Font.BOLD, 15));
            lblH.setForeground(Color.WHITE);
            hdr.add(lblH, BorderLayout.WEST);
            content.add(hdr, BorderLayout.NORTH);

            // Info + Table
            JPanel body = new JPanel(new BorderLayout());
            body.setBackground(Color.WHITE);

            // Info grid
            JPanel info = new JPanel(new GridLayout(0, 2, 10, 4));
            info.setBackground(Color.WHITE);
            info.setBorder(new EmptyBorder(12, 20, 12, 20));
            info.add(makeInfoLabel("Ngày bán:"));
            info.add(makeInfoValue(inv.getNgayBan() != null ? inv.getNgayBan().format(DATETIME_FMT) : "---"));
            info.add(makeInfoLabel("Khách hàng:"));
            info.add(makeInfoValue(inv.getTenKH() + " (" + inv.getSoDT() + ")"));
            info.add(makeInfoLabel("Nhân viên:"));
            info.add(makeInfoValue(inv.getTenNhanVien()));
            info.add(makeInfoLabel("Phương thức:"));
            info.add(makeInfoValue(inv.getPhuongThucTT()));
            info.add(makeInfoLabel("Tổng tiền:"));
            JLabel lblTotal = makeInfoValue(inv.getTongTien() != null ? String.format("%,.0f VNĐ", inv.getTongTien()) : "0");
            lblTotal.setForeground(AppColors.SUCCESS);
            lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 14));
            info.add(lblTotal);

            if (isVoided && inv.getLyDoHuy() != null) {
                info.add(makeInfoLabel("Lý do hủy:"));
                JLabel lblReason = makeInfoValue(inv.getLyDoHuy());
                lblReason.setForeground(AppColors.DANGER);
                info.add(lblReason);
            }

            body.add(info, BorderLayout.NORTH);

            // Detail table
            JTable detailTable = new JTable(detailModel);
            detailTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            detailTable.setRowHeight(28);
            detailTable.setShowGrid(false);
            detailTable.setFillsViewportHeight(true);
            detailTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
            detailTable.getTableHeader().setBackground(AppColors.TABLE_HEADER_BG);
            detailTable.getTableHeader().setForeground(AppColors.TABLE_HEADER_FG);

            JScrollPane sp = new JScrollPane(detailTable);
            sp.setBorder(new EmptyBorder(0, 20, 12, 20));
            body.add(sp, BorderLayout.CENTER);

            content.add(body, BorderLayout.CENTER);
            dlg.setContentPane(content);
            dlg.setVisible(true);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi xem chi tiết: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JLabel makeInfoLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(AppColors.TEXT_SECONDARY);
        return lbl;
    }

    private JLabel makeInfoValue(String text) {
        JLabel lbl = new JLabel(text != null ? text : "---");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(AppColors.TEXT_PRIMARY);
        return lbl;
    }

    // ================================================================
    //  VOID INVOICE (Hủy hóa đơn + hoàn kho)
    // ================================================================
    private void doVoidInvoice(int maHD) {
        // Check current status
        int row = table.getSelectedRow();
        if (row < 0) return;
        String currentStatus = tableModel.getValueAt(row, 8).toString();
        if (currentStatus.contains("hủy") || currentStatus.contains("Hủy")) {
            JOptionPane.showMessageDialog(this, "Hóa đơn này đã bị hủy trước đó.",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Ask reason
        String lyDo = JOptionPane.showInputDialog(this,
                "Nhập lý do hủy hóa đơn #" + maHD + ":",
                "Hủy hóa đơn", JOptionPane.WARNING_MESSAGE);
        if (lyDo == null || lyDo.trim().isEmpty()) return;

        // Confirm
        int confirm = JOptionPane.showConfirmDialog(this,
                "Xác nhận HỦY hóa đơn #" + maHD + "?\n\n" +
                "Lý do: " + lyDo + "\n\n" +
                "Thao tác này sẽ:\n" +
                "- Đổi trạng thái hóa đơn thành 'Đã hủy'\n" +
                "- Hoàn trả số lượng tồn kho về các lô hàng\n\n" +
                "Không thể hoàn tác!",
                "Xác nhận hủy", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        // Execute transaction
        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Update invoice status
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE HoaDon SET TrangThai = N'Da huy', LyDoHuy = ? WHERE MaHD = ? AND TrangThai = N'Thanh cong'")) {
                    ps.setNString(1, lyDo.trim());
                    ps.setInt(2, maHD);
                    int updated = ps.executeUpdate();
                    if (updated == 0) throw new SQLException("Hóa đơn đã bị hủy hoặc không tồn tại.");
                }

                // 2. Restore batch quantities (hoàn kho)
                String sqlDetails = "SELECT MaLo, SoLuong FROM ChiTietHoaDon WHERE MaHD = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlDetails)) {
                    ps.setInt(1, maHD);
                    try (ResultSet rs = ps.executeQuery()) {
                        PreparedStatement psUpdate = conn.prepareStatement(
                                "UPDATE LoHang SET SoLuong = SoLuong + ? WHERE MaLo = ?");
                        while (rs.next()) {
                            int maLo = rs.getInt("MaLo");
                            int soLuong = rs.getInt("SoLuong");
                            psUpdate.setInt(1, soLuong);
                            psUpdate.setInt(2, maLo);
                            psUpdate.executeUpdate();
                        }
                        psUpdate.close();
                    }
                }

                conn.commit();
                JOptionPane.showMessageDialog(this,
                        "Đã hủy hóa đơn #" + maHD + " thành công.\nSố lượng tồn kho đã được hoàn trả.",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                loadInvoices(); // refresh

            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi hủy hóa đơn: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
