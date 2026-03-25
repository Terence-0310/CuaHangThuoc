package presentation.dialog;

import common.AppColors;
import domain.entity.Batch;
import domain.entity.Product;
import infrastructure.repository.NhapKhoDAO;
import infrastructure.repository.ProductRepositoryImpl;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog: Chi ti\u1ebft s\u1ea3n ph\u1ea9m + b\u1ea3ng l\u00f4 h\u00e0ng
 *
 * \u2605 TRAP #1: Right-click selection \u2014 handled in ProductPanel
 * \u2605 TRAP #2: Checkbox "Ch\u1ec9 hi\u1ec7n l\u00f4 c\u00f2n h\u00e0ng" (default ON)
 * \u2605 TRAP #3: LocalDate only (no time component)
 */
public class ProductDetailDialog extends JDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final List<Batch> allBatches;
    private DefaultTableModel batchModel;
    private JTable batchTable;

    public ProductDetailDialog(Frame parent, int maSP) {
        super(parent, "Chi ti\u1ebft s\u1ea3n ph\u1ea9m", true);
        setSize(850, 560);
        setLocationRelativeTo(parent);
        setResizable(true);

        ProductRepositoryImpl productRepo = new ProductRepositoryImpl();
        NhapKhoDAO nhapKhoDAO = new NhapKhoDAO();

        Product product = productRepo.getById(maSP);
        if (product == null) {
            allBatches = new ArrayList<>();
            JOptionPane.showMessageDialog(parent, "Kh\u00f4ng t\u00ecm th\u1ea5y s\u1ea3n ph\u1ea9m!", "L\u1ed7i", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        allBatches = nhapKhoDAO.getBatchesByMaSP(maSP);
        int tongTon = 0;
        for (Batch b : allBatches) tongTon += b.getSoLuong();

        JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
        contentPanel.setBackground(AppColors.NEUTRAL);
        contentPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // === TOP: Product Info ===
        contentPanel.add(createProductInfoPanel(product, tongTon), BorderLayout.NORTH);

        // === CENTER: Filter + Batch Table ===
        JPanel centerPanel = new JPanel(new BorderLayout(0, 6));
        centerPanel.setOpaque(false);

        JCheckBox chkOnlyInStock = new JCheckBox("Ch\u1ec9 hi\u1ec7n l\u00f4 c\u00f2n h\u00e0ng", true);
        chkOnlyInStock.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chkOnlyInStock.setOpaque(false);
        chkOnlyInStock.setForeground(AppColors.TEXT_SECONDARY);
        chkOnlyInStock.addActionListener(e -> refreshBatchTable(chkOnlyInStock.isSelected()));

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filterBar.setOpaque(false);
        filterBar.add(chkOnlyInStock);
        centerPanel.add(filterBar, BorderLayout.NORTH);
        centerPanel.add(createBatchTable(), BorderLayout.CENTER);

        contentPanel.add(centerPanel, BorderLayout.CENTER);

        // === BOTTOM: Close button ===
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottomPanel.setOpaque(false);
        JButton btnClose = new JButton("\u0110\u00f3ng");
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
        refreshBatchTable(true);
    }

    // ================================================================
    //  Product Info Panel
    // ================================================================

    private JPanel createProductInfoPanel(Product p, int tongTon) {
        JPanel panel = new JPanel(new GridLayout(2, 3, 16, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AppColors.NEUTRAL_DARK, 1),
                new EmptyBorder(14, 18, 14, 18)
        ));

        addInfoField(panel, "M\u00e3 SP", String.valueOf(p.getMaSP()));
        addInfoField(panel, "T\u00ean s\u1ea3n ph\u1ea9m", p.getTenSP());
        addInfoField(panel, "\u0110\u01a1n v\u1ecb t\u00ednh", p.getDonViTinh());
        addInfoField(panel, "Giá bán",
                p.getGiaBan() != null ? String.format("%,.0f VNĐ", p.getGiaBan()) : "---");

        JPanel tonPanel = new JPanel(new BorderLayout());
        tonPanel.setOpaque(false);
        JLabel lblKey = new JLabel("T\u1ed5ng t\u1ed3n kho");
        lblKey.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblKey.setForeground(AppColors.TEXT_SECONDARY);
        JLabel lblVal = new JLabel(String.format("%,d", tongTon));
        lblVal.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblVal.setForeground(tongTon > 0 ? AppColors.SUCCESS : AppColors.DANGER);
        tonPanel.add(lblKey, BorderLayout.NORTH);
        tonPanel.add(lblVal, BorderLayout.CENTER);
        panel.add(tonPanel);

        return panel;
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

    // ================================================================
    //  Batch Table (+ NCC column)
    // ================================================================

    private JScrollPane createBatchTable() {
        String[] cols = {"STT", "M\u00e3 L\u00f4", "S\u1ed1 L\u00f4", "S\u1ed1 L\u01b0\u1ee3ng", "H\u1ea1n S\u1eed D\u1ee5ng", "Nh\u00e0 Cung C\u1ea5p", "Tr\u1ea1ng Th\u00e1i"};
        batchModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        batchTable = new JTable(batchModel);
        batchTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        batchTable.setRowHeight(30);
        batchTable.setShowGrid(true);
        batchTable.setGridColor(AppColors.NEUTRAL_DARK);
        batchTable.setFillsViewportHeight(true);
        batchTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader th = batchTable.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 12));
        th.setBackground(AppColors.TABLE_HEADER_BG);
        th.setForeground(AppColors.TABLE_HEADER_FG);
        th.setPreferredSize(new Dimension(0, 34));
        th.setReorderingAllowed(false);

        // Column widths
        batchTable.getColumnModel().getColumn(0).setPreferredWidth(35);
        batchTable.getColumnModel().getColumn(0).setMaxWidth(45);
        batchTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        batchTable.getColumnModel().getColumn(2).setPreferredWidth(55);
        batchTable.getColumnModel().getColumn(2).setMaxWidth(65);
        batchTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        batchTable.getColumnModel().getColumn(4).setPreferredWidth(95);
        batchTable.getColumnModel().getColumn(5).setPreferredWidth(150);
        batchTable.getColumnModel().getColumn(6).setPreferredWidth(100);

        // Center align
        DefaultTableCellRenderer centerR = new DefaultTableCellRenderer();
        centerR.setHorizontalAlignment(SwingConstants.CENTER);
        batchTable.getColumnModel().getColumn(0).setCellRenderer(centerR);
        batchTable.getColumnModel().getColumn(2).setCellRenderer(centerR);
        batchTable.getColumnModel().getColumn(3).setCellRenderer(centerR);
        batchTable.getColumnModel().getColumn(4).setCellRenderer(centerR);

        // Status column — color renderer
        batchTable.getColumnModel().getColumn(6).setCellRenderer(new StatusCellRenderer());

        // Alt row renderer
        batchTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) c.setBackground(row % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(batchTable);
        scroll.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1));
        return scroll;
    }

    // ================================================================
    //  Dynamic refresh — filter + NCC column
    // ================================================================

    private void refreshBatchTable(boolean onlyInStock) {
        batchModel.setRowCount(0);

        LocalDate today = LocalDate.now();
        int stt = 1;

        for (Batch b : allBatches) {
            if (onlyInStock && b.getSoLuong() <= 0) continue;

            String status = calcStatus(b, today);
            String hsd = b.getHanSuDung() != null ? b.getHanSuDung().format(DATE_FMT) : "---";
            String ncc = b.getTenNCC() != null ? b.getTenNCC() : "---";
            batchModel.addRow(new Object[]{
                    stt++,
                    b.getSoLo() != null ? b.getSoLo() : "---",
                    b.getMaLo(),
                    b.getSoLuong(),
                    hsd,
                    ncc,
                    status
            });
        }

        if (batchModel.getRowCount() == 0) {
            String msg = onlyInStock ? "Kh\u00f4ng c\u00f3 l\u00f4 n\u00e0o c\u00f2n h\u00e0ng" : "Ch\u01b0a c\u00f3 l\u00f4 h\u00e0ng";
            batchModel.addRow(new Object[]{"", "", "", "", "", "", msg});
        }
    }

    // ================================================================
    //  Status logic — LocalDate only (TRAP #3)
    // ================================================================

    private String calcStatus(Batch b, LocalDate today) {
        // ★ Ưu tiên #1: Hết hàng (SoLuong=0) → không cần xét HSD
        // Lý do: Nếu lô đã bán sạch + hết HSD, nhân viên thấy "Hết HSD" đỏ lòm
        // sẽ chạy vào kho tìm thuốc để vứt → nhưng kho trống! Lãng phí công sức.
        if (b.getSoLuong() == 0) return "H\u1ebft h\u00e0ng";
        // ★ Ưu tiên #2: Sắp hết (SoLuong ≤ 10)
        if (b.getSoLuong() <= 10) {
            // Nếu vừa sắp hết VÀ hết HSD → ưu tiên "Hết HSD" (nguy hiểm hơn)
            if (b.getHanSuDung() != null && b.getHanSuDung().isBefore(today)) return "H\u1ebft HSD";
            return "S\u1eafp h\u1ebft";
        }
        // ★ Ưu tiên #3: Hết hạn sử dụng
        if (b.getHanSuDung() != null && b.getHanSuDung().isBefore(today)) return "H\u1ebft HSD";
        // ★ Ưu tiên #4: Cận date (≤ 30 ngày)
        if (b.getHanSuDung() != null && !b.getHanSuDung().isAfter(today.plusDays(30))) return "C\u1eadn Date";
        // ★ Mặc định: Tốt
        return "T\u1ed1t";
    }

    /**
     * Custom Renderer cho c\u1ed9t Tr\u1ea1ng Th\u00e1i
     */
    private static class StatusCellRenderer extends DefaultTableCellRenderer {
        StatusCellRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }
        @Override
        public Component getTableCellRendererComponent(JTable t, Object val,
                boolean sel, boolean foc, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
            if (sel) return c;

            String status = val != null ? val.toString() : "";
            switch (status) {
                case "H\u1ebft HSD":
                case "H\u1ebft h\u00e0ng":
                    c.setBackground(new Color(0xF8, 0xD7, 0xDA));
                    c.setForeground(new Color(0x72, 0x1C, 0x24));
                    setFont(getFont().deriveFont(Font.BOLD));
                    break;
                case "C\u1eadn Date":
                case "S\u1eafp h\u1ebft":
                    c.setBackground(new Color(0xFF, 0xF3, 0xCD));
                    c.setForeground(new Color(0x85, 0x6D, 0x04));
                    setFont(getFont().deriveFont(Font.BOLD));
                    break;
                case "T\u1ed1t":
                    c.setBackground(new Color(0xD4, 0xED, 0xDA));
                    c.setForeground(new Color(0x15, 0x57, 0x24));
                    setFont(getFont().deriveFont(Font.PLAIN));
                    break;
                default:
                    c.setBackground(row % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);
                    c.setForeground(AppColors.TEXT_SECONDARY);
                    break;
            }
            return c;
        }
    }
}
