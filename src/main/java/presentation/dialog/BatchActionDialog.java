package presentation.dialog;

import common.AppColors;
import common.Session;
import infrastructure.database.DatabaseHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.text.DecimalFormat;

/**
 * BatchActionDialog — Modal xử lý lô hàng
 *
 * RETURN: Trả hàng NCC
 * DESTROY: Hủy hàng / Tiêu hủy
 *
 * Công thức: Đơn giá 1 SP = GiaNhapLo / SoLuongGoc
 *            Thành tiền   = Đơn giá 1 SP × SL nhập
 *
 * Validation:
 *   - DocumentFilter: chỉ cho phép số nguyên (chặn chữ, ký tự đặc biệt, số thập phân)
 *   - Real-time: SL > tồn kho → lỗi đỏ + disable nút xác nhận
 */
public class BatchActionDialog extends JDialog {

    public enum ActionType { RETURN, DESTROY }

    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0");

    // === Input ===
    private final ActionType actionType;
    private final int maLo;
    private final int maSP;
    private final String tenSP;
    private final String soLo;
    private final int tonKho;
    private final long giaNhapLo;
    private final int soLuongGoc;
    private final String tenNCC;
    private final BigDecimal donGia1SP;

    // === Form ===
    private JTextField txtSoLuong;
    private JTextField txtThanhTien;
    private JLabel lblError;
    private JComboBox<String> cboOption;
    private JComboBox<String> cboTinhTrang; // RETURN only: Tình trạng hàng
    private JTextArea txtLyDo;
    private JButton btnConfirm;
    private boolean confirmed = false;

    public BatchActionDialog(Window owner, ActionType type,
                             int maLo, int maSP, String tenSP, String soLo,
                             int tonKho, long giaNhapLo, int soLuongGoc, String tenNCC) {
        super(owner, type == ActionType.RETURN ? "Trả Hàng NCC" : "Hủy Hàng",
                ModalityType.APPLICATION_MODAL);
        this.actionType = type;
        this.maLo = maLo;
        this.maSP = maSP;
        this.tenSP = tenSP;
        this.soLo = soLo;
        this.tonKho = tonKho;
        this.giaNhapLo = giaNhapLo;
        this.soLuongGoc = soLuongGoc > 0 ? soLuongGoc : 1;
        this.tenNCC = (tenNCC == null || tenNCC.isEmpty() || "---".equals(tenNCC))
                ? "Không xác định" : tenNCC;
        this.donGia1SP = BigDecimal.valueOf(giaNhapLo)
                .divide(BigDecimal.valueOf(this.soLuongGoc), 0, RoundingMode.HALF_UP);

        initComponents();
        setSize(500, type == ActionType.RETURN ? 660 : 600);
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    // ================================================================
    //  UI
    // ================================================================

    private void initComponents() {
        boolean isReturn = actionType == ActionType.RETURN;
        Color accent = isReturn ? new Color(0x17, 0xA2, 0xB8) : AppColors.DANGER;

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // === HEADER ===
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(accent);
        header.setBorder(new EmptyBorder(14, 24, 14, 24));

        JLabel lblTitle = new JLabel(isReturn ? "Trả Hàng Nhà Cung Cấp" : "Hủy Hàng / Tiêu Hủy");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(Color.WHITE);
        header.add(lblTitle, BorderLayout.WEST);

        JLabel lblMaLo = new JLabel("Mã Lô: " + maLo);
        lblMaLo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblMaLo.setForeground(new Color(255, 255, 255, 200));
        header.add(lblMaLo, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // === FORM ===
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(14, 24, 8, 24));

        // Read-only
        addInfoRow(form, "Tên sản phẩm:", tenSP);
        addInfoRow(form, "Số lô:", soLo);
        addInfoRow(form, "Tồn kho hiện tại:", String.format("%,d", tonKho));
        if (isReturn) {
            addInfoRow(form, "Nhà cung cấp:", tenNCC);
        }
        addInfoRow(form, "Giá nhập cả lô:", MONEY_FMT.format(giaNhapLo) + " VNĐ");
        addInfoRow(form, "SL gốc khi nhập:", String.format("%,d", soLuongGoc));
        addInfoRow(form, "Đơn giá 1 SP:", MONEY_FMT.format(donGia1SP) + " VNĐ");

        addSeparator(form);

        // --- Số lượng (DocumentFilter: integer only) ---
        addLabel(form, isReturn ? "Số lượng trả *" : "Số lượng hủy *", accent);
        txtSoLuong = new JTextField();
        txtSoLuong.setFont(new Font("Segoe UI", Font.BOLD, 16));
        styleInput(txtSoLuong, accent);
        // ★ DocumentFilter: chỉ cho phép số nguyên
        ((AbstractDocument) txtSoLuong.getDocument()).setDocumentFilter(new IntegerOnlyFilter());
        form.add(txtSoLuong);

        // Error label (ẩn mặc định)
        lblError = new JLabel(" ");
        lblError.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblError.setForeground(AppColors.DANGER);
        lblError.setAlignmentX(LEFT_ALIGNMENT);
        form.add(lblError);
        form.add(Box.createRigidArea(new Dimension(0, 4)));

        // --- Thành tiền (auto-calc, read-only) ---
        addLabel(form, isReturn ? "Thành tiền hoàn lại:" : "Giá trị thiệt hại:", accent);
        txtThanhTien = new JTextField("0 VNĐ");
        txtThanhTien.setFont(new Font("Segoe UI", Font.BOLD, 18));
        txtThanhTien.setEditable(false);
        txtThanhTien.setForeground(isReturn ? new Color(0x28, 0xA7, 0x45) : AppColors.DANGER);
        txtThanhTien.setAlignmentX(LEFT_ALIGNMENT);
        txtThanhTien.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        txtThanhTien.setPreferredSize(new Dimension(0, 42));
        Color moneyBg = isReturn ? new Color(0xD4, 0xED, 0xDA) : new Color(0xF8, 0xD7, 0xDA);
        txtThanhTien.setBackground(moneyBg);
        txtThanhTien.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isReturn
                        ? new Color(0x28, 0xA7, 0x45) : AppColors.DANGER, 2),
                new EmptyBorder(0, 12, 0, 12)
        ));
        form.add(txtThanhTien);

        JLabel lblCalcHint = new JLabel("  = Đơn giá 1 SP x Số lượng (tự động)");
        lblCalcHint.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        lblCalcHint.setForeground(AppColors.TEXT_SECONDARY);
        lblCalcHint.setAlignmentX(LEFT_ALIGNMENT);
        form.add(lblCalcHint);
        form.add(Box.createRigidArea(new Dimension(0, 8)));

        // ★ DocumentListener → auto-calc + validate
        txtSoLuong.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { onQuantityChanged(); }
            public void removeUpdate(DocumentEvent e) { onQuantityChanged(); }
            public void changedUpdate(DocumentEvent e) {}
        });

        // --- Dropdown: HinhThuc / PhanLoai ---
        if (isReturn) {
            addLabel(form, "Hình thức nhận tiền:", accent);
            cboOption = new JComboBox<>(new String[]{"Tiền mặt", "Chuyển khoản"});
        } else {
            addLabel(form, "Phân loại lý do:", accent);
            cboOption = new JComboBox<>(new String[]{
                    "Hết hạn sử dụng", "Hư hỏng do bảo quản", "Lỗi bao bì", "Khác"
            });
        }
        cboOption.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cboOption.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        cboOption.setAlignmentX(LEFT_ALIGNMENT);
        form.add(cboOption);
        form.add(Box.createRigidArea(new Dimension(0, 8)));

        // --- ★ Tình trạng hàng (RETURN only) → Thu hồi logic ---
        if (isReturn) {
            addLabel(form, "Tình trạng hàng:", accent);
            cboTinhTrang = new JComboBox<>(new String[]{
                    "Lỗi bao bì", "Cận date", "Lỗi nghiêm trọng / Thu hồi"
            });
            cboTinhTrang.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            cboTinhTrang.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            cboTinhTrang.setAlignmentX(LEFT_ALIGNMENT);
            form.add(cboTinhTrang);
            form.add(Box.createRigidArea(new Dimension(0, 4)));

            // ★ ItemListener: nếu chọn "Thu hồi" → hiện nút xem lịch sử bán
            JButton btnViewSales = createFlatButton("Xem lịch sử bán lô này", new Color(0xE8, 0x3E, 0x8C));
            btnViewSales.setAlignmentX(LEFT_ALIGNMENT);
            btnViewSales.setVisible(false);
            btnViewSales.addActionListener(e -> showSalesHistoryDialog());
            form.add(btnViewSales);
            form.add(Box.createRigidArea(new Dimension(0, 6)));

            cboTinhTrang.addItemListener(e -> {
                if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                    String sel = (String) cboTinhTrang.getSelectedItem();
                    boolean isRecall = sel != null && sel.contains("Thu hồi");
                    btnViewSales.setVisible(isRecall);
                    if (isRecall) {
                        // Tự động mở
                        showSalesHistoryDialog();
                    }
                }
            });
        }

        // --- Ghi chú ---
        addLabel(form, "Ghi chú *", accent);
        txtLyDo = new JTextArea(3, 20);
        txtLyDo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtLyDo.setLineWrap(true);
        txtLyDo.setWrapStyleWord(true);
        txtLyDo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.NEUTRAL_DARKER, 1),
                new EmptyBorder(8, 10, 8, 10)));
        JScrollPane scrollLyDo = new JScrollPane(txtLyDo);
        scrollLyDo.setAlignmentX(LEFT_ALIGNMENT);
        scrollLyDo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        form.add(scrollLyDo);

        if (!isReturn) {
            txtLyDo.setText("Thuốc hết hạn sử dụng, tiêu hủy theo quy định");
        }

        form.add(Box.createVerticalGlue());
        root.add(form, BorderLayout.CENTER);

        // === BUTTONS ===
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnPanel.setBackground(AppColors.NEUTRAL);
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppColors.NEUTRAL_DARK));

        JButton btnCancel = createButton("Hủy bỏ", AppColors.SECONDARY);
        btnCancel.addActionListener(e -> dispose());

        btnConfirm = createButton(
                isReturn ? "Xác Nhận Trả Hàng" : "Xác Nhận Hủy", accent);
        btnConfirm.addActionListener(e -> doConfirm());

        btnPanel.add(btnCancel);
        btnPanel.add(btnConfirm);
        root.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(root);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                txtSoLuong.requestFocusInWindow();
            }
        });
    }

    // ================================================================
    //  ★ DocumentFilter: chỉ cho phép số nguyên
    // ================================================================

    private static class IntegerOnlyFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            if (string != null && string.matches("\\d+")) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (text != null && text.matches("\\d*")) {
                super.replace(fb, offset, length, text, attrs);
            }
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length)
                throws BadLocationException {
            super.remove(fb, offset, length);
        }
    }

    // ================================================================
    //  ★ Auto-Calculate + Real-Time Validation
    // ================================================================

    private void onQuantityChanged() {
        String text = txtSoLuong.getText().trim();
        if (text.isEmpty()) {
            txtThanhTien.setText("0 VNĐ");
            lblError.setText(" ");
            btnConfirm.setEnabled(true);
            return;
        }

        try {
            int sl = Integer.parseInt(text);
            if (sl < 0) {
                txtThanhTien.setText("0 VNĐ");
                lblError.setText("Số lượng không được âm");
                btnConfirm.setEnabled(false);
                return;
            }
            if (sl > tonKho) {
                // ★ VƯỢT TỒN KHO → lỗi đỏ + disable
                BigDecimal thanhTien = donGia1SP.multiply(BigDecimal.valueOf(sl));
                txtThanhTien.setText(MONEY_FMT.format(thanhTien) + " VNĐ");
                lblError.setText("Vượt tồn kho! Tối đa: " + String.format("%,d", tonKho));
                btnConfirm.setEnabled(false);
                return;
            }

            // OK
            BigDecimal thanhTien = donGia1SP.multiply(BigDecimal.valueOf(sl));
            txtThanhTien.setText(MONEY_FMT.format(thanhTien) + " VNĐ");
            lblError.setText("Tối đa: " + String.format("%,d", tonKho));
            lblError.setForeground(AppColors.TEXT_SECONDARY);
            btnConfirm.setEnabled(true);
        } catch (NumberFormatException e) {
            txtThanhTien.setText("--- VNĐ");
            lblError.setText("Số lượng không hợp lệ");
            btnConfirm.setEnabled(false);
        }
    }

    // ================================================================
    //  ★ Lịch sử bán hàng (Product Recall)
    // ================================================================

    private void showSalesHistoryDialog() {
        String sql =
            "SELECT hd.MaHD, hd.NgayBan, ct.SoLuong AS SLBan, " +
            "ISNULL(kh.HoTen, N'Khách vãng lai') AS TenKH, " +
            "ISNULL(kh.SoDienThoai, N'---') AS SDT, " +
            "ISNULL(kh.DiaChi, N'---') AS DiaChi " +
            "FROM ChiTietHoaDon ct " +
            "JOIN HoaDon hd ON ct.MaHD = hd.MaHD " +
            "LEFT JOIN KhachHang kh ON hd.MaKH = kh.MaKH " +
            "WHERE ct.MaLo = ? " +
            "ORDER BY hd.NgayBan DESC";

        String[] cols = {"Mã HĐ", "Ngày bán", "SL bán", "Khách hàng", "SĐT", "Địa chỉ"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maLo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ngay = rs.getTimestamp("NgayBan");
                    String ngayStr = ngay != null
                            ? ngay.toLocalDateTime().format(
                                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                            : "---";
                    model.addRow(new Object[]{
                        rs.getInt("MaHD"),
                        ngayStr,
                        rs.getInt("SLBan"),
                        rs.getNString("TenKH"),
                        rs.getNString("SDT"),
                        rs.getNString("DiaChi")
                    });
                }
            }
        } catch (SQLException e) {
            // Tables might not have MaLo FK — show empty
        }

        // Dialog
        JDialog dlg = new JDialog(this,
                "Lịch sử bán — Lô " + soLo + " (" + tenSP + ")",
                ModalityType.APPLICATION_MODAL);
        dlg.setSize(700, 400);
        dlg.setLocationRelativeTo(this);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.WHITE);

        // Header
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(new Color(0xE8, 0x3E, 0x8C));
        hdr.setBorder(new EmptyBorder(12, 20, 12, 20));
        JLabel lblH = new JLabel("THU HỒI — Danh sách KH đã mua lô " + soLo);
        lblH.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblH.setForeground(Color.WHITE);
        hdr.add(lblH, BorderLayout.WEST);
        JLabel lblC = new JLabel(model.getRowCount() + " hóa đơn");
        lblC.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblC.setForeground(new Color(255, 255, 255, 200));
        hdr.add(lblC, BorderLayout.EAST);
        content.add(hdr, BorderLayout.NORTH);

        if (model.getRowCount() == 0) {
            JLabel emptyLbl = new JLabel("Lô này chưa bán cho khách hàng nào.", SwingConstants.CENTER);
            emptyLbl.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            emptyLbl.setForeground(AppColors.TEXT_SECONDARY);
            content.add(emptyLbl, BorderLayout.CENTER);
        } else {
            JTable salesTable = new JTable(model);
            salesTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            salesTable.setRowHeight(28);
            salesTable.setShowGrid(false);
            salesTable.setFillsViewportHeight(true);
            salesTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
            salesTable.getTableHeader().setBackground(new Color(0xF8, 0xD7, 0xDA));

            // Highlight SĐT column
            salesTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable t, Object v,
                        boolean sel, boolean foc, int r, int c) {
                    Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                    setFont(new Font("Segoe UI", Font.BOLD, 12));
                    setForeground(new Color(0xE8, 0x3E, 0x8C));
                    if (!sel) setBackground(new Color(0xFF, 0xF3, 0xF8));
                    return comp;
                }
            });

            JScrollPane sp = new JScrollPane(salesTable);
            sp.setBorder(BorderFactory.createEmptyBorder());
            content.add(sp, BorderLayout.CENTER);

            // Warning
            JPanel warn = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
            warn.setBackground(new Color(0xFD, 0xF2, 0xE9));
            JLabel warnLbl = new JLabel("Liên hệ khách hàng để thu hồi sản phẩm lỗi nghiêm trọng!");
            warnLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            warnLbl.setForeground(new Color(0xE8, 0x6B, 0x00));
            warn.add(warnLbl);
            content.add(warn, BorderLayout.SOUTH);
        }

        dlg.setContentPane(content);
        dlg.setVisible(true);
    }

    // ================================================================
    //  CONFIRM & TRANSACTION
    // ================================================================

    private void doConfirm() {
        boolean isReturn = actionType == ActionType.RETURN;

        String slText = txtSoLuong.getText().trim();
        if (slText.isEmpty()) { warn("Vui lòng nhập số lượng!"); return; }

        int soLuong;
        try { soLuong = Integer.parseInt(slText); }
        catch (NumberFormatException e) { warn("Số lượng không hợp lệ!"); return; }

        if (soLuong <= 0) { warn("Số lượng phải > 0!"); return; }
        if (soLuong > tonKho) {
            warn("Số lượng (" + soLuong + ") vượt tồn kho (" + tonKho + ")!");
            return;
        }

        String ghiChu = txtLyDo.getText().trim();
        if (ghiChu.isEmpty()) { warn("Vui lòng nhập ghi chú!"); return; }
        if (ghiChu.length() < 5) { warn("Ghi chú phải >= 5 ký tự!"); return; }

        String option = (String) cboOption.getSelectedItem();
        BigDecimal tongTien = donGia1SP.multiply(BigDecimal.valueOf(soLuong));

        // Tình trạng hàng (RETURN)
        String tinhTrang = isReturn && cboTinhTrang != null
                ? (String) cboTinhTrang.getSelectedItem() : null;

        // Confirm
        String summary = (isReturn ? "TRẢ HÀNG NCC" : "HỦY HÀNG") + "\n\n" +
                "Sản phẩm: " + tenSP + "\n" +
                "Số lô: " + soLo + "\n" +
                "Số lượng: " + String.format("%,d", soLuong) + "\n" +
                "Đơn giá 1 SP: " + MONEY_FMT.format(donGia1SP) + " VNĐ\n" +
                (isReturn
                    ? "Tiền hoàn: " + MONEY_FMT.format(tongTien) + " VNĐ\n" +
                      "Hình thức: " + option + "\n" +
                      "Tình trạng: " + tinhTrang + "\n"
                    : "Thiệt hại: " + MONEY_FMT.format(tongTien) + " VNĐ\n" +
                      "Phân loại: " + option + "\n") +
                "Ghi chú: " + ghiChu + "\n" +
                "Tồn kho sau: " + String.format("%,d", tonKho - soLuong);

        int confirmRes = JOptionPane.showConfirmDialog(this, summary,
                "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmRes != JOptionPane.YES_OPTION) return;

        try {
            executeTransaction(soLuong, tongTien.longValue(), option, ghiChu, tinhTrang);
            confirmed = true;
            JOptionPane.showMessageDialog(this,
                    (isReturn ? "Trả hàng" : "Hủy hàng") + " thành công!\n" +
                    "Đã xử lý " + soLuong + " " + tenSP + "\n" +
                    "Thành tiền: " + MONEY_FMT.format(tongTien) + " VNĐ",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);

            // In phiếu
            common.DocumentPrinter.DocumentData pd = new common.DocumentPrinter.DocumentData();
            pd.isReturn = isReturn;
            pd.maLo = maLo;
            pd.tenSP = tenSP;
            pd.soLo = soLo;
            pd.soLuong = soLuong;
            pd.donGia = donGia1SP.longValue();
            pd.tongTien = tongTien.longValue();
            pd.lyDo = ghiChu;
            pd.nguoiThucHien = Session.getCurrentUser().getHoTen();
            pd.tenNCC = tenNCC;
            pd.hinhThucHoan = isReturn ? option : null;
            pd.phanLoaiLyDo = !isReturn ? option : null;
            common.DocumentPrinter.askAndPrint(this, pd);

            dispose();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi:\n" + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void executeTransaction(int soLuong, long tongTien,
                                    String option, String ghiChu, String tinhTrang) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseHelper.getConnection();
            conn.setAutoCommit(false);

            // 1. Re-verify
            int currentStock;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT SoLuong FROM LoHang WHERE MaLo = ?")) {
                ps.setInt(1, maLo);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Lô hàng không tồn tại!");
                    currentStock = rs.getInt("SoLuong");
                }
            }
            if (soLuong > currentStock) {
                throw new SQLException("Tồn kho đã thay đổi! Hiện còn " + currentStock);
            }

            // 2. Trừ tồn kho
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE LoHang SET SoLuong = SoLuong - ? WHERE MaLo = ? AND SoLuong >= ?")) {
                ps.setInt(1, soLuong);
                ps.setInt(2, maLo);
                ps.setInt(3, soLuong);
                if (ps.executeUpdate() == 0) throw new SQLException("Không thể trừ tồn kho!");
            }

            // 3. Ghi chứng từ
            int maND = Session.getCurrentUser().getMaND();
            if (actionType == ActionType.RETURN) {
                ensureTable(conn, "TraHangNCC",
                    "CREATE TABLE TraHangNCC (MaTra INT IDENTITY(1,1) PRIMARY KEY, " +
                    "MaLo INT NOT NULL, MaSP INT NOT NULL, SoLuongTra INT NOT NULL, " +
                    "GiaNhapLo DECIMAL(18,0) DEFAULT 0, TongTienHoan DECIMAL(18,0) DEFAULT 0, " +
                    "HinhThucHoan NVARCHAR(50) DEFAULT N'Tiền mặt', " +
                    "TinhTrang NVARCHAR(100) DEFAULT N'', " +
                    "GhiChu NVARCHAR(500) DEFAULT N'', " +
                    "MaND INT NOT NULL, NgayTra DATETIME DEFAULT GETDATE())");

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO TraHangNCC (MaLo,MaSP,SoLuongTra,GiaNhapLo,TongTienHoan,HinhThucHoan,TinhTrang,GhiChu,LyDo,MaND) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?)")) {
                    ps.setInt(1, maLo); ps.setInt(2, maSP); ps.setInt(3, soLuong);
                    ps.setLong(4, giaNhapLo); ps.setLong(5, tongTien);
                    ps.setNString(6, option);
                    ps.setNString(7, tinhTrang != null ? tinhTrang : "");
                    ps.setNString(8, ghiChu);
                    ps.setNString(9, ghiChu); // backward compat: LyDo = GhiChu
                    ps.setInt(10, maND);
                    ps.executeUpdate();
                }
            } else {
                ensureTable(conn, "HuyHang",
                    "CREATE TABLE HuyHang (MaHuy INT IDENTITY(1,1) PRIMARY KEY, " +
                    "MaLo INT NOT NULL, MaSP INT NOT NULL, SoLuongHuy INT NOT NULL, " +
                    "GiaNhapLo DECIMAL(18,0) DEFAULT 0, TongThietHai DECIMAL(18,0) DEFAULT 0, " +
                    "PhanLoaiLyDo NVARCHAR(100) DEFAULT N'Hết hạn sử dụng', ChiTietLyDo NVARCHAR(500) NOT NULL, " +
                    "MaND INT NOT NULL, NgayHuy DATETIME DEFAULT GETDATE())");

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO HuyHang (MaLo,MaSP,SoLuongHuy,GiaNhapLo,TongThietHai,PhanLoaiLyDo,ChiTietLyDo,MaND) " +
                        "VALUES (?,?,?,?,?,?,?,?)")) {
                    ps.setInt(1, maLo); ps.setInt(2, maSP); ps.setInt(3, soLuong);
                    ps.setLong(4, giaNhapLo); ps.setLong(5, tongTien);
                    ps.setNString(6, option); ps.setNString(7, ghiChu); ps.setInt(8, maND);
                    ps.executeUpdate();
                }
            }

            // 4. Audit Log
            String hanhDong = actionType == ActionType.RETURN ? "TRA_HANG" : "HUY_HANG";
            String doiTuong = "Lô " + soLo + " (MaLo=" + maLo + ") - " + tenSP;
            String chiTiet = "SL=" + soLuong + ", Tiền=" + MONEY_FMT.format(tongTien) + " VNĐ, " + option;
            common.SystemLogger.logInTransaction(conn, hanhDong, doiTuong, chiTiet);

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
            throw e;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
        }
    }

    private void ensureTable(Connection conn, String name, String ddl) {
        try (Statement s = conn.createStatement()) {
            s.execute("IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name='" + name + "') " + ddl);
        } catch (SQLException ignored) {}

        // ★ Auto-migrate old column names → new
        migrateColumn(conn, name, "DonGiaNhap", "GiaNhapLo", "DECIMAL(18,0) DEFAULT 0");
        migrateColumn(conn, name, "DonGiaVon", "GiaNhapLo", "DECIMAL(18,0) DEFAULT 0");

        // Ensure all columns exist (for old tables missing new fields)
        ensureColumn(conn, name, "GiaNhapLo", "DECIMAL(18,0) DEFAULT 0");
        ensureColumn(conn, name, "TongTienHoan", "DECIMAL(18,0) DEFAULT 0");
        ensureColumn(conn, name, "TongThietHai", "DECIMAL(18,0) DEFAULT 0");
        ensureColumn(conn, name, "HinhThucHoan", "NVARCHAR(50) DEFAULT N'Tiền mặt'");
        ensureColumn(conn, name, "TinhTrang", "NVARCHAR(100) DEFAULT N''");
        ensureColumn(conn, name, "GhiChu", "NVARCHAR(500) DEFAULT N''");
        ensureColumn(conn, name, "PhanLoaiLyDo", "NVARCHAR(100) DEFAULT N'Hết hạn sử dụng'");
        ensureColumn(conn, name, "ChiTietLyDo", "NVARCHAR(500) DEFAULT N''");

        // ★ Cho phép NULL trên cột cũ (nếu tồn tại)
        allowNullColumn(conn, name, "LyDo", "NVARCHAR(500)");
        allowNullColumn(conn, name, "ChiTietLyDo", "NVARCHAR(500)");
    }

    /**
     * Rename old column to new, or add new column if neither exists.
     */
    private void migrateColumn(Connection conn, String table, String oldCol, String newCol, String colDef) {
        try (Statement s = conn.createStatement()) {
            // Check if old column exists → rename
            ResultSet rs = s.executeQuery(
                "SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('" + table + "') AND name = '" + oldCol + "'");
            if (rs.next()) {
                s.execute("EXEC sp_rename '" + table + "." + oldCol + "', '" + newCol + "', 'COLUMN'");
                return;
            }
            // Check if new column already exists
            rs = s.executeQuery(
                "SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('" + table + "') AND name = '" + newCol + "'");
            if (rs.next()) return; // already correct

            // Neither exists → add
            s.execute("ALTER TABLE " + table + " ADD " + newCol + " " + colDef);
        } catch (SQLException ignored) {}
    }

    private void ensureColumn(Connection conn, String table, String col, String colDef) {
        try (Statement s = conn.createStatement()) {
            ResultSet rs = s.executeQuery(
                "SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('" + table + "') AND name = '" + col + "'");
            if (!rs.next()) {
                s.execute("ALTER TABLE " + table + " ADD " + col + " " + colDef);
            }
        } catch (SQLException ignored) {}
    }

    /**
     * ALTER existing column to allow NULLs (fix NOT NULL constraint on old columns).
     */
    private void allowNullColumn(Connection conn, String table, String col, String colType) {
        try (Statement s = conn.createStatement()) {
            ResultSet rs = s.executeQuery(
                "SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('" + table + "') AND name = '" + col + "'");
            if (rs.next()) {
                s.execute("ALTER TABLE " + table + " ALTER COLUMN " + col + " " + colType + " NULL");
            }
        } catch (SQLException ignored) {}
    }

    public boolean isConfirmed() { return confirmed; }

    // ================================================================
    //  UI HELPERS
    // ================================================================

    private void addInfoRow(JPanel p, String key, String val) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(Color.WHITE);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel k = new JLabel(key);
        k.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        k.setForeground(AppColors.TEXT_SECONDARY);
        k.setPreferredSize(new Dimension(120, 22));
        row.add(k, BorderLayout.WEST);
        JLabel v = new JLabel(val);
        v.setFont(new Font("Segoe UI", Font.BOLD, 12));
        v.setForeground(AppColors.TEXT_PRIMARY);
        row.add(v, BorderLayout.CENTER);
        p.add(row);
        p.add(Box.createRigidArea(new Dimension(0, 2)));
    }

    private void addLabel(JPanel p, String text, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(color);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        p.add(lbl);
        p.add(Box.createRigidArea(new Dimension(0, 3)));
    }

    private void addSeparator(JPanel p) {
        p.add(Box.createRigidArea(new Dimension(0, 4)));
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setForeground(AppColors.NEUTRAL_DARK);
        p.add(sep);
        p.add(Box.createRigidArea(new Dimension(0, 6)));
    }

    private void styleInput(JTextField f, Color border) {
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        f.setPreferredSize(new Dimension(0, 40));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 2),
                new EmptyBorder(0, 12, 0, 12)));
        f.setAlignmentX(LEFT_ALIGNMENT);
    }

    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(170, 36));
        Color hover = bg.darker();
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (btn.isEnabled()) btn.setBackground(hover); }
            public void mouseExited(MouseEvent e) { if (btn.isEnabled()) btn.setBackground(bg); }
        });
        return btn;
    }

    private JButton createFlatButton(String text, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(fg);
        btn.setBackground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(fg, 1),
                new EmptyBorder(4, 12, 4, 12)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        return btn;
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Cảnh báo", JOptionPane.WARNING_MESSAGE);
    }
}
