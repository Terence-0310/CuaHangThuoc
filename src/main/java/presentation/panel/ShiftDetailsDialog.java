package presentation.panel;

import domain.entity.AttendanceHistory;
import infrastructure.repository.AttendanceDAO;
import common.AppColors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Dialog chi tiết ca làm việc.
 * Hiển thị: Thông tin NV, Giờ vào/ra, Bảng hóa đơn trong ca, Tổng doanh thu, Ghi chú.
 */
public class ShiftDetailsDialog extends JDialog {
    private final AttendanceHistory attendance;
    private final AttendanceDAO attendanceDAO;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ShiftDetailsDialog(Frame owner, AttendanceHistory attendance, AttendanceDAO attendanceDAO) {
        super(owner, "Chi Tiết Ca Làm Việc — #" + attendance.getRecordID(), true);
        this.attendance = attendance;
        this.attendanceDAO = attendanceDAO;

        setSize(750, 580);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBackground(Color.WHITE);

        mainPanel.add(buildHeaderPanel(), BorderLayout.NORTH);
        mainPanel.add(buildCenterPanel(), BorderLayout.CENTER);
        mainPanel.add(buildFooterPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    // ================================================
    // HEADER: Thông tin nhân viên + ca
    // ================================================
    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel(new GridLayout(3, 2, 15, 8));
        header.setBackground(new Color(0xF0, 0xF4, 0xFF));
        header.setBorder(new EmptyBorder(15, 20, 15, 20));

        header.add(createBoldLabel("Nhân viên:"));
        header.add(createValueLabel(attendance.getEmpName()));

        header.add(createBoldLabel("Ca làm:"));
        String shiftLine = attendance.getShiftName() + " — " +
                (attendance.getWorkDate() != null ? attendance.getWorkDate().format(DATE_FMT) : "");
        header.add(createValueLabel(shiftLine));

        header.add(createBoldLabel("Giờ Vào / Ra:"));
        String inTime = attendance.getClockIn() != null ? attendance.getClockIn().format(TIME_FMT) : "---";
        String outTime = attendance.getClockOut() != null ? attendance.getClockOut().format(TIME_FMT) : "---";
        String totalStr = "0g 0p";
        if (attendance.getTotalHours() != null) {
            double h = attendance.getTotalHours().doubleValue();
            int hrs = (int) h;
            int mins = (int) Math.round((h - hrs) * 60);
            totalStr = hrs + "g " + mins + "p";
        }
        header.add(createValueLabel(inTime + " → " + outTime + "  (" + totalStr + ")"));

        return header;
    }

    // ================================================
    // CENTER: Bảng hóa đơn trong ca
    // ================================================
    private JPanel buildCenterPanel() {
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(Color.WHITE);
        center.setBorder(new EmptyBorder(10, 20, 5, 20));

        JLabel lblTitle = new JLabel("📋 Hóa đơn trong ca làm việc này:");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setBorder(new EmptyBorder(0, 0, 8, 0));
        center.add(lblTitle, BorderLayout.NORTH);

        String[] cols = {"Mã HĐ", "Giờ tạo", "Khách hàng", "Tổng tiền (₫)", "Trạng thái"};
        DefaultTableModel invoiceModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable tbInvoices = new JTable(invoiceModel);
        tbInvoices.setRowHeight(32);
        tbInvoices.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tbInvoices.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tbInvoices.getTableHeader().setBackground(AppColors.SECONDARY);
        tbInvoices.getTableHeader().setForeground(Color.WHITE);

        // Align cột tiền phải
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tbInvoices.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);

        // Load invoices
        try {
            List<Map<String, Object>> invoices = attendanceDAO.getInvoicesInShift(attendance.getRecordID());
            for (Map<String, Object> inv : invoices) {
                invoiceModel.addRow(new Object[]{
                        inv.get("MaHD"),
                        inv.get("NgayBan"),
                        inv.get("KhachHang"),
                        inv.get("TongTien"),
                        inv.get("TrangThai")
                });
            }

            if (invoices.isEmpty()) {
                invoiceModel.addRow(new Object[]{"", "", "— Không có hóa đơn nào trong ca này —", "", ""});
            }
        } catch (Exception ex) {
            invoiceModel.addRow(new Object[]{"", "", "Lỗi truy vấn: " + ex.getMessage(), "", ""});
        }

        JScrollPane sp = new JScrollPane(tbInvoices);
        sp.setPreferredSize(new Dimension(700, 200));
        center.add(sp, BorderLayout.CENTER);

        return center;
    }

    // ================================================
    // FOOTER: Tổng doanh thu + Ghi chú
    // ================================================
    private JPanel buildFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout(10, 10));
        footer.setBackground(Color.WHITE);
        footer.setBorder(new EmptyBorder(10, 20, 15, 20));

        // Doanh thu + Lương
        JPanel statsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        statsPanel.setOpaque(false);

        String earnedStr = attendance.getDailyEarned() != null ? String.format("%,.0f", attendance.getDailyEarned()) : "0";
        String revenueStr = attendance.getTotalRevenue() != null && attendance.getTotalRevenue().doubleValue() > 0
                ? String.format("%,.0f", attendance.getTotalRevenue()) : "0";
        int invoiceCount = attendance.getInvoiceCount();

        JLabel lblEarned = new JLabel("💰 Lương Ca: " + earnedStr + " ₫");
        lblEarned.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblEarned.setForeground(AppColors.PRIMARY);

        JLabel lblRevenue = new JLabel("📊 Doanh Thu: " + revenueStr + " ₫ (" + invoiceCount + " hóa đơn)");
        lblRevenue.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblRevenue.setForeground(new Color(0, 128, 0));

        statsPanel.add(lblEarned);
        statsPanel.add(lblRevenue);
        footer.add(statsPanel, BorderLayout.NORTH);

        // Ghi chú (read-only, cleaned)
        String reason = attendance.getLateReason() != null ? attendance.getLateReason().trim() : "(Không có ghi chú)";
        if (reason.startsWith("|")) reason = reason.substring(1).trim();
        // Tách phần [Admin sửa: ...] ra riêng dòng cho dễ đọc
        reason = reason.replace(" | ", "\n• ");
        if (!reason.startsWith("•") && !reason.startsWith("(")) reason = "• " + reason;

        JTextArea txtNote = new JTextArea(reason);
        txtNote.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtNote.setEditable(false);
        txtNote.setLineWrap(true);
        txtNote.setWrapStyleWord(true);
        txtNote.setRows(3);
        txtNote.setBackground(new Color(0xF8, 0xF8, 0xF8));
        txtNote.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Ghi chú / Lý do"),
                new EmptyBorder(5, 5, 5, 5)));

        footer.add(new JScrollPane(txtNote), BorderLayout.CENTER);

        // Nút đóng
        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnClose.setBackground(AppColors.PRIMARY);
        btnClose.setForeground(Color.WHITE);
        btnClose.setFocusPainted(false);
        btnClose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClose.setPreferredSize(new Dimension(100, 34));
        btnClose.addActionListener(e -> dispose());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        btnPanel.add(btnClose);
        footer.add(btnPanel, BorderLayout.SOUTH);

        return footer;
    }

    // Helpers
    private JLabel createBoldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        return lbl;
    }

    private JLabel createValueLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return lbl;
    }
}
