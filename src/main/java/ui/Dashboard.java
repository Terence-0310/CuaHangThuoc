package ui;

import java.awt.*;
import javax.swing.*;

import app.SessionManager;
import model.Permission;
import model.User;
import service.PermissionService;

public class Dashboard extends JFrame {

    private User currentUser;
    private JPanel contentPanel;

    public Dashboard() {
        this.currentUser = SessionManager.getCurrentUser();
        initUI();
    }

    private void initUI() {
        setTitle("Hệ thống quản lý nhà thuốc");
        setSize(1100, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(createHeader(), BorderLayout.NORTH);
        add(createSidebar(), BorderLayout.WEST);

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(
            new JLabel("Chọn chức năng bên trái", SwingConstants.CENTER),
            BorderLayout.CENTER
        );

        add(contentPanel, BorderLayout.CENTER);
    }

    // ================= HEADER =================
    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(0, 50));
        panel.setBackground(new Color(30, 144, 255));

        JLabel lblTitle = new JLabel("HỆ THỐNG QUẢN LÝ NHÀ THUỐC");
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 16));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));

        JLabel lblUser = new JLabel(
            "Xin chào: " + currentUser.getUsername() +
            " (" + currentUser.getRole() + ")"
        );
        lblUser.setForeground(Color.WHITE);
        lblUser.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));

        panel.add(lblTitle, BorderLayout.WEST);
        panel.add(lblUser, BorderLayout.EAST);
        return panel;
    }

    // ================= SIDEBAR =================
    private JPanel createSidebar() {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(220, 0));
        panel.setLayout(new GridLayout(0, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        addMenu(panel, "Quản lý người dùng",
                Permission.USER_MANAGE,
                this::showUserPanel);

        addMenu(panel, "Quản lý thuốc",
                Permission.MEDICINE_MANAGE,
                this::showMedicinePanel);

        addMenu(panel, "Thống kê",
                Permission.REPORT_VIEW,
                this::showReportPanel);

        panel.add(new JSeparator());

        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.addActionListener(e -> handleLogout());
        panel.add(btnLogout);

        return panel;
    }

    // ================= MENU HANDLER =================
    private void addMenu(JPanel panel,
                         String title,
                         Permission permission,
                         Runnable action) {

        // ADMIN → luôn thấy
        if (!PermissionService.has(currentUser, permission)) return;

        JButton btn = new JButton(title);
        btn.setFocusPainted(false);
        btn.addActionListener(e -> action.run());
        panel.add(btn);
    }

    // ================= CONTENT =================
    private void setContent(JComponent component) {
        contentPanel.removeAll();
        contentPanel.add(component, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // ================= MODULE PANELS =================
    private void showUserPanel() {
        if (!checkPermission(Permission.USER_MANAGE)) return;

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(
            "MÀN HÌNH QUẢN LÝ NGƯỜI DÙNG",
            SwingConstants.CENTER
        ), BorderLayout.CENTER);

        setContent(panel);
    }

    private void showMedicinePanel() {
        if (!checkPermission(Permission.MEDICINE_MANAGE)) return;

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(
            "MÀN HÌNH QUẢN LÝ THUỐC",
            SwingConstants.CENTER
        ), BorderLayout.CENTER);

        setContent(panel);
    }

    private void showReportPanel() {
        if (!checkPermission(Permission.REPORT_VIEW)) return;

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(
            "MÀN HÌNH THỐNG KÊ",
            SwingConstants.CENTER
        ), BorderLayout.CENTER);

        setContent(panel);
    }

    // ================= PERMISSION GUARD =================
    private boolean checkPermission(Permission permission) {
        if (PermissionService.has(currentUser, permission)) {
            return true;
        }

        JOptionPane.showMessageDialog(
            this,
            "Bạn không có quyền truy cập chức năng này",
            "Từ chối truy cập",
            JOptionPane.WARNING_MESSAGE
        );
        return false;
    }

    // ================= LOGOUT =================
    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Bạn có chắc muốn đăng xuất?",
            "Đăng xuất",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            SessionManager.logout();
            dispose();
            new LoginForm().setVisible(true);
        }
    }
}
