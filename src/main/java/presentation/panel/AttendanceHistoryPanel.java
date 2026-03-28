package presentation.panel;

import domain.entity.AttendanceHistory;
import infrastructure.repository.AttendanceDAO;
import common.DatePickerField;
import common.AppColors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Tab 4: Lịch Sử Chấm Công — Super Dashboard.
 * Tính năng: Auto-load 3 ngày gần nhất, phân trang 50 dòng/trang,
 * JPopupMenu "Sửa giờ thủ công" + "Xem chi tiết ca", kết nối doanh thu hóa đơn.
 */
public class AttendanceHistoryPanel extends JPanel {
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();

    private DatePickerField dpFrom;
    private DatePickerField dpTo;
    private JTable tbHistory;
    private DefaultTableModel tbModel;

    // Pagination
    private int currentPage = 1;
    private int totalPages = 1;
    private static final int PAGE_SIZE = 50;
    private JButton btnFirst, btnPrev, btnNext, btnLast;
    private JLabel lblPageInfo;
    private JPanel paginationNumPanel;

    // Full data cache for pagination
    private List<AttendanceHistory> fullDataList;

    private final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public AttendanceHistoryPanel() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(Color.WHITE);

        initTopPanel();
        initTable();
        initPagination();

        // Auto-load 3 ngày gần nhất
        SwingUtilities.invokeLater(this::doFilter);
    }

    // ================================================
    // NORTH PANEL: Chỉ Từ ngày + Đến ngày + Lọc
    // ================================================
    private void initTopPanel() {
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        filterPanel.setBackground(Color.WHITE);
        filterPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Từ ngày (mặc định 3 ngày trước)
        filterPanel.add(createLabel("Từ ngày:"));
        dpFrom = new DatePickerField(LocalDate.now().minusDays(3));
        dpFrom.setPreferredSize(new Dimension(140, 32));
        filterPanel.add(dpFrom);

        // Đến ngày
        filterPanel.add(createLabel("Đến ngày:"));
        dpTo = new DatePickerField(LocalDate.now());
        dpTo.setPreferredSize(new Dimension(140, 32));
        filterPanel.add(dpTo);

        // Nút lọc
        JButton btnFilter = new JButton("Lọc Dữ Liệu");
        btnFilter.setBackground(AppColors.PRIMARY);
        btnFilter.setForeground(Color.WHITE);
        btnFilter.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnFilter.setFocusPainted(false);
        btnFilter.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnFilter.setPreferredSize(new Dimension(130, 32));
        btnFilter.addActionListener(e -> { currentPage = 1; doFilter(); });
        filterPanel.add(btnFilter);

        add(filterPanel, BorderLayout.NORTH);
    }

    // ================================================
    // CENTER: JTable với Custom Renderer
    // ================================================
    private void initTable() {
        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setBackground(Color.WHITE);
        tableWrapper.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(0, 15, 0, 15),
                new LineBorder(Color.LIGHT_GRAY, 1)
        ));

        String[] cols = {"ID", "Tên NV", "Ca làm", "Ngày làm", "Giờ Vào", "Giờ Ra",
                "Tổng Giờ", "Lương Nhận (₫)", "Hóa Đơn", "Doanh Thu Ca", "Lý do / Ghi chú"};
        tbModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tbHistory = new JTable(tbModel);
        tbHistory.setRowHeight(36);
        tbHistory.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tbHistory.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tbHistory.getTableHeader().setBackground(AppColors.SECONDARY);
        tbHistory.getTableHeader().setForeground(Color.WHITE);
        tbHistory.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Column widths
        int[] widths = {40, 140, 80, 90, 75, 75, 65, 100, 60, 110, 200};
        for (int i = 0; i < widths.length; i++) {
            tbHistory.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Custom renderer
        AttendanceColorRenderer renderer = new AttendanceColorRenderer();
        for (int i = 0; i < cols.length; i++) {
            tbHistory.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        JScrollPane scrollPane = new JScrollPane(tbHistory);
        tableWrapper.add(scrollPane, BorderLayout.CENTER);
        add(tableWrapper, BorderLayout.CENTER);

        // Right-click menu
        tbHistory.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int r = tbHistory.rowAtPoint(e.getPoint());
                    if (r >= 0 && r < tbHistory.getRowCount()) {
                        tbHistory.setRowSelectionInterval(r, r);
                    } else return;

                    JPopupMenu popup = new JPopupMenu();

                    JMenuItem mnuDetails = new JMenuItem("📋 Xem chi tiết ca làm việc");
                    mnuDetails.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                    mnuDetails.addActionListener(evt -> doShowShiftDetails(r));
                    popup.add(mnuDetails);

                    popup.addSeparator();

                    JMenuItem mnuEdit = new JMenuItem("✏️ Sửa giờ thủ công (Admin)");
                    mnuEdit.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                    mnuEdit.addActionListener(evt -> doManualEdit(r));
                    popup.add(mnuEdit);

                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    // ================================================
    // SOUTH: Pagination
    // ================================================
    private void initPagination() {
        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        paginationPanel.setBackground(Color.WHITE);
        paginationPanel.setBorder(new EmptyBorder(4, 0, 8, 0));

        btnFirst = createPageNavButton("« Đầu");
        btnPrev = createPageNavButton("‹ Trước");
        paginationNumPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        paginationNumPanel.setOpaque(false);
        btnNext = createPageNavButton("Sau ›");
        btnLast = createPageNavButton("Cuối »");
        lblPageInfo = new JLabel("Trang 1 / 1");
        lblPageInfo.setFont(new Font("Segoe UI", Font.BOLD, 12));

        btnFirst.addActionListener(e -> { currentPage = 1; displayPage(); });
        btnPrev.addActionListener(e -> { if (currentPage > 1) { currentPage--; displayPage(); } });
        btnNext.addActionListener(e -> { if (currentPage < totalPages) { currentPage++; displayPage(); } });
        btnLast.addActionListener(e -> { currentPage = totalPages; displayPage(); });

        paginationPanel.add(btnFirst);
        paginationPanel.add(btnPrev);
        paginationPanel.add(paginationNumPanel);
        paginationPanel.add(btnNext);
        paginationPanel.add(btnLast);
        paginationPanel.add(Box.createHorizontalStrut(15));
        paginationPanel.add(lblPageInfo);

        add(paginationPanel, BorderLayout.SOUTH);
    }

    // ================================================
    // FILTER + LOAD DATA
    // ================================================
    private void doFilter() {
        try {
            LocalDate from = dpFrom.getDate();
            LocalDate to = dpTo.getDate();
            if (from == null || to == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn ngày hợp lệ!");
                return;
            }

            // ★ Tự động đánh dấu vắng mặt cho các ca đã qua mà NV không nhận ca
            int absentMarked = attendanceDAO.markAbsentSchedules();
            if (absentMarked > 0) {
                System.out.println("[Attendance] Đã đánh dấu " + absentMarked + " ca vắng mặt.");
            }

            fullDataList = attendanceDAO.getAttendanceWithRevenue(from, to);
            totalPages = Math.max(1, (int) Math.ceil((double) fullDataList.size() / PAGE_SIZE));
            currentPage = Math.min(currentPage, totalPages);
            displayPage();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi khi lọc dữ liệu: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void displayPage() {
        tbModel.setRowCount(0);
        if (fullDataList == null || fullDataList.isEmpty()) {
            updatePaginationUI();
            return;
        }

        int startIdx = (currentPage - 1) * PAGE_SIZE;
        int endIdx = Math.min(startIdx + PAGE_SIZE, fullDataList.size());

        for (int i = startIdx; i < endIdx; i++) {
            AttendanceHistory h = fullDataList.get(i);

            // ★ Kiểm tra vắng mặt (ClockIn == null && reason chứa VẮNG)
            boolean isAbsent = h.getClockIn() == null && h.getLateReason() != null
                    && h.getLateReason().contains("VẮNG MẶT");

            String inTime, outTime, totalHourStr, earnedStr;

            if (isAbsent) {
                inTime = "❌ VẮNG";
                outTime = "❌ VẮNG";
                totalHourStr = "0g 0p";
                earnedStr = "0";
            } else {
                inTime = h.getClockIn() != null ? h.getClockIn().format(TIME_FORMAT) : "---";
                outTime = h.getClockOut() != null ? h.getClockOut().format(TIME_FORMAT) : "---";
                if (h.getTotalHours() != null) {
                    double decimalHours = h.getTotalHours().doubleValue();
                    int hrs = (int) decimalHours;
                    int mins = (int) Math.round((decimalHours - hrs) * 60);
                    totalHourStr = hrs + "g " + mins + "p";
                } else {
                    totalHourStr = "0g 0p";
                }
                earnedStr = h.getDailyEarned() != null ? String.format("%,.0f", h.getDailyEarned()) : "0";
            }

            String invoiceCountStr = h.getInvoiceCount() > 0 ? String.valueOf(h.getInvoiceCount()) : "—";
            String revenueStr = h.getTotalRevenue() != null && h.getTotalRevenue().doubleValue() > 0
                    ? String.format("%,.0f", h.getTotalRevenue()) : "—";

            // Clean lý do: tách bằng " | ", bỏ phần rỗng
            String reason = h.getLateReason() != null ? h.getLateReason().trim() : "";
            if (reason.startsWith("|")) reason = reason.substring(1).trim();

            tbModel.addRow(new Object[]{
                    h.getRecordID(),
                    h.getEmpName(),
                    h.getShiftName(),
                    h.getWorkDate() != null ? h.getWorkDate().format(DATE_FORMAT) : "",
                    inTime,
                    outTime,
                    totalHourStr,
                    earnedStr,
                    invoiceCountStr,
                    revenueStr,
                    reason
            });
        }
        updatePaginationUI();
    }

    private void updatePaginationUI() {
        btnFirst.setEnabled(currentPage > 1);
        btnPrev.setEnabled(currentPage > 1);
        btnNext.setEnabled(currentPage < totalPages);
        btnLast.setEnabled(currentPage < totalPages);

        int total = fullDataList != null ? fullDataList.size() : 0;
        lblPageInfo.setText("Trang " + currentPage + " / " + totalPages + " (" + total + " dòng)");

        // Number buttons
        paginationNumPanel.removeAll();
        int start = Math.max(1, currentPage - 2);
        int end = Math.min(totalPages, currentPage + 2);
        for (int p = start; p <= end; p++) {
            JButton btn = new JButton(String.valueOf(p));
            btn.setFont(new Font("Segoe UI", p == currentPage ? Font.BOLD : Font.PLAIN, 12));
            btn.setPreferredSize(new Dimension(36, 28));
            btn.setFocusPainted(false);
            btn.setBackground(p == currentPage ? AppColors.PRIMARY : Color.WHITE);
            btn.setForeground(p == currentPage ? Color.WHITE : AppColors.TEXT_PRIMARY);
            btn.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            final int page = p;
            btn.addActionListener(e -> { currentPage = page; displayPage(); });
            paginationNumPanel.add(btn);
        }
        paginationNumPanel.revalidate();
        paginationNumPanel.repaint();
    }

    // ================================================
    // SHOW SHIFT DETAILS
    // ================================================
    private void doShowShiftDetails(int row) {
        int recordId = (int) tbModel.getValueAt(row, 0);
        // Find the data object
        AttendanceHistory target = findByRecordId(recordId);
        if (target == null) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy dữ liệu!");
            return;
        }

        Window owner = SwingUtilities.getWindowAncestor(this);
        ShiftDetailsDialog dialog = new ShiftDetailsDialog(
                owner instanceof Frame ? (Frame) owner : null,
                target, attendanceDAO);
        dialog.setVisible(true);
    }

    // ================================================
    // MANUAL EDIT (SỬA GIỜ THỦ CÔNG)
    // ================================================
    private void doManualEdit(int row) {
        int recordId = (int) tbModel.getValueAt(row, 0);
        String empName = (String) tbModel.getValueAt(row, 1);
        String inStr = (String) tbModel.getValueAt(row, 4);
        String outStr = (String) tbModel.getValueAt(row, 5);

        AttendanceHistory target = findByRecordId(recordId);
        if (target == null) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy dữ liệu gốc!");
            return;
        }

        JPanel pnl = new JPanel(new GridLayout(6, 2, 10, 10));
        pnl.add(new JLabel("ID Chấm công:"));
        pnl.add(new JLabel(String.valueOf(recordId)));
        pnl.add(new JLabel("Tên Nhân viên:"));
        pnl.add(new JLabel(empName));
        pnl.add(new JLabel("Giờ Vào mới (HH:mm:ss):"));
        JTextField txtIn = new JTextField(inStr);
        pnl.add(txtIn);
        pnl.add(new JLabel("Giờ Ra mới (HH:mm:ss):"));
        JTextField txtOut = new JTextField(outStr.equals("---") ? inStr : outStr);
        pnl.add(txtOut);
        pnl.add(new JLabel("Ghi chú của Admin:"));
        JTextField txtNote = new JTextField();
        pnl.add(txtNote);

        int confirm = JOptionPane.showConfirmDialog(this, pnl, "Sửa Giờ Thủ Công", JOptionPane.OK_CANCEL_OPTION);
        if (confirm == JOptionPane.OK_OPTION) {
            try {
                if (txtNote.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Bắt buộc nhập ghi chú để truy vết!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                LocalTime newInTime = LocalTime.parse(txtIn.getText().trim(), TIME_FORMAT);
                LocalTime newOutTime = LocalTime.parse(txtOut.getText().trim(), TIME_FORMAT);

                LocalDate wd = target.getWorkDate();
                LocalDateTime newInDT = LocalDateTime.of(wd, newInTime);
                LocalDateTime newOutDT = LocalDateTime.of(wd, newOutTime);
                if (newOutDT.isBefore(newInDT)) {
                    newOutDT = newOutDT.plusDays(1);
                }

                long minutes = java.time.Duration.between(newInDT, newOutDT).toMinutes();
                java.math.BigDecimal totalHours = java.math.BigDecimal.valueOf(minutes)
                    .divide(java.math.BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);

                attendanceDAO.updateManualTime(recordId, newInDT, newOutDT, totalHours, txtNote.getText().trim());

                java.math.BigDecimal rateUsed = target.getHourlyRate() != null ? target.getHourlyRate() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal earned = totalHours.multiply(rateUsed);
                JOptionPane.showMessageDialog(this,
                        "Cập nhật thành công!\nTổng giờ mới: " + totalHours + "h\n" +
                                "Lương (SnapshotRate " + String.format("%,.0f", rateUsed) + "đ/h): " + String.format("%,.0f", earned) + "đ",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                doFilter();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi định dạng giờ hoặc lỗi DB: " + ex.getMessage());
            }
        }
    }

    // ================================================
    // HELPERS
    // ================================================
    private AttendanceHistory findByRecordId(int recordId) {
        if (fullDataList == null) return null;
        for (AttendanceHistory h : fullDataList) {
            if (h.getRecordID() == recordId) return h;
        }
        return null;
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        return lbl;
    }

    private JButton createPageNavButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setPreferredSize(new Dimension(70, 28));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBackground(Color.WHITE);
        btn.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK));
        return btn;
    }

    // ================================================
    // CUSTOM CELL RENDERER
    // ================================================
    class AttendanceColorRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (isSelected) {
                c.setBackground(table.getSelectionBackground());
                c.setForeground(table.getSelectionForeground());
                return c;
            }

            c.setForeground(table.getForeground());

            String reason = "";
            Object reasonObj = table.getModel().getValueAt(row, 10); // Cột Lý do (index 10)
            if (reasonObj != null) reason = reasonObj.toString().toLowerCase();

            if (reason.contains("vắng mặt")) {
                // ★ VẮNG MẶT: Nền đỏ nhạt + chữ đỏ đậm
                c.setBackground(new Color(255, 220, 220));
                c.setForeground(new Color(180, 0, 0));
                c.setFont(c.getFont().deriveFont(Font.BOLD));
            } else if (reason.contains("hệ thống tự chốt")) {
                c.setBackground(Color.PINK);
            } else if (reason.contains("trễ") || reason.contains("sớm") || reason.contains("làm thay")
                    || reason.contains("tăng ca") || reason.contains("admin sửa")) {
                c.setBackground(new Color(255, 255, 204)); // Vàng nhạt
            } else {
                c.setBackground(Color.WHITE);
            }

            // Cột doanh thu bold xanh lá (trừ dòng vắng)
            if (column == 9 && value != null && !value.toString().equals("—") && !reason.contains("vắng mặt")) {
                c.setForeground(new Color(0, 128, 0));
                c.setFont(c.getFont().deriveFont(Font.BOLD));
            }

            return c;
        }
    }
}
