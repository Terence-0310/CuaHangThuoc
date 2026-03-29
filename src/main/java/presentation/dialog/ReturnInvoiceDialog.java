package presentation.dialog;

import common.AppColors;
import common.CurrencyFormatter;
import common.ServiceFactory;
import common.Session;
import service.IInvoiceService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * ReturnInvoiceDialog — Trả hàng khách hàng từ hóa đơn gốc
 *
 * Luồng:
 *  1. Load danh sách Chi tiết HĐ gốc (từng lô, trừ SL đã trả trước đó)
 *  2. User nhập SL trả cho từng dòng (cột SpinnerEditor)
 *  3. Click "Xác nhận trả" → build XML → gọi sp_TraHangKhach
 */
public class ReturnInvoiceDialog extends JDialog {

    private final IInvoiceService invoiceService = ServiceFactory.getInvoiceService();
    private final int maHDGoc;
    private DefaultTableModel tableModel;
    private JTable table;
    private JLabel lblTotalRefund;
    private JTextArea txtLyDo;

    // Data columns from getReturnBatchData:
    // [0] MaLo, [1] MaSP, [2] SoLuongMua, [3] DaTra, [4] DonGia(BigDecimal),
    // [5] TenSP, [6] SoLo, [7] HSD
    private List<Object[]> batchData;

    public ReturnInvoiceDialog(Frame owner, int maHDGoc) {
        super(owner, "Trả hàng — Hóa đơn #" + maHDGoc, true);
        this.maHDGoc = maHDGoc;
        initComponents();
        loadData();
        setSize(850, 600);
        setMinimumSize(new Dimension(750, 500));
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));

        // === HEADER ===
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0x17, 0xA2, 0xB8));
        header.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel lblTitle = new JLabel("Trả Hàng — HĐ #" + maHDGoc);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(Color.WHITE);
        header.add(lblTitle, BorderLayout.WEST);

        JLabel lblNote = new JLabel("Nhập số lượng cần trả cho từng dòng");
        lblNote.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblNote.setForeground(new Color(0xD1, 0xEC, 0xF1));
        header.add(lblNote, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // === TABLE ===
        String[] cols = {"Tên SP", "Số Lô", "HSD", "Đã mua", "Đã trả", "Có thể trả", "SL Trả", "Đơn giá", "Tiền hoàn"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 6; // only "SL Trả" column editable
            }

            @Override
            public Class<?> getColumnClass(int col) {
                if (col == 3 || col == 4 || col == 5 || col == 6) return Integer.class;
                return String.class;
            }
        };

        table = new JTable(tableModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(32);
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

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(180); // TenSP
        table.getColumnModel().getColumn(1).setPreferredWidth(90);  // SoLo
        table.getColumnModel().getColumn(2).setPreferredWidth(85);  // HSD
        table.getColumnModel().getColumn(3).setPreferredWidth(60);  // Đã mua
        table.getColumnModel().getColumn(4).setPreferredWidth(60);  // Đã trả
        table.getColumnModel().getColumn(5).setPreferredWidth(70);  // Có thể trả
        table.getColumnModel().getColumn(6).setPreferredWidth(65);  // SL Trả
        table.getColumnModel().getColumn(7).setPreferredWidth(100); // Đơn giá
        table.getColumnModel().getColumn(8).setPreferredWidth(110); // Tiền hoàn

        // Alt-row colors
        table.setDefaultRenderer(Object.class, createAltRowRenderer());
        table.setDefaultRenderer(Integer.class, createAltRowRenderer());

        // Spinner editor for "SL Trả" column
        table.getColumnModel().getColumn(6).setCellEditor(new SpinnerCellEditor());

        // Listen for changes to recalculate totals
        tableModel.addTableModelListener(e -> {
            int col = e.getColumn();
            if (col == 6 || col == javax.swing.event.TableModelEvent.ALL_COLUMNS) {
                recalculate();
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        scroll.getViewport().setBackground(Color.WHITE);
        add(scroll, BorderLayout.CENTER);

        // === BOTTOM ===
        JPanel bottom = new JPanel(new BorderLayout(12, 0));
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(new EmptyBorder(12, 20, 16, 20));

        // Left: Lý do
        JPanel leftPanel = new JPanel(new BorderLayout(0, 4));
        leftPanel.setOpaque(false);

        JLabel lblLyDo = new JLabel("Lý do trả hàng *");
        lblLyDo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblLyDo.setForeground(AppColors.TEXT_PRIMARY);
        leftPanel.add(lblLyDo, BorderLayout.NORTH);

        txtLyDo = new JTextArea(3, 30);
        txtLyDo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtLyDo.setLineWrap(true);
        txtLyDo.setWrapStyleWord(true);
        txtLyDo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1),
                new EmptyBorder(6, 8, 6, 8)));
        JScrollPane lyDoScroll = new JScrollPane(txtLyDo);
        lyDoScroll.setBorder(null);
        leftPanel.add(lyDoScroll, BorderLayout.CENTER);

        bottom.add(leftPanel, BorderLayout.CENTER);

        // Right: Total + Buttons
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(250, 0));

        lblTotalRefund = new JLabel("Tổng hoàn: 0 ₫");
        lblTotalRefund.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTotalRefund.setForeground(new Color(0x17, 0xA2, 0xB8));
        lblTotalRefund.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(lblTotalRefund);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        JButton btnConfirm = new JButton("Xác nhận trả hàng");
        btnConfirm.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnConfirm.setBackground(new Color(0x17, 0xA2, 0xB8));
        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.setFocusPainted(false);
        btnConfirm.setBorderPainted(false);
        btnConfirm.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnConfirm.setMaximumSize(new Dimension(250, 38));
        btnConfirm.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnConfirm.addActionListener(e -> doReturn());

        // Hover
        btnConfirm.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnConfirm.setBackground(new Color(0x13, 0x8D, 0x9E));
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btnConfirm.setBackground(new Color(0x17, 0xA2, 0xB8));
            }
        });

        rightPanel.add(btnConfirm);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 6)));

        JButton btnCancel = new JButton("Hủy bỏ");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnCancel.setBackground(Color.WHITE);
        btnCancel.setForeground(AppColors.TEXT_SECONDARY);
        btnCancel.setFocusPainted(false);
        btnCancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCancel.setMaximumSize(new Dimension(250, 34));
        btnCancel.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnCancel.addActionListener(e -> dispose());
        rightPanel.add(btnCancel);

        bottom.add(rightPanel, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
    }

    // ================================================================
    //  DATA
    // ================================================================

    private void loadData() {
        tableModel.setRowCount(0);
        batchData = invoiceService.getReturnBatchData(maHDGoc);

        if (batchData.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Hóa đơn này không có mặt hàng nào có thể trả.\n" +
                    "(Đã trả hết hoặc không hợp lệ)",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            SwingUtilities.invokeLater(this::dispose);
            return;
        }

        for (Object[] row : batchData) {
            int soLuongMua = (int) row[2];
            int daTra = (int) row[3];
            int conLai = soLuongMua - daTra;
            BigDecimal donGia = (BigDecimal) row[4];

            tableModel.addRow(new Object[]{
                    row[5],  // TenSP
                    row[6],  // SoLo
                    row[7],  // HSD
                    soLuongMua,
                    daTra,
                    conLai,
                    0,       // SL Trả (user input)
                    CurrencyFormatter.format(donGia),
                    "0 ₫"    // Tiền hoàn
            });
        }
    }

    private boolean recalculating = false;

    private void recalculate() {
        if (recalculating) return; // ★ guard against re-entry
        recalculating = true;
        try {
            BigDecimal total = BigDecimal.ZERO;
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                Object slObj = tableModel.getValueAt(i, 6);
                int slTra = slObj instanceof Integer ? (int) slObj : 0;
                int conLai = (int) tableModel.getValueAt(i, 5);

                // Clamp
                if (slTra < 0) slTra = 0;
                if (slTra > conLai) slTra = conLai;
                tableModel.setValueAt(slTra, i, 6);

                BigDecimal donGia = (BigDecimal) batchData.get(i)[4];
                BigDecimal refund = donGia.multiply(BigDecimal.valueOf(slTra));
                tableModel.setValueAt(CurrencyFormatter.format(refund), i, 8);
                total = total.add(refund);
            }
            lblTotalRefund.setText("Tổng hoàn: " + CurrencyFormatter.format(total));
        } finally {
            recalculating = false;
        }
    }

    // ================================================================
    //  EXECUTE RETURN
    // ================================================================

    private void doReturn() {
        // Stop editing
        if (table.isEditing()) table.getCellEditor().stopCellEditing();

        String lyDo = txtLyDo.getText().trim();
        if (lyDo.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập lý do trả hàng.",
                    "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            txtLyDo.requestFocusInWindow();
            return;
        }

        // Build XML: <items><i maLo="1" maSP="2" soLuong="3" donGia="50000"/></items>
        StringBuilder xml = new StringBuilder("<items>");
        int totalItems = 0;

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Object slObj = tableModel.getValueAt(i, 6);
            int slTra = slObj instanceof Integer ? (int) slObj : 0;
            if (slTra <= 0) continue;

            int maLo = (int) batchData.get(i)[0];
            int maSP = (int) batchData.get(i)[1];
            BigDecimal donGia = (BigDecimal) batchData.get(i)[4];

            xml.append(String.format("<i maLo=\"%d\" maSP=\"%d\" soLuong=\"%d\" donGia=\"%s\"/>",
                    maLo, maSP, slTra, donGia.toBigInteger().toString()));
            totalItems += slTra;
        }
        xml.append("</items>");

        if (totalItems == 0) {
            JOptionPane.showMessageDialog(this,
                    "Chưa nhập số lượng trả cho bất kỳ sản phẩm nào.",
                    "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Confirm
        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("Xác nhận trả %d sản phẩm?\n\n%s\n\nLý do: %s",
                        totalItems, lblTotalRefund.getText(), lyDo),
                "Xác nhận trả hàng", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        // Execute
        try {
            int maND = Session.getCurrentUser().getMaND();
            int maHDTra = invoiceService.executeReturnInvoice(maHDGoc, maND, lyDo, xml.toString());
            JOptionPane.showMessageDialog(this,
                    "Trả hàng thành công!\n\nMã HĐ trả: #" + maHDTra + "\n" + lblTotalRefund.getText(),
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi trả hàng: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================================================================
    //  UI HELPERS
    // ================================================================

    private DefaultTableCellRenderer createAltRowRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) c.setBackground(row % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);

                // Align
                if (col >= 3 && col <= 6) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else if (col == 7 || col == 8) {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }

                // Color "Có thể trả"
                if (col == 5 && val instanceof Integer) {
                    int v = (int) val;
                    if (!sel) c.setForeground(v > 0 ? AppColors.SUCCESS : AppColors.TEXT_SECONDARY);
                    setFont(getFont().deriveFont(Font.BOLD));
                }
                // Color "Tiền hoàn"
                else if (col == 8) {
                    if (!sel) c.setForeground(new Color(0x17, 0xA2, 0xB8));
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    if (!sel) c.setForeground(AppColors.TEXT_PRIMARY);
                }

                return c;
            }
        };
    }

    /**
     * SpinnerCellEditor — cho phép user nhập SL trả bằng JSpinner
     */
    private class SpinnerCellEditor extends DefaultCellEditor {
        private JSpinner spinner;

        SpinnerCellEditor() {
            super(new JTextField());
            spinner = new JSpinner(new SpinnerNumberModel(0, 0, 9999, 1));
            spinner.setFont(new Font("Segoe UI", Font.BOLD, 13));
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object val, boolean sel, int row, int col) {
            int conLai = (int) tableModel.getValueAt(row, 5);
            int current = val instanceof Integer ? (int) val : 0;
            spinner.setModel(new SpinnerNumberModel(current, 0, conLai, 1));
            return spinner;
        }

        @Override
        public Object getCellEditorValue() {
            return spinner.getValue();
        }

        @Override
        public boolean stopCellEditing() {
            try {
                spinner.commitEdit();
            } catch (java.text.ParseException e) {
                // ignore
            }
            return super.stopCellEditing();
        }
    }
}
