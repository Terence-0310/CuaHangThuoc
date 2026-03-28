package presentation.panel;

import domain.entity.Employee;
import domain.entity.Schedule;
import infrastructure.repository.AttendanceDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TimeclockPanel extends JPanel {
    private JLabel lblClock;
    private JPasswordField txtPin;
    private JButton btnClockIn, btnClockOut;
    private AttendanceDAO attendanceDAO;

    public TimeclockPanel() {
        attendanceDAO = new AttendanceDAO();
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 250)); // AppColors.NEUTRAL equivalent
        initComponents();
        startClock();
    }

    private void initComponents() {
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(new Color(245, 245, 250));
        centerPanel.setBorder(new EmptyBorder(50, 50, 50, 50));

        // TITLE
        JLabel lblTitle = new JLabel("MÁY CHẤM CÔNG", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(new Color(41, 128, 185));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(lblTitle);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // CLOCK
        lblClock = new JLabel("00:00:00", SwingConstants.CENTER);
        lblClock.setFont(new Font("Consolas", Font.BOLD, 84));
        lblClock.setForeground(new Color(52, 73, 94));
        lblClock.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(lblClock);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 40)));

        // PIN INPUT
        JPanel pinPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pinPanel.setOpaque(false);
        JLabel lblPin = new JLabel("NHẬP MÃ PIN: ");
        lblPin.setFont(new Font("Segoe UI", Font.BOLD, 18));
        txtPin = new JPasswordField(10);
        txtPin.setFont(new Font("Consolas", Font.BOLD, 32));
        txtPin.setHorizontalAlignment(JTextField.CENTER);
        pinPanel.add(lblPin);
        pinPanel.add(txtPin);
        centerPanel.add(pinPanel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 40)));

        // BUTTONS
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        btnPanel.setOpaque(false);

        btnClockIn = new JButton("NHẬN CA (IN)");
        btnClockIn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btnClockIn.setPreferredSize(new Dimension(200, 60));
        btnClockIn.setBackground(new Color(46, 204, 113));
        btnClockIn.setForeground(Color.WHITE);
        btnClockIn.setFocusPainted(false);
        btnClockIn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClockIn.addActionListener(e -> doClockIn());

        btnClockOut = new JButton("KẾT CA (OUT)");
        btnClockOut.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btnClockOut.setPreferredSize(new Dimension(200, 60));
        btnClockOut.setBackground(new Color(231, 76, 60));
        btnClockOut.setForeground(Color.WHITE);
        btnClockOut.setFocusPainted(false);
        btnClockOut.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClockOut.addActionListener(e -> doClockOut());

        btnPanel.add(btnClockIn);
        btnPanel.add(btnClockOut);
        centerPanel.add(btnPanel);

        add(centerPanel, BorderLayout.CENTER);
    }

    private void startClock() {
        Timer timer = new Timer(1000, e -> {
            lblClock.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        });
        timer.start();
    }

    private void doClockIn() {
        String pin = new String(txtPin.getPassword()).trim();
        if (pin.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập mã PIN!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Employee emp = attendanceDAO.getEmployeeByPin(pin);
            if (emp == null) {
                JOptionPane.showMessageDialog(this, "Mã PIN không hợp lệ hoặc nhân viên đã khóa!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Task 5: DỌN CA CÚP ĐIỆN (ZOMBIE SHIFT)
            attendanceDAO.closeZombieShifts(emp.getEmpID());

            if (attendanceDAO.isCurrentlyClockedIn(emp.getEmpID())) {
                JOptionPane.showMessageDialog(this, emp.getFullName() + " đang trong ca! Hãy Kết Ca trước khi Nhận Ca mới.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Schedule sched = attendanceDAO.getNextAvailableSchedule(emp.getEmpID());
            int targetScheduleId = -1;
            String lateReason = null;

            if (sched == null) {
                // LOGIC: ĐI LÀM THAY (SMART SHIFT COVER)
                int confirm = JOptionPane.showConfirmDialog(this, 
                    "Bạn đi làm thay đúng không?", 
                    "Xác nhận đi làm thay", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                
                if (confirm != JOptionPane.YES_OPTION) return;

                List<Schedule> unclocked = attendanceDAO.getUnclockedSchedulesToday();
                if (unclocked.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Hôm nay không có ca nào đang trống để làm thay!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                String[] options = new String[unclocked.size()];
                for (int i = 0; i < unclocked.size(); i++) {
                    Schedule s = unclocked.get(i);
                    options[i] = s.getEmpName() + " (" + s.getShiftName() + " " + s.getActualStart() + "-" + s.getActualEnd() + ")";
                }

                String choice = (String) JOptionPane.showInputDialog(this, 
                        "Chọn người được bạn làm thay:", "Chọn ca làm thay", 
                        JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

                if (choice == null) return; // Canceled

                int selIdx = -1;
                for (int i=0; i<options.length; i++) {
                    if (options[i].equals(choice)) { selIdx = i; break; }
                }

                Schedule replacedSched = unclocked.get(selIdx);
                targetScheduleId = replacedSched.getScheduleID();
                lateReason = "Làm thay ca cho NV: " + replacedSched.getEmpName();

                // Logic đi trễ (Late entry)
                if (LocalTime.now().isAfter(replacedSched.getActualStart().plusMinutes(15))) {
                    String extraReason = JOptionPane.showInputDialog(this, "Ca làm đã bắt đầu trễ hơn 15 phút. Nhập lý do đi trễ:");
                    if (extraReason == null || extraReason.trim().isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Không cho nhận ca nếu bỏ trống lý do đi trễ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    lateReason += " | Lý do đi trễ: " + extraReason.trim();
                }
            } else {
                // LOGIC: ĐI LÀM ĐÚNG LỊCH (LATE ENTRY)
                targetScheduleId = sched.getScheduleID();
                if (LocalTime.now().isAfter(sched.getActualStart().plusMinutes(15))) {
                    lateReason = JOptionPane.showInputDialog(this, "Bạn đi làm trễ hơn 15 phút so với quy định (" + sched.getActualStart() + "). Vui lòng nhập lý do:");
                    if (lateReason == null || lateReason.trim().isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Không cho nhận ca nếu bỏ trống lý do đi trễ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }

            attendanceDAO.clockIn(emp.getEmpID(), targetScheduleId, lateReason);
            JOptionPane.showMessageDialog(this, "Nhận ca thành công!\nXin chào " + emp.getFullName() + 
                    "\n(" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + ")", 
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi khi Nhận ca: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } finally {
            txtPin.setText(""); // 1. Bấm xong BẮT BUỘC reset ô PIN
        }
    }

    private void doClockOut() {
        String pin = new String(txtPin.getPassword()).trim();
        if (pin.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập mã PIN!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Employee emp = attendanceDAO.getEmployeeByPin(pin);
            if (emp == null) {
                JOptionPane.showMessageDialog(this, "Mã PIN không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!attendanceDAO.isCurrentlyClockedIn(emp.getEmpID())) {
                JOptionPane.showMessageDialog(this, emp.getFullName() + " chưa Nhận ca nên không thể Kết ca!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Task 4: TĂNG CA HẬU KIỂM (POST-AUDIT OVERTIME)
            double[] preview = attendanceDAO.checkoutPreview(emp.getEmpID());
            String overtimeReason = null;
            if (preview != null && preview[0] > preview[1]) {
                overtimeReason = JOptionPane.showInputDialog(this, 
                    "Bạn đang làm lố giờ quy định. Vui lòng nhập lý do tăng ca:", 
                    "Tăng ca hậu kiểm", JOptionPane.WARNING_MESSAGE);
                
                if (overtimeReason == null || overtimeReason.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Bạn phải giải trình lý do tăng ca để được kết ca!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            attendanceDAO.clockOut(emp.getEmpID(), overtimeReason);
            JOptionPane.showMessageDialog(this, "Kết ca thành công!\nTạm biệt " + emp.getFullName() + 
                    "\n(" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + ")", 
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi khi Kết ca: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } finally {
            txtPin.setText(""); // 1. Bấm xong BẮT BUỘC reset ô PIN
        }
    }
}
