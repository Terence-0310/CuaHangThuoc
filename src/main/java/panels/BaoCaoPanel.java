package panels;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;

import java.awt.Color;

import common.ColorScheme;
import common.UIHelper;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import entity.NguoiDung;

/**
 * BaoCaoPanel - Panel báo cáo với biểu đồ
 * 
 * @author Generated
 * @version 1.0
 */
public class BaoCaoPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private JComboBox<String> comboDisplayType;
	private JRadioButton radioChart;
	private JRadioButton radioReport;
	private JRadioButton radioTime;
	private JRadioButton radioProfit;
	private JRadioButton radioDiscount;
	private JRadioButton radioReturn;
	private JRadioButton radioEmployee;
	private ChartPanel chartPanel;

	/**
	 * Create the panel.
	 */
	public BaoCaoPanel(NguoiDung currentUser) {
		initialize();
	}

	/**
	 * Initialize the contents of the panel.
	 */
	private void initialize() {
		setLayout(new BorderLayout());
		setBackground(ColorScheme.BACKGROUND);
		setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		// Top bar with title and export button
		var topPanel = new JPanel(new BorderLayout());
		topPanel.setOpaque(false);

		var lblTitle = new JLabel("Báo cáo bán hàng");
		lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
		lblTitle.setForeground(ColorScheme.TEXT_PRIMARY);
		topPanel.add(lblTitle, BorderLayout.WEST);

		var btnExport = UIHelper.createDangerButton("Xuất tất cả");
		topPanel.add(btnExport, BorderLayout.EAST);

		add(topPanel, BorderLayout.NORTH);

		// Main content area
		var mainPanel = new JPanel(new GridLayout(1, 2, 15, 0));

		// Left panel - Filters
		var leftPanel = createFilterPanel();
		mainPanel.add(leftPanel);

		// Right panel - Chart
		var rightPanel = createChartPanel();
		mainPanel.add(rightPanel);

		add(mainPanel, BorderLayout.CENTER);
	}

	/**
	 * Tạo panel filter bên trái
	 */
	private JPanel createFilterPanel() {
		var panel = new JPanel();
		panel.setBackground(ColorScheme.PANEL_BG);
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.BORDER, 1),
			BorderFactory.createEmptyBorder(20, 20, 20, 20)
		));
		panel.setLayout(new java.awt.GridLayout(0, 1, 0, 20));

		// Kiểu hiển thị
		var displayTypePanel = createSectionPanel("Kiểu hiển thị");
		radioChart = new JRadioButton("Biểu đồ", true);
		radioReport = new JRadioButton("Báo cáo");
		var displayGroup = new ButtonGroup();
		displayGroup.add(radioChart);
		displayGroup.add(radioReport);
		displayTypePanel.add(radioChart);
		displayTypePanel.add(radioReport);
		panel.add(displayTypePanel);

		// Mối quan tâm
		var interestPanel = createSectionPanel("Mối quan tâm");
		radioTime = new JRadioButton("Thời gian");
		radioProfit = new JRadioButton("Lợi nhuận", true);
		radioDiscount = new JRadioButton("Giảm giá hóa đơn");
		radioReturn = new JRadioButton("Trả hàng");
		radioEmployee = new JRadioButton("Nhân viên");
		var interestGroup = new ButtonGroup();
		interestGroup.add(radioTime);
		interestGroup.add(radioProfit);
		interestGroup.add(radioDiscount);
		interestGroup.add(radioReturn);
		interestGroup.add(radioEmployee);
		interestPanel.add(radioTime);
		interestPanel.add(radioProfit);
		interestPanel.add(radioDiscount);
		interestPanel.add(radioReturn);
		interestPanel.add(radioEmployee);
		panel.add(interestPanel);

		// Thời gian
		var timePanel = createSectionPanel("Thời gian");
		var dateFrom = LocalDate.now().minusMonths(3);
		var dateTo = LocalDate.now();
		var lblDateRange = new JLabel(
			dateFrom.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " - " +
			dateTo.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
		);
		lblDateRange.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		timePanel.add(lblDateRange);
		panel.add(timePanel);

		return panel;
	}

	/**
	 * Tạo section panel
	 */
	private JPanel createSectionPanel(String title) {
		var panel = new JPanel();
		panel.setOpaque(false);
		panel.setLayout(new java.awt.GridLayout(0, 1, 5, 5));
		panel.setBorder(BorderFactory.createTitledBorder(
			BorderFactory.createLineBorder(ColorScheme.BORDER, 1),
			title,
			javax.swing.border.TitledBorder.LEFT,
			javax.swing.border.TitledBorder.TOP,
			new Font("Segoe UI", Font.PLAIN, 13),
			ColorScheme.TEXT_SECONDARY
		));
		return panel;
	}

	/**
	 * Tạo panel chart bên phải
	 */
	private JPanel createChartPanel() {
		var panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.PANEL_BG);
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.BORDER, 1),
			BorderFactory.createEmptyBorder(20, 20, 20, 20)
		));

		// Chart title
		var titlePanel = new JPanel(new BorderLayout());
		titlePanel.setOpaque(false);
		var lblChartTitle = new JLabel("Báo cáo lợi nhuận theo lợi nhuận");
		lblChartTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
		lblChartTitle.setForeground(ColorScheme.TEXT_PRIMARY);
		titlePanel.add(lblChartTitle, BorderLayout.WEST);
		panel.add(titlePanel, BorderLayout.NORTH);

		// Create sample chart
		var dataset = new DefaultCategoryDataset();
		dataset.addValue(150000, "Lợi nhuận", "07-2024");
		dataset.addValue(750000, "Doanh thu", "07-2024");
		dataset.addValue(600000, "Giá vốn", "07-2024");

		JFreeChart chart = ChartFactory.createBarChart(
			"",
			"Thời gian",
			"Giá trị",
			dataset,
			PlotOrientation.VERTICAL,
			true,
			true,
			false
		);

		// Customize chart
		chart.getTitle().setFont(new Font("Segoe UI", Font.BOLD, 14));
		chart.getLegend().setItemFont(new Font("Segoe UI", Font.PLAIN, 12));
		chart.setBackgroundPaint(ColorScheme.PANEL_BG);

		chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 400));
		panel.add(chartPanel, BorderLayout.CENTER);

		// Legend
		var legendPanel = new JPanel();
		legendPanel.setOpaque(false);
		legendPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 15, 5));

		legendPanel.add(createLegendItem(ColorScheme.CHART_1, "Lợi nhuận"));
		legendPanel.add(createLegendItem(ColorScheme.CHART_2, "Doanh thu"));
		legendPanel.add(createLegendItem(ColorScheme.CHART_3, "Giá vốn"));

		panel.add(legendPanel, BorderLayout.SOUTH);

		return panel;
	}

	/**
	 * Tạo legend item
	 */
	private JPanel createLegendItem(java.awt.Color color, String text) {
		var panel = new JPanel();
		panel.setOpaque(false);
		panel.setLayout(new java.awt.BorderLayout(8, 0));

		var colorBox = new JPanel();
		colorBox.setPreferredSize(new java.awt.Dimension(18, 18));
		colorBox.setBackground(color);
		colorBox.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		panel.add(colorBox, BorderLayout.WEST);

		var lbl = new JLabel(text);
		lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		lbl.setForeground(ColorScheme.TEXT_PRIMARY);
		panel.add(lbl, BorderLayout.CENTER);

		return panel;
	}
}
