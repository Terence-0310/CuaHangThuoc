package common;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * DatePickerField — Reusable date picker component.
 * Click mở popup calendar với điều hướng tháng + năm.
 * Format: dd/MM/yyyy
 */
public class DatePickerField extends JPanel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JTextField txtDate;
    private LocalDate selectedDate;

    public DatePickerField() {
        this(null);
    }

    public DatePickerField(LocalDate initialDate) {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);

        txtDate = new JTextField();
        txtDate.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtDate.setEditable(false);
        txtDate.setBackground(Color.WHITE);
        txtDate.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        txtDate.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1),
                new EmptyBorder(0, 10, 0, 4)));
        add(txtDate, BorderLayout.CENTER);

        // ★ Dùng text thay emoji để tránh lỗi font
        JButton btnCal = new JButton("...");
        btnCal.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCal.setPreferredSize(new Dimension(28, 0));
        btnCal.setFocusPainted(false);
        btnCal.setBorderPainted(false);
        btnCal.setBackground(AppColors.PRIMARY);
        btnCal.setForeground(Color.WHITE);
        btnCal.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCal.setToolTipText("Chọn ngày");
        btnCal.addActionListener(e -> showCalendarPopup());
        add(btnCal, BorderLayout.EAST);

        txtDate.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { showCalendarPopup(); }
        });

        if (initialDate != null) setDate(initialDate);
    }

    // === Public API ===

    public LocalDate getDate() { return selectedDate; }

    public void setDate(LocalDate date) {
        this.selectedDate = date;
        txtDate.setText(date != null ? date.format(FMT) : "");
    }

    public String getText() { return txtDate.getText().trim(); }

    public void setText(String text) {
        if (text == null || text.isBlank()) {
            selectedDate = null;
            txtDate.setText("");
        } else {
            try {
                selectedDate = LocalDate.parse(text, FMT);
                txtDate.setText(text);
            } catch (Exception e) {
                txtDate.setText(text);
            }
        }
    }

    public void clear() {
        selectedDate = null;
        txtDate.setText("");
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        txtDate.setEnabled(enabled);
        for (Component c : getComponents()) c.setEnabled(enabled);
    }

    // === Calendar Popup ===

    private void showCalendarPopup() {
        if (!isEnabled()) return;

        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(AppColors.PRIMARY, 2));
        popup.setLayout(new BorderLayout());

        LocalDate display = selectedDate != null ? selectedDate : LocalDate.now();
        final LocalDate[] viewMonth = { display.withDayOfMonth(1) };

        JPanel calPanel = new JPanel(new BorderLayout());
        calPanel.setBackground(Color.WHITE);
        calPanel.setPreferredSize(new Dimension(290, 290));

        buildCalendar(calPanel, viewMonth, popup);
        popup.add(calPanel);
        popup.show(this, 0, getHeight());
    }

    private void buildCalendar(JPanel calPanel, LocalDate[] viewMonth, JPopupMenu popup) {
        calPanel.removeAll();

        YearMonth ym = YearMonth.from(viewMonth[0]);

        // ================================================================
        //  HEADER: <  [Tháng ▼]  [Năm ▲▼]  >
        // ================================================================
        JPanel header = new JPanel(new BorderLayout(4, 0));
        header.setBackground(AppColors.PRIMARY);
        header.setBorder(new EmptyBorder(5, 6, 5, 6));

        // Left: < prev month
        JButton btnPrev = navButton("<");
        btnPrev.setToolTipText("Tháng trước");
        btnPrev.addActionListener(e -> {
            viewMonth[0] = viewMonth[0].minusMonths(1);
            buildCalendar(calPanel, viewMonth, popup);
        });

        // Right: > next month
        JButton btnNext = navButton(">");
        btnNext.setToolTipText("Tháng sau");
        btnNext.addActionListener(e -> {
            viewMonth[0] = viewMonth[0].plusMonths(1);
            buildCalendar(calPanel, viewMonth, popup);
        });

        // Center: Month combo + Year spinner
        JPanel centerNav = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        centerNav.setOpaque(false);

        // Month combo (1-12)
        String[] monthNames = new String[12];
        for (int m = 1; m <= 12; m++) {
            monthNames[m - 1] = "Tháng " + String.format("%02d", m);
        }
        JComboBox<String> cboMonth = new JComboBox<>(monthNames);
        cboMonth.setSelectedIndex(ym.getMonthValue() - 1);
        cboMonth.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cboMonth.setPreferredSize(new Dimension(100, 24));
        cboMonth.setFocusable(false);
        cboMonth.addActionListener(e -> {
            int newMonth = cboMonth.getSelectedIndex() + 1;
            viewMonth[0] = viewMonth[0].withMonth(newMonth);
            buildCalendar(calPanel, viewMonth, popup);
        });

        // Year spinner (2000 - 2100)
        SpinnerNumberModel yearModel = new SpinnerNumberModel(
                ym.getYear(), 2000, 2100, 1);
        JSpinner spnYear = new JSpinner(yearModel);
        spnYear.setFont(new Font("Segoe UI", Font.BOLD, 12));
        spnYear.setPreferredSize(new Dimension(72, 24));
        // Remove thousand separator from spinner
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spnYear, "#");
        spnYear.setEditor(editor);
        spnYear.addChangeListener(e -> {
            int newYear = (int) spnYear.getValue();
            viewMonth[0] = viewMonth[0].withYear(newYear);
            buildCalendar(calPanel, viewMonth, popup);
        });

        centerNav.add(cboMonth);
        centerNav.add(spnYear);

        header.add(btnPrev, BorderLayout.WEST);
        header.add(centerNav, BorderLayout.CENTER);
        header.add(btnNext, BorderLayout.EAST);
        calPanel.add(header, BorderLayout.NORTH);

        // ================================================================
        //  DAY-OF-WEEK HEADERS
        // ================================================================
        JPanel grid = new JPanel(new GridLayout(0, 7, 1, 1));
        grid.setBackground(Color.WHITE);
        grid.setBorder(new EmptyBorder(4, 4, 4, 4));

        String[] dayNames = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
        for (String d : dayNames) {
            JLabel lbl = new JLabel(d, SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lbl.setForeground(d.equals("CN") ? AppColors.DANGER : AppColors.TEXT_SECONDARY);
            lbl.setPreferredSize(new Dimension(36, 22));
            grid.add(lbl);
        }

        // ================================================================
        //  DAYS GRID
        // ================================================================
        LocalDate firstDay = ym.atDay(1);
        int startDow = firstDay.getDayOfWeek().getValue(); // Mon=1
        LocalDate today = LocalDate.now();

        for (int i = 1; i < startDow; i++) {
            grid.add(new JLabel(""));
        }

        for (int day = 1; day <= ym.lengthOfMonth(); day++) {
            LocalDate date = ym.atDay(day);
            JButton btn = new JButton(String.valueOf(day));
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            btn.setMargin(new Insets(1, 1, 1, 1));
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setPreferredSize(new Dimension(36, 28));

            // Today border
            if (date.equals(today)) {
                btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
                btn.setBorder(BorderFactory.createLineBorder(AppColors.PRIMARY, 2));
            } else {
                btn.setBorder(BorderFactory.createLineBorder(new Color(0xE8, 0xE8, 0xE8), 1));
            }

            // Colors
            boolean isSelected = selectedDate != null && date.equals(selectedDate);
            if (isSelected) {
                btn.setBackground(AppColors.PRIMARY);
                btn.setForeground(Color.WHITE);
            } else if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                btn.setBackground(new Color(0xFF, 0xF0, 0xF0));
                btn.setForeground(AppColors.DANGER);
            } else if (date.getDayOfWeek() == DayOfWeek.SATURDAY) {
                btn.setBackground(Color.WHITE);
                btn.setForeground(new Color(0x00, 0x78, 0xD4));
            } else {
                btn.setBackground(Color.WHITE);
                btn.setForeground(AppColors.TEXT_PRIMARY);
            }

            final LocalDate clickDate = date;
            btn.addActionListener(e -> {
                setDate(clickDate);
                popup.setVisible(false);
            });

            // Hover
            final Color normalBg = btn.getBackground();
            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!clickDate.equals(selectedDate)) {
                        btn.setBackground(AppColors.PRIMARY_VERY_LIGHT);
                    }
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    if (clickDate.equals(selectedDate)) {
                        btn.setBackground(AppColors.PRIMARY);
                    } else {
                        btn.setBackground(normalBg);
                    }
                }
            });

            grid.add(btn);
        }

        calPanel.add(grid, BorderLayout.CENTER);

        // ================================================================
        //  FOOTER: Hôm nay | Xóa
        // ================================================================
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppColors.NEUTRAL_DARK));

        JButton btnToday = new JButton("Hôm nay (" + today.format(FMT) + ")");
        btnToday.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnToday.setForeground(AppColors.PRIMARY);
        btnToday.setBackground(Color.WHITE);
        btnToday.setFocusPainted(false);
        btnToday.setBorderPainted(false);
        btnToday.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnToday.addActionListener(e -> {
            setDate(LocalDate.now());
            popup.setVisible(false);
        });

        JButton btnClear = new JButton("Xóa");
        btnClear.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnClear.setForeground(AppColors.DANGER);
        btnClear.setBackground(Color.WHITE);
        btnClear.setFocusPainted(false);
        btnClear.setBorderPainted(false);
        btnClear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClear.addActionListener(e -> {
            clear();
            popup.setVisible(false);
        });

        footer.add(btnToday);
        footer.add(btnClear);
        calPanel.add(footer, BorderLayout.SOUTH);

        calPanel.revalidate();
        calPanel.repaint();
    }

    private JButton navButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setForeground(Color.WHITE);
        btn.setBackground(AppColors.PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(28, 24));
        return btn;
    }
}
