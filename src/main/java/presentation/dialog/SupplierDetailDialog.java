package presentation.dialog;

import common.AppColors;
import infrastructure.database.DatabaseHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Dialog: Chi tiết nhà cung cấp — lịch sử nhập hàng + danh sách lô hàng
 * ★ Sortable columns on both tables via header click
 */
public class SupplierDetailDialog extends JDialog {

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final int PAGE_SIZE = 5;

    // === Import Ticket sort + pagination state ===
    private int ticketSortCol = 2;   // default: Ngày Nhập
    private boolean ticketSortAsc = false;
    private int ticketPage = 1;
    private int ticketTotalPages = 1;
    private DefaultTableModel ticketModel;
    private JTable ticketTable;
    private final List<Object[]> ticketData = new ArrayList<>();
    private JLabel ticketPageLabel;
    private JButton ticketBtnFirst, ticketBtnPrev, ticketBtnNext, ticketBtnLast;

    // === Batch sort + pagination state ===
    private int batchSortCol = 0;    // default: STT
    private boolean batchSortAsc = true;
    private int batchPage = 1;
    private int batchTotalPages = 1;
    private DefaultTableModel batchModel;
    private JTable batchTable;
    private final List<Object[]> batchData = new ArrayList<>();
    private JLabel batchPageLabel;
    private JButton batchBtnFirst, batchBtnPrev, batchBtnNext, batchBtnLast;

    private final int maNCC;

    public SupplierDetailDialog(Frame parent, int maNCC, String tenNCC) {
        super(parent, "Chi tiết NCC: " + tenNCC, true);
        this.maNCC = maNCC;
        setSize(950, 620);
        setLocationRelativeTo(parent);
        setResizable(true);

        JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
        contentPanel.setBackground(AppColors.NEUTRAL);
        contentPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // === TOP: NCC Info ===
        contentPanel.add(createInfoPanel(), BorderLayout.NORTH);

        // === CENTER: 2 Tables stacked ===
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(200);
        splitPane.setDividerSize(4);
        splitPane.setBorder(null);

        loadTicketData();
        loadBatchData();

        splitPane.setTopComponent(createImportTicketPanel());
        splitPane.setBottomComponent(createBatchPanel());
        contentPanel.add(splitPane, BorderLayout.CENTER);

        // === BOTTOM: Close ===
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottomPanel.setOpaque(false);
        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClose.setBackground(AppColors.PRIMARY);
        btnClose.setForeground(Color.WHITE);
        btnClose.setFocusPainted(false);
        btnClose.setBorderPainted(false);
        btnClose.setPreferredSize(new Dimension(100, 34));
        btnClose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dispose());
        bottomPanel.add(btnClose);
        contentPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(contentPanel);

        // Initial sort render
        refreshTicketTable();
        refreshBatchTable();
    }

    // ================================================================
    //  DATA LOADING
    // ================================================================

    private void loadTicketData() {
        ticketData.clear();
        String sql = "SELECT p.MaPN, p.NgayNhap, nd.HoTen, p.TongTien, p.GhiChu " +
                "FROM PhieuNhap p " +
                "LEFT JOIN NguoiDung nd ON p.MaND = nd.MaND " +
                "WHERE p.MaNCC = ? " +
                "ORDER BY p.NgayNhap DESC";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maNCC);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // ★ Store LocalDateTime for correct chronological sorting
                    LocalDateTime ngayNhap = null;
                    Timestamp ts = rs.getTimestamp("NgayNhap");
                    if (ts != null) {
                        ngayNhap = ts.toLocalDateTime();
                    }
                    BigDecimal tongTien = rs.getBigDecimal("TongTien");
                    if (tongTien == null) tongTien = BigDecimal.ZERO;
                    ticketData.add(new Object[]{
                            rs.getInt("MaPN"),
                            "PN-" + rs.getInt("MaPN"),
                            ngayNhap,   // LocalDateTime (sortable)
                            rs.getNString("HoTen") != null ? rs.getNString("HoTen") : "---",
                            tongTien,
                            rs.getNString("GhiChu") != null ? rs.getNString("GhiChu") : ""
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadBatchData() {
        batchData.clear();
        String sql = "SELECT l.SoLo, sp.TenSP, sp.DonViTinh, l.SoLuong, l.GiaNhap, l.HanSuDung, l.MaPN " +
                "FROM LoHang l " +
                "JOIN SanPham sp ON l.MaSP = sp.MaSP " +
                "JOIN PhieuNhap p ON l.MaPN = p.MaPN " +
                "WHERE p.MaNCC = ? " +
                "ORDER BY p.NgayNhap DESC, l.MaLo ASC";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maNCC);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // ★ Store LocalDate for correct chronological sorting
                    LocalDate hsd = null;
                    java.sql.Date hsdDate = rs.getDate("HanSuDung");
                    if (hsdDate != null) {
                        hsd = hsdDate.toLocalDate();
                    }
                    BigDecimal giaNhap = rs.getBigDecimal("GiaNhap");
                    if (giaNhap == null) giaNhap = BigDecimal.ZERO;
                    batchData.add(new Object[]{
                            rs.getNString("SoLo"),
                            rs.getNString("TenSP"),
                            rs.getNString("DonViTinh"),
                            rs.getInt("SoLuong"),
                            giaNhap,
                            hsd,        // LocalDate (sortable)
                            "PN-" + rs.getInt("MaPN")
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================================================================
    //  NCC Info Panel
    // ================================================================

    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 4, 16, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AppColors.NEUTRAL_DARK, 1),
                new EmptyBorder(14, 18, 14, 18)
        ));

        String sql = "SELECT MaNCC, TenNCC, SoDT, DiaChi, Email, TrangThai, NgayTao, " +
                "(SELECT COUNT(*) FROM PhieuNhap WHERE MaNCC = n.MaNCC) AS SoPhieuNhap, " +
                "(SELECT ISNULL(SUM(l.SoLuong), 0) FROM LoHang l JOIN PhieuNhap p ON l.MaPN = p.MaPN WHERE p.MaNCC = n.MaNCC) AS TongSoLuong " +
                "FROM NhaCungCap n WHERE MaNCC = ?";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maNCC);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    addInfoField(panel, "Mã NCC", String.valueOf(rs.getInt("MaNCC")));
                    addInfoField(panel, "Tên NCC", rs.getNString("TenNCC"));
                    addInfoField(panel, "Số ĐT", rs.getString("SoDT") != null ? rs.getString("SoDT") : "---");
                    addInfoField(panel, "Email", rs.getString("Email") != null ? rs.getString("Email") : "---");
                    addInfoField(panel, "Địa chỉ", rs.getNString("DiaChi") != null ? rs.getNString("DiaChi") : "---");
                    boolean tt = rs.getBoolean("TrangThai");
                    addInfoFieldColored(panel, "Trạng thái",
                            tt ? "Hoạt động" : "Ngừng HT",
                            tt ? AppColors.SUCCESS : AppColors.DANGER);

                    int soPhieu = rs.getInt("SoPhieuNhap");
                    addInfoFieldColored(panel, "Tổng phiếu nhập",
                            String.format("%,d lần", soPhieu),
                            soPhieu > 0 ? AppColors.PRIMARY : AppColors.TEXT_SECONDARY);

                    int tongSL = rs.getInt("TongSoLuong");
                    addInfoFieldColored(panel, "Tổng SL đã nhập",
                            String.format("%,d", tongSL),
                            tongSL > 0 ? AppColors.SUCCESS : AppColors.TEXT_SECONDARY);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return panel;
    }

    // ================================================================
    //  Import Tickets Table (sortable)
    // ================================================================

    private JPanel createImportTicketPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 4));
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(new EmptyBorder(4, 0, 0, 0));

        JLabel lblTitle = new JLabel("  \uD83D\uDCE6 Lịch Sử Phiếu Nhập");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(AppColors.PRIMARY);
        wrapper.add(lblTitle, BorderLayout.NORTH);

        String[] cols = {"STT", "Mã PN", "Ngày Nhập", "Người Nhập", "Tổng Tiền (VNĐ)", "Ghi Chú"};
        ticketModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        ticketTable = createStyledTable(ticketModel);

        // Column widths
        ticketTable.getColumnModel().getColumn(0).setPreferredWidth(35);
        ticketTable.getColumnModel().getColumn(0).setMaxWidth(45);
        ticketTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        ticketTable.getColumnModel().getColumn(2).setPreferredWidth(130);
        ticketTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        ticketTable.getColumnModel().getColumn(4).setPreferredWidth(120);
        ticketTable.getColumnModel().getColumn(5).setPreferredWidth(200);

        // ★ Sort: all columns except STT(0)
        ticketTable.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = ticketTable.columnAtPoint(e.getPoint());
                if (col <= 0) return;
                if (ticketSortCol == col) {
                    ticketSortAsc = !ticketSortAsc;
                } else {
                    ticketSortCol = col;
                    ticketSortAsc = true;
                }
                ticketPage = 1;
                refreshTicketTable();
            }
        });

        JScrollPane scroll = new JScrollPane(ticketTable);
        scroll.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1));
        wrapper.add(scroll, BorderLayout.CENTER);

        // ★ Pagination
        ticketPageLabel = new JLabel();
        ticketBtnFirst = createPageBtn("\u00AB");
        ticketBtnPrev  = createPageBtn("\u2039");
        ticketBtnNext  = createPageBtn("\u203A");
        ticketBtnLast  = createPageBtn("\u00BB");
        ticketBtnFirst.addActionListener(e -> { ticketPage = 1; refreshTicketTable(); });
        ticketBtnPrev .addActionListener(e -> { if (ticketPage > 1) { ticketPage--; refreshTicketTable(); } });
        ticketBtnNext .addActionListener(e -> { if (ticketPage < ticketTotalPages) { ticketPage++; refreshTicketTable(); } });
        ticketBtnLast .addActionListener(e -> { ticketPage = ticketTotalPages; refreshTicketTable(); });
        wrapper.add(buildPageBar(ticketBtnFirst, ticketBtnPrev, ticketPageLabel, ticketBtnNext, ticketBtnLast), BorderLayout.SOUTH);

        return wrapper;
    }

    private void refreshTicketTable() {
        List<Object[]> sorted = new ArrayList<>(ticketData);
        sorted.sort(buildComparator(ticketSortCol, ticketSortAsc, true));

        ticketTotalPages = Math.max(1, (int) Math.ceil((double) sorted.size() / PAGE_SIZE));
        if (ticketPage > ticketTotalPages) ticketPage = ticketTotalPages;

        int from = (ticketPage - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, sorted.size());

        ticketModel.setRowCount(0);
        if (sorted.isEmpty()) {
            ticketModel.addRow(new Object[]{"", "", "Chưa có phiếu nhập nào", "", "", ""});
        } else {
            for (int i = from; i < to; i++) {
                Object[] row = sorted.get(i);
                String ngayNhapStr = "---";
                if (row[2] instanceof LocalDateTime) {
                    ngayNhapStr = ((LocalDateTime) row[2]).format(DATETIME_FMT);
                }
                ticketModel.addRow(new Object[]{
                        i + 1,
                        row[1],
                        ngayNhapStr,
                        row[3],
                        new java.text.DecimalFormat("#,##0").format(row[4]),
                        row[5]
                });
            }
        }
        updatePageControls(ticketPageLabel, ticketBtnFirst, ticketBtnPrev,
                ticketBtnNext, ticketBtnLast, ticketPage, ticketTotalPages, sorted.size());
    }

    // ================================================================
    //  Batches Table (sortable)
    // ================================================================

    private JPanel createBatchPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 4));
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(new EmptyBorder(4, 0, 0, 0));

        JLabel lblTitle = new JLabel("  \uD83D\uDCCB Danh Sách Lô Hàng Đã Nhập");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(AppColors.PRIMARY);
        wrapper.add(lblTitle, BorderLayout.NORTH);

        String[] cols = {"STT", "Số Lô", "Tên Sản Phẩm", "ĐVT", "SL Nhập", "Giá Nhập", "Hạn SD", "Mã PN"};
        batchModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        batchTable = createStyledTable(batchModel);

        // Column widths
        batchTable.getColumnModel().getColumn(0).setPreferredWidth(35);
        batchTable.getColumnModel().getColumn(0).setMaxWidth(45);
        batchTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        batchTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        batchTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        batchTable.getColumnModel().getColumn(4).setPreferredWidth(65);
        batchTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        batchTable.getColumnModel().getColumn(6).setPreferredWidth(85);
        batchTable.getColumnModel().getColumn(7).setPreferredWidth(70);

        // ★ Sort: all columns except STT(0)
        batchTable.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = batchTable.columnAtPoint(e.getPoint());
                if (col <= 0) return;
                if (batchSortCol == col) {
                    batchSortAsc = !batchSortAsc;
                } else {
                    batchSortCol = col;
                    batchSortAsc = true;
                }
                batchPage = 1;
                refreshBatchTable();
            }
        });

        JScrollPane scroll = new JScrollPane(batchTable);
        scroll.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1));
        wrapper.add(scroll, BorderLayout.CENTER);

        // ★ Pagination
        batchPageLabel = new JLabel();
        batchBtnFirst = createPageBtn("\u00AB");
        batchBtnPrev  = createPageBtn("\u2039");
        batchBtnNext  = createPageBtn("\u203A");
        batchBtnLast  = createPageBtn("\u00BB");
        batchBtnFirst.addActionListener(e -> { batchPage = 1; refreshBatchTable(); });
        batchBtnPrev .addActionListener(e -> { if (batchPage > 1) { batchPage--; refreshBatchTable(); } });
        batchBtnNext .addActionListener(e -> { if (batchPage < batchTotalPages) { batchPage++; refreshBatchTable(); } });
        batchBtnLast .addActionListener(e -> { batchPage = batchTotalPages; refreshBatchTable(); });
        wrapper.add(buildPageBar(batchBtnFirst, batchBtnPrev, batchPageLabel, batchBtnNext, batchBtnLast), BorderLayout.SOUTH);

        return wrapper;
    }

    private void refreshBatchTable() {
        List<Object[]> sorted = new ArrayList<>(batchData);
        sorted.sort(buildComparator(batchSortCol, batchSortAsc, false));

        batchTotalPages = Math.max(1, (int) Math.ceil((double) sorted.size() / PAGE_SIZE));
        if (batchPage > batchTotalPages) batchPage = batchTotalPages;

        int from = (batchPage - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, sorted.size());

        batchModel.setRowCount(0);
        if (sorted.isEmpty()) {
            batchModel.addRow(new Object[]{"", "", "Chưa có lô hàng nào", "", "", "", "", ""});
        } else {
            for (int i = from; i < to; i++) {
                Object[] row = sorted.get(i);
                String hsdStr = "---";
                if (row[5] instanceof LocalDate) {
                    hsdStr = ((LocalDate) row[5]).format(DATE_FMT);
                }
                batchModel.addRow(new Object[]{
                        i + 1,
                        row[0],
                        row[1],
                        row[2],
                        row[3],
                        new java.text.DecimalFormat("#,##0").format(row[4]),
                        hsdStr,
                        row[6]
                });
            }
        }
        updatePageControls(batchPageLabel, batchBtnFirst, batchBtnPrev,
                batchBtnNext, batchBtnLast, batchPage, batchTotalPages, sorted.size());
    }

    // ================================================================
    //  SORT COMPARATOR
    // ================================================================

    /**
     * Build a comparator for sorting the data arrays.
     * @param col  column index in the JTable (includes STT at 0)
     * @param asc  ascending or descending
     * @param isTicket  true = ticket data layout, false = batch data layout
     */
    private Comparator<Object[]> buildComparator(int col, boolean asc, boolean isTicket) {
        // Map table col → data array index
        int dataIdx;
        if (isTicket) {
            // Table: STT(0), MaPN(1), NgayNhap(2), NguoiNhap(3), TongTien(4), GhiChu(5)
            // Data:  [MaPN(0-int), MaPNStr(1), NgayNhap(2), NguoiNhap(3), TongTien(4-BigDecimal), GhiChu(5)]
            switch (col) {
                case 1: dataIdx = 0; break; // MaPN (int)
                case 2: dataIdx = 2; break; // NgayNhap (string)
                case 3: dataIdx = 3; break; // NguoiNhap (string)
                case 4: dataIdx = 4; break; // TongTien (BigDecimal)
                case 5: dataIdx = 5; break; // GhiChu (string)
                default: dataIdx = 0;
            }
        } else {
            // Table: STT(0), SoLo(1), TenSP(2), DVT(3), SLNhap(4), GiaNhap(5), HSD(6), MaPN(7)
            // Data:  [SoLo(0), TenSP(1), DVT(2), SoLuong(3-int), GiaNhap(4-BigDecimal), HSD(5), MaPNStr(6)]
            switch (col) {
                case 1: dataIdx = 0; break; // SoLo (string)
                case 2: dataIdx = 1; break; // TenSP (string)
                case 3: dataIdx = 2; break; // DVT (string)
                case 4: dataIdx = 3; break; // SoLuong (int)
                case 5: dataIdx = 4; break; // GiaNhap (BigDecimal)
                case 6: dataIdx = 5; break; // HSD (string)
                case 7: dataIdx = 6; break; // MaPN (string)
                default: dataIdx = 0;
            }
        }

        final int idx = dataIdx;
        Comparator<Object[]> cmp = (a, b) -> {
            Object va = a[idx];
            Object vb = b[idx];
            if (va == null && vb == null) return 0;
            if (va == null) return -1;
            if (vb == null) return 1;
            if (va instanceof Integer) return Integer.compare((int) va, (int) vb);
            if (va instanceof BigDecimal) return ((BigDecimal) va).compareTo((BigDecimal) vb);
            if (va instanceof LocalDate) return ((LocalDate) va).compareTo((LocalDate) vb);
            if (va instanceof LocalDateTime) return ((LocalDateTime) va).compareTo((LocalDateTime) vb);
            return va.toString().compareToIgnoreCase(vb.toString());
        };

        return asc ? cmp : cmp.reversed();
    }

    // ================================================================
    //  UI Helpers
    // ================================================================

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(30);
        table.setShowGrid(true);
        table.setGridColor(AppColors.NEUTRAL_DARK);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(false);

        JTableHeader th = table.getTableHeader();
        th.setBackground(AppColors.TABLE_HEADER_BG);
        th.setForeground(AppColors.TABLE_HEADER_FG);
        th.setFont(new Font("Segoe UI", Font.BOLD, 12));
        th.setPreferredSize(new Dimension(0, 34));
        th.setReorderingAllowed(false);
        th.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Alt-row renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) c.setBackground(row % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);
                // Center align STT
                if (col == 0) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }
                return c;
            }
        });

        return table;
    }

    private void addInfoField(JPanel panel, String label, String value) {
        JPanel field = new JPanel(new BorderLayout());
        field.setOpaque(false);
        JLabel lblKey = new JLabel(label);
        lblKey.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblKey.setForeground(AppColors.TEXT_SECONDARY);
        JLabel lblVal = new JLabel(value);
        lblVal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblVal.setForeground(AppColors.TEXT_PRIMARY);
        field.add(lblKey, BorderLayout.NORTH);
        field.add(lblVal, BorderLayout.CENTER);
        panel.add(field);
    }

    private void addInfoFieldColored(JPanel panel, String label, String value, Color color) {
        JPanel field = new JPanel(new BorderLayout());
        field.setOpaque(false);
        JLabel lblKey = new JLabel(label);
        lblKey.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblKey.setForeground(AppColors.TEXT_SECONDARY);
        JLabel lblVal = new JLabel(value);
        lblVal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblVal.setForeground(color);
        field.add(lblKey, BorderLayout.NORTH);
        field.add(lblVal, BorderLayout.CENTER);
        panel.add(field);
    }

    // ================================================================
    //  PAGINATION HELPERS
    // ================================================================

    private JButton createPageBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setBackground(AppColors.NEUTRAL);
        btn.setForeground(AppColors.PRIMARY);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(36, 28));
        btn.setMargin(new Insets(0, 0, 0, 0));
        return btn;
    }

    private JPanel buildPageBar(JButton first, JButton prev, JLabel lbl, JButton next, JButton last) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
        bar.setOpaque(false);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(AppColors.TEXT_SECONDARY);
        bar.add(first);
        bar.add(prev);
        bar.add(lbl);
        bar.add(next);
        bar.add(last);
        return bar;
    }

    private void updatePageControls(JLabel lbl, JButton first, JButton prev,
                                     JButton next, JButton last,
                                     int page, int total, int dataSize) {
        lbl.setText("Trang " + page + " / " + total + "  (" + dataSize + " dòng)");
        first.setEnabled(page > 1);
        prev.setEnabled(page > 1);
        next.setEnabled(page < total);
        last.setEnabled(page < total);
    }
}
