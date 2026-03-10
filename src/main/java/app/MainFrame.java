package app;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import common.ColorScheme;
import common.ConnectDB;
import common.IconHelper;
import entity.NguoiDung;
import panels.BaoCaoPanel;
import panels.BanHangPanel;
import panels.DashboardPanel;
import panels.KhachHangPanel;
import panels.LoHangPanel;
import panels.NguoiDungPanel;
import panels.NhaCungCapPanel;
import panels.NhapHangPanel;
import panels.SanPhamPanel;

/**
 * MainFrame - Frame chính của ứng dụng với sidebar navigation cải tiến
 * Updated: Premium Dark Sidebar Support with Rounded Buttons
 * 
 * @author Improved by Agent
 * @version 3.0
 */
public class MainFrame extends JFrame {
	private static final long serialVersionUID = 1L;

	private NguoiDung currentUser;
	private JPanel sidebarPanel;
	private JPanel mainContentPanel;
	private String currentAction = "dashboard";
	private java.util.Map<String, JButton> menuItems = new java.util.HashMap<>();

	/**
	 * Create the frame.
	 */
	public MainFrame(NguoiDung nguoiDung) {
		this.currentUser = nguoiDung;
		
		// Setup FlatLaf
		try {
			javax.swing.UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
			
			// Customize FlatLaf colors
			javax.swing.UIManager.put("Panel.background", ColorScheme.BACKGROUND);
			javax.swing.UIManager.put("Button.arc", 12);
			javax.swing.UIManager.put("TextComponent.arc", 8);
			javax.swing.UIManager.put("Component.focusWidth", 2);
			javax.swing.UIManager.put("Component.focusColor", ColorScheme.BORDER_FOCUS);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		initialize();
		setupPermissions();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		setTitle("Cửa hàng thuốc - " + currentUser.getHoTen() + " (" + currentUser.getVaiTro() + ")");
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		setLocationRelativeTo(null);

		// Đóng connection pool khi đóng frame
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				int option = javax.swing.JOptionPane.showConfirmDialog(
					MainFrame.this,
					"Bạn có chắc chắn muốn thoát?",
					"Xác nhận",
					javax.swing.JOptionPane.YES_NO_OPTION
				);
				if (option == javax.swing.JOptionPane.YES_OPTION) {
					ConnectDB.closeConnectionPool();
					System.exit(0);
				}
			}
		});

		var contentPane = new JPanel();
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		// Sidebar (Left) matches full height
		createSidebar();

		// Main Content (Header + Body)
		createMainContentContainer();

		// Load dashboard by default và highlight menu
		handleMenuClick("dashboard");
	}

	/**
	 * Create Main Content Container (Header + Content)
	 */
	private void createMainContentContainer() {
		var container = new JPanel(new BorderLayout());
		container.setBackground(ColorScheme.BACKGROUND);
		
		// Header ở trên cùng của Main Content
		var headerPanel = createHeaderPanel();
		container.add(headerPanel, BorderLayout.NORTH);

		// Main Content
		mainContentPanel = new JPanel();
		mainContentPanel.setLayout(new BorderLayout());
		mainContentPanel.setBackground(ColorScheme.BACKGROUND);
		mainContentPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		container.add(mainContentPanel, BorderLayout.CENTER);

		getContentPane().add(container, BorderLayout.CENTER);
	}

	/**
	 * Tạo header bar (Updated)
	 */
	private JPanel createHeaderPanel() {
		var headerPanel = new JPanel();
		headerPanel.setBackground(ColorScheme.PANEL_BG);
		headerPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.BORDER),
			BorderFactory.createEmptyBorder(0, 20, 0, 20)
		));
		headerPanel.setPreferredSize(new Dimension(0, 60));
		headerPanel.setLayout(new BorderLayout());

		// Left: Title
		var lblPageTitle = new JLabel("MEPHAR QUẢN LÝ");
		lblPageTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
		lblPageTitle.setForeground(ColorScheme.PRIMARY);
		headerPanel.add(lblPageTitle, BorderLayout.WEST);

		// Right: User Info
		var userPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 15, 10));
		userPanel.setOpaque(false);

		var lblUser = new JLabel("Xin chào, " + currentUser.getHoTen());
		lblUser.setFont(new Font("Segoe UI", Font.BOLD, 14));
		lblUser.setForeground(ColorScheme.TEXT_PRIMARY);
		userPanel.add(lblUser);

		var btnLogout = new JButton("Đăng xuất");
		btnLogout.setBackground(ColorScheme.BACKGROUND); // Light gray button
		btnLogout.setForeground(ColorScheme.DANGER);
		btnLogout.setFocusPainted(false);
		btnLogout.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER));
		btnLogout.addActionListener(e -> {
			int option = javax.swing.JOptionPane.showConfirmDialog(
				this, "Bạn có muốn đăng xuất không?", "Đăng xuất", javax.swing.JOptionPane.YES_NO_OPTION
			);
			if (option == javax.swing.JOptionPane.YES_OPTION) {
				this.dispose();
				new LoginFrame().setVisible(true);
			}
		});
		userPanel.add(btnLogout);

		headerPanel.add(userPanel, BorderLayout.EAST);
		return headerPanel;
	}

	/**
	 * Tạo sidebar navigation với Premium Dark Theme và Rounded Buttons
	 */
	private void createSidebar() {
		sidebarPanel = new JPanel();
		sidebarPanel.setBackground(ColorScheme.SIDEBAR_BG);
		sidebarPanel.setPreferredSize(new Dimension(260, 0));
		sidebarPanel.setLayout(new BorderLayout());

		// Sidebar header (Logo area)
		var sidebarHeader = new JPanel();
		sidebarHeader.setOpaque(false);
		sidebarHeader.setLayout(new java.awt.GridBagLayout());
		sidebarHeader.setPreferredSize(new Dimension(260, 80));
		sidebarHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(255,255,255,30)));

		var lblLogo = new JLabel("MEPHAR");
		lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 24));
		lblLogo.setForeground(Color.WHITE);
		sidebarHeader.add(lblLogo);

		sidebarPanel.add(sidebarHeader, BorderLayout.NORTH);

		// Navigation menu
		var menuPanel = new JPanel();
		menuPanel.setOpaque(false);
		menuPanel.setLayout(new java.awt.GridLayout(0, 1, 0, 8)); // 8px spacing
		menuPanel.setBorder(new EmptyBorder(15, 10, 15, 10));

		// Menu items
		addMenuItem(menuPanel, null, "Tổng quan", "dashboard", true);
		addMenuItem(menuPanel, null, "Bán hàng", "banhang", true);
		addMenuItem(menuPanel, null, "Nhập hàng", "nhaphang", true);
		addMenuItem(menuPanel, null, "Sản phẩm", "sanpham", true);
		addMenuItem(menuPanel, null, "Lô hàng", "lohang", true);
		addMenuItem(menuPanel, null, "Khách hàng", "khachhang", true);
		addMenuItem(menuPanel, null, "Nhà cung cấp", "nhacungcap", true);
		addMenuItem(menuPanel, null, "Báo cáo", "baocao", true);
		addMenuItem(menuPanel, null, "Người dùng", "nguoidung", true);

		// Add dummy filler to push items up if needed, or put inside a ScrollPane if many items
		var wrapperPanel = new JPanel(new BorderLayout());
		wrapperPanel.setOpaque(false);
		wrapperPanel.add(menuPanel, BorderLayout.NORTH);
		
		sidebarPanel.add(wrapperPanel, BorderLayout.CENTER);

		getContentPane().add(sidebarPanel, BorderLayout.WEST);
	}

	/**
	 * Thêm menu item vào sidebar (JButton implementation)
	 */
	private void addMenuItem(JPanel parent, Object iconCode, String text, String action, boolean enabled) {
		var btn = new JButton(text);
		btn.setHorizontalAlignment(SwingConstants.LEFT);
		btn.setIconTextGap(15);
		btn.setFont(new Font("Segoe UI", Font.PLAIN, 15));
		btn.setForeground(ColorScheme.SIDEBAR_TEXT);
		btn.setBackground(ColorScheme.SIDEBAR_BG);
		btn.setBorderPainted(false);
		btn.setFocusPainted(false);
		btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
		
		// Rounded Styling via FlatLaf
		btn.putClientProperty("JButton.buttonType", "roundRect");
		btn.putClientProperty("Component.arc", 16);
		btn.setBorder(new EmptyBorder(10, 15, 10, 15));

		// Icon logic (Placeholder)
		// if (iconCode != null) btn.setIcon(...);

		if (!enabled) {
			btn.setVisible(false);
		} else {
			btn.addActionListener(e -> handleMenuClick(action));
			
			// Hover effect
			btn.addMouseListener(new java.awt.event.MouseAdapter() {
				@Override
				public void mouseEntered(java.awt.event.MouseEvent e) {
					if (!action.equals(currentAction)) {
						btn.setBackground(ColorScheme.SIDEBAR_HOVER);
					}
				}

				@Override
				public void mouseExited(java.awt.event.MouseEvent e) {
					if (!action.equals(currentAction)) {
						btn.setBackground(ColorScheme.SIDEBAR_BG);
					}
				}
			});
		}

		parent.add(btn);
		menuItems.put(action, btn);
	}

	/**
	 * Xử lý click menu
	 */
	private void handleMenuClick(String action) {
		// Reset active state of previous item
		if (currentAction != null && menuItems.containsKey(currentAction)) {
			var oldBtn = menuItems.get(currentAction);
			oldBtn.setBackground(ColorScheme.SIDEBAR_BG);
			oldBtn.setForeground(ColorScheme.SIDEBAR_TEXT);
			oldBtn.setFont(new Font("Segoe UI", Font.PLAIN, 15));
		}
		
		// Set new active state
		currentAction = action;
		if (menuItems.containsKey(action)) {
			var newBtn = menuItems.get(action);
			newBtn.setBackground(ColorScheme.SIDEBAR_ACTIVE);
			newBtn.setForeground(ColorScheme.SIDEBAR_TEXT_ACTIVE);
			newBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
		}
		
		// Show panel
		switch (action) {
			case "dashboard": showPanel(new DashboardPanel(currentUser)); break;
			case "banhang": showPanel(new BanHangPanel(currentUser)); break;
			case "nhaphang": showPanel(new NhapHangPanel(currentUser)); break;
			case "sanpham": showPanel(new SanPhamPanel(currentUser)); break;
			case "lohang": showPanel(new LoHangPanel(currentUser)); break;
			case "khachhang": showPanel(new KhachHangPanel(currentUser)); break;
			case "nhacungcap": showPanel(new NhaCungCapPanel(currentUser)); break;
			case "baocao": showPanel(new BaoCaoPanel(currentUser)); break;
			case "nguoidung": showPanel(new NguoiDungPanel(currentUser)); break;
		}
	}

	/**
	 * Hiển thị panel
	 */
	private void showPanel(JPanel panel) {
		if (mainContentPanel != null) {
			mainContentPanel.removeAll();
			mainContentPanel.add(panel, BorderLayout.CENTER);
			mainContentPanel.revalidate();
			mainContentPanel.repaint();
		}
	}

	/**
	 * Setup permissions
	 */
	private void setupPermissions() {
		if (currentUser.getVaiTro().equalsIgnoreCase("Nhân viên")) {
			if (menuItems.containsKey("nhaphang")) menuItems.get("nhaphang").setVisible(false);
			if (menuItems.containsKey("nguoidung")) menuItems.get("nguoidung").setVisible(false);
			if (menuItems.containsKey("nhacungcap")) menuItems.get("nhacungcap").setVisible(false);
		} else if (currentUser.getVaiTro().equalsIgnoreCase("Quản lý")) {
			if (menuItems.containsKey("nguoidung")) menuItems.get("nguoidung").setVisible(false);
		}
	}
}
