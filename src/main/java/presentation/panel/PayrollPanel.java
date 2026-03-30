package presentation.panel;

import common.AppColors;
import infrastructure.database.DatabaseHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * PayrollPanel — Tab 5: Bảng Lương Tháng.
 * Quét HR_Attendances GROUP BY employee/month/year,
 * LEFT JOIN HR_Payroll để biết đã thanh toán hay chưa.
 */
public class PayrollPanel extends JPanel {

    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0");

    private JComboBox<Integer> cboMonth, cboYear;
    private DefaultTableModel tableModel;
    private JTable table;
    private JButton btnPay, btnPayAll;
    private JLabel lblSummary;

    // In-memory data backing
    private List<PayrollRow> currentData = new ArrayList<>();

    public PayrollPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(Color.WHITE);
        initComponents();

        // Mặc định load tháng hiện tại
        cboMonth.setSelectedItem(LocalDate.now().getMonthValue());
        cboYear.setSelectedItem(LocalDate.now().getYear());
        refreshData();
    }

    // ================================================================
    //  UI INIT
    // ================================================================

    private void initComponents() {
        add(createFilterBar(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createBottomBar(), BorderLayout.SOUTH);
    }

    private JPanel createFilterBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(new EmptyBorder(14, 20, 10, 20));

        JLabel lblTitle = new JLabel("Bảng Lương Tháng");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitle.setForeground(AppColors.TEXT_PRIMARY);
        bar.add(lblTitle, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);

        controls.add(makeLabel("Tháng:"));
        cboMonth = new JComboBox<>();
        for (int i = 1; i <= 12; i++) cboMonth.addItem(i);
        cboMonth.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cboMonth.setPreferredSize(new Dimension(70, 30));
        controls.add(cboMonth);

        controls.add(makeLabel("Năm:"));
        cboYear = new JComboBox<>();
        int curYear = LocalDate.now().getYear();
        for (int y = curYear - 3; y <= curYear; y++) cboYear.addItem(y);
        cboYear.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cboYear.setPreferredSize(new Dimension(90, 30));
        controls.add(cboYear);

        controls.add(Box.createHorizontalStrut(10));

        JButton btnLoad = new JButton("Xem Bảng Lương");
        styleButton(btnLoad, AppColors.PRIMARY);
        btnLoad.addActionListener(e -> refreshData());
        controls.add(btnLoad);

        bar.add(controls, BorderLayout.EAST);
        return bar;
    }

    private JPanel createTablePanel() {
        String[] cols = {"EmpID", "Tên Nhân Viên", "Tháng/Năm", "Tổng Giờ", "Lương (VNĐ)", "Trạng Thái"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
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
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(140);
        table.getColumnModel().getColumn(5).setPreferredWidth(120);

        // Status color renderer
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            {
                setHorizontalAlignment(SwingConstants.CENTER);
            }
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel && val != null) {
                    String s = val.toString();
                    if (s.contains("Chờ")) {
                        c.setForeground(AppColors.DANGER);
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else if (s.contains("Đã")) {
                        c.setForeground(new Color(0x27, 0xAE, 0x60));
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        c.setForeground(AppColors.TEXT_PRIMARY);
                    }
                    c.setBackground(row % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);
                }
                return c;
            }
        });

        // Default renderers for other columns
        DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) c.setBackground(row % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);
                return c;
            }
        };
        for (int i = 0; i < 5; i++) {
            if (i == 0) {
                DefaultTableCellRenderer center = new DefaultTableCellRenderer() {
                    { setHorizontalAlignment(SwingConstants.CENTER); }
                    @Override
                    public Component getTableCellRendererComponent(JTable t, Object val,
                            boolean sel, boolean foc, int row, int col) {
                        Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                        if (!sel) c.setBackground(row % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);
                        return c;
                    }
                };
                table.getColumnModel().getColumn(i).setCellRenderer(center);
            } else if (i == 3 || i == 4) {
                DefaultTableCellRenderer right = new DefaultTableCellRenderer() {
                    { setHorizontalAlignment(SwingConstants.RIGHT); }
                    @Override
                    public Component getTableCellRendererComponent(JTable t, Object val,
                            boolean sel, boolean foc, int row, int col) {
                        Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                        if (!sel) c.setBackground(row % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);
                        return c;
                    }
                };
                table.getColumnModel().getColumn(i).setCellRenderer(right);
            } else {
                table.getColumnModel().getColumn(i).setCellRenderer(defaultRenderer);
            }
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK));
        scroll.getViewport().setBackground(Color.WHITE);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(new EmptyBorder(0, 20, 0, 20));
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createBottomBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(0xF8, 0xF9, 0xFA));
        bar.setBorder(new EmptyBorder(10, 20, 14, 20));

        lblSummary = new JLabel(" ");
        lblSummary.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSummary.setForeground(AppColors.TEXT_SECONDARY);
        bar.add(lblSummary, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        btnPay = new JButton("Thanh Toán Dòng Đã Chọn");
        styleButton(btnPay, new Color(0x27, 0xAE, 0x60));
        btnPay.addActionListener(e -> paySingle());
        btnPanel.add(btnPay);

        btnPayAll = new JButton("Thanh Toán Tất Cả");
        styleButton(btnPayAll, new Color(0xE6, 0x7E, 0x22));
        btnPayAll.addActionListener(e -> payAll());
        btnPanel.add(btnPayAll);

        bar.add(btnPanel, BorderLayout.EAST);
        return bar;
    }

    // ================================================================
    //  DATA LOADING
    // ================================================================

    public void refreshData() {
        int month = (Integer) cboMonth.getSelectedItem();
        int year = (Integer) cboYear.getSelectedItem();

        currentData.clear();
        tableModel.setRowCount(0);

        // Query: Group attendance by employee for the selected month/year,
        // LEFT JOIN payroll to check payment status
        String sql =
            "SELECT e.EmpID, e.FullName, " +
            "   ISNULL(att.TongGio, 0) AS TongGio, " +
            "   ISNULL(att.TongTien, 0) AS TongTien, " +
            "   p.PayrollID, p.TrangThai, p.NgayThanhToan, " +
            "   ISNULL(p.TongTien, ISNULL(att.TongTien, 0)) AS LuongThucNhan " +
            "FROM HR_Employees e " +
            "LEFT JOIN ( " +
            "   SELECT a.EmpID, SUM(a.TotalHours) AS TongGio, SUM(a.DailyEarned) AS TongTien " +
            "   FROM HR_Attendances a " +
            "   JOIN HR_Schedules s ON a.ScheduleID = s.ScheduleID " +
            "   WHERE MONTH(s.WorkDate) = ? AND YEAR(s.WorkDate) = ? " +
            "   GROUP BY a.EmpID " +
            ") att ON e.EmpID = att.EmpID " +
            "LEFT JOIN HR_Payroll p ON e.EmpID = p.EmpID AND p.Thang = ? AND p.Nam = ? " +
            "WHERE e.Status = N'Đang làm' OR att.TongGio IS NOT NULL OR p.PayrollID IS NOT NULL " +
            "ORDER BY e.FullName";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, month);
            ps.setInt(2, year);
            ps.setInt(3, month);
            ps.setInt(4, year);
            ResultSet rs = ps.executeQuery();

            BigDecimal grandTotal = BigDecimal.ZERO;
            int countPending = 0, countPaid = 0;

            while (rs.next()) {
                PayrollRow row = new PayrollRow();
                row.empId = rs.getInt("EmpID");
                row.fullName = rs.getNString("FullName");
                row.tongGio = rs.getBigDecimal("TongGio");
                row.tongTien = rs.getBigDecimal("LuongThucNhan");
                row.paid = rs.getString("TrangThai") != null
                        && rs.getString("TrangThai").contains("Đã");
                row.month = month;
                row.year = year;

                String status = row.paid ? "Đã thanh toán" : "Chờ thanh toán";
                if (row.tongGio.compareTo(BigDecimal.ZERO) == 0 && !row.paid) {
                    status = "Không có ca";
                }
                currentData.add(row);

                tableModel.addRow(new Object[]{
                    row.empId,
                    row.fullName,
                    String.format("T%d/%d", month, year),
                    String.format("%.1f h", row.tongGio),
                    MONEY_FMT.format(row.tongTien) + " VNĐ",
                    status
                });

                if (row.paid) {
                    countPaid++;
                    grandTotal = grandTotal.add(row.tongTien);
                } else if (row.tongGio.compareTo(BigDecimal.ZERO) > 0) {
                    countPending++;
                    grandTotal = grandTotal.add(row.tongTien);
                }
            }

            lblSummary.setText(String.format(
                "Tổng lương: %s VNĐ  |  Đã thanh toán: %d  |  Chờ: %d",
                MONEY_FMT.format(grandTotal), countPaid, countPending));

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Lỗi tải bảng lương: " + e.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================================================================
    //  PAYMENT ACTIONS
    // ================================================================

    private void paySingle() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng chọn một nhân viên để thanh toán.",
                "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        PayrollRow data = currentData.get(row);
        if (data.paid) {
            JOptionPane.showMessageDialog(this,
                "Nhân viên " + data.fullName + " đã được thanh toán rồi.",
                "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (data.tongGio.compareTo(BigDecimal.ZERO) == 0) {
            JOptionPane.showMessageDialog(this,
                "Nhân viên " + data.fullName + " không có giờ làm trong tháng này.",
                "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            String.format("Xác nhận chi trả %s VNĐ cho nhân viên %s?\n(Tháng %d/%d - Tổng %.1f giờ)",
                MONEY_FMT.format(data.tongTien), data.fullName,
                data.month, data.year, data.tongGio),
            "Xác Nhận Thanh Toán",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            executePayment(data);
            refreshData();
        }
    }

    private void payAll() {
        List<PayrollRow> pending = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (PayrollRow r : currentData) {
            if (!r.paid && r.tongGio.compareTo(BigDecimal.ZERO) > 0) {
                pending.add(r);
                total = total.add(r.tongTien);
            }
        }
        if (pending.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Không có nhân viên nào cần thanh toán.",
                "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            String.format("Xác nhận thanh toán cho %d nhân viên?\nTổng chi: %s VNĐ",
                pending.size(), MONEY_FMT.format(total)),
            "Xác Nhận Thanh Toán Tất Cả",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            int success = 0;
            for (PayrollRow r : pending) {
                try {
                    executePayment(r);
                    success++;
                } catch (Exception e) {
                    // skip duplicates
                }
            }
            JOptionPane.showMessageDialog(this,
                "Đã thanh toán thành công " + success + "/" + pending.size() + " nhân viên.",
                "Hoàn Tất", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
        }
    }

    private void executePayment(PayrollRow data) {
        String sql = "INSERT INTO HR_Payroll (EmpID, Thang, Nam, TongGio, TongTien, TrangThai, NgayThanhToan) " +
                     "VALUES (?, ?, ?, ?, ?, N'Đã thanh toán', GETDATE())";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, data.empId);
            ps.setInt(2, data.month);
            ps.setInt(3, data.year);
            ps.setBigDecimal(4, data.tongGio);
            ps.setBigDecimal(5, data.tongTien);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi thanh toán: " + e.getMessage());
        }
    }

    // ================================================================
    //  HELPERS
    // ================================================================

    private JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(AppColors.TEXT_SECONDARY);
        return lbl;
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(200, 32));
        Color hover = bg.darker();
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(hover); }
            public void mouseExited(java.awt.event.MouseEvent e) { btn.setBackground(bg); }
        });
    }

    // ================================================================
    //  INNER DATA CLASS
    // ================================================================

    private static class PayrollRow {
        int empId;
        String fullName;
        BigDecimal tongGio;
        BigDecimal tongTien;
        int payrollId;
        boolean paid;
        int month, year;
    }
}
