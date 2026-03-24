package presentation;

import common.AppColors;
import common.Session;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * MainFrame: Apothecary Pro theme — Dark navy sidebar + Light content area
 */
public class MainFrame extends JFrame {

    private JPanel sidebar;
    private JPanel contentPanel;
    private CardLayout cardLayout;

    private JButton btnDashboard;
    private JButton btnPOS;
    private JButton btnProduct;
    private JButton btnImport;
    private JButton btnInventory;
    private JButton btnSupplier;
    private JButton btnCustomer;
    private JButton btnLogout;

    private JButton activeButton = null;

    public MainFrame() {
        initComponents();
        applyPermissions();
    }

    private void initComponents() {
        String role = Session.getCurrentUser().getVaiTro();
        setTitle("Apothecary Pro — " + Session.getCurrentUser().getHoTen() + " (" + role + ")");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 780);
        setMinimumSize(new Dimension(1000, 600));
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout(0, 0));

        // === SIDEBAR (Dark Navy) ===
        sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(AppColors.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(new EmptyBorder(0, 0, 0, 0));

        // --- Logo area ---
        JPanel logoPanel = new JPanel();
        logoPanel.setLayout(new BoxLayout(logoPanel, BoxLayout.Y_AXIS));
        logoPanel.setBackground(AppColors.SIDEBAR_BG);
        logoPanel.setBorder(new EmptyBorder(24, 20, 20, 20));
        logoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel lblLogo = new JLabel("Apothecary Pro");
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblLogo.setForeground(Color.WHITE);
        lblLogo.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoPanel.add(lblLogo);

        JLabel lblVersion = new JLabel("Pharmacy Management v1.0");
        lblVersion.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblVersion.setForeground(AppColors.SECONDARY_LIGHT);
        lblVersion.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoPanel.add(Box.createRigidArea(new Dimension(0, 2)));
        logoPanel.add(lblVersion);

        sidebar.add(logoPanel);
        sidebar.add(createSidebarDivider());
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));

        // --- Section label ---
        sidebar.add(createSectionLabel("CHỨC NĂNG"));

        // --- Menu buttons ---
        btnDashboard = createSidebarButton("Dashboard");
        btnPOS       = createSidebarButton("Bán Hàng");
        btnProduct   = createSidebarButton("Sản Phẩm");
        btnImport    = createSidebarButton("Nhập Kho");
        btnInventory = createSidebarButton("Quản Lý Kho");
        btnSupplier  = createSidebarButton("Nhà Cung Cấp");
        btnCustomer  = createSidebarButton("Khách Hàng");

        sidebar.add(btnDashboard);
        sidebar.add(btnPOS);
        sidebar.add(btnProduct);
        sidebar.add(btnImport);
        sidebar.add(btnInventory);
        sidebar.add(btnSupplier);
        sidebar.add(btnCustomer);

        // --- Spacer ---
        sidebar.add(Box.createVerticalGlue());

        // --- User info ---
        sidebar.add(createSidebarDivider());
        JPanel userPanel = new JPanel();
        userPanel.setLayout(new BoxLayout(userPanel, BoxLayout.Y_AXIS));
        userPanel.setBackground(AppColors.SIDEBAR_BG);
        userPanel.setBorder(new EmptyBorder(10, 20, 6, 20));
        userPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JLabel lblUserName = new JLabel(Session.getCurrentUser().getHoTen());
        lblUserName.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblUserName.setForeground(Color.WHITE);
        lblUserName.setAlignmentX(Component.LEFT_ALIGNMENT);
        userPanel.add(lblUserName);

        JLabel lblRole = new JLabel(Session.isAdmin() ? "Quản trị viên" : "Nhân viên");
        lblRole.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblRole.setForeground(AppColors.SECONDARY_LIGHT);
        lblRole.setAlignmentX(Component.LEFT_ALIGNMENT);
        userPanel.add(lblRole);

        sidebar.add(userPanel);

        // --- Logout ---
        btnLogout = createSidebarButton("Đăng Xuất");
        sidebar.add(btnLogout);
        sidebar.add(Box.createRigidArea(new Dimension(0, 12)));

        add(sidebar, BorderLayout.WEST);

        // === CONTENT AREA (Light) ===
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(AppColors.NEUTRAL);

        contentPanel.add(createPlaceholder("Dashboard", "Thống kê doanh thu, cảnh báo hết hạn, top bán chạy"), "dashboard");
        contentPanel.add(createPlaceholder("Bán Hàng (POS)", "Tìm thuốc, thêm giỏ hàng, thanh toán FEFO"), "pos");
        contentPanel.add(new ProductPanel(), "product");
        contentPanel.add(new presentation.panel.ImportPanel(), "import");
        contentPanel.add(new presentation.panel.InventoryPanel(), "inventory");
        contentPanel.add(createPlaceholder("Nhà Cung Cấp", "Quản lý nhà cung cấp — Đang phát triển..."), "supplier");
        contentPanel.add(createPlaceholder("Khách Hàng", "Danh sách khách hàng & lịch sử mua hàng"), "customer");

        add(contentPanel, BorderLayout.CENTER);

        // === EVENTS ===
        btnDashboard.addActionListener(e -> switchPanel("dashboard", btnDashboard));
        btnPOS.addActionListener(e -> switchPanel("pos", btnPOS));
        btnProduct.addActionListener(e -> switchPanel("product", btnProduct));
        btnImport.addActionListener(e -> switchPanel("import", btnImport));
        btnInventory.addActionListener(e -> switchPanel("inventory", btnInventory));
        btnSupplier.addActionListener(e -> switchPanel("supplier", btnSupplier));
        btnCustomer.addActionListener(e -> switchPanel("customer", btnCustomer));
        btnLogout.addActionListener(e -> doLogout());

        // Default view
        if (Session.isAdmin()) {
            switchPanel("dashboard", btnDashboard);
        } else {
            switchPanel("pos", btnPOS);
        }
    }

    private void applyPermissions() {
        if (!Session.isAdmin()) {
            btnDashboard.setVisible(false);
            btnProduct.setVisible(false);
            btnImport.setVisible(false);
            btnSupplier.setVisible(false);
        }
    }

    private void switchPanel(String name, JButton button) {
        cardLayout.show(contentPanel, name);
        setActiveButton(button);
    }

    private void setActiveButton(JButton button) {
        // Reset previous
        if (activeButton != null) {
            activeButton.setBackground(AppColors.SIDEBAR_BG);
            activeButton.setForeground(AppColors.SIDEBAR_TEXT);
        }
        activeButton = button;
        if (button != btnLogout) {
            button.setBackground(AppColors.PRIMARY);
            button.setForeground(Color.WHITE);
        }
    }

    private void doLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn đăng xuất?", "Xác nhận",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            Session.logout();
            new LoginFrame().setVisible(true);
            this.dispose();
        }
    }

    // ================ UI HELPERS ================

    private JButton createSidebarButton(String text) {
        JButton btn = new JButton("  " + text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setForeground(AppColors.SIDEBAR_TEXT);
        btn.setBackground(AppColors.SIDEBAR_BG);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setPreferredSize(new Dimension(220, 40));
        btn.setBorder(new EmptyBorder(0, 12, 0, 12));

        // Hover effect
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn != activeButton) {
                    btn.setBackground(AppColors.SIDEBAR_HOVER);
                    btn.setForeground(Color.WHITE);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (btn != activeButton) {
                    btn.setBackground(AppColors.SIDEBAR_BG);
                    btn.setForeground(AppColors.SIDEBAR_TEXT);
                }
            }
        });

        return btn;
    }

    private JPanel createSidebarDivider() {
        JPanel divider = new JPanel();
        divider.setBackground(AppColors.SECONDARY_DARK);
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setPreferredSize(new Dimension(0, 1));
        return divider;
    }

    private JLabel createSectionLabel(String text) {
        JLabel label = new JLabel("  " + text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 10));
        label.setForeground(AppColors.SECONDARY_LIGHT);
        label.setBorder(new EmptyBorder(8, 14, 6, 0));
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        return label;
    }

    private JPanel createPlaceholder(String title, String description) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(AppColors.NEUTRAL);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(AppColors.PRIMARY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(lblTitle);
        inner.add(Box.createRigidArea(new Dimension(0, 6)));

        JLabel lblDesc = new JLabel(description);
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblDesc.setForeground(AppColors.TEXT_SECONDARY);
        lblDesc.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(lblDesc);
        inner.add(Box.createRigidArea(new Dimension(0, 16)));

        JLabel lblStatus = new JLabel("Đang phát triển...");
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblStatus.setForeground(AppColors.SECONDARY_LIGHT);
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(lblStatus);

        panel.add(inner);
        return panel;
    }
}
