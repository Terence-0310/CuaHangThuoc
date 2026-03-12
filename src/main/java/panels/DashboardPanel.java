package panels;

import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

import common.ColorScheme;
import dao.ThongKeDao.*;
import entity.NguoiDung;
import service.DashboardService;
import ui.StatusBadgeRenderer;

/**
 * DashboardPanel – Tổng quan nhà thuốc.
 *
 * Layout (top → bottom):
 *   A. Header      – Tiêu đề, ngày, user, nút làm mới
 *   B. KPI row     – 6 thẻ số liệu chính
 *   C. Alerts row  – Tồn kho thấp (trái) | Lô sắp hết hạn (phải)
 *   D. Recent      – Hóa đơn gần đây
 *   E. Stats row   – Top bán chạy (trái) | Trạng thái tồn kho (phải)
 *
 * Kiến trúc:  Panel → DashboardService → ThongKeDao
 *
 * @version 6.0 – Commercial pharmacy style
 */
public class DashboardPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    /* ============ PALETTE ============ */
    // Nền & viền
    private static final Color BG_PAGE       = new Color(246, 248, 250);
    private static final Color BG_CARD       = Color.WHITE;
    private static final Color BORDER        = new Color(226, 232, 240);
    private static final Color DIVIDER       = new Color(241, 245, 249);

    // Accent cho KPI
    private static final Color ACCENT_BLUE   = new Color(37, 99, 235);
    private static final Color ACCENT_GREEN  = new Color(22, 163, 74);
    private static final Color ACCENT_ORANGE = new Color(234, 88, 12);
    private static final Color ACCENT_RED    = new Color(220, 38, 38);
    private static final Color ACCENT_VIOLET = new Color(124, 58, 237);
    private static final Color ACCENT_TEAL   = new Color(13, 148, 136);

    // Text
    private static final Color TEXT_PRIMARY   = new Color(15, 23, 42);
    private static final Color TEXT_SECONDARY = new Color(100, 116, 139);
    private static final Color TEXT_MUTED     = new Color(148, 163, 184);

    // Table
    private static final Color TBL_HEADER_BG  = new Color(248, 250, 252);
    private static final Color TBL_ALT_ROW    = new Color(248, 250, 252);
    private static final Color TBL_HOVER      = new Color(241, 245, 249);
    private static final Color TBL_GRID       = new Color(241, 245, 249);

    /* ============ TYPOGRAPHY ============ */
    private static final Font F_TITLE       = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font F_SUBTITLE    = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font F_SECTION     = new Font("Segoe UI", Font.BOLD, 15);
    private static final Font F_KPI_VALUE   = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font F_KPI_LABEL   = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font F_KPI_TREND   = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font F_TABLE       = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font F_TABLE_HDR   = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font F_BADGE       = new Font("Segoe UI", Font.BOLD, 11);
    private static final Font F_BTN         = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font F_DATE        = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font F_BAR_LABEL   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font F_BAR_VALUE   = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font F_EMPTY       = new Font("Segoe UI", Font.ITALIC, 13);

    /* ============ FORMAT ============ */
    private static final NumberFormat VND = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    /* ============ SPACING ============ */
    private static final int GAP         = 14;   // Khoảng cách giữa các section
    private static final int CARD_PAD    = 16;   // Padding bên trong card
    private static final int TABLE_ROW_H = 36;   // Chiều cao dòng table

    /* ============ STATE ============ */
    private final NguoiDung currentUser;
    private final DashboardService svc;

    // KPI labels
    private JLabel valRevenue, valInvoice, valLowStock, valExpiring, valProduct, valCustomer;
    private JLabel lblTrend;

    // Loading
    private JLabel lblStatus;

    // Table models
    private DefaultTableModel mdlLowStock, mdlExpiring, mdlRecent;

    // Badge labels
    private JLabel badgeLow, badgeExp;

    // Bottom panels + dynamic body containers
    private JPanel pnlTopSelling, pnlInventory;
    private JPanel bodyTopSelling, bodyInventory;

    // Tables (kept for renderer attachment)
    private JTable tblLowStock, tblExpiring;

    /* ============ CONSTRUCTOR ============ */

    public DashboardPanel(NguoiDung currentUser) {
        this.currentUser = currentUser;
        this.svc = new DashboardService();
        buildUI();
    }

    /* ================================================================
       UI CONSTRUCTION
       ================================================================ */

    private void buildUI() {
        setLayout(new BorderLayout());
        setBackground(BG_PAGE);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(16, 20, 20, 20));

        content.add(createHeaderPanel());
        content.add(vGap(GAP));
        content.add(createKpiPanel());
        content.add(vGap(GAP));
        content.add(createAlertRow());
        content.add(vGap(GAP));
        content.add(createRecentInvoicePanel());
        content.add(vGap(GAP));
        content.add(createBottomSummaryPanel());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);

        loadAllData();
    }

    /* ======================== A. HEADER ======================== */

    private JPanel createHeaderPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

        // Left
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel t = label("Tổng quan nhà thuốc", F_TITLE, TEXT_PRIMARY);
        t.setAlignmentX(LEFT_ALIGNMENT);
        left.add(t);

        JLabel sub = label("Theo dõi tình hình bán hàng, tồn kho và cảnh báo vận hành", F_SUBTITLE, TEXT_SECONDARY);
        sub.setBorder(new EmptyBorder(2, 0, 0, 0));
        sub.setAlignmentX(LEFT_ALIGNMENT);
        left.add(sub);

        p.add(left, BorderLayout.WEST);

        // Right
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        right.setOpaque(false);

        lblStatus = label("", F_DATE, TEXT_MUTED);
        right.add(lblStatus);

        right.add(chipLabel(new SimpleDateFormat("dd/MM/yyyy").format(new Date())));

        if (currentUser != null && currentUser.getHoTen() != null) {
            right.add(chipLabel(currentUser.getHoTen()));
        }

        JButton btn = pillButton("↻  Làm mới", ACCENT_BLUE);
        btn.addActionListener(e -> loadAllData());
        right.add(btn);

        p.add(right, BorderLayout.EAST);
        return p;
    }

    /* ======================== B. KPI ROW ======================== */

    private JPanel createKpiPanel() {
        JPanel row = new JPanel(new GridLayout(1, 6, 10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 108));

        valRevenue  = new JLabel("–");
        lblTrend    = new JLabel(" ");
        valInvoice  = new JLabel("–");
        valLowStock = new JLabel("–");
        valExpiring = new JLabel("–");
        valProduct  = new JLabel("–");
        valCustomer = new JLabel("–");

        row.add(kpiCard("Doanh thu hôm nay", valRevenue, lblTrend, ACCENT_BLUE));
        row.add(kpiCard("Hóa đơn hôm nay",  valInvoice, null,     ACCENT_GREEN));
        row.add(kpiCard("Thuốc sắp hết",     valLowStock, null,    ACCENT_ORANGE));
        row.add(kpiCard("Lô sắp hết hạn",    valExpiring, null,    ACCENT_RED));
        row.add(kpiCard("Tổng sản phẩm",     valProduct, null,     ACCENT_TEAL));
        row.add(kpiCard("Khách hàng",         valCustomer, null,    ACCENT_VIOLET));

        return row;
    }

    private JPanel kpiCard(String title, JLabel valueLabel, JLabel trendLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            new EmptyBorder(12, CARD_PAD, 12, CARD_PAD)
        ));

        // 4px accent stripe
        JPanel stripe = new JPanel();
        stripe.setPreferredSize(new Dimension(4, 0));
        stripe.setBackground(accent);
        card.add(stripe, BorderLayout.WEST);

        // Body
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(0, 10, 0, 0));

        JLabel lbl = label(title, F_KPI_LABEL, TEXT_SECONDARY);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        body.add(lbl);
        body.add(Box.createVerticalStrut(3));

        valueLabel.setFont(F_KPI_VALUE);
        valueLabel.setForeground(accent);
        valueLabel.setAlignmentX(LEFT_ALIGNMENT);
        body.add(valueLabel);

        if (trendLabel != null) {
            body.add(Box.createVerticalStrut(2));
            trendLabel.setFont(F_KPI_TREND);
            trendLabel.setForeground(TEXT_MUTED);
            trendLabel.setAlignmentX(LEFT_ALIGNMENT);
            body.add(trendLabel);
        }

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    /* ======================== C. ALERTS ROW ======================== */

    private JPanel createAlertRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, GAP, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 380));

        row.add(createLowStockPanel());
        row.add(createExpiringBatchPanel());
        return row;
    }

    /* --- C1. Tồn kho thấp --- */

    private JPanel createLowStockPanel() {
        mdlLowStock = nonEditableModel("Mã SP", "Tên thuốc", "Đơn vị", "Tồn kho", "Mức tối thiểu", "Trạng thái");
        badgeLow = badge("0", ACCENT_ORANGE);

        tblLowStock = styledTable(mdlLowStock);
        // Gán badge renderer cho cột "Trạng thái" (index 5)
        tblLowStock.getColumnModel().getColumn(5).setCellRenderer(new StatusBadgeRenderer());
        setColumnWidths(tblLowStock, 55, 160, 50, 60, 80, 75);

        return sectionCard("Cảnh báo tồn kho thấp", badgeLow, tblLowStock);
    }

    /* --- C2. Lô sắp hết hạn --- */

    private JPanel createExpiringBatchPanel() {
        mdlExpiring = nonEditableModel("Mã lô", "Tên thuốc", "Hạn dùng", "SL còn", "Còn lại", "Cảnh báo");
        badgeExp = badge("0", ACCENT_RED);

        tblExpiring = styledTable(mdlExpiring);
        tblExpiring.getColumnModel().getColumn(5).setCellRenderer(new StatusBadgeRenderer());
        setColumnWidths(tblExpiring, 60, 160, 80, 55, 70, 75);

        return sectionCard("Lô thuốc sắp hết hạn (≤ 30 ngày)", badgeExp, tblExpiring);
    }

    /* ======================== D. RECENT INVOICES ======================== */

    private JPanel createRecentInvoicePanel() {
        mdlRecent = nonEditableModel("Mã HĐ", "Thời gian", "Nhân viên", "Khách hàng", "Tổng tiền", "Trạng thái");

        JTable tbl = styledTable(mdlRecent);
        tbl.getColumnModel().getColumn(5).setCellRenderer(new StatusBadgeRenderer());
        setColumnWidths(tbl, 70, 130, 120, 140, 110, 90);

        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(BG_CARD);
        card.setBorder(cardBorder());
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 360));

        // Header
        JPanel hdr = sectionHeader("Hóa đơn gần đây", null);
        JButton btnHist = linkButton("Xem lịch sử hóa đơn →", ACCENT_BLUE);
        hdr.add(btnHist, BorderLayout.EAST);
        card.add(hdr, BorderLayout.NORTH);

        // Table scroll
        JScrollPane sp = tableScroll(tbl);
        sp.setPreferredSize(new Dimension(0, 290));
        card.add(sp, BorderLayout.CENTER);

        return card;
    }

    /* ======================== E. BOTTOM SUMMARY ======================== */

    private JPanel createBottomSummaryPanel() {
        JPanel row = new JPanel(new GridLayout(1, 2, GAP, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        // Left: Top bán chạy
        pnlTopSelling = new JPanel(new BorderLayout(0, 8));
        pnlTopSelling.setBackground(BG_CARD);
        pnlTopSelling.setBorder(cardBorder());
        pnlTopSelling.add(sectionHeader("Top thuốc bán chạy hôm nay", null), BorderLayout.NORTH);
        bodyTopSelling = new JPanel(new BorderLayout());
        bodyTopSelling.setOpaque(false);
        pnlTopSelling.add(bodyTopSelling, BorderLayout.CENTER);
        row.add(pnlTopSelling);

        // Right: Tình trạng tồn kho
        pnlInventory = new JPanel(new BorderLayout(0, 8));
        pnlInventory.setBackground(BG_CARD);
        pnlInventory.setBorder(cardBorder());
        pnlInventory.add(sectionHeader("Tình trạng tồn kho", null), BorderLayout.NORTH);
        bodyInventory = new JPanel(new BorderLayout());
        bodyInventory.setOpaque(false);
        pnlInventory.add(bodyInventory, BorderLayout.CENTER);
        row.add(pnlInventory);

        return row;
    }

    /* ================================================================
       REUSABLE UI BUILDERS
       ================================================================ */

    /** Card container cho section cảnh báo (header + scrollable table). */
    private JPanel sectionCard(String title, JLabel badge, JTable table) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(BG_CARD);
        card.setBorder(cardBorder());

        // Header
        JPanel hdr = sectionHeader(title, badge);
        JButton btnAll = linkButton("Xem tất cả →",
                title.contains("tồn kho") ? ACCENT_ORANGE : ACCENT_RED);
        hdr.add(btnAll, BorderLayout.EAST);
        card.add(hdr, BorderLayout.NORTH);

        // Table
        JScrollPane sp = tableScroll(table);
        sp.setPreferredSize(new Dimension(0, 240));
        card.add(sp, BorderLayout.CENTER);

        return card;
    }

    /** Section header row: title (+ optional badge). */
    private JPanel sectionHeader(String title, JLabel badge) {
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        hdr.setBorder(new MatteBorder(0, 0, 1, 0, DIVIDER));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);

        JLabel lbl = label(title, F_SECTION, TEXT_PRIMARY);
        left.add(lbl);

        if (badge != null) {
            left.add(Box.createHorizontalStrut(8));
            left.add(badge);
        }

        hdr.add(left, BorderLayout.WEST);
        return hdr;
    }

    /** Non-editable table model factory. */
    private DefaultTableModel nonEditableModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    /** Styled JTable. */
    private JTable styledTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setRowHeight(TABLE_ROW_H);
        t.setFont(F_TABLE);
        t.setShowGrid(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(TBL_GRID);
        t.setSelectionBackground(new Color(219, 234, 254));
        t.setSelectionForeground(TEXT_PRIMARY);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setFillsViewportHeight(true);
        t.getTableHeader().setReorderingAllowed(false);

        // Header style
        JTableHeader hdr = t.getTableHeader();
        hdr.setFont(F_TABLE_HDR);
        hdr.setBackground(TBL_HEADER_BG);
        hdr.setForeground(TEXT_SECONDARY);
        hdr.setBorder(new MatteBorder(0, 0, 2, 0, BORDER));
        hdr.setPreferredSize(new Dimension(0, 38));

        // Row renderer with hover
        t.setDefaultRenderer(Object.class, new HoverRowRenderer());
        return t;
    }

    /** Scroll pane factory for tables (vertical scroll, fixed header). */
    private JScrollPane tableScroll(JTable table) {
        JScrollPane sp = new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    /** Set preferred column widths. */
    private void setColumnWidths(JTable table, int... widths) {
        TableColumnModel cm = table.getColumnModel();
        for (int i = 0; i < widths.length && i < cm.getColumnCount(); i++) {
            cm.getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    /* ---------- Small widget helpers ---------- */

    private JLabel label(String text, Font font, Color fg) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(fg);
        return l;
    }

    private JLabel chipLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(F_DATE);
        l.setForeground(TEXT_SECONDARY);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(5, 10, 5, 10)));
        l.setOpaque(true);
        l.setBackground(BG_CARD);
        return l;
    }

    private JLabel badge(String text, Color bg) {
        JLabel l = new JLabel(text);
        l.setFont(F_BADGE);
        l.setForeground(Color.WHITE);
        l.setOpaque(true);
        l.setBackground(bg);
        l.setBorder(new EmptyBorder(2, 8, 2, 8));
        return l;
    }

    private JButton pillButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(F_BTN);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(108, 32));
        b.putClientProperty("JButton.buttonType", "roundRect");
        Color hover = bg.darker();
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(hover); }
            public void mouseExited(java.awt.event.MouseEvent e)  { b.setBackground(bg); }
        });
        return b;
    }

    private JButton linkButton(String text, Color fg) {
        JButton b = new JButton(text);
        b.setFont(F_BTN);
        b.setForeground(fg);
        b.setBackground(BG_CARD);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { b.setForeground(fg.darker()); }
            public void mouseExited(java.awt.event.MouseEvent e)  { b.setForeground(fg); }
        });
        return b;
    }

    private javax.swing.border.Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(CARD_PAD, CARD_PAD, CARD_PAD, CARD_PAD));
    }

    private Component vGap(int h) {
        return Box.createRigidArea(new Dimension(0, h));
    }

    /* ================================================================
       ROW RENDERER (hover + alternating)
       ================================================================ */

    private class HoverRowRenderer extends DefaultTableCellRenderer {
        private int hoveredRow = -1;
        private boolean listenerAdded = false;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            // Attach listener once
            if (!listenerAdded) {
                listenerAdded = true;
                table.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                    @Override public void mouseMoved(java.awt.event.MouseEvent e) {
                        int r = table.rowAtPoint(e.getPoint());
                        if (r != hoveredRow) { hoveredRow = r; table.repaint(); }
                    }
                });
                table.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseExited(java.awt.event.MouseEvent e) {
                        hoveredRow = -1; table.repaint();
                    }
                });
            }

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setBackground(row == hoveredRow ? TBL_HOVER
                        : (row % 2 == 0 ? BG_CARD : TBL_ALT_ROW));
            }
            setBorder(new EmptyBorder(0, 10, 0, 10));
            return c;
        }
    }

    /* ================================================================
       DATA LOADING  (SwingWorker)
       ================================================================ */

    private void loadAllData() {
        lblStatus.setText("Đang tải dữ liệu...");
        lblStatus.setForeground(TEXT_MUTED);

        new SwingWorker<Void, Void>() {
            ThongKeNgay stats;
            BigDecimal revYesterday;
            int totalProducts, lowStockCount, expiringCount, customerCount;
            List<CanhBaoTonKho> lowStockList;
            List<CanhBaoHetHan> expiringList;
            List<HoaDonGanDay> recentList;
            List<SanPhamBanChay> topSelling;
            Map<String, Integer> invStatus;
            String error;

            @Override protected Void doInBackground() {
                try {
                    stats         = svc.getThongKeHomNay();
                    revYesterday  = svc.getDoanhThuHomQua();
                    totalProducts = svc.getTongSanPham();
                    lowStockCount = svc.getSoSanPhamSapHet();
                    expiringCount = svc.getSoLoSapHetHan();
                    customerCount = svc.getTongKhachHang();
                    lowStockList  = svc.getDanhSachTonKhoThap();
                    expiringList  = svc.getDanhSachLoSapHetHan();
                    recentList    = svc.getHoaDonGanDay();
                    topSelling    = svc.getTopSanPhamBanChayHomNay();
                    invStatus     = svc.getTyLeTonKho();
                } catch (Exception e) {
                    e.printStackTrace();
                    error = e.getMessage();
                }
                return null;
            }

            @Override protected void done() {
                if (error != null) {
                    lblStatus.setText("⚠ Không thể tải dữ liệu");
                    lblStatus.setForeground(ACCENT_RED);
                    return;
                }
                try {
                    loadSummaryCards(stats, revYesterday, totalProducts,
                            lowStockCount, expiringCount, customerCount);
                    loadLowStockData(lowStockList);
                    loadExpiringBatches(expiringList);
                    loadRecentInvoices(recentList);
                    loadTopSellingProducts(topSelling);
                    loadInventoryStatus(invStatus);

                    lblStatus.setText("✓ " + new SimpleDateFormat("HH:mm:ss").format(new Date()));
                    lblStatus.setForeground(ACCENT_GREEN);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    lblStatus.setText("⚠ Lỗi hiển thị");
                    lblStatus.setForeground(ACCENT_RED);
                }
            }
        }.execute();
    }

    /* ================================================================
       DATA → UI  BINDING
       ================================================================ */

    /* --- B. KPI --- */
    private void loadSummaryCards(ThongKeNgay s, BigDecimal revYesterday,
                                  int totalProd, int lowStock, int expiring, int cust) {
        valRevenue.setText(VND.format(s.doanhThu));
        valInvoice.setText(String.valueOf(s.soHoaDon));
        valLowStock.setText(String.valueOf(lowStock));
        valExpiring.setText(String.valueOf(expiring));
        valProduct.setText(String.valueOf(totalProd));
        valCustomer.setText(String.valueOf(cust));

        // Trend
        if (revYesterday != null && revYesterday.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = s.doanhThu.subtract(revYesterday);
            double pct = diff.doubleValue() / revYesterday.doubleValue() * 100;
            if (diff.signum() >= 0) {
                lblTrend.setText(String.format("▲ +%.0f%% vs hôm qua", pct));
                lblTrend.setForeground(ACCENT_GREEN);
            } else {
                lblTrend.setText(String.format("▼ %.0f%% vs hôm qua", pct));
                lblTrend.setForeground(ACCENT_RED);
            }
        } else {
            lblTrend.setText("Chưa có dữ liệu hôm qua");
            lblTrend.setForeground(TEXT_MUTED);
        }
    }

    /* --- C1. Low stock --- */
    private void loadLowStockData(List<CanhBaoTonKho> data) {
        mdlLowStock.setRowCount(0);
        if (data != null && !data.isEmpty()) {
            for (CanhBaoTonKho i : data) {
                String status = i.tongTon == 0 ? "Hết hàng" : "Tồn thấp";
                mdlLowStock.addRow(new Object[]{
                    "SP" + i.maSanPham, i.tenSanPham, i.donViTinh,
                    i.tongTon, i.mucTonToiThieu, status
                });
            }
            badgeLow.setText(String.valueOf(data.size()));
        } else {
            mdlLowStock.addRow(new Object[]{"", "Tất cả sản phẩm đủ tồn kho", "", "", "", "Ổn định"});
            badgeLow.setText("0");
        }
    }

    /* --- C2. Expiring batches --- */
    private void loadExpiringBatches(List<CanhBaoHetHan> data) {
        mdlExpiring.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        if (data != null && !data.isEmpty()) {
            for (CanhBaoHetHan i : data) {
                String remaining = i.soNgayConLai + " ngày";
                String level;
                if (i.soNgayConLai <= 7)       level = "Nguy cấp";
                else if (i.soNgayConLai <= 14)  level = "Cao";
                else                            level = "Cảnh báo";
                mdlExpiring.addRow(new Object[]{
                    i.soLo, i.tenSanPham, sdf.format(i.hanSuDung),
                    i.soLuongTon, remaining, level
                });
            }
            badgeExp.setText(String.valueOf(data.size()));
        } else {
            mdlExpiring.addRow(new Object[]{"", "Không có lô sắp hết hạn", "", "", "", "Ổn định"});
            badgeExp.setText("0");
        }
    }

    /* --- D. Recent invoices --- */
    private void loadRecentInvoices(List<HoaDonGanDay> data) {
        mdlRecent.setRowCount(0);
        if (data != null && !data.isEmpty()) {
            for (HoaDonGanDay i : data) {
                mdlRecent.addRow(new Object[]{
                    "HD" + String.format("%04d", i.maHoaDon),
                    i.thoiGian, i.tenNhanVien, i.tenKhachHang,
                    VND.format(i.tongTien), "Hoàn thành"
                });
            }
        } else {
            mdlRecent.addRow(new Object[]{"", "Chưa có hóa đơn nào", "", "", "", ""});
        }
    }

    /* --- E1. Top selling --- */
    private void loadTopSellingProducts(List<SanPhamBanChay> data) {
        bodyTopSelling.removeAll();

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(new EmptyBorder(6, 0, 0, 0));

        if (data != null && !data.isEmpty()) {
            int max = data.stream().mapToInt(d -> d.tongSoLuong).max().orElse(1);
            for (int idx = 0; idx < data.size(); idx++) {
                SanPhamBanChay item = data.get(idx);
                list.add(progressRow(idx + 1, item.tenSanPham, item.tongSoLuong,
                        item.tongDoanhThu, max));
                if (idx < data.size() - 1) list.add(Box.createVerticalStrut(6));
            }
        } else {
            JLabel empty = label("Chưa có dữ liệu bán hàng hôm nay", F_EMPTY, TEXT_MUTED);
            empty.setBorder(new EmptyBorder(24, 0, 0, 0));
            empty.setAlignmentX(LEFT_ALIGNMENT);
            list.add(empty);
        }

        bodyTopSelling.add(list, BorderLayout.NORTH);
        bodyTopSelling.revalidate();
        bodyTopSelling.repaint();
    }

    private JPanel progressRow(int rank, String name, int qty, BigDecimal rev, int max) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        row.setAlignmentX(LEFT_ALIGNMENT);

        JLabel lbl = label(rank + ". " + name, F_BAR_LABEL, TEXT_PRIMARY);
        lbl.setPreferredSize(new Dimension(180, 30));
        row.add(lbl, BorderLayout.WEST);

        JProgressBar bar = new JProgressBar(0, max);
        bar.setValue(qty);
        bar.setStringPainted(false);
        bar.setBackground(new Color(241, 245, 249));
        bar.setForeground(ACCENT_BLUE);
        bar.setBorderPainted(false);
        bar.setPreferredSize(new Dimension(100, 14));

        JPanel barWrap = new JPanel(new BorderLayout());
        barWrap.setOpaque(false);
        barWrap.setBorder(new EmptyBorder(8, 0, 8, 0));
        barWrap.add(bar, BorderLayout.CENTER);
        row.add(barWrap, BorderLayout.CENTER);

        JLabel val = label(qty + " sp · " + VND.format(rev), F_BAR_VALUE, TEXT_SECONDARY);
        val.setHorizontalAlignment(SwingConstants.RIGHT);
        val.setPreferredSize(new Dimension(170, 30));
        row.add(val, BorderLayout.EAST);

        return row;
    }

    /* --- E2. Inventory status --- */
    private void loadInventoryStatus(Map<String, Integer> data) {
        bodyInventory.removeAll();

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(new EmptyBorder(6, 0, 0, 0));

        if (data != null && !data.isEmpty()) {
            int total = Math.max(1, data.values().stream().mapToInt(Integer::intValue).sum());

            Color[] colors = { ACCENT_GREEN, ACCENT_ORANGE, ACCENT_RED, new Color(234, 88, 12) };
            int ci = 0;
            for (var entry : data.entrySet()) {
                Color c = ci < colors.length ? colors[ci++] : TEXT_SECONDARY;
                int pct = Math.round((float) entry.getValue() / total * 100);
                list.add(statusRow(entry.getKey(), entry.getValue(), pct, c, total));
                list.add(Box.createVerticalStrut(8));
            }
        } else {
            JLabel empty = label("Chưa có dữ liệu tồn kho", F_EMPTY, TEXT_MUTED);
            empty.setBorder(new EmptyBorder(24, 0, 0, 0));
            list.add(empty);
        }

        bodyInventory.add(list, BorderLayout.NORTH);
        bodyInventory.revalidate();
        bodyInventory.repaint();
    }

    private JPanel statusRow(String name, int count, int pct, Color color, int total) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        row.setAlignmentX(LEFT_ALIGNMENT);

        // Label line
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        top.setAlignmentX(LEFT_ALIGNMENT);

        top.add(label(name, F_BAR_LABEL, TEXT_PRIMARY), BorderLayout.WEST);
        top.add(label(count + " SP (" + pct + "%)", F_BAR_VALUE, color), BorderLayout.EAST);
        row.add(top);
        row.add(Box.createVerticalStrut(3));

        // Bar
        JProgressBar bar = new JProgressBar(0, total);
        bar.setValue(count);
        bar.setStringPainted(false);
        bar.setBackground(new Color(241, 245, 249));
        bar.setForeground(color);
        bar.setBorderPainted(false);
        bar.setPreferredSize(new Dimension(100, 8));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        bar.setAlignmentX(LEFT_ALIGNMENT);
        row.add(bar);

        return row;
    }
}
