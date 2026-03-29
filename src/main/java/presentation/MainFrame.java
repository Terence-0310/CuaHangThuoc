package presentation;

import common.AppColors;
import common.Session;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * MainFrame: MerPhar — Dark navy sidebar with collapsible sections
 */
public class MainFrame extends JFrame {

    private JPanel sidebar;
    private JPanel contentPanel;
    private CardLayout cardLayout;

    // Top-level buttons (always visible)
    private JButton btnDashboard;
    private JButton btnCheckIn;
    private JButton btnPOS;
    private JButton btnImport;

    // Quản Lý sub-buttons
    private JButton btnProduct;
    private JButton btnInventory;
    private JButton btnSupplier;
    private JButton btnCustomer;
    private JButton btnInvoice;

    // Quản Trị sub-buttons
    private JButton btnUserMgmt;
    private JButton btnHrAdmin;

    private JButton btnLogout;

    private JButton activeButton = null;
    private final List<JButton> allNavButtons = new ArrayList<>();

    public MainFrame() {
        initComponents();
        applyPermissions();
        
        // === Auto No-Show Detection (chạy nền) ===
        new Thread(() -> {
            try {
                infrastructure.repository.AttendanceDAO dao = new infrastructure.repository.AttendanceDAO();
                int noShowCount = dao.autoMarkNoShow();
                if (noShowCount > 0) {
                    System.out.println("[Startup] Auto-marked " + noShowCount + " no-show records.");
                }
            } catch (Exception ex) {
                System.err.println("[Startup] Auto no-show error: " + ex.getMessage());
            }
        }, "AutoNoShow-Thread").start();
    }

    private void initComponents() {
        String role = Session.getCurrentUser().getVaiTro();
        setTitle("MerPhar — " + Session.getCurrentUser().getHoTen() + " (" + role + ")");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1280, 780);
        setMinimumSize(new Dimension(1000, 600));
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout(0, 0));

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                Session.goOffline();
                System.exit(0);
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(Session::goOffline));

        // === SIDEBAR ===
        sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(AppColors.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setBorder(new EmptyBorder(0, 0, 0, 0));

        // --- Logo ---
        JPanel logoPanel = new JPanel();
        logoPanel.setLayout(new BoxLayout(logoPanel, BoxLayout.Y_AXIS));
        logoPanel.setBackground(AppColors.SIDEBAR_BG);
        logoPanel.setBorder(new EmptyBorder(24, 20, 20, 20));
        logoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel lblLogo = new JLabel("MerPhar");
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

        // ============================================================
        //  TOP-LEVEL: Dashboard, Bán Hàng, Nhập Kho
        // ============================================================
        btnDashboard = createSidebarButton("Dashboard");
        btnCheckIn   = createSidebarButton("Chấm Công (V3)");
        btnPOS       = createSidebarButton("Bán Hàng");
        btnImport    = createSidebarButton("Nhập Kho");

        sidebar.add(btnDashboard);
        sidebar.add(btnCheckIn);
        sidebar.add(btnPOS);
        sidebar.add(btnImport);

        sidebar.add(Box.createRigidArea(new Dimension(0, 6)));
        sidebar.add(createSidebarDivider());
        sidebar.add(Box.createRigidArea(new Dimension(0, 6)));

        // ============================================================
        //  COLLAPSIBLE: Quản Lý (Sản Phẩm, Kho, NCC, Khách Hàng)
        // ============================================================
        btnProduct   = createSubButton("Sản Phẩm");
        btnInventory = createSubButton("Quản Lý Kho");
        btnSupplier  = createSubButton("Nhà Cung Cấp");
        btnCustomer  = createSubButton("Khách Hàng");
        btnHrAdmin   = createSubButton("Nhân Sự (HRM)");
        btnInvoice   = createSubButton("Hóa Đơn");

        JPanel quanLyContainer = new JPanel();
        quanLyContainer.setLayout(new BoxLayout(quanLyContainer, BoxLayout.Y_AXIS));
        quanLyContainer.setBackground(AppColors.SIDEBAR_BG);
        quanLyContainer.setVisible(false); // collapsed by default

        quanLyContainer.add(btnProduct);
        quanLyContainer.add(btnInventory);
        quanLyContainer.add(btnSupplier);
        quanLyContainer.add(btnCustomer);
        quanLyContainer.add(btnHrAdmin);
        quanLyContainer.add(btnInvoice);

        JButton btnQuanLyToggle = createSectionToggle("QUẢN LÝ", quanLyContainer);
        sidebar.add(btnQuanLyToggle);
        sidebar.add(quanLyContainer);

        sidebar.add(Box.createRigidArea(new Dimension(0, 2)));

        // ============================================================
        //  COLLAPSIBLE: Quản Trị (Người Dùng)
        // ============================================================
        btnUserMgmt = createSubButton("Người Dùng");

        JPanel quanTriContainer = new JPanel();
        quanTriContainer.setLayout(new BoxLayout(quanTriContainer, BoxLayout.Y_AXIS));
        quanTriContainer.setBackground(AppColors.SIDEBAR_BG);
        quanTriContainer.setVisible(false); // collapsed by default

        quanTriContainer.add(btnUserMgmt);

        JButton btnQuanTriToggle = createSectionToggle("QUẢN TRỊ", quanTriContainer);
        sidebar.add(btnQuanTriToggle);
        sidebar.add(quanTriContainer);

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

        // === CONTENT AREA ===
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(AppColors.NEUTRAL);

        contentPanel.add(new presentation.panel.ReportPanel(), "dashboard");
        contentPanel.add(new presentation.panel.TimeclockPanel(), "timeclock");
        contentPanel.add(new presentation.panel.POSPanel(), "pos");
        contentPanel.add(new ProductPanel(), "product");
        contentPanel.add(new presentation.panel.ImportPanel(), "import");
        contentPanel.add(new presentation.panel.InventoryPanel(), "inventory");
        contentPanel.add(new presentation.panel.SupplierPanel(), "supplier");
        contentPanel.add(new presentation.panel.CustomerPanel(), "customer");
        contentPanel.add(new presentation.panel.InvoicePanel(), "invoice");
        contentPanel.add(new presentation.panel.UserManagementPanel(), "usermgmt");
        contentPanel.add(new presentation.panel.HrAdminPanel(), "hradmin");

        add(contentPanel, BorderLayout.CENTER);

        // === EVENTS ===
        btnDashboard.addActionListener(e -> switchPanel("dashboard", btnDashboard));
        btnCheckIn.addActionListener(e -> switchPanel("timeclock", btnCheckIn));
        btnPOS.addActionListener(e -> switchPanel("pos", btnPOS));
        btnImport.addActionListener(e -> switchPanel("import", btnImport));
        btnProduct.addActionListener(e -> switchPanel("product", btnProduct));
        btnInventory.addActionListener(e -> switchPanel("inventory", btnInventory));
        btnSupplier.addActionListener(e -> switchPanel("supplier", btnSupplier));
        btnCustomer.addActionListener(e -> switchPanel("customer", btnCustomer));
        btnInvoice.addActionListener(e -> switchPanel("invoice", btnInvoice));
        btnUserMgmt.addActionListener(e -> switchPanel("usermgmt", btnUserMgmt));
        btnHrAdmin.addActionListener(e -> switchPanel("hradmin", btnHrAdmin));
        btnLogout.addActionListener(e -> doLogout());

        // Default view
        if (Session.isAdmin()) {
            switchPanel("dashboard", btnDashboard);
        } else {
            switchPanel("pos", btnPOS); // Default as POS since check-in is popup
        }
    }

    private void applyPermissions() {
        if (!Session.isAdmin()) {
            btnDashboard.setVisible(false);
            btnImport.setVisible(false);
            // Quản Lý: ẩn NCC, Kho, HRM (NV chỉ coi SP + KH + Hóa đơn)
            btnSupplier.setVisible(false);
            btnInventory.setVisible(false);
            btnHrAdmin.setVisible(false);
            // Quản Trị: ẩn toàn bộ
            btnUserMgmt.getParent().setVisible(false);
            for (Component c : sidebar.getComponents()) {
                if (c instanceof JButton) {
                    JButton b = (JButton) c;
                    if (b.getText() != null && b.getText().contains("QUẢN TRỊ")) {
                        b.setVisible(false);
                    }
                }
            }
        }
    }

    private void switchPanel(String name, JButton button) {
        cardLayout.show(contentPanel, name);
        setActiveButton(button);
    }

    private void setActiveButton(JButton button) {
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

    /**
     * Top-level sidebar button (Dashboard, Bán Hàng, Nhập Kho, Đăng Xuất)
     */
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
        btn.setPreferredSize(new Dimension(230, 40));
        btn.setBorder(new EmptyBorder(0, 16, 0, 12));

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

        allNavButtons.add(btn);
        return btn;
    }

    /**
     * Sub-button inside a collapsible section — indented further left
     */
    private JButton createSubButton(String text) {
        JButton btn = new JButton("  " + text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setForeground(AppColors.SIDEBAR_TEXT);
        btn.setBackground(AppColors.SIDEBAR_BG);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btn.setPreferredSize(new Dimension(230, 36));
        btn.setBorder(new EmptyBorder(0, 36, 0, 12)); // extra indent

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

        allNavButtons.add(btn);
        return btn;
    }

    /**
     * Collapsible section header — click to show/hide the container
     */
    private JButton createSectionToggle(String title, JPanel container) {
        // Arrow indicator
        String arrowDown = "\u25BC"; // ▼
        String arrowUp   = "\u25B2"; // ▲

        JButton toggle = new JButton("  " + title + "   " + arrowDown);
        toggle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        toggle.setForeground(AppColors.SECONDARY_LIGHT);
        toggle.setBackground(AppColors.SIDEBAR_BG);
        toggle.setHorizontalAlignment(SwingConstants.LEFT);
        toggle.setBorderPainted(false);
        toggle.setFocusPainted(false);
        toggle.setOpaque(true);
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        toggle.setPreferredSize(new Dimension(230, 34));
        toggle.setBorder(new EmptyBorder(6, 14, 4, 12));

        // Hover
        toggle.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                toggle.setBackground(new Color(0x3A, 0x42, 0x56));
                toggle.setForeground(Color.WHITE);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                toggle.setBackground(AppColors.SIDEBAR_BG);
                toggle.setForeground(AppColors.SECONDARY_LIGHT);
            }
        });

        // Toggle action
        toggle.addActionListener(e -> {
            boolean nowVisible = !container.isVisible();
            container.setVisible(nowVisible);
            toggle.setText("  " + title + "   " + (nowVisible ? arrowUp : arrowDown));
            sidebar.revalidate();
            sidebar.repaint();
        });

        return toggle;
    }

    private JPanel createSidebarDivider() {
        JPanel divider = new JPanel();
        divider.setBackground(AppColors.SECONDARY_DARK);
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setPreferredSize(new Dimension(0, 1));
        return divider;
    }
}
