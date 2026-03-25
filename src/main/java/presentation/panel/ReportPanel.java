package presentation.panel;

import common.AppColors;
import infrastructure.database.DatabaseHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Panel Thống Kê & Báo Cáo
 *
 * Layout:
 *   TOP:    4 stat cards (Doanh thu hôm nay, Tháng này, Tổng SP, Lô sắp hết hạn)
 *   CENTER: 2 tables side-by-side:
 *     LEFT:  Top 10 SP bán chạy (tháng này)
 *     RIGHT: Lô hàng sắp hết hạn (< 30 ngày)
 *   BOTTOM: Doanh thu 7 ngày gần nhất (table)
 */
public class ReportPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0");

    // Stat card labels
    private JLabel lblRevenueToday, lblRevenueMonth, lblTotalProducts, lblExpiringBatches;

    public ReportPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(AppColors.NEUTRAL);
        initComponents();
        loadData();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                loadData();
            }
        });
    }

    private void initComponents() {
        // === HEADER ===
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel lblTitle = new JLabel("Thống Kê & Báo Cáo");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(AppColors.TEXT_PRIMARY);
        header.add(lblTitle, BorderLayout.WEST);

        JLabel lblDate = new JLabel("Cập nhật: " + LocalDate.now().format(DATE_FMT));
        lblDate.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblDate.setForeground(AppColors.TEXT_SECONDARY);
        header.add(lblDate, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // === CENTER ===
        JPanel centerWrapper = new JPanel(new BorderLayout(0, 12));
        centerWrapper.setBackground(AppColors.NEUTRAL);
        centerWrapper.setBorder(new EmptyBorder(12, 16, 12, 16));

        // --- Stat cards ---
        centerWrapper.add(createStatCardsPanel(), BorderLayout.NORTH);

        // --- Tables ---
        JPanel tablesWrapper = new JPanel(new BorderLayout(0, 12));
        tablesWrapper.setOpaque(false);

        // Top row: 2 tables side-by-side
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(500);
        splitPane.setDividerSize(8);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);
        splitPane.setLeftComponent(createTopProductsPanel());
        splitPane.setRightComponent(createExpiringBatchesPanel());
        tablesWrapper.add(splitPane, BorderLayout.CENTER);

        // Bottom: Revenue last 7 days
        tablesWrapper.add(createRevenueHistoryPanel(), BorderLayout.SOUTH);

        centerWrapper.add(tablesWrapper, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);
    }

    // ================================================================
    //  STAT CARDS (4 cards in a row)
    // ================================================================

    private JPanel createStatCardsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 12, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        lblRevenueToday = new JLabel("0 VNĐ");
        panel.add(createStatCard("Doanh Thu Hôm Nay", lblRevenueToday,
                AppColors.PRIMARY, new Color(0xE3, 0xEF, 0xFA)));

        lblRevenueMonth = new JLabel("0 VNĐ");
        panel.add(createStatCard("Doanh Thu Tháng Này", lblRevenueMonth,
                AppColors.SUCCESS, new Color(0xD4, 0xED, 0xDA)));

        lblTotalProducts = new JLabel("0");
        panel.add(createStatCard("Tổng Sản Phẩm", lblTotalProducts,
                new Color(0x17, 0xA2, 0xB8), new Color(0xD1, 0xEC, 0xF1)));

        lblExpiringBatches = new JLabel("0");
        panel.add(createStatCard("Lô Sắp Hết Hạn", lblExpiringBatches,
                AppColors.DANGER, new Color(0xF8, 0xD7, 0xDA)));

        return panel;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accentColor, Color bgColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(bgColor);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(accentColor.brighter(), 1),
                new EmptyBorder(14, 18, 14, 18)
        ));

        // Left accent strip
        JPanel accent = new JPanel();
        accent.setPreferredSize(new Dimension(4, 0));
        accent.setBackground(accentColor);
        card.add(accent, BorderLayout.WEST);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);
        inner.setBorder(new EmptyBorder(0, 10, 0, 0));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTitle.setForeground(accentColor.darker());
        lblTitle.setAlignmentX(LEFT_ALIGNMENT);
        inner.add(lblTitle);
        inner.add(Box.createRigidArea(new Dimension(0, 6)));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(accentColor);
        valueLabel.setAlignmentX(LEFT_ALIGNMENT);
        inner.add(valueLabel);

        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    // ================================================================
    //  TOP 10 PRODUCTS TABLE
    // ================================================================

    private JPanel topProductsPanel;
    private DefaultTableModel topProductsModel;

    private JPanel createTopProductsPanel() {
        topProductsPanel = new JPanel(new BorderLayout(0, 6));
        topProductsPanel.setBackground(Color.WHITE);
        topProductsPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AppColors.NEUTRAL_DARK, 1),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel lblTitle = new JLabel("🏆 Top 10 SP Bán Chạy (Tháng Này)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(AppColors.PRIMARY);
        topProductsPanel.add(lblTitle, BorderLayout.NORTH);

        String[] cols = {"#", "Tên SP", "ĐVT", "SL Bán", "Doanh Thu"};
        topProductsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = createStyledTable(topProductsModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(60);
        table.getColumnModel().getColumn(3).setPreferredWidth(70);
        table.getColumnModel().getColumn(4).setPreferredWidth(110);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1));
        topProductsPanel.add(scroll, BorderLayout.CENTER);

        return topProductsPanel;
    }

    // ================================================================
    //  EXPIRING BATCHES TABLE
    // ================================================================

    private DefaultTableModel expiringModel;

    private JPanel createExpiringBatchesPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AppColors.NEUTRAL_DARK, 1),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel lblTitle = new JLabel("⚠ Lô Hàng Sắp Hết Hạn (< 30 ngày)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(AppColors.DANGER);
        panel.add(lblTitle, BorderLayout.NORTH);

        String[] cols = {"#", "Tên SP", "Số Lô", "SL Tồn", "Hạn SD", "Còn"};
        expiringModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = createStyledTable(expiringModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(60);
        table.getColumnModel().getColumn(4).setPreferredWidth(85);
        table.getColumnModel().getColumn(5).setPreferredWidth(60);

        // Color the "Còn" column
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            { setHorizontalAlignment(SwingConstants.CENTER); }
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel && val != null) {
                    String text = val.toString();
                    if (text.contains("HẾT")) {
                        c.setForeground(AppColors.DANGER);
                        setFont(getFont().deriveFont(Font.BOLD));
                        c.setBackground(new Color(0xF8, 0xD7, 0xDA));
                    } else {
                        c.setForeground(new Color(0x85, 0x6D, 0x04));
                        c.setBackground(new Color(0xFF, 0xF3, 0xCD));
                        setFont(getFont().deriveFont(Font.BOLD));
                    }
                }
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ================================================================
    //  REVENUE HISTORY TABLE (7 days)
    // ================================================================

    private DefaultTableModel revenueModel;

    private JPanel createRevenueHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AppColors.NEUTRAL_DARK, 1),
                new EmptyBorder(12, 14, 12, 14)
        ));
        panel.setPreferredSize(new Dimension(0, 200));

        JLabel lblTitle = new JLabel("📊 Doanh Thu 7 Ngày Gần Nhất");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(AppColors.PRIMARY);
        panel.add(lblTitle, BorderLayout.NORTH);

        String[] cols = {"Ngày", "Số Hóa Đơn", "Tổng Doanh Thu (VNĐ)", "Trung Bình / HĐ"};
        revenueModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = createStyledTable(revenueModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);

        // Right-align money columns
        DefaultTableCellRenderer rightR = new DefaultTableCellRenderer();
        rightR.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(2).setCellRenderer(rightR);
        table.getColumnModel().getColumn(3).setCellRenderer(rightR);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ================================================================
    //  DATA LOADING
    // ================================================================

    private void loadData() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try (Connection conn = DatabaseHelper.getConnection()) {
            loadStatCards(conn);
            loadTopProducts(conn);
            loadExpiringBatches(conn);
            loadRevenueHistory(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        setCursor(Cursor.getDefaultCursor());
    }

    private void loadStatCards(Connection conn) throws SQLException {
        // 1. Doanh thu hôm nay
        String sql1 = "SELECT ISNULL(SUM(TongTien), 0) FROM HoaDon WHERE CAST(NgayBan AS DATE) = CAST(GETDATE() AS DATE)";
        try (PreparedStatement ps = conn.prepareStatement(sql1);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                BigDecimal val = rs.getBigDecimal(1);
                if (val == null) val = BigDecimal.ZERO;
                lblRevenueToday.setText(MONEY_FMT.format(val) + " VNĐ");
            }
        }

        // 2. Doanh thu tháng này
        String sql2 = "SELECT ISNULL(SUM(TongTien), 0) FROM HoaDon WHERE MONTH(NgayBan) = MONTH(GETDATE()) AND YEAR(NgayBan) = YEAR(GETDATE())";
        try (PreparedStatement ps = conn.prepareStatement(sql2);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                BigDecimal val = rs.getBigDecimal(1);
                if (val == null) val = BigDecimal.ZERO;
                lblRevenueMonth.setText(MONEY_FMT.format(val) + " VNĐ");
            }
        }

        // 3. Tổng SP active
        String sql3 = "SELECT COUNT(*) FROM SanPham WHERE TrangThai = 1";
        try (PreparedStatement ps = conn.prepareStatement(sql3);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                lblTotalProducts.setText(String.format("%,d sản phẩm", rs.getInt(1)));
            }
        }

        // 4. Lô sắp hết hạn (< 30 ngày, SoLuong > 0)
        String sql4 = "SELECT COUNT(*) FROM LoHang WHERE SoLuong > 0 AND HanSuDung <= DATEADD(DAY, 30, GETDATE())";
        try (PreparedStatement ps = conn.prepareStatement(sql4);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int count = rs.getInt(1);
                lblExpiringBatches.setText(count + " lô hàng");
                lblExpiringBatches.setForeground(count > 0 ? AppColors.DANGER : AppColors.SUCCESS);
            }
        }
    }

    private void loadTopProducts(Connection conn) throws SQLException {
        topProductsModel.setRowCount(0);
        String sql =
            "SELECT TOP 10 sp.TenSP, sp.DonViTinh, " +
            "SUM(ct.SoLuong) AS TongBan, SUM(ct.ThanhTien) AS DoanhThu " +
            "FROM ChiTietHoaDon ct " +
            "JOIN SanPham sp ON ct.MaSP = sp.MaSP " +
            "JOIN HoaDon hd ON ct.MaHD = hd.MaHD " +
            "WHERE MONTH(hd.NgayBan) = MONTH(GETDATE()) AND YEAR(hd.NgayBan) = YEAR(GETDATE()) " +
            "GROUP BY sp.TenSP, sp.DonViTinh " +
            "ORDER BY TongBan DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int stt = 1;
            while (rs.next()) {
                BigDecimal dt = rs.getBigDecimal("DoanhThu");
                if (dt == null) dt = BigDecimal.ZERO;
                topProductsModel.addRow(new Object[]{
                        stt++,
                        rs.getNString("TenSP"),
                        rs.getNString("DonViTinh"),
                        String.format("%,d", rs.getInt("TongBan")),
                        MONEY_FMT.format(dt) + " VNĐ"
                });
            }
        }

        if (topProductsModel.getRowCount() == 0) {
            topProductsModel.addRow(new Object[]{"", "Chưa có dữ liệu bán hàng", "", "", ""});
        }
    }

    private void loadExpiringBatches(Connection conn) throws SQLException {
        expiringModel.setRowCount(0);
        String sql =
            "SELECT sp.TenSP, l.SoLo, l.SoLuong, l.HanSuDung " +
            "FROM LoHang l " +
            "JOIN SanPham sp ON l.MaSP = sp.MaSP " +
            "WHERE l.SoLuong > 0 AND l.HanSuDung <= DATEADD(DAY, 30, GETDATE()) " +
            "ORDER BY l.HanSuDung ASC";

        LocalDate today = LocalDate.now();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int stt = 1;
            while (rs.next()) {
                java.sql.Date hsdDate = rs.getDate("HanSuDung");
                LocalDate hsd = hsdDate.toLocalDate();
                long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, hsd);
                String daysStr;
                if (daysLeft < 0) {
                    daysStr = "HẾT HẠN";
                } else if (daysLeft == 0) {
                    daysStr = "Hôm nay";
                } else {
                    daysStr = daysLeft + " ngày";
                }

                expiringModel.addRow(new Object[]{
                        stt++,
                        rs.getNString("TenSP"),
                        rs.getNString("SoLo"),
                        rs.getInt("SoLuong"),
                        hsd.format(DATE_FMT),
                        daysStr
                });
            }
        }

        if (expiringModel.getRowCount() == 0) {
            expiringModel.addRow(new Object[]{"", "Không có lô nào sắp hết hạn", "", "", "", ""});
        }
    }

    private void loadRevenueHistory(Connection conn) throws SQLException {
        revenueModel.setRowCount(0);
        String sql =
            "SELECT CAST(NgayBan AS DATE) AS Ngay, COUNT(*) AS SoHD, " +
            "SUM(TongTien) AS DoanhThu " +
            "FROM HoaDon " +
            "WHERE NgayBan >= DATEADD(DAY, -6, CAST(GETDATE() AS DATE)) " +
            "GROUP BY CAST(NgayBan AS DATE) " +
            "ORDER BY Ngay DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                java.sql.Date ngay = rs.getDate("Ngay");
                int soHD = rs.getInt("SoHD");
                BigDecimal doanhThu = rs.getBigDecimal("DoanhThu");
                if (doanhThu == null) doanhThu = BigDecimal.ZERO;
                BigDecimal tbHD = soHD > 0
                        ? doanhThu.divide(BigDecimal.valueOf(soHD), 0, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                revenueModel.addRow(new Object[]{
                        ngay.toLocalDate().format(DATE_FMT),
                        String.format("%,d", soHD),
                        MONEY_FMT.format(doanhThu) + " VNĐ",
                        MONEY_FMT.format(tbHD) + " VNĐ"
                });
            }
        }

        if (revenueModel.getRowCount() == 0) {
            revenueModel.addRow(new Object[]{"Chưa có dữ liệu", "", "", ""});
        }
    }

    // ================================================================
    //  UI HELPERS
    // ================================================================

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(30);
        table.setShowGrid(true);
        table.setGridColor(AppColors.NEUTRAL_DARK);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader th = table.getTableHeader();
        th.setBackground(AppColors.TABLE_HEADER_BG);
        th.setForeground(AppColors.TABLE_HEADER_FG);
        th.setFont(new Font("Segoe UI", Font.BOLD, 12));
        th.setPreferredSize(new Dimension(0, 34));
        th.setReorderingAllowed(false);

        // Alt-row + center STT
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) c.setBackground(row % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);
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
}
