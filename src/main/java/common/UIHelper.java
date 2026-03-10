package common;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JButton;

/**
 * UIHelper - Helper methods cho UI components
 * 
 * @author Generated
 * @version 1.0
 */
public class UIHelper {

	/**
	 * Tạo button với style thống nhất và hover effect (pastel)
	 */
	public static JButton createStyledButton(String text, Color bgColor) {
		var btn = new JButton(text);
		btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		btn.setBackground(bgColor);
		
		// Text color - nếu màu nền quá sáng thì dùng text đậm
		if (isLightColor(bgColor)) {
			btn.setForeground(ColorScheme.TEXT_PRIMARY);
		} else {
			btn.setForeground(ColorScheme.TEXT_WHITE);
		}
		
		btn.setFocusPainted(false);
		btn.setBorderPainted(false);
		
		// Hover effect
		Color hoverColor = ColorScheme.getHoverColor(bgColor);
		btn.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseEntered(java.awt.event.MouseEvent e) {
				btn.setBackground(hoverColor);
			}
			@Override
			public void mouseExited(java.awt.event.MouseEvent e) {
				btn.setBackground(bgColor);
			}
		});
		
		return btn;
	}
	
	/**
	 * Kiểm tra màu có phải màu sáng không
	 */
	private static boolean isLightColor(Color color) {
		// Tính độ sáng (luminance)
		double luminance = 0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue();
		return luminance > 200; // Nếu luminance > 200 thì là màu sáng
	}

	/**
	 * Tạo button primary (xanh dương)
	 */
	public static JButton createPrimaryButton(String text) {
		return createStyledButton(text, ColorScheme.PRIMARY);
	}

	/**
	 * Tạo button success (xanh lá)
	 */
	public static JButton createSuccessButton(String text) {
		return createStyledButton(text, ColorScheme.SUCCESS);
	}

	/**
	 * Tạo button danger (đỏ)
	 */
	public static JButton createDangerButton(String text) {
		return createStyledButton(text, ColorScheme.DANGER);
	}

	/**
	 * Tạo button warning (vàng cam)
	 */
	public static JButton createWarningButton(String text) {
		return createStyledButton(text, ColorScheme.WARNING);
	}

	/**
	 * Tạo button neutral (xám)
	 */
	public static JButton createNeutralButton(String text) {
		return createStyledButton(text, ColorScheme.NEUTRAL);
	}
	
	/**
	 * Tạo text field với style thống nhất (pastel)
	 */
	public static javax.swing.JTextField createStyledTextField() {
		var txt = new javax.swing.JTextField();
		txt.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		txt.setBorder(javax.swing.BorderFactory.createCompoundBorder(
			javax.swing.BorderFactory.createLineBorder(ColorScheme.BORDER, 1),
			new javax.swing.border.EmptyBorder(10, 12, 10, 12)
		));
		txt.setBackground(ColorScheme.INPUT_FOCUS);
		return txt;
	}
	
	/**
	 * Tạo text field disabled với style thống nhất
	 */
	public static javax.swing.JTextField createStyledTextFieldDisabled() {
		var txt = createStyledTextField();
		txt.setEditable(false);
		txt.setBackground(ColorScheme.INPUT_DISABLED);
		return txt;
	}
}
