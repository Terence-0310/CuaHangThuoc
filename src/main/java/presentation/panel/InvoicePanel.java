package presentation.panel;

import common.AppColors;
import common.DatePickerField;
import common.ServiceFactory;
import domain.dto.InvoiceDetailDTO;
import domain.dto.InvoiceFilterCriteria;
import domain.dto.InvoiceListDTO;
import domain.dto.PagedResult;
import domain.entity.Invoice;
import service.IInvoiceService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/**
 * InvoicePanel — Quản Lý Hóa Đơn (Admin only)
 * - View invoices with filters (Tách biệt 3 Tab)
 * - Double-click to view detail
 * - Void invoice (hoàn kho)
 * Clean Architecture version.
 */
public class InvoicePanel extends JPanel {

    private final IInvoiceService invoiceService = ServiceFactory.getInvoiceService();

    private JTabbedPane tabbedPane;

    // --- TAB 1: KHÁCH ĐĂNG KÝ ---
    private DatePickerField dpFromDate1, dpToDate1;
    private JTextField txtSearchKH1, txtSearchMaHD1;
    private JComboBox<String> cboTrangThai1;

    // --- TAB 2: KHÁCH VÃNG LAI ---
    private DatePickerField dpNgayMua2;
    private JSpinner spinTuGio2, spinDenGio2;
    private JTextField txtSearchSP2;
    private JComboBox<String> cboTrangThai2;

    // --- TAB 3: TRẢ HÀNG ---
    private DatePickerField dpFromDate3, dpToDate3;
    private JTextField txtSearchMaHD3;

    private DefaultTableModel tableModel;
    private JTable table;

    // Pagination
    private int currentPage = 1;
    private int totalPages = 1;
    private static final int PAGE_SIZE = 15;
    private JLabel lblPageInfo;
    private JPanel pageNumbersPanel;

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");

    public InvoicePanel() {
        setLayout(new BorderLayout());
        setBackground(AppColors.NEUTRAL);

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("Quản Lý Hóa Đơn");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(AppColors.TEXT_PRIMARY);
        lblTitle.setBorder(new EmptyBorder(16, 24, 8, 24));
        
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(Color.WHITE);
        titlePanel.add(lblTitle, BorderLayout.WEST);
        northPanel.add(titlePanel);

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.setBackground(Color.WHITE);

        tabbedPane.addTab("Khách Đăng Ký (SALE)", createTab1Panel());
        tabbedPane.addTab("Khách Vãng Lai (SALE)", createTab2Panel());
        tabbedPane.addTab("Lịch Sử Trả Hàng (RETURN)", createTab3Panel());

        tabbedPane.addChangeListener(e -> {
            currentPage = 1;
            loadInvoices();
        });

        northPanel.add(tabbedPane);
        add(northPanel, BorderLayout.NORTH);

        add(createTablePanel(), BorderLayout.CENTER);
        add(createPaginationPanel(), BorderLayout.SOUTH);

        SwingUtilities.invokeLater(this::loadInvoices);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                loadInvoices();
            }
        });
    }

    private JPanel createTab1Panel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        pnl.setBackground(Color.WHITE);
        pnl.setBorder(new EmptyBorder(8, 16, 8, 16));

        pnl.add(makeFilterLabel("Từ ngày:"));
        dpFromDate1 = new DatePickerField();
        dpFromDate1.setPreferredSize(new Dimension(130, 28));
        pnl.add(dpFromDate1);

        pnl.add(makeFilterLabel("Đến ngày:"));
        dpToDate1 = new DatePickerField();
        dpToDate1.setPreferredSize(new Dimension(130, 28));
        pnl.add(dpToDate1);

        pnl.add(makeFilterLabel("Khách hàng:"));
        txtSearchKH1 = makeFilterField(12);
        pnl.add(txtSearchKH1);

        pnl.add(makeFilterLabel("Mã HĐ:"));
        txtSearchMaHD1 = makeFilterField(6);
        pnl.add(txtSearchMaHD1);

        pnl.add(makeFilterLabel("Trạng thái:"));
        cboTrangThai1 = new JComboBox<>(new String[]{"Tất cả", "Thành công", "Đã hủy"});
        cboTrangThai1.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pnl.add(cboTrangThai1);

        JButton btnSearch = createSearchButton();
        pnl.add(btnSearch);
        return pnl;
    }

    private JPanel createTab2Panel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        pnl.setBackground(Color.WHITE);
        pnl.setBorder(new EmptyBorder(8, 16, 8, 16));

        pnl.add(makeFilterLabel("Ngày mua:"));
        dpNgayMua2 = new DatePickerField();
        dpNgayMua2.setPreferredSize(new Dimension(130, 28));
        pnl.add(dpNgayMua2);

        pnl.add(makeFilterLabel("Từ giờ:"));
        spinTuGio2 = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor timeEditor1 = new JSpinner.DateEditor(spinTuGio2, "HH:mm");
        spinTuGio2.setEditor(timeEditor1);
        pnl.add(spinTuGio2);

        pnl.add(makeFilterLabel("Đến giờ:"));
        spinDenGio2 = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor timeEditor2 = new JSpinner.DateEditor(spinDenGio2, "HH:mm");
        spinDenGio2.setEditor(timeEditor2);
        pnl.add(spinDenGio2);

        pnl.add(makeFilterLabel("Tên/Mã SP:"));
        txtSearchSP2 = makeFilterField(12);
        pnl.add(txtSearchSP2);

        pnl.add(makeFilterLabel("Trạng thái:"));
        cboTrangThai2 = new JComboBox<>(new String[]{"Tất cả", "Thành công", "Đã hủy"});
        cboTrangThai2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pnl.add(cboTrangThai2);

        JButton btnSearch = createSearchButton();
        pnl.add(btnSearch);
        return pnl;
    }

    private JPanel createTab3Panel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        pnl.setBackground(Color.WHITE);
        pnl.setBorder(new EmptyBorder(8, 16, 8, 16));

        pnl.add(makeFilterLabel("Từ ngày:"));
        dpFromDate3 = new DatePickerField();
        dpFromDate3.setPreferredSize(new Dimension(130, 28));
        pnl.add(dpFromDate3);

        pnl.add(makeFilterLabel("Đến ngày:"));
        dpToDate3 = new DatePickerField();
        dpToDate3.setPreferredSize(new Dimension(130, 28));
        pnl.add(dpToDate3);

        pnl.add(makeFilterLabel("Mã HĐ Trả:"));
        txtSearchMaHD3 = makeFilterField(8);
        pnl.add(txtSearchMaHD3);

        JButton btnSearch = createSearchButton();
        pnl.add(btnSearch);
        return pnl;
    }

    private JButton createSearchButton() {
        JButton btnSearch = new JButton("Tìm kiếm");
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSearch.setBackground(AppColors.PRIMARY);
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFocusPainted(false);
        btnSearch.setBorderPainted(false);
        btnSearch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSearch.addActionListener(e -> { currentPage = 1; loadInvoices(); });
        return btnSearch;
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
                BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1),
                new EmptyBorder(4, 8, 4, 8)));
        return tf;
    }

    private InvoiceFilterCriteria buildCriteria() {
        InvoiceFilterCriteria c = new InvoiceFilterCriteria();
        int tab = tabbedPane.getSelectedIndex();
        if (tab == 0) {
            c.setCustomerTypeFilter(1); // Đăng ký
            c.setInvoiceTypeFilter(1); // SALE
            c.setFromDate(dpFromDate1.getDate());
            c.setToDate(dpToDate1.getDate());
            c.setSearchKH(txtSearchKH1.getText());
            c.setSearchMaHD(txtSearchMaHD1.getText());
            c.setStatusFilter(cboTrangThai1.getSelectedIndex());
        } else if (tab == 1) {
            c.setCustomerTypeFilter(2); // Vãng lai
            c.setInvoiceTypeFilter(1); // SALE
            if (dpNgayMua2.getDate() != null) {
                c.setFromDate(dpNgayMua2.getDate());
                c.setToDate(dpNgayMua2.getDate());
            }
            c.setFromTime(new java.text.SimpleDateFormat("HH:mm").format(spinTuGio2.getValue()));
            c.setToTime(new java.text.SimpleDateFormat("HH:mm").format(spinDenGio2.getValue()));
            c.setSearchSP(txtSearchSP2.getText());
            c.setStatusFilter(cboTrangThai2.getSelectedIndex());
        } else if (tab == 2) {
            c.setCustomerTypeFilter(0); // All customers
            c.setInvoiceTypeFilter(2); // RETURN
            c.setFromDate(dpFromDate3.getDate());
            c.setToDate(dpToDate3.getDate());
            c.setSearchMaHD(txtSearchMaHD3.getText());
            c.setStatusFilter(0);
        }
        return c;
    }

    private void loadInvoices() {
        try {
            InvoiceFilterCriteria c = buildCriteria();
            PagedResult<InvoiceListDTO> pr = invoiceService.searchInvoices(c, currentPage, PAGE_SIZE);

            tableModel.setRowCount(0);
            totalPages = pr.getTotalPages();
            
            int stt = (currentPage - 1) * PAGE_SIZE;
            for (InvoiceListDTO dto : pr.getData()) {
                stt++;
                String ngayStr = dto.getNgayBan() != null ? dto.getNgayBan().format(DATETIME_FMT) : "---";
                String tienStr = dto.getTongTien() != null ? String.format("%,.0f VNĐ", dto.getTongTien()) : "0";
                
                String displayTT = "Thành công";
                if (dto.getTrangThai() != null && (dto.getTrangThai().contains("huy") || dto.getTrangThai().contains("Hủy"))) {
                    displayTT = "Đã hủy";
                }

                tableModel.addRow(new Object[]{
                    stt,
                    dto.getMaHD(),
                    ngayStr,
                    dto.getTenKH() != null ? dto.getTenKH() : "Khách vãng lai",
                    dto.getSoDT() != null ? dto.getSoDT() : "---",
                    dto.getTenNV(),
                    dto.getPhuongThucTT(),
                    tienStr,
                    displayTT
                });
            }

            // Update pagination UI
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

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi tải hóa đơn: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================================================================
    //  TABLE & DETAIL
    // ================================================================
    private JScrollPane createTablePanel() {
        String[] cols = {"STT", "Mã HĐ", "Ngày", "Khách hàng", "SĐT", "Nhân viên", "Phương thức", "Tổng tiền", "Trạng thái"};
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
        table.getColumnModel().getColumn(1).setPreferredWidth(60);
        table.getColumnModel().getColumn(2).setPreferredWidth(130);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(130);
        table.getColumnModel().getColumn(6).setPreferredWidth(90);
        table.getColumnModel().getColumn(7).setPreferredWidth(110);
        table.getColumnModel().getColumn(8).setPreferredWidth(90);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int r, int c) {
                Object displayVal = (c == 0) ? (r + 1) : v;
                Component comp = super.getTableCellRendererComponent(t, displayVal, sel, foc, r, c);
                if (!sel) comp.setBackground(r % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);
                setFont(new Font("Segoe UI", Font.PLAIN, 12));
                if (!sel) comp.setForeground(AppColors.TEXT_PRIMARY);

                if (c == 0 || c == 1) setHorizontalAlignment(SwingConstants.CENTER);
                else if (c == 7) {
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

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    showDetailDialog((int) tableModel.getValueAt(table.getSelectedRow(), 1));
                }
            }
        });

        JPopupMenu popup = new JPopupMenu();
        JMenuItem menuDetail = new JMenuItem("Xem chi tiết");
        menuDetail.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r >= 0) showDetailDialog((int) tableModel.getValueAt(r, 1));
        });
        popup.add(menuDetail);

        JMenuItem menuReturn = new JMenuItem("Trả hàng");
        menuReturn.setForeground(new Color(0x17, 0xA2, 0xB8));
        menuReturn.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r >= 0) {
                int maHD = (int) tableModel.getValueAt(r, 1);
                Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
                new presentation.dialog.ReturnInvoiceDialog(owner, maHD).setVisible(true);
                loadInvoices();
            }
        });
        popup.add(menuReturn);
        popup.addSeparator();

        JMenuItem menuVoid = new JMenuItem("Hủy hóa đơn");
        menuVoid.setForeground(AppColors.DANGER);
        menuVoid.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r >= 0) doVoidInvoice((int) tableModel.getValueAt(r, 1));
        });
        popup.add(menuVoid);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) { tryPopup(e); }
            @Override public void mouseReleased(java.awt.event.MouseEvent e) { tryPopup(e); }
            private void tryPopup(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        table.setRowSelectionInterval(row, row);
                        String status = tableModel.getValueAt(row, 8).toString();
                        boolean isActive = !status.contains("hủy") && !status.contains("Hủy");
                        menuVoid.setEnabled(isActive);
                        menuReturn.setEnabled(isActive);
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

    private void showDetailDialog(int maHD) {
        try {
            Invoice inv = invoiceService.getInvoiceHeader(maHD);
            if (inv == null) return;
            java.util.List<InvoiceDetailDTO> details = invoiceService.getInvoiceDetails(maHD);

            String[] detailCols = {"Tên sản phẩm", "Số Lô", "Số lượng", "Đơn giá", "Thành tiền"};
            DefaultTableModel detailModel = new DefaultTableModel(detailCols, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            
            for (InvoiceDetailDTO d : details) {
                detailModel.addRow(new Object[]{
                    d.getTenSP(), d.getSoLo(), d.getSoLuong(),
                    String.format("%,.0f VNĐ", d.getDonGia()),
                    String.format("%,.0f VNĐ", d.getThanhTien())
                });
            }

            JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chi tiết hóa đơn #" + maHD, true);
            dlg.setSize(800, 500);
            dlg.setLocationRelativeTo(this);

            JPanel content = new JPanel(new BorderLayout());
            content.setBackground(Color.WHITE);

            boolean isVoided = inv.getTrangThai() != null && inv.getTrangThai().contains("huy");
            JPanel hdr = new JPanel(new BorderLayout());
            hdr.setBackground(isVoided ? AppColors.DANGER : new Color(0x17, 0xA2, 0xB8));
            hdr.setBorder(new EmptyBorder(14, 20, 14, 20));

            JLabel lblH = new JLabel("Hóa đơn #" + maHD + (isVoided ? " — ĐÃ HỦY" : ""));
            lblH.setFont(new Font("Segoe UI", Font.BOLD, 15));
            lblH.setForeground(Color.WHITE);
            hdr.add(lblH, BorderLayout.WEST);
            content.add(hdr, BorderLayout.NORTH);

            JPanel body = new JPanel(new BorderLayout());
            body.setBackground(Color.WHITE);

            JPanel info = new JPanel(new GridLayout(0, 2, 10, 4));
            info.setBackground(Color.WHITE);
            info.setBorder(new EmptyBorder(12, 20, 12, 20));
            info.add(makeInfoLabel("Ngày:"));
            info.add(makeInfoValue(inv.getNgayBan() != null ? inv.getNgayBan().format(DATETIME_FMT) : "---"));
            info.add(makeInfoLabel("Khách hàng:"));
            info.add(makeInfoValue((inv.getTenKH() != null ? inv.getTenKH() : "Khách vãng lai") + " (" + (inv.getSoDT() != null ? inv.getSoDT() : "---") + ")"));
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

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi xem chi tiết: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
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

    private void doVoidInvoice(int maHD) {
        int row = table.getSelectedRow();
        if (row < 0) return;
        String currentStatus = tableModel.getValueAt(row, 8).toString();
        if (currentStatus.contains("hủy") || currentStatus.contains("Hủy")) {
            JOptionPane.showMessageDialog(this, "Hóa đơn này đã bị hủy trước đó.");
            return;
        }

        String lyDo = JOptionPane.showInputDialog(this, "Nhập lý do hủy hóa đơn #" + maHD + ":");
        if (lyDo == null || lyDo.trim().isEmpty()) return;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Xác nhận HỦY hóa đơn #" + maHD + "?\nLý do: " + lyDo + "\n(Thu hồi tồn kho)",
                "Xác nhận hủy", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            invoiceService.voidInvoice(maHD, lyDo.trim());
            JOptionPane.showMessageDialog(this, "Đã hủy hóa đơn #" + maHD + " thành công (Hoàn trả tồn kho).");
            loadInvoices();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi hủy hóa đơn: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

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
        btn.setForeground(AppColors.PRIMARY);
        btn.setBackground(Color.WHITE);
        btn.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1));
        return btn;
    }

    private JButton createPageNumButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setPreferredSize(new Dimension(32, 28));
        btn.setFocusPainted(false);
        btn.setBackground(Color.WHITE);
        btn.setForeground(AppColors.PRIMARY);
        btn.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1));
        return btn;
    }
}
