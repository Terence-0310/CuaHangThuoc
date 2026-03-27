package presentation.panel;

import common.AppColors;
import common.ServiceFactory;
import domain.dto.*;
import service.IReportService;

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
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ReportPanel — Thống Kê & Báo Cáo theo Quý/Năm
 *
 * Layout (Single Viewport — NO vertical scrollbar):
 *   NORTH:  Controls (Year/Quarter combo + Export PDF) + 3 KPI cards
 *   CENTER: 2x2 tables via nested JSplitPane:
 *     TOP-LEFT:     Top 10 SP bán chạy (Net Quantity)
 *     TOP-RIGHT:    Top Khách hàng VIP
 *     BOTTOM-LEFT:  Top 10 NCC nhập nhiều
 *     BOTTOM-RIGHT: Cảnh báo tồn kho (hết/sắp hết)
 *
 * KHÔNG SỬ DỤNG ICON. KHÔNG SCROLLBAR DỌC CHO TOÀN TRANG.
 */
public class ReportPanel extends JPanel {

    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0");

    private final IReportService reportService = ServiceFactory.getReportService();

    // Controls
    private JComboBox<Integer> cboYear;
    private JComboBox<String> cboQuarter;
    private boolean suppressComboEvent = false;

    // KPI Labels
    private JLabel lblNetRevenue, lblTotalInvoices, lblTotalProducts;

    // Table models
    private DefaultTableModel modelTopProducts;
    private DefaultTableModel modelTopCustomers;
    private DefaultTableModel modelTopSuppliers;
    private DefaultTableModel modelStockAlerts;

    // Current data for PDF export
    private BusinessMetricDTO currentKPIs;
    private List<TopSellingDTO> currentTopProducts;
    private List<TopCustomerDTO> currentTopCustomers;
    private List<TopSupplierDTO> currentTopSuppliers;
    private List<StockAlertDTO> currentStockAlerts;

    public ReportPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(AppColors.NEUTRAL);
        initComponents();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                refreshYears();
            }
        });

        // BUG FIX #2: Load data ngay khi khởi tạo (không phải đợi componentShown)
        SwingUtilities.invokeLater(this::refreshYears);
    }

    // ================================================================
    //  INIT COMPONENTS
    // ================================================================

    private void initComponents() {
        // === NORTH: Controls + KPI cards ===
        JPanel northWrapper = new JPanel();
        northWrapper.setLayout(new BoxLayout(northWrapper, BoxLayout.Y_AXIS));
        northWrapper.setBackground(Color.WHITE);

        northWrapper.add(createControlBar());
        northWrapper.add(createKPICardsPanel());

        add(northWrapper, BorderLayout.NORTH);

        // === CENTER: 2x2 tables via nested JSplitPane ===
        add(createTablesGrid(), BorderLayout.CENTER);
    }

    // ================================================================
    //  CONTROL BAR: Title + Year + Quarter + Export PDF
    // ================================================================

    private JPanel createControlBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(new EmptyBorder(14, 24, 10, 24));

        // Left: Title
        JLabel lblTitle = new JLabel("Thống Kê & Báo Cáo");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(AppColors.TEXT_PRIMARY);
        bar.add(lblTitle, BorderLayout.WEST);

        // Right: Year + Quarter + Button
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controls.setOpaque(false);

        // Year combo
        JLabel lblYear = new JLabel("Năm:");
        lblYear.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblYear.setForeground(AppColors.TEXT_SECONDARY);
        controls.add(lblYear);

        cboYear = new JComboBox<>();
        cboYear.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cboYear.setPreferredSize(new Dimension(90, 30));
        cboYear.addActionListener(e -> {
            if (!suppressComboEvent) loadAllData();
        });
        controls.add(cboYear);

        // Quarter combo
        JLabel lblQuarter = new JLabel("Quý:");
        lblQuarter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblQuarter.setForeground(AppColors.TEXT_SECONDARY);
        controls.add(lblQuarter);

        cboQuarter = new JComboBox<>(new String[]{"Cả năm", "Quý 1", "Quý 2", "Quý 3", "Quý 4"});
        cboQuarter.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cboQuarter.setPreferredSize(new Dimension(100, 30));
        cboQuarter.addActionListener(e -> {
            if (!suppressComboEvent) loadAllData();
        });
        controls.add(cboQuarter);

        controls.add(Box.createHorizontalStrut(12));

        // Export PDF button
        JButton btnExportPDF = new JButton("Xuất báo cáo PDF");
        btnExportPDF.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnExportPDF.setBackground(new Color(0x27, 0xAE, 0x60));
        btnExportPDF.setForeground(Color.WHITE);
        btnExportPDF.setFocusPainted(false);
        btnExportPDF.setBorderPainted(false);
        btnExportPDF.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnExportPDF.setPreferredSize(new Dimension(160, 30));
        btnExportPDF.addActionListener(e -> exportToPDF());

        // Hover
        btnExportPDF.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnExportPDF.setBackground(new Color(0x21, 0x96, 0x53));
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btnExportPDF.setBackground(new Color(0x27, 0xAE, 0x60));
            }
        });
        controls.add(btnExportPDF);

        bar.add(controls, BorderLayout.EAST);
        return bar;
    }

    // ================================================================
    //  KPI CARDS (3 cards in a row)
    // ================================================================

    private JPanel createKPICardsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 12, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(0, 24, 10, 24));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        panel.setPreferredSize(new Dimension(0, 90));

        lblNetRevenue = new JLabel("0 VND");
        panel.add(createKPICard("Tổng Doanh Thu Thuần", lblNetRevenue,
                AppColors.PRIMARY, new Color(0xE3, 0xEF, 0xFA)));

        lblTotalInvoices = new JLabel("0");
        panel.add(createKPICard("Tổng Số Hóa Đơn", lblTotalInvoices,
                AppColors.SUCCESS, new Color(0xD4, 0xED, 0xDA)));

        lblTotalProducts = new JLabel("0");
        panel.add(createKPICard("Tổng SP Kinh Doanh", lblTotalProducts,
                new Color(0x17, 0xA2, 0xB8), new Color(0xD1, 0xEC, 0xF1)));

        return panel;
    }

    private JPanel createKPICard(String title, JLabel valueLabel, Color accent, Color bg) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(bg);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(accent.brighter(), 1),
                new EmptyBorder(10, 14, 10, 14)
        ));

        // Left accent strip
        JPanel strip = new JPanel();
        strip.setPreferredSize(new Dimension(4, 0));
        strip.setBackground(accent);
        card.add(strip, BorderLayout.WEST);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);
        inner.setBorder(new EmptyBorder(0, 8, 0, 0));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTitle.setForeground(accent.darker());
        lblTitle.setAlignmentX(LEFT_ALIGNMENT);
        inner.add(lblTitle);
        inner.add(Box.createRigidArea(new Dimension(0, 4)));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valueLabel.setForeground(accent);
        valueLabel.setAlignmentX(LEFT_ALIGNMENT);
        inner.add(valueLabel);

        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    // ================================================================
    //  TABLES GRID: 2x2 nested JSplitPane
    // ================================================================

    private JSplitPane createTablesGrid() {
        // Top row: Top Products | Top Customers
        JSplitPane topRow = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        topRow.setLeftComponent(createTopProductsTable());
        topRow.setRightComponent(createTopCustomersTable());
        topRow.setDividerLocation(0.5);
        topRow.setResizeWeight(0.5);
        topRow.setDividerSize(6);
        topRow.setBorder(null);
        topRow.setOpaque(false);

        // Bottom row: Top Suppliers | Stock Alerts
        JSplitPane bottomRow = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        bottomRow.setLeftComponent(createTopSuppliersTable());
        bottomRow.setRightComponent(createStockAlertsTable());
        bottomRow.setDividerLocation(0.5);
        bottomRow.setResizeWeight(0.5);
        bottomRow.setDividerSize(6);
        bottomRow.setBorder(null);
        bottomRow.setOpaque(false);

        // Main: top | bottom
        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplit.setTopComponent(topRow);
        mainSplit.setBottomComponent(bottomRow);
        mainSplit.setDividerLocation(0.5);
        mainSplit.setResizeWeight(0.5);
        mainSplit.setDividerSize(6);
        mainSplit.setBorder(new EmptyBorder(6, 16, 10, 16));
        mainSplit.setOpaque(false);

        return mainSplit;
    }

    // --- Table 1: Top 10 SP bán chạy ---
    private JPanel createTopProductsTable() {
        String[] cols = {"#", "Tên SP", "ĐVT", "SL Bán (Net)", "Doanh Thu"};
        modelTopProducts = createModel(cols);
        JTable table = createStyledTable(modelTopProducts);
        table.getColumnModel().getColumn(0).setMaxWidth(35);
        table.getColumnModel().getColumn(2).setPreferredWidth(55);
        table.getColumnModel().getColumn(3).setPreferredWidth(85);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        return wrapTable("Top 10 Mặt Hàng Bán Chạy", table, AppColors.PRIMARY);
    }

    // --- Table 2: Top Khách hàng VIP ---
    private JPanel createTopCustomersTable() {
        String[] cols = {"#", "Khách Hàng", "SĐT", "Số Lần Mua", "Tổng Chi"};
        modelTopCustomers = createModel(cols);
        JTable table = createStyledTable(modelTopCustomers);
        table.getColumnModel().getColumn(0).setMaxWidth(35);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(3).setPreferredWidth(75);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        return wrapTable("Top Khách Hàng VIP", table, AppColors.SUCCESS);
    }

    // --- Table 3: Top 10 NCC nhập nhiều ---
    private JPanel createTopSuppliersTable() {
        String[] cols = {"#", "Nhà Cung Cấp", "SĐT", "Số Phiếu Nhập", "Tổng Tiền Nhập"};
        modelTopSuppliers = createModel(cols);
        JTable table = createStyledTable(modelTopSuppliers);
        table.getColumnModel().getColumn(0).setMaxWidth(35);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(3).setPreferredWidth(85);
        table.getColumnModel().getColumn(4).setPreferredWidth(110);
        return wrapTable("Top 10 Nhà Cung Cấp Nhập Nhiều", table, new Color(0x17, 0xA2, 0xB8));
    }

    // --- Table 4: Cảnh báo tồn kho ---
    private JPanel createStockAlertsTable() {
        String[] cols = {"#", "Tên SP", "Tổng Tồn", "Số Lô", "Trạng Thái"};
        modelStockAlerts = createModel(cols);
        JTable table = createStyledTable(modelStockAlerts);
        table.getColumnModel().getColumn(0).setMaxWidth(35);
        table.getColumnModel().getColumn(2).setPreferredWidth(65);
        table.getColumnModel().getColumn(3).setPreferredWidth(55);
        table.getColumnModel().getColumn(4).setPreferredWidth(90);

        // Color the "Trang Thai" column
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            { setHorizontalAlignment(SwingConstants.CENTER); }
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel && val != null) {
                    String s = val.toString();
                    if (s.contains("Hết") || s.contains("hết")) {
                        c.setForeground(AppColors.DANGER);
                        c.setBackground(new Color(0xF8, 0xD7, 0xDA));
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else if (s.contains("Sắp") || s.contains("sắp")) {
                        c.setForeground(new Color(0x85, 0x6D, 0x04));
                        c.setBackground(new Color(0xFF, 0xF3, 0xCD));
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        c.setForeground(AppColors.TEXT_PRIMARY);
                        c.setBackground(row % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);
                    }
                }
                return c;
            }
        });

        return wrapTable("Cảnh Báo Tồn Kho (Hết / Sắp Hết)", table, AppColors.DANGER);
    }

    // ================================================================
    //  DATA LOADING
    // ================================================================

    private void refreshYears() {
        suppressComboEvent = true;
        try {
            Integer selectedYear = (Integer) cboYear.getSelectedItem();
            cboYear.removeAllItems();

            List<Integer> years = reportService.getAvailableYears();
            if (years.isEmpty()) years.add(LocalDate.now().getYear());

            for (Integer y : years) cboYear.addItem(y);

            if (selectedYear != null && years.contains(selectedYear)) {
                cboYear.setSelectedItem(selectedYear);
            }
        } finally {
            suppressComboEvent = false;
        }
        loadAllData();
    }

    private int getSelectedYear() {
        Object sel = cboYear.getSelectedItem();
        return sel instanceof Integer ? (Integer) sel : LocalDate.now().getYear();
    }

    private int getSelectedQuarter() {
        int idx = cboQuarter.getSelectedIndex();
        return idx <= 0 ? 0 : idx; // 0 = full year
    }

    private void loadAllData() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        int year = getSelectedYear();
        int quarter = getSelectedQuarter();

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            BusinessMetricDTO kpis;
            List<TopSellingDTO> topProducts;
            List<TopCustomerDTO> topCustomers;
            List<TopSupplierDTO> topSuppliers;
            List<StockAlertDTO> stockAlerts;

            @Override
            protected Void doInBackground() {
                kpis = reportService.getKPIs(year, quarter);
                topProducts = reportService.getTopProducts(year, quarter);
                topCustomers = reportService.getTopCustomers(year, quarter);
                topSuppliers = reportService.getTopSuppliers(year, quarter);
                stockAlerts = reportService.getStockAlerts();
                return null;
            }

            @Override
            protected void done() {
                try {
                    currentKPIs = kpis;
                    currentTopProducts = topProducts;
                    currentTopCustomers = topCustomers;
                    currentTopSuppliers = topSuppliers;
                    currentStockAlerts = stockAlerts;

                    updateKPICards(kpis);
                    updateTopProducts(topProducts);
                    updateTopCustomers(topCustomers);
                    updateTopSuppliers(topSuppliers);
                    updateStockAlerts(stockAlerts);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };
        worker.execute();
    }

    private void updateKPICards(BusinessMetricDTO kpis) {
        BigDecimal rev = kpis.getNetRevenue() != null ? kpis.getNetRevenue() : BigDecimal.ZERO;
        lblNetRevenue.setText(MONEY_FMT.format(rev) + " VNĐ");
        lblTotalInvoices.setText(String.format("%,d hóa đơn", kpis.getTotalSaleInvoices()));
        lblTotalProducts.setText(String.format("%,d sản phẩm", kpis.getTotalProducts()));
    }

    private void updateTopProducts(List<TopSellingDTO> list) {
        modelTopProducts.setRowCount(0);
        if (list.isEmpty()) {
            modelTopProducts.addRow(new Object[]{"", "Chưa có dữ liệu", "", "", ""});
            return;
        }
        int stt = 1;
        for (TopSellingDTO dto : list) {
            modelTopProducts.addRow(new Object[]{
                    stt++,
                    dto.getTenSP(),
                    dto.getDonViTinh(),
                    String.format("%,d", dto.getTongSoLuongBan()),
                    MONEY_FMT.format(dto.getTongDoanhThu() != null ? dto.getTongDoanhThu() : BigDecimal.ZERO) + " VNĐ"
            });
        }
    }

    private void updateTopCustomers(List<TopCustomerDTO> list) {
        modelTopCustomers.setRowCount(0);
        if (list.isEmpty()) {
            modelTopCustomers.addRow(new Object[]{"", "Chưa có dữ liệu", "", "", ""});
            return;
        }
        int stt = 1;
        for (TopCustomerDTO dto : list) {
            modelTopCustomers.addRow(new Object[]{
                    stt++,
                    dto.getTenKH(),
                    dto.getSoDT(),
                    String.format("%,d", dto.getSoLanMua()),
                    MONEY_FMT.format(dto.getTongTienMua() != null ? dto.getTongTienMua() : BigDecimal.ZERO) + " VNĐ"
            });
        }
    }

    private void updateTopSuppliers(List<TopSupplierDTO> list) {
        modelTopSuppliers.setRowCount(0);
        if (list.isEmpty()) {
            modelTopSuppliers.addRow(new Object[]{"", "Chưa có dữ liệu", "", "", ""});
            return;
        }
        int stt = 1;
        for (TopSupplierDTO dto : list) {
            modelTopSuppliers.addRow(new Object[]{
                    stt++,
                    dto.getTenNCC(),
                    dto.getSoDT(),
                    String.format("%,d", dto.getSoPhieuNhap()),
                    MONEY_FMT.format(dto.getTongTienNhap() != null ? dto.getTongTienNhap() : BigDecimal.ZERO) + " VNĐ"
            });
        }
    }

    private void updateStockAlerts(List<StockAlertDTO> list) {
        modelStockAlerts.setRowCount(0);
        if (list.isEmpty()) {
            modelStockAlerts.addRow(new Object[]{"", "Không có cảnh báo", "", "", ""});
            return;
        }
        int stt = 1;
        for (StockAlertDTO dto : list) {
            modelStockAlerts.addRow(new Object[]{
                    stt++,
                    dto.getTenSP(),
                    String.format("%,d", dto.getTongTon()),
                    dto.getSoLo(),
                    dto.getTrangThai()
            });
        }
    }

    // ================================================================
    //  PDF EXPORT (OpenPDF / com.lowagie.text)
    // ================================================================

    private void exportToPDF() {
        if (currentKPIs == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng tải dữ liệu trước khi xuất PDF.",
                    "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int year = getSelectedYear();
        int quarter = getSelectedQuarter();
        String periodLabel = quarter == 0
                ? "CẢ NĂM " + year
                : "QUÝ " + quarter + " - " + year;

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("BaoCaoThongKe_" +
                (quarter == 0 ? "Năm" : "Quý" + quarter) + "_" + year + ".pdf"));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Files", "pdf"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        java.io.File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            file = new java.io.File(file.getAbsolutePath() + ".pdf");
        }

        try {
            generatePDF(file.getAbsolutePath(), periodLabel);
            JOptionPane.showMessageDialog(this,
                    "Xuất PDF thành công!\n" + file.getAbsolutePath(),
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);

            // Open file
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Lỗi xuất PDF: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generatePDF(String filePath, String periodLabel) throws Exception {
        com.lowagie.text.Document doc = new com.lowagie.text.Document(
                com.lowagie.text.PageSize.A4, 36, 36, 36, 36);
        com.lowagie.text.pdf.PdfWriter.getInstance(doc, new java.io.FileOutputStream(filePath));
        doc.open();

        // Fonts
        com.lowagie.text.Font fTitle = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD, new Color(0x00, 0x56, 0xB3));
        com.lowagie.text.Font fSubtitle = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, Color.GRAY);
        com.lowagie.text.Font fSection = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 13, com.lowagie.text.Font.BOLD, new Color(0x1B, 0x3A, 0x5C));
        com.lowagie.text.Font fTableHeader = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD, Color.WHITE);
        com.lowagie.text.Font fNormal = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.NORMAL);
        com.lowagie.text.Font fBold = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD);
        com.lowagie.text.Font fKPI = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD, new Color(0x27, 0xAE, 0x60));

        // === HEADER ===
        com.lowagie.text.Paragraph pTitle = new com.lowagie.text.Paragraph(
                "BÁO CÁO THỐNG KÊ " + periodLabel, fTitle);
        pTitle.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        doc.add(pTitle);

        com.lowagie.text.Paragraph pStore = new com.lowagie.text.Paragraph(
                "Apothecary Pro - Hệ thống Quản lý Nhà thuốc", fSubtitle);
        pStore.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        doc.add(pStore);

        com.lowagie.text.Paragraph pDate = new com.lowagie.text.Paragraph(
                "Ngày xuất: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fSubtitle);
        pDate.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        doc.add(pDate);
        doc.add(new com.lowagie.text.Paragraph(" "));

        // === KPI ===
        BigDecimal rev = currentKPIs.getNetRevenue() != null ? currentKPIs.getNetRevenue() : BigDecimal.ZERO;
        com.lowagie.text.pdf.PdfPTable kpiTable = new com.lowagie.text.pdf.PdfPTable(3);
        kpiTable.setWidthPercentage(100);
        addKPICell(kpiTable, "Tổng Doanh Thu Thuần", MONEY_FMT.format(rev) + " VNĐ", fBold, fKPI);
        addKPICell(kpiTable, "Tổng Số Hóa Đơn", String.format("%,d", currentKPIs.getTotalSaleInvoices()), fBold, fKPI);
        addKPICell(kpiTable, "Tổng SP Kinh Doanh", String.format("%,d", currentKPIs.getTotalProducts()), fBold, fKPI);
        doc.add(kpiTable);
        doc.add(new com.lowagie.text.Paragraph(" "));

        // === TOP 10 SP ===
        doc.add(new com.lowagie.text.Paragraph("Top 10 Mặt Hàng Bán Chạy", fSection));
        doc.add(new com.lowagie.text.Paragraph(" "));
        com.lowagie.text.pdf.PdfPTable tProducts = new com.lowagie.text.pdf.PdfPTable(new float[]{5, 35, 10, 20, 30});
        tProducts.setWidthPercentage(100);
        addPdfHeaderRow(tProducts, new String[]{"#", "Tên SP", "ĐVT", "SL Bán", "Doanh Thu"}, fTableHeader);
        int stt = 1;
        for (TopSellingDTO d : currentTopProducts) {
            addPdfRow(tProducts, new String[]{
                    String.valueOf(stt++), d.getTenSP(), d.getDonViTinh(),
                    String.format("%,d", d.getTongSoLuongBan()),
                    MONEY_FMT.format(d.getTongDoanhThu() != null ? d.getTongDoanhThu() : BigDecimal.ZERO) + " VNĐ"
            }, fNormal, stt);
        }
        doc.add(tProducts);
        doc.add(new com.lowagie.text.Paragraph(" "));

        // === TOP KH VIP ===
        doc.add(new com.lowagie.text.Paragraph("Top Khách Hàng VIP", fSection));
        doc.add(new com.lowagie.text.Paragraph(" "));
        com.lowagie.text.pdf.PdfPTable tCustomers = new com.lowagie.text.pdf.PdfPTable(new float[]{5, 30, 20, 15, 30});
        tCustomers.setWidthPercentage(100);
        addPdfHeaderRow(tCustomers, new String[]{"#", "Khách Hàng", "SĐT", "Số Lần Mua", "Tổng Chi"}, fTableHeader);
        stt = 1;
        for (TopCustomerDTO d : currentTopCustomers) {
            addPdfRow(tCustomers, new String[]{
                    String.valueOf(stt++), d.getTenKH(), d.getSoDT(),
                    String.format("%,d", d.getSoLanMua()),
                    MONEY_FMT.format(d.getTongTienMua() != null ? d.getTongTienMua() : BigDecimal.ZERO) + " VNĐ"
            }, fNormal, stt);
        }
        doc.add(tCustomers);
        doc.add(new com.lowagie.text.Paragraph(" "));

        // === TOP 10 NCC ===
        doc.add(new com.lowagie.text.Paragraph("Top 10 Nhà Cung Cấp Nhập Nhiều", fSection));
        doc.add(new com.lowagie.text.Paragraph(" "));
        com.lowagie.text.pdf.PdfPTable tSuppliers = new com.lowagie.text.pdf.PdfPTable(new float[]{5, 30, 20, 15, 30});
        tSuppliers.setWidthPercentage(100);
        addPdfHeaderRow(tSuppliers, new String[]{"#", "NCC", "SĐT", "Số Phiếu", "Tổng Tiền Nhập"}, fTableHeader);
        stt = 1;
        for (TopSupplierDTO d : currentTopSuppliers) {
            addPdfRow(tSuppliers, new String[]{
                    String.valueOf(stt++), d.getTenNCC(), d.getSoDT(),
                    String.format("%,d", d.getSoPhieuNhap()),
                    MONEY_FMT.format(d.getTongTienNhap() != null ? d.getTongTienNhap() : BigDecimal.ZERO) + " VNĐ"
            }, fNormal, stt);
        }
        doc.add(tSuppliers);
        doc.add(new com.lowagie.text.Paragraph(" "));

        // === STOCK ALERTS ===
        doc.add(new com.lowagie.text.Paragraph("Cảnh Báo Tồn Kho", fSection));
        doc.add(new com.lowagie.text.Paragraph(" "));
        com.lowagie.text.pdf.PdfPTable tStock = new com.lowagie.text.pdf.PdfPTable(new float[]{5, 40, 15, 10, 30});
        tStock.setWidthPercentage(100);
        addPdfHeaderRow(tStock, new String[]{"#", "Tên SP", "Tồn Kho", "Số Lô", "Trạng Thái"}, fTableHeader);
        stt = 1;
        for (StockAlertDTO d : currentStockAlerts) {
            addPdfRow(tStock, new String[]{
                    String.valueOf(stt++), d.getTenSP(),
                    String.format("%,d", d.getTongTon()),
                    String.valueOf(d.getSoLo()),
                    d.getTrangThai()
            }, fNormal, stt);
        }
        doc.add(tStock);

        // === FOOTER ===
        doc.add(new com.lowagie.text.Paragraph(" "));
        com.lowagie.text.Paragraph pFooter = new com.lowagie.text.Paragraph(
                "--- Hết báo cáo ---", fSubtitle);
        pFooter.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        doc.add(pFooter);

        doc.close();
    }

    private void addKPICell(com.lowagie.text.pdf.PdfPTable table, String label, String value,
                            com.lowagie.text.Font fLabel, com.lowagie.text.Font fValue) {
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell();
        cell.setBorder(com.lowagie.text.Rectangle.BOX);
        cell.setPadding(8);
        cell.setBackgroundColor(new Color(0xF8, 0xF9, 0xFA));
        cell.addElement(new com.lowagie.text.Paragraph(label, fLabel));
        cell.addElement(new com.lowagie.text.Paragraph(value, fValue));
        table.addCell(cell);
    }

    private void addPdfHeaderRow(com.lowagie.text.pdf.PdfPTable table, String[] headers,
                                 com.lowagie.text.Font font) {
        for (String h : headers) {
            com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Phrase(h, font));
            cell.setBackgroundColor(new Color(0x1B, 0x3A, 0x5C));
            cell.setPadding(5);
            cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }

    private void addPdfRow(com.lowagie.text.pdf.PdfPTable table, String[] values,
                           com.lowagie.text.Font font, int rowNum) {
        Color bg = rowNum % 2 == 0 ? new Color(0xF0, 0xF4, 0xF8) : Color.WHITE;
        for (String v : values) {
            com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Phrase(v != null ? v : "", font));
            cell.setBackgroundColor(bg);
            cell.setPadding(4);
            table.addCell(cell);
        }
    }

    // ================================================================
    //  UI HELPERS
    // ================================================================

    private DefaultTableModel createModel(String[] cols) {
        return new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(28);
        table.setShowGrid(true);
        table.setGridColor(AppColors.NEUTRAL_DARK);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader th = table.getTableHeader();
        th.setBackground(AppColors.TABLE_HEADER_BG);
        th.setForeground(AppColors.TABLE_HEADER_FG);
        th.setFont(new Font("Segoe UI", Font.BOLD, 11));
        th.setPreferredSize(new Dimension(0, 30));
        th.setReorderingAllowed(false);

        // Alt-row + center STT column
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) c.setBackground(row % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);
                if (col == 0) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else if (col >= 3) {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }
                return c;
            }
        });

        return table;
    }

    private JPanel wrapTable(String title, JTable table, Color accentColor) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AppColors.NEUTRAL_DARK, 1),
                new EmptyBorder(8, 10, 8, 10)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(accentColor);
        panel.add(lblTitle, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1));
        scroll.getViewport().setBackground(Color.WHITE);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }
}
