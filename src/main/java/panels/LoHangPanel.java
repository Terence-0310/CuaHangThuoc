package panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import common.ColorScheme;
import common.UIHelper;
import components.LoHangTableModel;
import dao.LoHangDao;
import dao.SanPhamDao;
import entity.LoHang;
import entity.NguoiDung;

/**
 * LoHangPanel - Panel quản lý lô hàng
 * 
 * @author Generated
 * @version 1.0
 */
public class LoHangPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private LoHangDao loHangDao;
	private SanPhamDao sanPhamDao;
	private LoHangTableModel tableModel;
	private JTable table;
	private JTextField txtMaLo;
	private JComboBox<String> comboSanPham;
	private JTextField txtSoLo;
	private JTextField txtHanSuDung;
	private JTextField txtGiaNhap;
	private JTextField txtSoLuongTon;
	private JComboBox<String> comboTrangThai;
	private JButton btnCapNhatTrangThai;
	private JButton btnLamMoi;

	/**
	 * Create the panel.
	 */
	public LoHangPanel(NguoiDung currentUser) {
		loHangDao = new LoHangDao();
		sanPhamDao = new SanPhamDao();
		initialize();
		loadData();
	}

	/**
	 * Initialize the contents of the panel.
	 */
	private void initialize() {
		setLayout(new BorderLayout());
		setBackground(ColorScheme.BACKGROUND);
		setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		var titlePanel = new JPanel(new BorderLayout(0, 10));
		titlePanel.setOpaque(false);
		titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
		var lblTitle = new JLabel("Quản lý lô hàng");
		lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
		lblTitle.setForeground(ColorScheme.TEXT_PRIMARY);
		titlePanel.add(lblTitle, BorderLayout.WEST);
		
		var infoBanner = UIHelper.createInfoBanner("<html>ℹ️ <b>Nghiệp vụ Lô hàng:</b> Quản lý chi tiết từng lô thuốc, theo dõi tồn kho và cảnh báo <b>hạn sử dụng</b> thực tế.</html>");
		titlePanel.add(infoBanner, BorderLayout.SOUTH);
		
		add(titlePanel, BorderLayout.NORTH);

		var mainPanel = new JPanel(new BorderLayout(15, 0));
		mainPanel.setOpaque(false);

		var infoPanel = createInfoPanel();
		mainPanel.add(infoPanel, BorderLayout.WEST);

		var tablePanel = createTablePanel();
		mainPanel.add(tablePanel, BorderLayout.CENTER);

		add(mainPanel, BorderLayout.CENTER);
	}

	/**
	 * Tạo info panel (read-only, chỉ xem thông tin)
	 */
	private JPanel createInfoPanel() {
		var panel = new JPanel();
		panel.setBackground(ColorScheme.PANEL_BG);
		panel.setBorder(new TitledBorder(
				BorderFactory.createLineBorder(ColorScheme.BORDER, 1),
				"Thông tin lô hàng",
				TitledBorder.LEADING,
				TitledBorder.TOP,
				new Font("Segoe UI", Font.BOLD, 14),
				ColorScheme.TEXT_PRIMARY));
		panel.setLayout(null);
		panel.setPreferredSize(new java.awt.Dimension(350, 0));

		int y = 30;
		int labelWidth = 120;
		int fieldWidth = 200;
		int fieldHeight = 30;
		int spacing = 40;

		var lblMaLo = new JLabel("Mã lô:");
		lblMaLo.setBounds(20, y, labelWidth, 25);
		panel.add(lblMaLo);

		txtMaLo = new JTextField();
		txtMaLo.setEditable(false);
		txtMaLo.setBackground(ColorScheme.INPUT_DISABLED);
		txtMaLo.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtMaLo.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtMaLo);

		y += spacing;

		var lblSanPham = new JLabel("Sản phẩm:");
		lblSanPham.setBounds(20, y, labelWidth, 25);
		panel.add(lblSanPham);

		comboSanPham = new JComboBox<>();
		comboSanPham.setEnabled(false);
		comboSanPham.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(comboSanPham);

		y += spacing;

		var lblSoLo = new JLabel("Số lô:");
		lblSoLo.setBounds(20, y, labelWidth, 25);
		panel.add(lblSoLo);

		txtSoLo = new JTextField();
		txtSoLo.setEditable(false);
		txtSoLo.setBackground(ColorScheme.INPUT_DISABLED);
		txtSoLo.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtSoLo.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtSoLo);

		y += spacing;

		var lblHanSuDung = new JLabel("Hạn sử dụng:");
		lblHanSuDung.setBounds(20, y, labelWidth, 25);
		panel.add(lblHanSuDung);

		txtHanSuDung = new JTextField();
		txtHanSuDung.setEditable(false);
		txtHanSuDung.setBackground(ColorScheme.INPUT_DISABLED);
		txtHanSuDung.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtHanSuDung.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtHanSuDung);

		y += spacing;

		var lblGiaNhap = new JLabel("Giá nhập:");
		lblGiaNhap.setBounds(20, y, labelWidth, 25);
		panel.add(lblGiaNhap);

		txtGiaNhap = new JTextField();
		txtGiaNhap.setEditable(false);
		txtGiaNhap.setBackground(ColorScheme.INPUT_DISABLED);
		txtGiaNhap.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtGiaNhap.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtGiaNhap);

		y += spacing;

		var lblSoLuongTon = new JLabel("SL tồn:");
		lblSoLuongTon.setBounds(20, y, labelWidth, 25);
		panel.add(lblSoLuongTon);

		txtSoLuongTon = new JTextField();
		txtSoLuongTon.setEditable(false);
		txtSoLuongTon.setBackground(ColorScheme.INPUT_DISABLED);
		txtSoLuongTon.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtSoLuongTon.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtSoLuongTon);

		y += spacing;

		var lblTrangThai = new JLabel("Trạng thái:");
		lblTrangThai.setBounds(20, y, labelWidth, 25);
		panel.add(lblTrangThai);

		comboTrangThai = new JComboBox<>(new String[] { "Đang bán", "Ngưng bán", "Hết hàng" });
		comboTrangThai.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(comboTrangThai);

		y += spacing + 10;

		btnCapNhatTrangThai = UIHelper.createPrimaryButton("Cập nhật trạng thái");
		btnCapNhatTrangThai.setBounds(20, y, 320, 40);
		btnCapNhatTrangThai.addActionListener(e -> handleCapNhatTrangThai());
		panel.add(btnCapNhatTrangThai);

		y += 50;

		btnLamMoi = UIHelper.createNeutralButton("Làm mới");
		btnLamMoi.setBounds(20, y, 320, 40);
		btnLamMoi.addActionListener(e -> handleLamMoi());
		panel.add(btnLamMoi);

		return panel;
	}

	/**
	 * Tạo table panel
	 */
	private JPanel createTablePanel() {
		var panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.PANEL_BG);
		panel.setBorder(new TitledBorder(
				BorderFactory.createLineBorder(ColorScheme.BORDER, 1),
				"Danh sách lô hàng",
				TitledBorder.LEADING,
				TitledBorder.TOP,
				new Font("Segoe UI", Font.BOLD, 14),
				ColorScheme.TEXT_PRIMARY));

		tableModel = new LoHangTableModel();
		table = new JTable(tableModel);
		table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		table.setRowHeight(28);
		table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		table.setShowGrid(true);
		table.setGridColor(new Color(235, 238, 242));
		table.setIntercellSpacing(new java.awt.Dimension(1, 1));
		table.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				handleTableSelection();
			}
		});

		// ===== AUTO RESIZE + COLUMN WIDTHS =====
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		var cm = table.getColumnModel();
		cm.getColumn(0).setPreferredWidth(55); // Mã lô
		cm.getColumn(0).setMaxWidth(70);
		cm.getColumn(1).setPreferredWidth(55); // Mã SP
		cm.getColumn(1).setMaxWidth(70);
		cm.getColumn(2).setPreferredWidth(80); // Số lô
		cm.getColumn(2).setMaxWidth(100);
		cm.getColumn(3).setPreferredWidth(100); // Hạn sử dụng
		cm.getColumn(3).setMaxWidth(120);
		cm.getColumn(4).setPreferredWidth(100); // Giá nhập
		cm.getColumn(4).setMaxWidth(130);
		cm.getColumn(5).setPreferredWidth(65); // SL nhập
		cm.getColumn(5).setMaxWidth(80);
		cm.getColumn(6).setPreferredWidth(65); // SL tồn
		cm.getColumn(6).setMaxWidth(80);
		cm.getColumn(7).setPreferredWidth(90); // Trạng thái
		cm.getColumn(7).setMaxWidth(110);

		// Header styling
		table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

		var scrollPane = new JScrollPane(table);
		scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER));
		scrollPane.getViewport().setBackground(ColorScheme.PANEL_BG);
		panel.add(scrollPane, BorderLayout.CENTER);

		return panel;
	}

	public void loadData() {
		tableModel.setRowCount(0);
		var list = loHangDao.getAll();
		for (var lh : list) {
			tableModel.addRow(new Object[] {
					lh.getMaLoHang(),
					lh.getMaSanPham(),
					lh.getSoLo(),
					lh.getHanSuDung(),
					lh.getGiaNhap(),
					lh.getSoLuongNhap(),
					lh.getSoLuongTon(),
					lh.getTrangThai()
			});
		}

		// Load combo sản phẩm
		comboSanPham.removeAllItems();
		var sanPhamList = sanPhamDao.getAll();
		for (var sp : sanPhamList) {
			comboSanPham.addItem(sp.getMaSanPham() + " - " + sp.getTenSanPham());
		}
	}

	private void handleTableSelection() {
		int row = table.getSelectedRow();
		if (row >= 0) {
			int maLo = (Integer) tableModel.getValueAt(row, 0);
			var lh = loHangDao.findById(maLo);
			if (lh != null) {
				fillForm(lh);
			}
		}
	}

	private void fillForm(LoHang lh) {
		txtMaLo.setText(String.valueOf(lh.getMaLoHang()));

		// Tìm sản phẩm
		var sp = sanPhamDao.findById(lh.getMaSanPham());
		if (sp != null) {
			String item = sp.getMaSanPham() + " - " + sp.getTenSanPham();
			for (int i = 0; i < comboSanPham.getItemCount(); i++) {
				if (comboSanPham.getItemAt(i).equals(item)) {
					comboSanPham.setSelectedIndex(i);
					break;
				}
			}
		}

		txtSoLo.setText(lh.getSoLo());
		if (lh.getHanSuDung() != null) {
			txtHanSuDung.setText(lh.getHanSuDung().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
		}
		txtGiaNhap.setText(lh.getGiaNhap().toString());
		txtSoLuongTon.setText(String.valueOf(lh.getSoLuongTon()));

		// Set trạng thái
		String trangThai = lh.getTrangThai();
		for (int i = 0; i < comboTrangThai.getItemCount(); i++) {
			if (comboTrangThai.getItemAt(i).equals(trangThai)) {
				comboTrangThai.setSelectedIndex(i);
				break;
			}
		}
	}

	private void handleCapNhatTrangThai() {
		if (txtMaLo.getText().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng chọn lô hàng cần cập nhật!", "Thông báo",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		int maLo = Integer.parseInt(txtMaLo.getText());
		String trangThai = (String) comboTrangThai.getSelectedItem();

		if (loHangDao.updateTrangThai(maLo, trangThai)) {
			JOptionPane.showMessageDialog(this, "Cập nhật trạng thái thành công!", "Thông báo",
					JOptionPane.INFORMATION_MESSAGE);
			handleLamMoi();
			loadData();
		} else {
			JOptionPane.showMessageDialog(this, "Cập nhật trạng thái thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void handleLamMoi() {
		txtMaLo.setText("");
		comboSanPham.setSelectedIndex(-1);
		txtSoLo.setText("");
		txtHanSuDung.setText("");
		txtGiaNhap.setText("");
		txtSoLuongTon.setText("");
		comboTrangThai.setSelectedIndex(0);
		table.clearSelection();
	}
}
