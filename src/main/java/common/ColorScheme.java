package common;

import java.awt.Color;

/**
 * ColorScheme - Quản lý màu sắc thống nhất cho toàn bộ ứng dụng
 * Updated: Premium Dark Sidebar Theme (Giao diện quản trị hiện đại)
 * 
 * @author Improved by Agent
 * @version 3.0
 */
public class ColorScheme {
	
	// ========== MÀU CHÍNH (BRANDING) ==========
	/** Màu primary - Xanh dương đậm (Dùng cho nút chính, highlight) */
	public static final Color PRIMARY = new Color(13, 110, 253); // Bootstrap Primary
	
	/** Màu primary hover */
	public static final Color PRIMARY_HOVER = new Color(11, 94, 215);
	
	/** Màu primary dark */
	public static final Color PRIMARY_DARK = new Color(10, 88, 202);
	
	// ========== SIDEBAR (DARK NAVY THEME) ==========
	/** Màu nền sidebar - Xanh đen đậm (Dark Navy) */
	public static final Color SIDEBAR_BG = new Color(28, 35, 49); // #1C2331
	
	/** Màu nền sidebar header - Đậm hơn chút hoặc bằng */
	// public static final Color SIDEBAR_HEADER_BG = new Color(23, 29, 41);
	
	/** Màu text sidebar - Trắng mờ */
	public static final Color SIDEBAR_TEXT = new Color(200, 200, 200);
	
	/** Màu text sidebar khi hover/active - Trắng tinh */
	public static final Color SIDEBAR_TEXT_ACTIVE = Color.WHITE;
	
	/** Màu nền sidebar item khi hover */
	public static final Color SIDEBAR_HOVER = new Color(255, 255, 255, 20); // White with alpha
	
	/** Màu nền sidebar item khi active (Đang chọn) - Gradient hoặc màu nổi */
	public static final Color SIDEBAR_ACTIVE = new Color(220, 53, 69); // Red Accent (như ảnh mẫu) hoặc giữ Blue
	// Let's use the Red/Pink accent from the user reference image if possible, or a strong Blue.
	// The user image shows blue. Let's stick to a strong Primary Blue for active background.
	// public static final Color SIDEBAR_ACTIVE_BG = new Color(13, 110, 253); 
	
	// ========== MÀU NỀN & PANEL (LIGHT THEME) ==========
	/** Màu nền chính - Xám rất nhạt (Tạo độ nổi cho Card) */
	public static final Color BACKGROUND = new Color(245, 247, 250); // #F5F7FA
	
	/** Màu nền panel/card - Trắng tinh */
	public static final Color PANEL_BG = Color.WHITE;
	
	// ========== MÀU TEXT NỘI DUNG ==========
	/** Màu text chính - Đen xám */
	public static final Color TEXT_PRIMARY = new Color(33, 37, 41);
	
	/** Màu text phụ - Xám trung tính */
	public static final Color TEXT_SECONDARY = new Color(108, 117, 125);
	
	// ========== TRẠNG THÁI ==========
	public static final Color SUCCESS = new Color(25, 135, 84);  // Xanh lá đậm
	public static final Color WARNING = new Color(255, 193, 7);  // Vàng
	public static final Color DANGER = new Color(220, 53, 69);   // Đỏ
	public static final Color INFO = new Color(13, 202, 240);    // Xanh cyan
	
	// ========== CHART COLORS (VIVID) ==========
	public static final Color CHART_1 = new Color(54, 162, 235); // Blue
	public static final Color CHART_2 = new Color(75, 192, 192); // Green
	public static final Color CHART_3 = new Color(255, 206, 86); // Yellow
	public static final Color CHART_4 = new Color(255, 159, 64); // Orange
	public static final Color CHART_5 = new Color(255, 99, 132); // Red
	
	// ========== BORDER ==========
	public static final Color BORDER = new Color(222, 226, 230);
	public static final Color BORDER_FOCUS = new Color(13, 110, 253);
	
	// ========== MÀU INPUT & EXTRAS (Restored) ==========
	public static final Color INPUT_FOCUS = Color.WHITE;
	public static final Color INPUT_DISABLED = new Color(248, 248, 250);
	public static final Color DANGER_HOVER = DANGER.darker();
	public static final Color NEUTRAL = new Color(200, 210, 220);
	public static final Color TEXT_WHITE = Color.WHITE;
	public static final Color SHADOW = new Color(0, 0, 0, 15);

	// ========== UTILS ==========
	public static Color getHoverColor(Color color) {
		return color.darker();
	}
}
