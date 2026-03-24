package presentation;

import com.formdev.flatlaf.FlatClientProperties;
import common.AppColors;
import common.ServiceFactory;
import common.Session;
import domain.entity.User;
import service.IAuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * LoginFrame: Apothecary Pro theme
 */
public class LoginFrame extends JFrame {

    private final IAuthService authService = ServiceFactory.getAuthService();
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;

    public LoginFrame() {
        initComponents();
    }

    private void initComponents() {
        setTitle("Đăng Nhập — Quản Lý Cửa Hàng Thuốc");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setSize(480, 400);
        setLocationRelativeTo(null);
        getContentPane().setBackground(AppColors.NEUTRAL);

        // === Outer wrapper ===
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(AppColors.NEUTRAL);
        wrapper.setBorder(new EmptyBorder(20, 20, 20, 20));

        // === Card Panel ===
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.NEUTRAL_DARK, 1),
                new EmptyBorder(36, 40, 36, 40)
        ));
        card.setPreferredSize(new Dimension(400, 330));

        // --- Header bar ---
        JPanel headerBar = new JPanel();
        headerBar.setLayout(new BoxLayout(headerBar, BoxLayout.Y_AXIS));
        headerBar.setBackground(Color.WHITE);
        headerBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTitle = new JLabel("Apothecary Pro");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(AppColors.PRIMARY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerBar.add(lblTitle);

        JLabel lblSub = new JLabel("Quản Lý Cửa Hàng Thuốc");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(AppColors.TEXT_SECONDARY);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerBar.add(lblSub);

        card.add(headerBar);
        card.add(Box.createRigidArea(new Dimension(0, 24)));

        // --- Username ---
        txtUsername = new JTextField();
        txtUsername.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Tên đăng nhập");
        styleInput(txtUsername);
        card.add(txtUsername);
        card.add(Box.createRigidArea(new Dimension(0, 12)));

        // --- Password ---
        txtPassword = new JPasswordField();
        txtPassword.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Mật khẩu");
        styleInput(txtPassword);
        card.add(txtPassword);
        card.add(Box.createRigidArea(new Dimension(0, 20)));

        // --- Login Button ---
        btnLogin = new JButton("Đăng Nhập");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setBackground(AppColors.PRIMARY);
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setBorderPainted(false);
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btnLogin.setPreferredSize(new Dimension(0, 42));
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(btnLogin);
        card.add(Box.createRigidArea(new Dimension(0, 14)));

        // --- Footer hint ---
        JLabel lblHint = new JLabel("admin / admin123  ·  nhanvien01 / nv123");
        lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblHint.setForeground(AppColors.SECONDARY_LIGHT);
        lblHint.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblHint);

        wrapper.add(card);
        setContentPane(wrapper);

        // === Events ===
        btnLogin.addActionListener(e -> doLogin());
        KeyAdapter enterKey = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doLogin();
            }
        };
        txtUsername.addKeyListener(enterKey);
        txtPassword.addKeyListener(enterKey);

        // Hover effect
        btnLogin.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnLogin.setBackground(AppColors.PRIMARY_DARK);
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btnLogin.setBackground(AppColors.PRIMARY);
            }
        });

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent e) {
                txtUsername.requestFocusInWindow();
            }
        });
    }

    private void styleInput(JComponent field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        field.setPreferredSize(new Dimension(0, 40));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.NEUTRAL_DARKER, 1),
                new EmptyBorder(0, 12, 0, 12)
        ));
        field.setBackground(AppColors.NEUTRAL);
    }

    private void doLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        try {
            User user = authService.login(username, password);
            Session.setCurrentUser(user);
            new MainFrame().setVisible(true);
            this.dispose();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Lỗi đăng nhập", JOptionPane.ERROR_MESSAGE);
            txtPassword.setText("");
            txtPassword.requestFocusInWindow();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Không thể kết nối database!\n" + ex.getMessage(),
                    "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
        }
    }
}
