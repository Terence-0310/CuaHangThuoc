package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import app.SessionManager;
import model.User;
import service.AuthService;

public class LoginForm extends JFrame {

	private JTextField txtUsername;
	private JPasswordField txtPassword;
	private AuthService authService = new AuthService();

	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				new LoginForm().setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public LoginForm() {
		setTitle("Pharmacy Management - Login");
		setSize(420, 320);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setResizable(false);


		var contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(20, 30, 20, 30));
		contentPane.setLayout(new BorderLayout(10, 10));
		setContentPane(contentPane);

		// ===== TITLE =====
		var lblTitle = new JLabel("PHARMACY LOGIN");
		lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
		lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
		lblTitle.setForeground(new Color(0, 102, 204));
		contentPane.add(lblTitle, BorderLayout.NORTH);

		// ===== FORM =====
		var formPanel = new JPanel(new GridBagLayout());
		var gbc = new GridBagConstraints();
		gbc.insets = new Insets(8, 8, 8, 8);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		var lblUsername = new JLabel("Username");
		lblUsername.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		gbc.gridx = 0;
		gbc.gridy = 0;
		formPanel.add(lblUsername, gbc);

		txtUsername = new JTextField();
		txtUsername.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1;
		formPanel.add(txtUsername, gbc);

		var lblPassword = new JLabel("Password");
		lblPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 0;
		formPanel.add(lblPassword, gbc);

		txtPassword = new JPasswordField();
		txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.weightx = 1;
		formPanel.add(txtPassword, gbc);

		contentPane.add(formPanel, BorderLayout.CENTER);

		// ===== BUTTON =====
		var btnLogin = new JButton("LOGIN");
		btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
		btnLogin.setBackground(new Color(0, 102, 204));
		btnLogin.setForeground(Color.BLUE);
		btnLogin.setFocusPainted(false);
		btnLogin.setPreferredSize(new Dimension(120, 40));

		var btnPanel = new JPanel();
		btnPanel.add(btnLogin);
		contentPane.add(btnPanel, BorderLayout.SOUTH);

		btnLogin.addActionListener(e -> handleLogin());
	}

	private boolean validateLoginForm() {
	    if (txtUsername.getText().trim().isEmpty()
	            || txtPassword.getPassword().length == 0) {

	        JOptionPane.showMessageDialog(
	            this,
	            "Vui lòng nhập đầy đủ tài khoản và mật khẩu",
	            "Thiếu thông tin",
	            JOptionPane.WARNING_MESSAGE
	        );
	        return false;
	    }
	    return true;
	}

	
	private void handleLogin() {
	    if (!validateLoginForm()) return;

	    try {
	        var user = authService.login(
	            txtUsername.getText().trim(),
	            new String(txtPassword.getPassword())
	        );

	        SessionManager.login(user);
	        new Dashboard().setVisible(true);
	        dispose();

	    } catch (Exception ex) {
	        JOptionPane.showMessageDialog(
	            this,
	            ex.getMessage(),
	            "Login failed",
	            JOptionPane.ERROR_MESSAGE
	        );
	    }
	}
}
