package panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import common.ColorScheme;
import entity.NguoiDung;

/**
 * SidebarPanel - Thanh điều hướng bên trái với phân quyền động và Layout co giãn.
 */
public class SidebarPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private Consumer<String> onMenuSelection;
	private String currentAction = "dashboard";
	private Map<String, JButton> menuItems = new HashMap<>();
	private JPanel menuPanel;

	public SidebarPanel(NguoiDung currentUser, Consumer<String> onMenuSelection) {
		this.onMenuSelection = onMenuSelection;
		
		initialize(currentUser);
	}

	private void initialize(NguoiDung currentUser) {
		setBackground(ColorScheme.SIDEBAR_BG);
		setPreferredSize(new Dimension(260, 0));
		setLayout(new BorderLayout());

		// Sidebar header (Logo area)
		var sidebarHeader = new JPanel();
		sidebarHeader.setOpaque(false);
		sidebarHeader.setLayout(new java.awt.GridBagLayout());
		sidebarHeader.setPreferredSize(new Dimension(260, 80));
		sidebarHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(255, 255, 255, 30)));

		var lblLogo = new JLabel("MEPHAR");
		lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 24));
		lblLogo.setForeground(Color.WHITE);
		sidebarHeader.add(lblLogo);

		add(sidebarHeader, BorderLayout.NORTH);

		// Navigation menu
		menuPanel = new JPanel();
		menuPanel.setOpaque(false);
		// Sử dụng BoxLayout để các nút xếp sát nhau không có khoảng trống (Requirement: ALIGNMENT)
		menuPanel.setLayout(new javax.swing.BoxLayout(menuPanel, javax.swing.BoxLayout.Y_AXIS));
		menuPanel.setBorder(new EmptyBorder(15, 10, 15, 10));

		// Menu items with Permission Check (Requirement: CLEAN_RBAC_ARCHITECTURE)
		String role = currentUser.getVaiTro();
		addMenuItemIfAllowed(menuPanel, "Tổng quan", "dashboard", role);
		addMenuItemIfAllowed(menuPanel, "Bán hàng", "banhang", role);
		addMenuItemIfAllowed(menuPanel, "Nhập hàng", "nhaphang", role);
		addMenuItemIfAllowed(menuPanel, "Sản phẩm", "sanpham", role);
		addMenuItemIfAllowed(menuPanel, "Lô hàng", "lohang", role);
		addMenuItemIfAllowed(menuPanel, "Khách hàng", "khachhang", role);
		addMenuItemIfAllowed(menuPanel, "Nhà cung cấp", "nhacungcap", role);
		addMenuItemIfAllowed(menuPanel, "Báo cáo", "baocao", role);
		addMenuItemIfAllowed(menuPanel, "Người dùng", "nguoidung", role);

		var wrapperPanel = new JPanel(new BorderLayout());
		wrapperPanel.setOpaque(false);
		wrapperPanel.add(menuPanel, BorderLayout.NORTH);

		add(wrapperPanel, BorderLayout.CENTER);
	}

	private void addMenuItemIfAllowed(JPanel parent, String text, String action, String role) {
		if (utils.PermissionManager.hasAccess(role, action)) {
			addMenuItem(parent, text, action);
		}
	}

	private void addMenuItem(JPanel parent, String text, String action) {
		var btn = new JButton(text);
		btn.setHorizontalAlignment(SwingConstants.LEFT);
		btn.setIconTextGap(15);
		btn.setFont(new Font("Segoe UI", Font.PLAIN, 15));
		btn.setForeground(ColorScheme.SIDEBAR_TEXT);
		btn.setBackground(ColorScheme.SIDEBAR_BG);
		btn.setBorderPainted(false);
		btn.setFocusPainted(false);
		btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
		
		// Đảm bảo nút chiếm hết chiều ngang trong BoxLayout
		btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
		btn.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

		btn.putClientProperty("JButton.buttonType", "roundRect");
		btn.putClientProperty("Component.arc", 16);
		btn.setBorder(new EmptyBorder(10, 15, 10, 15));

		btn.addActionListener(e -> {
			updateActiveState(action);
			if (onMenuSelection != null) {
				onMenuSelection.accept(action);
			}
		});

		// Hover effect (Requirement: DISABLED_VISUALS)
		btn.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseEntered(java.awt.event.MouseEvent e) {
				if (btn.isEnabled() && !action.equals(currentAction)) {
					btn.setBackground(ColorScheme.SIDEBAR_HOVER);
				} else if (!btn.isEnabled()) {
					btn.setCursor(new Cursor(Cursor.WAIT_CURSOR)); // (Requirement: CURSOR_FEEDBACK)
				}
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent e) {
				if (btn.isEnabled() && !action.equals(currentAction)) {
					btn.setBackground(ColorScheme.SIDEBAR_BG);
				}
			}
		});

		// Listen for property changes to update visuals when disabled
		btn.addPropertyChangeListener("enabled", evt -> {
			if (!(boolean)evt.getNewValue()) {
				btn.setForeground(Color.GRAY);
			} else {
				btn.setForeground(ColorScheme.SIDEBAR_TEXT);
			}
		});

		// Thêm khoảng cách nhỏ giữa các nút
		parent.add(javax.swing.Box.createRigidArea(new Dimension(0, 5)));
		parent.add(btn);
		menuItems.put(action, btn);
	}

	public void updateActiveState(String action) {
		// Reset active state of previous item
		if (currentAction != null && menuItems.containsKey(currentAction)) {
			var oldBtn = menuItems.get(currentAction);
			oldBtn.setBackground(ColorScheme.SIDEBAR_BG);
			oldBtn.setForeground(oldBtn.isEnabled() ? ColorScheme.SIDEBAR_TEXT : Color.GRAY);
			oldBtn.setFont(new Font("Segoe UI", Font.PLAIN, 15));
		}

		// Set new active state (Requirement: PREVENT_DISABLED_ACTIVE)
		currentAction = action;
		if (menuItems.containsKey(action)) {
			var newBtn = menuItems.get(action);
			if (newBtn.isEnabled()) {
				newBtn.setBackground(ColorScheme.SIDEBAR_ACTIVE);
				newBtn.setForeground(ColorScheme.SIDEBAR_TEXT_ACTIVE);
				newBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
			}
		}
	}
}
