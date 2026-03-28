package presentation.panel;

import common.AppColors;
import common.DatePickerField;
import domain.entity.Employee;
import domain.entity.Schedule;
import domain.entity.Shift;
import service.HrService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * HrAdminPanel — Quản Lý Nhân Sự.
 * Tab 1: Employee CRUD | Tab 2: Shift Config | Tab 3: Xếp Ca Theo Tuần
 */
public class HrAdminPanel extends JPanel {

    private final HrService hrService;

    // === TAB 1: Employee ===
    private JTextField txtEmpID, txtFullName, txtPinCode, txtPhone, txtHourlyRate, txtOvertimeRate;
    private common.DatePickerField dpHireDate;
    private JComboBox<String> cboStatus;
    private JButton btnAdd, btnUpdate, btnDelete, btnClear;
    private DefaultTableModel empModel;
    private JTable empTable;
    private TableRowSorter<DefaultTableModel> empSorter;
    private JTextField txtEmpSearch;
    private int selectedEmpID = -1;

    // === TAB 2: Shift Config ===
    private DefaultTableModel shiftModel;
    private JTable shiftTable;
    private JTextField txtShiftStart, txtShiftEnd;

    // === TAB 3: Schedule (Xếp Ca) ===
    private JComboBox<String> cboSchedEmp, cboSchedShift;
    private DatePickerField dpFrom, dpTo;
    private JSpinner spnActualStart, spnActualEnd;
    private DefaultTableModel schedModel;
    private JTable schedTable;
    private List<Employee> activeEmployees;
    private List<Shift> shiftList;
    private int currentEditScheduleId = -1;   // -1 = Tạo mới, >0 = Sửa
    private JButton btnSchedAction, btnSchedClear;
    private JLabel lblSchedTitle;
    private int rightClickedRow = -1;          // Lưu row right-click cho Sửa
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JComboBox<String> cboFilterShift;

    public HrAdminPanel() {
        setLayout(new BorderLayout());
        setBackground(AppColors.NEUTRAL);

        hrService = new HrService();
        if (!hrService.isAdmin()) {
            showAccessDenied();
            return;
        }

        initComponents();
        SwingUtilities.invokeLater(this::loadEmpData);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) { loadEmpData(); }
        });
    }

    private void showAccessDenied() {
        JLabel lock = new JLabel("<html><center><h1>ACCESS DENIED</h1>" +
                "Tab Quản Lý Nhân Sự Yêu Cầu Quyền Quản Trị Hệ Thống.<br>" +
                "Vui Lòng Đăng Nhập Tài Khoản Admin!</center></html>");
        lock.setForeground(Color.RED);
        lock.setHorizontalAlignment(SwingConstants.CENTER);
        add(lock, BorderLayout.CENTER);
    }

    // ================================================================
    //  LAYOUT — Clone từ CustomerPanel
    // ================================================================

    private void initComponents() {
        // === TOP BAR ===
        JPanel topBar = new JPanel(new BorderLayout(12, 0));
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel lblTitle = new JLabel("Quản Lý Nhân Sự (HRM)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(AppColors.TEXT_PRIMARY);
        topBar.add(lblTitle, BorderLayout.WEST);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filterPanel.setOpaque(false);

        txtEmpSearch = new JTextField(20);
        txtEmpSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtEmpSearch.setPreferredSize(new Dimension(220, 34));
        txtEmpSearch.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AppColors.NEUTRAL_DARK, 1),
                new EmptyBorder(4, 10, 4, 10)));
        txtEmpSearch.setToolTipText("Tìm theo tên hoặc mã PIN...");
        txtEmpSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onSearch(); }
            @Override public void removeUpdate(DocumentEvent e) { onSearch(); }
            @Override public void changedUpdate(DocumentEvent e) { onSearch(); }
        });
        filterPanel.add(new JLabel("Tìm kiếm:"));
        filterPanel.add(txtEmpSearch);
        topBar.add(filterPanel, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // === TABBED PANE ===
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.setBackground(Color.WHITE);

        // Tab 1: Quản lý Nhân viên
        JPanel mainTab = new JPanel(new BorderLayout());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(380);
        splitPane.setDividerSize(1);
        splitPane.setBorder(null);

        splitPane.setLeftComponent(createFormPanel());
        splitPane.setRightComponent(createTablePanel());

        mainTab.add(splitPane, BorderLayout.CENTER);
        tabbedPane.addTab("  1. Quản lý Nhân Viên  ", mainTab);

        // Tab 2: Cấu hình Ca
        tabbedPane.addTab("  2. Cấu Hình Ca Làm Việc  ", createShiftConfigTab());

        // Tab 3: Xếp Ca Theo Tuần
        tabbedPane.addTab("  3. Xếp Ca Theo Tuần  ", createScheduleTab());

        // Tab 4: Lịch Sử Chấm Công
        tabbedPane.addTab("  4. Lịch Sử Ca Làm  ", new AttendanceHistoryPanel());

        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 0) loadEmpData();
            else if (tabbedPane.getSelectedIndex() == 1) loadShiftData();
            else if (tabbedPane.getSelectedIndex() == 2) loadScheduleData();
        });

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ================================================================
    //  TAB 1: FORM PANEL (Left) — Clone từ CustomerPanel.createFormPanel()
    // ================================================================

    private JPanel createFormPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);

        JLabel lblForm = new JLabel("Thông Tin Nhân Viên");
        lblForm.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblForm.setForeground(AppColors.PRIMARY);
        lblForm.setAlignmentX(LEFT_ALIGNMENT);
        formPanel.add(lblForm);
        formPanel.add(Box.createRigidArea(new Dimension(0, 16)));

        // Mã NV (readonly)
        txtEmpID = new JTextField();
        txtEmpID.setEditable(false);
        txtEmpID.setBackground(AppColors.NEUTRAL);
        addFormRow(formPanel, "Mã NV:", txtEmpID);

        // Tên NV
        txtFullName = new JTextField();
        addFormRow(formPanel, "Tên nhân viên: *", txtFullName);

        // Mã PIN (quan trọng — VARCHAR)
        txtPinCode = new JTextField();
        txtPinCode.setToolTipText("Mã PIN dùng để chấm công (VD: 0001)");
        addFormRow(formPanel, "Mã PIN: *", txtPinCode);

        // Lương/Giờ (số only)
        txtHourlyRate = new JTextField();
        txtHourlyRate.setToolTipText("VNĐ/giờ (VD: 25000)");
        applyNumberFilter(txtHourlyRate);
        addFormRow(formPanel, "Lương/Giờ (VNĐ): *", txtHourlyRate);

        // Lương tăng ca
        txtOvertimeRate = new JTextField();
        txtOvertimeRate.setToolTipText("VNĐ/giờ tăng ca (VD: 37500)");
        applyNumberFilter(txtOvertimeRate);
        addFormRow(formPanel, "Lương tăng ca/Giờ:", txtOvertimeRate);

        // SĐT
        txtPhone = new JTextField();
        txtPhone.setToolTipText("Số điện thoại nhân viên");
        addFormRow(formPanel, "SĐT:", txtPhone);

        // Ngày vào làm
        dpHireDate = new common.DatePickerField(java.time.LocalDate.now());
        dpHireDate.setPreferredSize(new Dimension(150, 32));
        addFormRow(formPanel, "Ngày vào làm:", dpHireDate);

        // Trạng thái
        cboStatus = new JComboBox<>(new String[]{"Đang làm", "Đã nghỉ"});
        cboStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addFormRow(formPanel, "Trạng thái:", cboStatus);

        formPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // === Buttons (2x2 grid) ===
        JPanel btnPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        btnPanel.setOpaque(false);
        btnPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        btnPanel.setAlignmentX(LEFT_ALIGNMENT);

        btnAdd    = createActionButton("Thêm Mới", AppColors.SUCCESS);
        btnUpdate = createActionButton("Cập Nhật",  AppColors.PRIMARY);
        btnClear  = createActionButton("Làm Mới",   AppColors.SECONDARY);
        btnDelete = createActionButton("Cho Nghỉ Việc", AppColors.DANGER);

        btnAdd.addActionListener(e -> doInsert());
        btnUpdate.addActionListener(e -> doUpdate());
        btnClear.addActionListener(e -> clearForm());
        btnDelete.addActionListener(e -> doSoftDelete());

        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);

        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnClear);
        btnPanel.add(btnDelete);
        formPanel.add(btnPanel);

        formPanel.add(Box.createVerticalGlue());

        JScrollPane formScroll = new JScrollPane(formPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        wrapper.add(formScroll, BorderLayout.CENTER);
        return wrapper;
    }

    // ================================================================
    //  TAB 1: TABLE PANEL (Right) — Clone từ CustomerPanel.createTablePanel()
    // ================================================================

    private JPanel createTablePanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(new EmptyBorder(16, 12, 16, 24));

        String[] columns = {"Mã NV", "Tên Nhân Viên", "SĐT", "Mã PIN", "Lương/Giờ", "Lương TC", "Thời gian cống hiến", "Phép Tuần", "Phép Năm", "Trạng Thái"};
        empModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        empTable = new JTable(empModel);
        empTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        empTable.setRowHeight(32);
        empTable.setShowGrid(false);
        empTable.setIntercellSpacing(new Dimension(0, 0));
        empTable.setFillsViewportHeight(true);
        empTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Sorter (client-side cho đơn giản)
        empSorter = new TableRowSorter<>(empModel);
        empTable.setRowSorter(empSorter);

        JTableHeader header = empTable.getTableHeader();
        header.setBackground(AppColors.TABLE_HEADER_BG);
        header.setForeground(AppColors.TABLE_HEADER_FG);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setPreferredSize(new Dimension(0, 36));
        header.setReorderingAllowed(false);
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Column widths
        int[] widths = {55, 180, 80, 100, 100, 55, 55, 100};
        for (int i = 0; i < widths.length; i++) {
            empTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        empTable.getColumnModel().getColumn(0).setMaxWidth(65);

        // Cell renderer — Alternating rows + Status màu
        empTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);
                    c.setForeground(AppColors.TEXT_PRIMARY);
                }
                // Trạng thái: "Đã nghỉ" → đỏ
                if (col == 7 && val != null) {
                    if ("Đã nghỉ".equals(val.toString())) {
                        c.setForeground(AppColors.DANGER);
                        setFont(new Font("Segoe UI", Font.BOLD, 13));
                    } else {
                        c.setForeground(AppColors.SUCCESS);
                        setFont(new Font("Segoe UI", Font.BOLD, 13));
                    }
                }
                // Lương → xanh lá bold
                if ((col == 3 || col == 4) && val != null && !sel) {
                    c.setForeground(AppColors.SUCCESS);
                    setFont(new Font("Segoe UI", Font.BOLD, 13));
                }
                // Alignment
                if (col == 0 || col == 2 || col == 5 || col == 6) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else if (col == 3 || col == 4) {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                } else if (col == 7) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }
                return c;
            }
        });

        // Click row → fill form
        empTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = empTable.getSelectedRow();
                if (row >= 0 && e.getClickCount() == 1) {
                    fillFormFromRow(row);
                }
            }
        });

        JScrollPane scroll = new JScrollPane(empTable);
        scroll.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1));
        scroll.getViewport().setBackground(Color.WHITE);
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    // ================================================================
    //  TAB 1: FORM LOGIC
    // ================================================================

    private void fillFormFromRow(int viewRow) {
        int modelRow = empTable.convertRowIndexToModel(viewRow);
        selectedEmpID = (int) empModel.getValueAt(modelRow, 0);

        // Load tất cả từ DB để tránh sai dữ liệu
        Employee fullEmp = hrService.getEmployeeById(selectedEmpID);
        if (fullEmp == null) return;

        txtEmpID.setText(String.valueOf(selectedEmpID));
        txtFullName.setText(fullEmp.getFullName());
        txtPhone.setText(fullEmp.getPhone() != null ? fullEmp.getPhone() : "");
        txtPinCode.setText(fullEmp.getPinCode());
        txtHourlyRate.setText(String.format("%,.0f", fullEmp.getHourlyRate()));
        txtOvertimeRate.setText(fullEmp.getOvertimeRate() != null ? String.format("%,.0f", fullEmp.getOvertimeRate()) : "");
        if (fullEmp.getHireDate() != null) dpHireDate.setDate(fullEmp.getHireDate());
        cboStatus.setSelectedItem(fullEmp.getStatus());

        btnAdd.setEnabled(false);
        btnUpdate.setEnabled(true);
        btnDelete.setEnabled(true);
    }

    private void clearForm() {
        selectedEmpID = -1;
        txtEmpID.setText("");
        txtFullName.setText("");
        txtPinCode.setText("");
        txtPhone.setText("");
        txtHourlyRate.setText("");
        txtOvertimeRate.setText("");
        dpHireDate.setDate(java.time.LocalDate.now());
        cboStatus.setSelectedIndex(0);
        empTable.clearSelection();
        txtEmpSearch.setText("");

        btnAdd.setEnabled(true);
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);
        txtFullName.requestFocusInWindow();
    }

    private boolean validateForm() {
        if (txtFullName.getText().trim().isEmpty()) {
            showWarning("Tên nhân viên không được để trống!");
            txtFullName.requestFocusInWindow();
            return false;
        }
        String pin = txtPinCode.getText().trim();
        if (pin.isEmpty()) {
            showWarning("Mã PIN không được để trống!");
            txtPinCode.requestFocusInWindow();
            return false;
        }
        if (!pin.matches("\\d{1,10}")) {
            showWarning("Mã PIN chỉ chứa chữ số (tối đa 10 ký tự)!");
            txtPinCode.requestFocusInWindow();
            return false;
        }
        try {
            new BigDecimal(txtHourlyRate.getText().trim());
        } catch (NumberFormatException e) {
            showWarning("Lương/Giờ phải là số hợp lệ!");
            txtHourlyRate.requestFocusInWindow();
            return false;
        }
        try {
            String ot = txtOvertimeRate.getText().trim();
            if (!ot.isEmpty()) new BigDecimal(ot);
        } catch (NumberFormatException e) {
            showWarning("Lương tăng ca phải là số hợp lệ!");
            txtOvertimeRate.requestFocusInWindow();
            return false;
        }
        if (dpHireDate.getDate() == null) {
            showWarning("Ngày vào làm không hợp lệ!");
            return false;
        }
        return true;
    }

    private Employee buildEmployeeFromForm() {
        Employee emp = new Employee();
        emp.setEmpID(selectedEmpID);
        emp.setFullName(txtFullName.getText().trim());
        emp.setPinCode(txtPinCode.getText().trim());
        emp.setPhone(txtPhone.getText().trim());
        emp.setHourlyRate(new BigDecimal(txtHourlyRate.getText().trim().replace(",", "")));
        String ot = txtOvertimeRate.getText().trim().replace(",", "");
        emp.setOvertimeRate(ot.isEmpty() ? BigDecimal.ZERO : new BigDecimal(ot));
        emp.setHireDate(dpHireDate.getDate());
        emp.setStatus((String) cboStatus.getSelectedItem());
        return emp;
    }

    private void doInsert() {
        if (!validateForm()) return;
        try {
            Employee emp = buildEmployeeFromForm();
            int id = hrService.insertEmployee(emp);
            if (id > 0) {
                JOptionPane.showMessageDialog(this,
                        "Thêm nhân viên thành công!\nMã NV: " + id,
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadEmpData();
            }
        } catch (Exception ex) {
            String msg = ex.getMessage();
            if (msg != null && msg.contains("TRÙNG MÃ PIN")) {
                showWarning(msg);
            } else {
                JOptionPane.showMessageDialog(this, "Lỗi: " + msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void doUpdate() {
        if (selectedEmpID < 0 || !validateForm()) return;
        try {
            Employee emp = buildEmployeeFromForm();
            hrService.updateEmployee(emp);
            JOptionPane.showMessageDialog(this, "Cập nhật thành công!",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
            loadEmpData();
            // Giữ selection
            for (int i = 0; i < empModel.getRowCount(); i++) {
                if ((int) empModel.getValueAt(i, 0) == selectedEmpID) {
                    int viewRow = empTable.convertRowIndexToView(i);
                    empTable.setRowSelectionInterval(viewRow, viewRow);
                    fillFormFromRow(viewRow);
                    break;
                }
            }
        } catch (Exception ex) {
            String msg = ex.getMessage();
            if (msg != null && msg.contains("TRÙNG MÃ PIN")) {
                showWarning(msg);
            } else {
                JOptionPane.showMessageDialog(this, "Lỗi: " + msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * XÓA MỀM — Nút "Cho Nghỉ Việc"
     * KHÔNG DELETE FROM. Chỉ UPDATE Status = 'Đã nghỉ'.
     */
    private void doSoftDelete() {
        if (selectedEmpID < 0) return;
        int viewRow = empTable.getSelectedRow();
        if (viewRow < 0) return;
        int modelRow = empTable.convertRowIndexToModel(viewRow);
        String name = (String) empModel.getValueAt(modelRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn cho nghỉ việc:\n\n  \"" + name + "\"  (Mã: " + selectedEmpID + ")?\n\n" +
                "Nhân viên sẽ chuyển sang trạng thái 'Đã nghỉ'.\nDữ liệu KHÔNG bị xóa.",
                "Xác nhận cho nghỉ việc", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            hrService.softDeleteEmployee(selectedEmpID);
            JOptionPane.showMessageDialog(this, "Đã cập nhật trạng thái 'Đã nghỉ' thành công!",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            loadEmpData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================================================================
    //  TAB 1: DATA LOADING
    // ================================================================

    private void loadEmpData() {
        empModel.setRowCount(0);
        java.time.format.DateTimeFormatter dateFmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (Employee e : hrService.getAllEmployees()) {
            // Thời gian cống hiến
            String tenure = "";
            if (e.getHireDate() != null) {
                java.time.LocalDate end = e.getResignDate() != null ? e.getResignDate() : java.time.LocalDate.now();
                java.time.Period p = java.time.Period.between(e.getHireDate(), end);
                String dur = (p.getYears() > 0 ? p.getYears() + " năm " : "") + p.getMonths() + " tháng";
                tenure = e.getHireDate().format(dateFmt) + " - ";
                if (e.getResignDate() != null) {
                    tenure += e.getResignDate().format(dateFmt) + " (Đã nghỉ - " + dur + ")";
                } else {
                    tenure += "Nay (" + dur + ")";
                }
            }

            String weeklyStr = e.getWeeklyLeaveRemaining() + "/" + hrService.getDefaultWeeklyLeave();
            String annualStr = e.getAnnualLeaveRemaining() + "/" + e.getAnnualLeaveTotal();

            empModel.addRow(new Object[]{
                e.getEmpID(),
                e.getFullName(),
                e.getPhone() != null ? e.getPhone() : "",
                e.getPinCode(),
                String.format("%,.0f VNĐ", e.getHourlyRate()),
                String.format("%,.0f VNĐ", e.getOvertimeRate() != null ? e.getOvertimeRate() : BigDecimal.ZERO),
                tenure,
                weeklyStr,
                annualStr,
                e.getStatus()
            });
        }
    }

    // ================================================================
    //  TAB 1: SEARCH (Realtime — debounce 300ms)
    // ================================================================

    private Timer searchTimer;
    private void onSearch() {
        if (searchTimer != null) searchTimer.stop();
        searchTimer = new Timer(300, e -> {
            String text = txtEmpSearch.getText().trim();
            if (text.isEmpty()) {
                empSorter.setRowFilter(null);
            } else {
                empSorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
            }
        });
        searchTimer.setRepeats(false);
        searchTimer.start();
    }

    // ================================================================
    //  TAB 2: CẤU HÌNH CA LÀM VIỆC
    // ================================================================

    private JPanel createShiftConfigTab() {
        JPanel tab = new JPanel(new BorderLayout());
        tab.setBackground(Color.WHITE);
        tab.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel lblTitle = new JLabel("Cấu Hình Giờ Mặc Định Cho Các Ca Làm Việc");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(AppColors.PRIMARY);
        tab.add(lblTitle, BorderLayout.NORTH);

        // Table
        shiftModel = new DefaultTableModel(new Object[]{"Mã Ca", "Tên Ca", "Loại", "Giờ Bắt Đầu", "Giờ Kết Thúc"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        shiftTable = new JTable(shiftModel);
        shiftTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        shiftTable.setRowHeight(36);
        shiftTable.setShowGrid(false);
        shiftTable.setFillsViewportHeight(true);
        shiftTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        shiftTable.setSelectionBackground(AppColors.PRIMARY_VERY_LIGHT);

        JTableHeader sh = shiftTable.getTableHeader();
        sh.setBackground(AppColors.TABLE_HEADER_BG);
        sh.setForeground(AppColors.TABLE_HEADER_FG);
        sh.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sh.setPreferredSize(new Dimension(0, 38));
        sh.setReorderingAllowed(false);

        shiftTable.getColumnModel().getColumn(0).setMaxWidth(80);
        shiftTable.getColumnModel().getColumn(2).setPreferredWidth(120);

        shiftTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                
                // Lấy loại để tô màu
                String loai = t.getModel().getValueAt(row, 2).toString();
                
                if (sel) {
                    c.setBackground(AppColors.PRIMARY_VERY_LIGHT);
                    c.setForeground(AppColors.TEXT_PRIMARY);
                } else if (loai.contains("Phép Tuần")) {
                    c.setBackground(new Color(0xE8, 0xF5, 0xE9)); // Xanh nhạt
                    c.setForeground(new Color(0x2E, 0x7D, 0x32));
                } else if (loai.contains("Phép Năm")) {
                    c.setBackground(new Color(0xE3, 0xF2, 0xFD)); // Xanh dương nhạt
                    c.setForeground(new Color(0x15, 0x65, 0xC0));
                } else if (loai.contains("Không Lương")) {
                    c.setBackground(new Color(0xFF, 0xF3, 0xE0)); // Cam nhạt
                    c.setForeground(new Color(0xE6, 0x51, 0x00)); // Cam đậm
                } else {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);
                    c.setForeground(AppColors.TEXT_PRIMARY);
                }
                
                setHorizontalAlignment(col == 0 ? SwingConstants.CENTER : SwingConstants.LEFT);
                if (col == 3 || col == 4) {
                    setFont(new Font("Segoe UI Semibold", Font.BOLD, 14));
                    setHorizontalAlignment(SwingConstants.CENTER);
                }
                if (col == 2) {
                    setFont(new Font("Segoe UI", Font.BOLD, 13));
                    setHorizontalAlignment(SwingConstants.CENTER);
                }
                return c;
            }
        });

        JScrollPane scrollP = new JScrollPane(shiftTable);
        scrollP.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1));
        scrollP.getViewport().setBackground(Color.WHITE);
        tab.add(scrollP, BorderLayout.CENTER);

        // Bottom: Edit fields
        JPanel editPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        editPanel.setBackground(Color.WHITE);
        editPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppColors.NEUTRAL_DARK));

        editPanel.add(new JLabel("Giờ bắt đầu (HH:mm):"));
        txtShiftStart = new JTextField(8);
        txtShiftStart.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtShiftStart.setPreferredSize(new Dimension(100, 34));
        editPanel.add(txtShiftStart);

        editPanel.add(new JLabel("Giờ kết thúc (HH:mm):"));
        txtShiftEnd = new JTextField(8);
        txtShiftEnd.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtShiftEnd.setPreferredSize(new Dimension(100, 34));
        editPanel.add(txtShiftEnd);

        JButton btnSaveShift = createActionButton("Lưu Thay Đổi", AppColors.PRIMARY);
        btnSaveShift.setPreferredSize(new Dimension(140, 36));
        btnSaveShift.addActionListener(e -> doSaveShift());
        editPanel.add(btnSaveShift);

        tab.add(editPanel, BorderLayout.SOUTH);

        // Click row → fill shift fields (column indices shifted due to Loại column)
        shiftTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = shiftTable.getSelectedRow();
                if (row >= 0) {
                    txtShiftStart.setText(shiftModel.getValueAt(row, 3).toString());
                    txtShiftEnd.setText(shiftModel.getValueAt(row, 4).toString());
                }
            }
        });

        return tab;
    }

    private void loadShiftData() {
        shiftModel.setRowCount(0);
        for (Shift s : hrService.getAllShifts()) {
            String loai;
            String name = s.getShiftName();
            if (name.equals("Nghỉ Phép Tuần")) {
                loai = "Nghỉ Phép Tuần";
            } else if (name.equals("Nghỉ Phép Năm")) {
                loai = "Nghỉ Phép Năm";
            } else if (name.contains("Không Lương")) {
                loai = "Không Lương";
            } else {
                loai = "Ca Làm Việc";
            }
            shiftModel.addRow(new Object[]{
                s.getShiftID(),
                s.getShiftName(),
                loai,
                s.getStartTime().toString(),
                s.getEndTime().toString()
            });
        }
    }

    private void doSaveShift() {
        int row = shiftTable.getSelectedRow();
        if (row < 0) { showWarning("Chọn 1 ca để chỉnh sửa!"); return; }

        try {
            int shiftID = (int) shiftModel.getValueAt(row, 0);
            LocalTime start = LocalTime.parse(txtShiftStart.getText().trim());
            LocalTime end = LocalTime.parse(txtShiftEnd.getText().trim());
            hrService.updateShift(shiftID, start, end);
            JOptionPane.showMessageDialog(this, "Cập nhật giờ ca thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            loadShiftData();
        } catch (java.time.format.DateTimeParseException ex) {
            showWarning("Định dạng giờ không hợp lệ! Dùng HH:mm (VD: 06:00)");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================================================================
    //  TAB 3: XẾP CA THEO TUẦN (DUAL-MODE: Tạo Mới / Cập Nhật)
    // ================================================================

    private JPanel createScheduleTab() {
        JPanel tab = new JPanel(new BorderLayout());
        tab.setBackground(Color.WHITE);

        JSplitPane schedSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        schedSplit.setDividerLocation(380);
        schedSplit.setDividerSize(1);
        schedSplit.setBorder(null);

        // === LEFT: Form xếp ca (Dual-Mode) ===
        JPanel formWrapper = new JPanel(new BorderLayout());
        formWrapper.setBackground(Color.WHITE);
        formWrapper.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);

        lblSchedTitle = new JLabel("Tạo Lịch Làm Việc");
        lblSchedTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblSchedTitle.setForeground(AppColors.PRIMARY);
        lblSchedTitle.setAlignmentX(LEFT_ALIGNMENT);
        form.add(lblSchedTitle);
        form.add(Box.createRigidArea(new Dimension(0, 16)));

        activeEmployees = hrService.getAllEmployees().stream()
                .filter(Employee::isActive)
                .collect(java.util.stream.Collectors.toList());
        cboSchedEmp = new JComboBox<>();
        for (Employee e : activeEmployees) {
            cboSchedEmp.addItem(e.getEmpID() + " - " + e.getFullName());
        }
        cboSchedEmp.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addFormRow(form, "Nhân viên: *", cboSchedEmp);

        shiftList = hrService.getAllShifts();
        cboSchedShift = new JComboBox<>();
        for (Shift s : shiftList) {
            cboSchedShift.addItem(s.getShiftID() + " - " + s.getShiftName());
        }
        cboSchedShift.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addFormRow(form, "Ca làm việc: *", cboSchedShift);

        dpFrom = new DatePickerField(LocalDate.now());
        addFormRow(form, "Từ ngày: *", dpFrom);

        dpTo = new DatePickerField(LocalDate.now().plusDays(6));
        addFormRow(form, "Đến ngày (khóa khi sửa):", dpTo);

        spnActualStart = createTimeSpinner();
        addFormRow(form, "Giờ vào thực tế (HH:mm):", spnActualStart);

        spnActualEnd = createTimeSpinner();
        addFormRow(form, "Giờ ra thực tế (HH:mm):", spnActualEnd);

        cboSchedShift.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) autoFillShiftTimes();
        });
        if (!shiftList.isEmpty()) autoFillShiftTimes();

        form.add(Box.createRigidArea(new Dimension(0, 12)));

        JPanel btnRow = new JPanel(new GridLayout(0, 2, 8, 8));
        btnRow.setOpaque(false);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        btnRow.setAlignmentX(LEFT_ALIGNMENT);

        btnSchedAction = createActionButton("Tạo Lịch Hàng Loạt", AppColors.SUCCESS);
        btnSchedAction.addActionListener(e -> doSchedAction());
        
        JButton btnCopyLastWeek = createActionButton("Copy Lịch Tuần Trước", AppColors.PRIMARY);
        btnCopyLastWeek.addActionListener(e -> doCopySchedule());

        btnSchedClear = createActionButton("Làm Mới", AppColors.SECONDARY);
        btnSchedClear.addActionListener(e -> clearSchedForm());
        
        btnRow.add(btnSchedAction);
        btnRow.add(btnCopyLastWeek);
        btnRow.add(btnSchedClear);
        form.add(btnRow);
        form.add(Box.createVerticalGlue());

        JScrollPane formScroll = new JScrollPane(form,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formWrapper.add(formScroll, BorderLayout.CENTER);
        schedSplit.setLeftComponent(formWrapper);

        // === RIGHT: Table (Checkbox col 0) ===
        JPanel tableWrapper = new JPanel(new BorderLayout(0, 10)); // Khoảng cách giữa Filter và Table
        tableWrapper.setBackground(Color.WHITE);
        tableWrapper.setBorder(new EmptyBorder(16, 12, 16, 24));

        String[] cols = {"Chọn", "ID", "Tên NV", "Ngày Làm", "Tên Ca", "Giờ Vào", "Giờ Ra"};
        schedModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 0; }
            @Override public Class<?> getColumnClass(int col) {
                return col == 0 ? Boolean.class : Object.class;
            }
        };

        schedTable = new JTable(schedModel);
        schedTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        schedTable.setRowHeight(32);
        schedTable.setShowGrid(false);
        schedTable.setIntercellSpacing(new Dimension(0, 0));
        schedTable.setFillsViewportHeight(true);
        // Task 3: Tắt hoàn toàn selection (không bôi xanh)
        schedTable.setRowSelectionAllowed(false);
        schedTable.setCellSelectionEnabled(false);

        // KHỞI TẠO TABLEROWSORTER
        rowSorter = new TableRowSorter<>(schedModel);
        schedTable.setRowSorter(rowSorter);

        // THÊM UI COMPONENT & XỬ LÝ SỰ KIỆN LỌC (INSTANT FILTER)
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filterPanel.setBackground(Color.WHITE);
        
        JLabel lblFilter = new JLabel("Lọc theo ca chờ duyệt: ");
        lblFilter.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblFilter.setForeground(AppColors.PRIMARY);
        filterPanel.add(lblFilter);

        cboFilterShift = new JComboBox<>(new String[]{"Tất cả", "Ca Sáng", "Ca Chiều"});
        cboFilterShift.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cboFilterShift.setPreferredSize(new Dimension(150, 30));
        
        cboFilterShift.addActionListener(e -> {
            String selected = (String) cboFilterShift.getSelectedItem();
            if ("Tất cả".equals(selected)) {
                rowSorter.setRowFilter(null);
            } else {
                rowSorter.setRowFilter(RowFilter.regexFilter("^" + selected + "$", 4)); // Cột 4 là Tên Ca
            }
        });
        filterPanel.add(cboFilterShift);
        
        tableWrapper.add(filterPanel, BorderLayout.NORTH);

        JTableHeader sh = schedTable.getTableHeader();
        sh.setBackground(AppColors.TABLE_HEADER_BG);
        sh.setForeground(AppColors.TABLE_HEADER_FG);
        sh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sh.setPreferredSize(new Dimension(0, 36));
        sh.setReorderingAllowed(false);

        schedTable.getColumnModel().getColumn(0).setMaxWidth(45);
        schedTable.getColumnModel().getColumn(0).setMinWidth(45);
        schedTable.getColumnModel().getColumn(1).setMaxWidth(55);
        schedTable.getColumnModel().getColumn(3).setPreferredWidth(110);
        schedTable.getColumnModel().getColumn(5).setPreferredWidth(80);
        schedTable.getColumnModel().getColumn(6).setPreferredWidth(80);

        // Task 2: CheckBoxHeaderRenderer implements TableCellRenderer
        JCheckBox headerChk = new JCheckBox();
        headerChk.setHorizontalAlignment(SwingConstants.CENTER);
        headerChk.setBackground(AppColors.TABLE_HEADER_BG);
        headerChk.setOpaque(true);
        schedTable.getColumnModel().getColumn(0).setHeaderRenderer(
            (table, value, isSelected, hasFocus, row, column) -> {
                headerChk.setSelected(isAllChecked());
                return headerChk;
            });

        // MouseAdapter trên Header: click cột 0 → toggle all
        sh.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int colIdx = schedTable.columnAtPoint(e.getPoint());
                if (colIdx == 0) {
                    boolean newVal = !isAllChecked();
                    for (int i = 0; i < schedModel.getRowCount(); i++) {
                        schedModel.setValueAt(newVal, i, 0);
                    }
                    headerChk.setSelected(newVal);
                    schedTable.getTableHeader().repaint();
                }
            }
        });

        schedTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                c.setBackground(row % 2 == 0 ? Color.WHITE : AppColors.TABLE_ROW_ALT);
                c.setForeground(AppColors.TEXT_PRIMARY);
                if (col == 1) setHorizontalAlignment(SwingConstants.CENTER);
                else if (col == 5 || col == 6) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                    c.setForeground(AppColors.PRIMARY);
                    setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
                } else setHorizontalAlignment(SwingConstants.LEFT);
                return c;
            }
        });

        JPopupMenu schedPopup = new JPopupMenu();
        JMenuItem miEdit = new JMenuItem("Sửa lịch này");
        miEdit.setForeground(AppColors.PRIMARY);
        miEdit.addActionListener(e -> doEditSchedule());
        schedPopup.add(miEdit);
        schedPopup.addSeparator();
        JMenuItem miDeleteChecked = new JMenuItem("Xóa lịch đã tick (✓)");
        miDeleteChecked.setForeground(AppColors.DANGER);
        miDeleteChecked.addActionListener(e -> doDeleteSchedule());
        schedPopup.add(miDeleteChecked);
        schedTable.setComponentPopupMenu(schedPopup);

        // Task 4: Right-click lưu row cho "Sửa lịch này" (dùng rowAtPoint)
        schedTable.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    rightClickedRow = schedTable.rowAtPoint(e.getPoint());
                }
            }
        });

        JScrollPane scroll = new JScrollPane(schedTable);
        scroll.setBorder(BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1));
        scroll.getViewport().setBackground(Color.WHITE);
        tableWrapper.add(scroll, BorderLayout.CENTER);
        schedSplit.setRightComponent(tableWrapper);
        tab.add(schedSplit, BorderLayout.CENTER);
        return tab;
    }

    private boolean isAllChecked() {
        if (schedModel.getRowCount() == 0) return false;
        for (int i = 0; i < schedModel.getRowCount(); i++) {
            if (!Boolean.TRUE.equals(schedModel.getValueAt(i, 0))) return false;
        }
        return true;
    }

    private JSpinner createTimeSpinner() {
        SpinnerDateModel model = new SpinnerDateModel();
        JSpinner spinner = new JSpinner(model);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "HH:mm");
        spinner.setEditor(editor);
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return spinner;
    }

    @SuppressWarnings("deprecation")
    private void autoFillShiftTimes() {
        int idx = cboSchedShift.getSelectedIndex();
        if (idx < 0 || idx >= shiftList.size()) return;
        Shift s = shiftList.get(idx);
        java.util.Date sd = new java.util.Date();
        sd.setHours(s.getStartTime().getHour());
        sd.setMinutes(s.getStartTime().getMinute());
        sd.setSeconds(0);
        java.util.Date ed = new java.util.Date();
        ed.setHours(s.getEndTime().getHour());
        ed.setMinutes(s.getEndTime().getMinute());
        ed.setSeconds(0);
        spnActualStart.setValue(sd);
        spnActualEnd.setValue(ed);
    }

    @SuppressWarnings("deprecation")
    private void switchMode(boolean isEdit, Schedule sc) {
        if (isEdit && sc != null) {
            currentEditScheduleId = sc.getScheduleID();
            lblSchedTitle.setText("Sửa Lịch - #" + sc.getScheduleID());
            lblSchedTitle.setForeground(new Color(0xFF, 0x8F, 0x00));
            btnSchedAction.setText("Cập Nhật Lịch");
            btnSchedAction.setBackground(AppColors.PRIMARY);
            for (int i = 0; i < activeEmployees.size(); i++) {
                if (activeEmployees.get(i).getEmpID() == sc.getEmpID()) {
                    cboSchedEmp.setSelectedIndex(i); break;
                }
            }
            for (int i = 0; i < shiftList.size(); i++) {
                if (shiftList.get(i).getShiftID() == sc.getShiftID()) {
                    cboSchedShift.setSelectedIndex(i); break;
                }
            }
            dpFrom.setDate(sc.getWorkDate());
            dpTo.setDate(sc.getWorkDate());
            dpTo.setEnabled(false);
            java.util.Date sDate = new java.util.Date();
            sDate.setHours(sc.getActualStart().getHour());
            sDate.setMinutes(sc.getActualStart().getMinute());
            sDate.setSeconds(0);
            spnActualStart.setValue(sDate);
            java.util.Date eDate = new java.util.Date();
            eDate.setHours(sc.getActualEnd().getHour());
            eDate.setMinutes(sc.getActualEnd().getMinute());
            eDate.setSeconds(0);
            spnActualEnd.setValue(eDate);
        } else {
            clearSchedForm();
        }
    }

    private void clearSchedForm() {
        currentEditScheduleId = -1;
        lblSchedTitle.setText("Tạo Lịch Làm Việc");
        lblSchedTitle.setForeground(AppColors.PRIMARY);
        btnSchedAction.setText("Tạo Lịch Hàng Loạt");
        btnSchedAction.setBackground(AppColors.SUCCESS);
        if (cboSchedEmp.getItemCount() > 0) cboSchedEmp.setSelectedIndex(0);
        if (cboSchedShift.getItemCount() > 0) cboSchedShift.setSelectedIndex(0);
        dpFrom.setDate(LocalDate.now());
        dpTo.setDate(LocalDate.now().plusDays(6));
        dpTo.setEnabled(true);
        if (!shiftList.isEmpty()) autoFillShiftTimes();
        schedTable.clearSelection();
    }

    @SuppressWarnings("deprecation")
    private void doSchedAction() {
        if (cboSchedEmp.getSelectedIndex() < 0) { showWarning("Chọn nhân viên!"); return; }
        if (cboSchedShift.getSelectedIndex() < 0) { showWarning("Chọn ca làm việc!"); return; }
        LocalDate from = dpFrom.getDate();
        if (from == null) { showWarning("Chọn ngày bắt đầu!"); return; }
        int empID = activeEmployees.get(cboSchedEmp.getSelectedIndex()).getEmpID();
        int shiftID = shiftList.get(cboSchedShift.getSelectedIndex()).getShiftID();
        java.util.Date sv = (java.util.Date) spnActualStart.getValue();
        java.util.Date ev = (java.util.Date) spnActualEnd.getValue();
        LocalTime actualStart = LocalTime.of(sv.getHours(), sv.getMinutes());
        LocalTime actualEnd = LocalTime.of(ev.getHours(), ev.getMinutes());

        if (currentEditScheduleId > 0) {
            try {
                hrService.updateSchedule(currentEditScheduleId, empID, shiftID, from, actualStart, actualEnd);
                JOptionPane.showMessageDialog(this, "Cập nhật lịch thành công!",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                clearSchedForm();
                loadScheduleData();
            } catch (Exception ex) {
                String msg = ex.getMessage();
                if (msg != null && msg.contains("TRÙNG LỊCH")) showWarning(msg);
                else JOptionPane.showMessageDialog(this, "Lỗi: " + msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            LocalDate to = dpTo.getDate();
            if (to == null) { showWarning("Chọn ngày kết thúc!"); return; }
            if (from.isAfter(to)) { showWarning("Từ ngày phải <= Đến ngày!"); return; }
            long totalDays = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
            
            // Task 1: Check Max Range Validation
            if (totalDays > 35) {
                showWarning("Chỉ được phép tạo lịch tối đa 35 ngày/lần để dễ quản lý. Vui lòng chọn lại ngày!");
                return;
            }
            Employee emp = activeEmployees.get(cboSchedEmp.getSelectedIndex());
            Shift shift = shiftList.get(cboSchedShift.getSelectedIndex());
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Xếp ca cho: " + emp.getFullName() + "\n" +
                    "Ca: " + shift.getShiftName() + " (" + actualStart + " → " + actualEnd + ")\n" +
                    "Từ " + from.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                    " đến " + to.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                    " (" + totalDays + " ngày)\n\nNgày đã có lịch sẽ tự động bỏ qua.",
                    "Xác nhận xếp ca", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            try {
                // === LEAVE QUOTA VALIDATION ===
                String shiftName = shift.getShiftName();

                // --- NGHỈ PHÉP NĂM ---
                if (shiftName.equals("Nghỉ Phép Năm")) {
                    int usedYear = hrService.getUsedAnnualLeave(empID, from.getYear());
                    int annualTotal = emp.getAnnualLeaveTotal();
                    if (usedYear + totalDays > annualTotal) {
                        showWarning("NV " + emp.getFullName() + " đã vượt quá hạn mức phép NĂM (" + annualTotal + " ngày).\n" +
                                "Bao gồm: " + hrService.getDefaultAnnualLeave() + " ngày gốc + " + emp.getYearsWorked() + " năm thâm niên.\n" +
                                "Phép năm đã dùng: " + usedYear + " ngày.\n" +
                                "Đang xin thêm: " + totalDays + " ngày.\n" +
                                "Vui lòng chuyển thành Nghỉ Không Lương!");
                        return;
                    }
                }

                // --- NGHỈ PHÉP TUẦN ---
                if (shiftName.equals("Nghỉ Phép Tuần")) {
                    int defaultWeekly = hrService.getDefaultWeeklyLeave();
                    java.time.LocalDate checkDate = from;
                    while (!checkDate.isAfter(to)) {
                        java.time.LocalDate monday = checkDate.with(java.time.DayOfWeek.MONDAY);
                        java.time.LocalDate sunday = monday.plusDays(6);
                        int daysInThisWeek = 0;
                        java.time.LocalDate d = from.isAfter(monday) ? from : monday;
                        java.time.LocalDate rangeEnd = to.isBefore(sunday) ? to : sunday;
                        while (!d.isAfter(rangeEnd)) { daysInThisWeek++; d = d.plusDays(1); }

                        int usedWeek = hrService.getUsedWeeklyLeave(empID, checkDate);
                        if (usedWeek + daysInThisWeek > defaultWeekly) {
                            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM");
                            showWarning("NV " + emp.getFullName() + " sẽ vượt hạn mức phép TUẦN " +
                                    monday.format(fmt) + " - " + sunday.format(fmt) +
                                    " (" + defaultWeekly + " ngày/tuần).\n" +
                                    "Phép tuần đã dùng: " + usedWeek + " ngày.\n" +
                                    "Đang xin thêm: " + daysInThisWeek + " ngày.\n" +
                                    "Vui lòng chuyển thành Nghỉ Không Lương!");
                            return;
                        }
                        checkDate = sunday.plusDays(1);
                    }
                }

                int inserted = hrService.insertBulkSchedules(empID, shiftID, from, to, actualStart, actualEnd);
                JOptionPane.showMessageDialog(this,
                        "Đã xếp thành công " + inserted + "/" + totalDays + " ngày!",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                loadScheduleData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** Copy tất cả lịch của tuần trước gán vào tuần hiện tại (bắt đầu từ `Từ ngày`) */
    private void doCopySchedule() {
        LocalDate targetFrom = dpFrom.getDate();
        if (targetFrom == null) {
            showWarning("Vui lòng chọn 'Từ ngày' (thường là Thứ Hai) của tuần muốn copy đến!");
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Copy toàn bộ lịch của tuần trước sang tuần bắt đầu từ " + 
            targetFrom.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "?", 
            "Xác nhận Copy", JOptionPane.YES_NO_OPTION);
            
        if (confirm != JOptionPane.YES_OPTION) return;
        
        try {
            int copiedRows = hrService.copyScheduleLastWeek(targetFrom);
            JOptionPane.showMessageDialog(this, "Copy thành công " + copiedRows + " ca làm việc!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            loadScheduleData();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage(), "Lỗi Copy", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /** Sửa lịch: dùng rightClickedRow (rowAtPoint) thay vì getSelectedRow */
    private void doEditSchedule() {
        if (rightClickedRow < 0 || rightClickedRow >= schedTable.getRowCount()) {
            showWarning("Click chuột phải vào dòng cần sửa!");
            return;
        }
        int vr = rightClickedRow;
        int mr = schedTable.convertRowIndexToModel(vr);
        Schedule sc = new Schedule();
        sc.setScheduleID((int) schedModel.getValueAt(mr, 1));
        String empName = (String) schedModel.getValueAt(mr, 2);
        for (Employee emp : activeEmployees) {
            if (emp.getFullName().equals(empName)) { sc.setEmpID(emp.getEmpID()); break; }
        }
        String shName = (String) schedModel.getValueAt(mr, 4);
        for (Shift s : shiftList) {
            if (s.getShiftName().equals(shName)) { sc.setShiftID(s.getShiftID()); break; }
        }
        sc.setWorkDate(LocalDate.parse((String) schedModel.getValueAt(mr, 3),
                DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        sc.setActualStart(LocalTime.parse((String) schedModel.getValueAt(mr, 5)));
        sc.setActualEnd(LocalTime.parse((String) schedModel.getValueAt(mr, 6)));
        switchMode(true, sc);
    }

    private void doDeleteSchedule() {
        java.util.List<Integer> ids = new java.util.ArrayList<>();
        for (int i = 0; i < schedModel.getRowCount(); i++) {
            if (Boolean.TRUE.equals(schedModel.getValueAt(i, 0))) {
                ids.add((int) schedModel.getValueAt(i, 1));
            }
        }
        if (ids.isEmpty()) {
            showWarning("Chưa tick (✓) dòng nào!\nTick vào cột 'Chọn' để chọn lịch cần xóa.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Xóa " + ids.size() + " dòng lịch đã tick?\n\nKhông thể hoàn tác.",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            hrService.deleteMultipleSchedules(ids);
            JOptionPane.showMessageDialog(this,
                    "Đã xóa " + ids.size() + " dòng!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            loadScheduleData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadScheduleData() {
        schedModel.setRowCount(0);
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (Schedule sc : hrService.getAllSchedules()) {
            schedModel.addRow(new Object[]{
                false, sc.getScheduleID(), sc.getEmpName(),
                sc.getWorkDate().format(dateFmt), sc.getShiftName(),
                sc.getActualStart().toString(), sc.getActualEnd().toString()
            });
        }
    }

    // ================================================================
    //  UI HELPERS — Clone từ CustomerPanel
    // ================================================================

    private void addFormRow(JPanel panel, String label, JComponent field) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(label.contains("*") ? AppColors.TEXT_PRIMARY : AppColors.TEXT_SECONDARY);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(lbl);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));

        if (field instanceof JTextField) {
            JTextField tf = (JTextField) field;
            tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1),
                    new EmptyBorder(0, 10, 0, 10)));
        }
        if (field instanceof JComboBox) {
            field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        }
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        field.setPreferredSize(new Dimension(0, 34));
        field.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(field);
        panel.add(Box.createRigidArea(new Dimension(0, 12)));
    }

    private JButton createActionButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 36));
        return btn;
    }

    /** DocumentFilter: chỉ cho nhập số vào ô lương/ngày nghỉ */
    private void applyNumberFilter(JTextField field) {
        ((javax.swing.text.AbstractDocument) field.getDocument()).setDocumentFilter(
            new javax.swing.text.DocumentFilter() {
                @Override
                public void insertString(FilterBypass fb, int offset, String string,
                        javax.swing.text.AttributeSet attr) throws javax.swing.text.BadLocationException {
                    if (string != null) super.insertString(fb, offset, string.replaceAll("[^0-9]", ""), attr);
                }
                @Override
                public void replace(FilterBypass fb, int offset, int length, String text,
                        javax.swing.text.AttributeSet attrs) throws javax.swing.text.BadLocationException {
                    if (text != null) super.replace(fb, offset, length, text.replaceAll("[^0-9]", ""), attrs);
                }
            });
    }

    private void showWarning(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Cảnh báo", JOptionPane.WARNING_MESSAGE);
    }
}
