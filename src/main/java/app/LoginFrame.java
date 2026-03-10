package app;

import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import com.formdev.flatlaf.FlatClientProperties;
import common.ColorScheme;
import dao.NguoiDungDao;
import entity.NguoiDung;

/**
 * LoginFrame - Màn hình đăng nhập
 * 
 * @author Improved by Agent
 * @version 2.0
 */
public class LoginFrame extends JFrame {
	private static final long serialVersionUID = 1L;

	private JTextField txtUsername;
	private JPasswordField txtPassword;
	private JButton btnLogin;
	private JButton btnExit;

	/**
	 * Create the frame.
	 */
	public LoginFrame() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		setTitle("Đăng nhập - Cửa hàng thuốc");
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setResizable(false); // Không cho phép resize
		setLocationRelativeTo(null); // Căn giữa màn hình

		// Sử dụng pack() để tự động tính kích thước
		var contentPane = new javax.swing.JPanel();
		contentPane.setBackground(ColorScheme.BACKGROUND);
		contentPane.setLayout(new java.awt.BorderLayout());
		setContentPane(contentPane);

		// Main container với padding
		var mainContainer = new javax.swing.JPanel();
		mainContainer.setBackground(ColorScheme.BACKGROUND);
		mainContainer.setLayout(new java.awt.BorderLayout());
		mainContainer.setBorder(new EmptyBorder(50, 50, 50, 50));
		contentPane.add(mainContainer, java.awt.BorderLayout.CENTER);

		// Title Panel
		var titlePanel = new javax.swing.JPanel();
		titlePanel.setOpaque(false);
		titlePanel.setLayout(new java.awt.BorderLayout());
		titlePanel.setBorder(new EmptyBorder(0, 0, 30, 0));

		var lblTitle = new JLabel("CỬA HÀNG THUỐC");
		lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
		lblTitle.setForeground(ColorScheme.PRIMARY_DARK);
		lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
		titlePanel.add(lblTitle, java.awt.BorderLayout.CENTER);

		var lblSubtitle = new JLabel("Hệ thống quản lý bán hàng");
		lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		lblSubtitle.setForeground(ColorScheme.TEXT_SECONDARY);
		lblSubtitle.setHorizontalAlignment(SwingConstants.CENTER);
		lblSubtitle.setBorder(new EmptyBorder(8, 0, 0, 0));
		titlePanel.add(lblSubtitle, java.awt.BorderLayout.SOUTH);

		mainContainer.add(titlePanel, java.awt.BorderLayout.NORTH);

		// Form Panel
		var formPanel = new javax.swing.JPanel();
		formPanel.setBackground(ColorScheme.PANEL_BG);
		// Add subtle drop shadow if possible, otherwise simple border
		formPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(
				javax.swing.BorderFactory.createLineBorder(ColorScheme.BORDER, 1),
				new EmptyBorder(35, 35, 35, 35)));
		formPanel.setLayout(new java.awt.GridBagLayout());
		var gbc = new java.awt.GridBagConstraints();
		gbc.insets = new java.awt.Insets(5, 5, 15, 5);
		gbc.anchor = java.awt.GridBagConstraints.WEST;

		// Username
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		var lblUsername = new JLabel("Tên đăng nhập:");
		lblUsername.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		lblUsername.setForeground(ColorScheme.TEXT_PRIMARY);
		formPanel.add(lblUsername, gbc);

		gbc.gridy = 1;
		gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		txtUsername = new JTextField();
		txtUsername.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		txtUsername.setPreferredSize(new java.awt.Dimension(300, 40));
		// Setup Leading Icon
		// txtUsername.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
		// IconHelper.getIcon(MaterialDesignA.ACCOUNT, 18, ColorScheme.TEXT_SECONDARY));
		txtUsername.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập tên đăng nhập");
		txtUsername.putClientProperty(FlatClientProperties.STYLE, "arc: 10"); // Round corners

		txtUsername.setBackground(ColorScheme.INPUT_FOCUS);
		txtUsername.addActionListener(e -> txtPassword.requestFocus());
		formPanel.add(txtUsername, gbc);

		// Password
		gbc.gridy = 2;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		gbc.weightx = 0;
		var lblPassword = new JLabel("Mật khẩu:");
		lblPassword.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		lblPassword.setForeground(ColorScheme.TEXT_PRIMARY);
		formPanel.add(lblPassword, gbc);

		gbc.gridy = 3;
		gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		txtPassword = new JPasswordField();
		txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		txtPassword.setPreferredSize(new java.awt.Dimension(300, 40));
		// Setup Leading Icon and Reveal Button
		// txtPassword.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
		// IconHelper.getIcon(MaterialDesignA.ACCOUNT, 18, ColorScheme.TEXT_SECONDARY));
		txtPassword.putClientProperty(FlatClientProperties.STYLE, "arc: 10");
		txtPassword.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập mật khẩu");

		txtPassword.setBackground(ColorScheme.INPUT_FOCUS);
		txtPassword.addActionListener(this::btnLoginActionPerformed);
		formPanel.add(txtPassword, gbc);

		mainContainer.add(formPanel, java.awt.BorderLayout.CENTER);

		// Buttons Panel
		var buttonPanel = new javax.swing.JPanel();
		buttonPanel.setOpaque(false);
		buttonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 20, 0));
		buttonPanel.setBorder(new EmptyBorder(25, 0, 0, 0));

		btnLogin = new JButton("Đăng nhập");
		btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
		btnLogin.setBackground(ColorScheme.PRIMARY);
		btnLogin.setForeground(ColorScheme.TEXT_WHITE);
		btnLogin.setPreferredSize(new java.awt.Dimension(150, 42));
		btnLogin.setFocusPainted(false);
		btnLogin.setBorderPainted(false);
		btnLogin.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
		btnLogin.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseEntered(java.awt.event.MouseEvent e) {
				btnLogin.setBackground(ColorScheme.PRIMARY_HOVER);
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent e) {
				btnLogin.setBackground(ColorScheme.PRIMARY);
			}
		});
		btnLogin.addActionListener(this::btnLoginActionPerformed);
		buttonPanel.add(btnLogin);

		btnExit = new JButton("Thoát");
		btnExit.setFont(new Font("Segoe UI", Font.BOLD, 14));
		btnExit.setBackground(ColorScheme.DANGER);
		btnExit.setForeground(ColorScheme.TEXT_WHITE);
		btnExit.setPreferredSize(new java.awt.Dimension(150, 42));
		btnExit.setFocusPainted(false);
		btnExit.setBorderPainted(false);
		btnExit.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
		btnExit.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseEntered(java.awt.event.MouseEvent e) {
				btnExit.setBackground(ColorScheme.DANGER_HOVER);
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent e) {
				btnExit.setBackground(ColorScheme.DANGER);
			}
		});
		btnExit.addActionListener(e -> System.exit(0));
		buttonPanel.add(btnExit);

		mainContainer.add(buttonPanel, java.awt.BorderLayout.SOUTH);

		// Pack để tự động tính kích thước
		pack();

		// Đảm bảo form không thể resize
		setResizable(false);
		setLocationRelativeTo(null);

		// Focus on username field
		txtUsername.requestFocus();
	}

	/**
	 * Xử lý sự kiện đăng nhập
	 */
	protected void btnLoginActionPerformed(ActionEvent e) {
		String username = txtUsername.getText().trim();
		String password = new String(txtPassword.getPassword());

		if (username.isEmpty()) {
			JOptionPane.showMessageDialog(this,
					"Vui lòng nhập tên đăng nhập!",
					"Thông báo",
					JOptionPane.WARNING_MESSAGE);
			txtUsername.requestFocus();
			return;
		}

		if (password.isEmpty()) {
			JOptionPane.showMessageDialog(this,
					"Vui lòng nhập mật khẩu!",
					"Thông báo",
					JOptionPane.WARNING_MESSAGE);
			txtPassword.requestFocus();
			return;
		}

		// Thực hiện đăng nhập
		var dao = new NguoiDungDao();
		NguoiDung nguoiDung = dao.login(username, password);

		if (nguoiDung != null) {
			// Đăng nhập thành công
			JOptionPane.showMessageDialog(this,
					"Đăng nhập thành công!\nXin chào: " + nguoiDung.getHoTen(),
					"Thông báo",
					JOptionPane.INFORMATION_MESSAGE);

			// Mở MainFrame
			this.dispose();
			var mainFrame = new MainFrame(nguoiDung);
			mainFrame.setVisible(true);
		} else {
			// Đăng nhập thất bại
			JOptionPane.showMessageDialog(this,
					"Tên đăng nhập hoặc mật khẩu không đúng!",
					"Lỗi đăng nhập",
					JOptionPane.ERROR_MESSAGE);
			txtPassword.setText("");
			txtUsername.requestFocus();
		}
	}

	/**
	 * Main method
	 */
	public static void main(String[] args) {
		// Ép console xuất UTF-8 để hiển thị tiếng Việt và Emoji chuẩn (Requirement: FORCE_SYSTEM_OUT_UTF8)
		try {
			System.setOut(new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out), true, java.nio.charset.StandardCharsets.UTF_8.name()));
			System.setErr(new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.err), true, java.nio.charset.StandardCharsets.UTF_8.name()));
		} catch (Exception e) {}
		
		// Sử dụng FlatLaf với theme pastel
		try {
			UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");

			// Customize FlatLaf colors
			UIManager.put("Panel.background", ColorScheme.BACKGROUND);
			UIManager.put("Button.arc", 12);
			UIManager.put("TextComponent.arc", 8);
			UIManager.put("Component.focusWidth", 2);
			UIManager.put("Component.focusColor", ColorScheme.BORDER_FOCUS);
		} catch (Exception e) {
			e.printStackTrace();
		}

		java.awt.EventQueue.invokeLater(() -> {
			try {
				new LoginFrame().setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}
