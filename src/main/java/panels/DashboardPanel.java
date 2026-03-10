package panels;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import common.ColorScheme;
import common.IconHelper;
import entity.NguoiDung;

/**
 * DashboardPanel - Panel tổng quan với thống kê và cảnh báo
 * 
 * @author Improved by Agent
 * @version 2.2
 */
public class DashboardPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private javax.swing.JLabel lblProductCount;
	private javax.swing.JLabel lblBatchCount;
	private javax.swing.JLabel lblInvoiceCount;
	private javax.swing.JLabel lblRevenue;
	private javax.swing.table.DefaultTableModel modelExpiring;
	private javax.swing.table.DefaultTableModel modelLowStock;
	private dao.ThongKeDao thongKeDao;

	/**
	 * Create the panel.
	 */
	public DashboardPanel(NguoiDung currentUser) {
		initialize();
	}

	/**
	 * Initialize the contents of the panel.
	 */
	private void initialize() {
		thongKeDao = new dao.ThongKeDao();
		setLayout(new BorderLayout());
		setBackground(ColorScheme.BACKGROUND);
		setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

		// --- Header Section ---
		var topSection = new JPanel(new BorderLayout());
		topSection.setOpaque(false);
		topSection.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));

		// Title
		var titlePanel = new JPanel(new BorderLayout());
		titlePanel.setOpaque(false);
		var lblTitle = new JLabel("Tổng quan");
		lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
		lblTitle.setForeground(ColorScheme.TEXT_PRIMARY);
		titlePanel.add(lblTitle, BorderLayout.WEST);

		var lblSubtitle = new JLabel("Thống kê và cảnh báo hệ thống");
		lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		lblSubtitle.setForeground(ColorScheme.TEXT_SECONDARY);
		lblSubtitle.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
		titlePanel.add(lblSubtitle, BorderLayout.SOUTH);

		topSection.add(titlePanel, BorderLayout.WEST);

		// Refresh Button
		var btnRefresh = new javax.swing.JButton("Làm mới dữ liệu");
		btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 14));
		btnRefresh.setBackground(ColorScheme.PRIMARY);
		btnRefresh.setForeground(java.awt.Color.WHITE);
		btnRefresh.setFocusPainted(false);
		btnRefresh.putClientProperty("JButton.buttonType", "roundRect");
		btnRefresh.addActionListener(e -> refreshData());
		topSection.add(btnRefresh, BorderLayout.EAST);

		add(topSection, BorderLayout.NORTH);

		// --- Main Content (Scrollable) ---
		var mainContent = new JPanel();
		mainContent.setOpaque(false);
		mainContent.setLayout(new javax.swing.BoxLayout(mainContent, javax.swing.BoxLayout.Y_AXIS));
		
		// 1. Stats Cards
		var statsPanel = new JPanel();
		statsPanel.setOpaque(false);
		statsPanel.setLayout(new GridLayout(1, 4, 20, 0));
		statsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
		statsPanel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 140));

		// Create cards with member variables references
		statsPanel.add(createStatCard("Tổng sản phẩm", "0", ColorScheme.CHART_1, "product.png", "📦", lblProductCount = new JLabel()));
		statsPanel.add(createStatCard("Lô hàng", "0", ColorScheme.CHART_2, "batch.png", "📋", lblBatchCount = new JLabel()));
		statsPanel.add(createStatCard("Hóa đơn hôm nay", "0", ColorScheme.CHART_3, "invoice.png", "🧾", lblInvoiceCount = new JLabel()));
		statsPanel.add(createStatCard("Doanh thu hôm nay", "0₫", ColorScheme.CHART_5, "revenue.png", "💰", lblRevenue = new JLabel()));

		mainContent.add(statsPanel);

		// 2. Alerts Section
		var alertsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
		alertsPanel.setOpaque(false);

		// Table: Sắp hết hạn
		alertsPanel.add(createAlertTablePanel(
			"⚠️ Thuốc sắp hết hạn (3 tháng)", 
			new String[]{"Tên thuốc", "Số lô", "Hạn SD", "Còn lại"},
			modelExpiring = new javax.swing.table.DefaultTableModel(new Object[]{"Tên thuốc", "Số lô", "Hạn SD", "Còn lại"}, 0)
		));

		// Table: Sắp hết hàng
		alertsPanel.add(createAlertTablePanel(
			"📉 Thuốc cần nhập (Tồn kho thấp)", 
			new String[]{"Tên thuốc", "Tồn kho", "Mức tối thiểu"},
			modelLowStock = new javax.swing.table.DefaultTableModel(new Object[]{"Tên thuốc", "Tồn kho", "Mức tối thiểu"}, 0)
		));

		mainContent.add(alertsPanel);

		add(mainContent, BorderLayout.CENTER);

		// Initial Data Load
		refreshData();
	}

	/**
	 * Load real data from DB
	 */
	private void refreshData() {
		// 1. Stats
		var thongKeNgay = thongKeDao.getThongKeNgay();
		lblInvoiceCount.setText(String.valueOf(thongKeNgay.soHoaDon));
		lblRevenue.setText(java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("vi", "VN")).format(thongKeNgay.doanhThu));
		// TODO: Add methods for Product and Batch count if needed, or query Dao
		
		// 2. Expiring Products (Limit 90 days)
		modelExpiring.setRowCount(0);
		var listHetHan = thongKeDao.getThuocSapHetHan(90);
		for (var item : listHetHan) {
			modelExpiring.addRow(new Object[]{
				item.tenSanPham, 
				item.soLo, 
				new java.text.SimpleDateFormat("dd/MM/yyyy").format(item.hanSuDung),
				item.soNgayConLai + " ngày"
			});
		}

		// 3. Low Stock
		modelLowStock.setRowCount(0);
		var listCanNhap = thongKeDao.getThuocCanNhap();
		for (var item : listCanNhap) {
			modelLowStock.addRow(new Object[]{
				item.tenSanPham,
				item.tongTon,
				item.mucTonToiThieu
			});
		}
	}

	private JPanel createAlertTablePanel(String title, String[] columns, javax.swing.table.DefaultTableModel model) {
		var panel = new JPanel(new BorderLayout(0, 10));
		panel.setBackground(ColorScheme.PANEL_BG);
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.BORDER, 1),
			BorderFactory.createEmptyBorder(15, 15, 15, 15)
		));

		var lblTitle = new JLabel(title);
		lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
		lblTitle.setForeground(ColorScheme.DANGER);
		panel.add(lblTitle, BorderLayout.NORTH);

		var table = new javax.swing.JTable(model);
		table.setRowHeight(30);
		table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
		table.setShowGrid(false);
		table.setShowHorizontalLines(true);
		
		var scroll = new javax.swing.JScrollPane(table);
		scroll.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER)); // Kẻ khung
		panel.add(scroll, BorderLayout.CENTER);

		return panel;
	}

	/**
	 * Tạo stat card (Modified to accept label reference)
	 */
	private JPanel createStatCard(String title, String value, java.awt.Color color, String iconName, String fallbackIcon, javax.swing.JLabel valueLabelRef) {
		var card = new JPanel();
		card.setBackground(ColorScheme.PANEL_BG);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.BORDER, 1),
			BorderFactory.createEmptyBorder(20, 20, 20, 20)
		));
		card.setLayout(new BorderLayout(0, 10));

		// Header
		var headerPanel = new JPanel(new BorderLayout(10, 0));
		headerPanel.setOpaque(false);
		
		javax.swing.ImageIcon icon = IconHelper.loadIcon(iconName, 28);
		JLabel iconLabel = (icon != null) ? new JLabel(icon) : new JLabel(fallbackIcon);
		if (icon == null) iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 24));
		
		headerPanel.add(iconLabel, BorderLayout.WEST);
		
		var lblTitle = new JLabel(title);
		lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		lblTitle.setForeground(ColorScheme.TEXT_SECONDARY);
		headerPanel.add(lblTitle, BorderLayout.CENTER);
		
		card.add(headerPanel, BorderLayout.NORTH);

		// Value Label
		valueLabelRef.setText(value);
		valueLabelRef.setFont(new Font("Segoe UI", Font.BOLD, 28));
		valueLabelRef.setForeground(color);
		card.add(valueLabelRef, BorderLayout.CENTER);

		return card;
	}
}
