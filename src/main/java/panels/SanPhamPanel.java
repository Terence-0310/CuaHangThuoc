package panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.math.BigDecimal;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import common.ColorScheme;
import common.UIHelper;
import components.SanPhamTableModel;
import dao.SanPhamDao;
import entity.NguoiDung;
import entity.SanPham;

/**
 * SanPhamPanel - Panel quản lý sản phẩm
 * 
 * @author Generated
 * @version 1.0
 */
public class SanPhamPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private SanPhamDao dao;
	private SanPhamTableModel tableModel;
	private JTable table;
	private JTextField txtMaSP;
	private JTextField txtTenSP;
	private JTextField txtDonViTinh;
	private JTextField txtGiaBan;
	private JComboBox<String> comboLoaiSP;
	private JTextField txtMoTa;
	private JTextField txtMucTonToiThieu;
	private JButton btnThem;
	private JButton btnSua;
	private JButton btnXoa;
	private JButton btnLamMoi;
	private JButton btnTimKiem;
	private JTextField txtTimKiem;

	/**
	 * Create the panel.
	 */
	public SanPhamPanel(NguoiDung currentUser) {
		dao = new SanPhamDao();
		initialize();
		loadData();
	}

	/**
	 * Initialize the contents of the panel.
	 */
	private void initialize() {
		setLayout(new BorderLayout());
		setBackground(ColorScheme.BACKGROUND);
		setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

		// Title
		var titlePanel = new JPanel(new BorderLayout());
		titlePanel.setOpaque(false);
		titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
		var lblTitle = new JLabel("Quản lý sản phẩm");
		lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
		lblTitle.setForeground(ColorScheme.TEXT_PRIMARY);
		titlePanel.add(lblTitle, BorderLayout.WEST);
		add(titlePanel, BorderLayout.NORTH);

		// Main content
		var mainPanel = new JPanel(new BorderLayout(20, 0));
		mainPanel.setOpaque(false);

		// Left panel - Form
		var formPanel = createFormPanel();
		mainPanel.add(formPanel, BorderLayout.WEST);

		// Right panel - Table
		var tablePanel = createTablePanel();
		mainPanel.add(tablePanel, BorderLayout.CENTER);

		add(mainPanel, BorderLayout.CENTER);
	}

	/**
	 * Tạo form panel
	 */
	private JPanel createFormPanel() {
		var panel = new JPanel();
		panel.setBackground(ColorScheme.PANEL_BG);
		panel.setBorder(new TitledBorder(
			BorderFactory.createLineBorder(ColorScheme.BORDER, 1),
			"Thông tin sản phẩm",
			TitledBorder.LEADING,
			TitledBorder.TOP,
			new Font("Segoe UI", Font.BOLD, 16),
			ColorScheme.TEXT_PRIMARY
		));
		panel.setLayout(null);
		panel.setPreferredSize(new java.awt.Dimension(380, 0));

		// Removed manual title label to avoid overlap

		int y = 40; // Start y position inside TitledBorder
		int labelWidth = 130;
		int fieldWidth = 210;
		int fieldHeight = 35;
		int spacing = 45;

		// Mã SP (read-only)
		var lblMaSP = new JLabel("Mã SP:");
		lblMaSP.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		lblMaSP.setForeground(ColorScheme.TEXT_PRIMARY);
		lblMaSP.setBounds(20, y, labelWidth, 25);
		panel.add(lblMaSP);

		txtMaSP = new JTextField();
		txtMaSP.setEditable(false);
		txtMaSP.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		txtMaSP.setBackground(ColorScheme.INPUT_DISABLED);
		txtMaSP.setBorder(javax.swing.BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.BORDER, 1),
			new EmptyBorder(8, 12, 8, 12)
		));
		txtMaSP.setBounds(20, y + 25, fieldWidth, fieldHeight);
		panel.add(txtMaSP);

		y += spacing + 25; // Adjusted spacing for layout

		// Tên sản phẩm
		var lblTenSP = new JLabel("Tên sản phẩm:*");
		lblTenSP.setBounds(20, y, labelWidth, 25);
		panel.add(lblTenSP);

		txtTenSP = new JTextField();
		txtTenSP.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtTenSP.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtTenSP);

		y += spacing;

		// Đơn vị tính
		var lblDonViTinh = new JLabel("Đơn vị tính:");
		lblDonViTinh.setBounds(20, y, labelWidth, 25);
		panel.add(lblDonViTinh);

		txtDonViTinh = new JTextField("Hộp");
		txtDonViTinh.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtDonViTinh.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtDonViTinh);

		y += spacing;

		// Giá bán
		var lblGiaBan = new JLabel("Giá bán đề xuất:*");
		lblGiaBan.setBounds(20, y, labelWidth, 25);
		panel.add(lblGiaBan);

		txtGiaBan = new JTextField();
		txtGiaBan.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtGiaBan.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtGiaBan);

		y += spacing;

		// Loại sản phẩm
		var lblLoaiSP = new JLabel("Loại sản phẩm:*");
		lblLoaiSP.setBounds(20, y, labelWidth, 25);
		panel.add(lblLoaiSP);

		comboLoaiSP = new JComboBox<>(new String[]{
			"Thuoc", "DuocMiPham", "ThucPhamChucNang", "ChamSocCaNhan", "ThietBiYTe"
		});
		comboLoaiSP.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(comboLoaiSP);

		y += spacing;

		// Mô tả
		var lblMoTa = new JLabel("Mô tả:");
		lblMoTa.setBounds(20, y, labelWidth, 25);
		panel.add(lblMoTa);

		txtMoTa = new JTextField();
		txtMoTa.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtMoTa.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtMoTa);

		y += spacing;

		// Mức tồn tối thiểu
		var lblMucTon = new JLabel("Mức tồn tối thiểu:");
		lblMucTon.setBounds(20, y, labelWidth, 25);
		panel.add(lblMucTon);

		txtMucTonToiThieu = new JTextField("10");
		txtMucTonToiThieu.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtMucTonToiThieu.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtMucTonToiThieu);

		y += spacing + 20;

		// Buttons
		btnThem = UIHelper.createSuccessButton("Thêm mới");
		btnThem.setBounds(20, y, 100, 38);
		btnThem.addActionListener(e -> handleThem());
		panel.add(btnThem);

		btnSua = UIHelper.createPrimaryButton("Cập nhật");
		btnSua.setBounds(130, y, 100, 38);
		btnSua.addActionListener(e -> handleSua());
		panel.add(btnSua);

		btnXoa = UIHelper.createDangerButton("Xóa");
		btnXoa.setBounds(240, y, 100, 38);
		btnXoa.addActionListener(e -> handleXoa());
		panel.add(btnXoa);

		y += 48;

		btnLamMoi = UIHelper.createNeutralButton("Làm mới");
		btnLamMoi.setBounds(20, y, 320, 38);
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
			"Danh sách sản phẩm",
			TitledBorder.LEADING,
			TitledBorder.TOP,
			new Font("Segoe UI", Font.BOLD, 14),
			ColorScheme.TEXT_PRIMARY
		));

		// Search panel
		var searchPanel = new JPanel(new BorderLayout(10, 0));
		searchPanel.setOpaque(false);
		searchPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		var lblSearch = new JLabel("Tìm kiếm:");
		lblSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		searchPanel.add(lblSearch, BorderLayout.WEST);

		txtTimKiem = new JTextField();
		txtTimKiem.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		searchPanel.add(txtTimKiem, BorderLayout.CENTER);

		btnTimKiem = UIHelper.createPrimaryButton("Tìm");
		btnTimKiem.setPreferredSize(new java.awt.Dimension(90, 35));
		btnTimKiem.addActionListener(e -> handleTimKiem());
		searchPanel.add(btnTimKiem, BorderLayout.EAST);

		panel.add(searchPanel, BorderLayout.NORTH);

		// Table
		tableModel = new SanPhamTableModel();
		table = new JTable(tableModel);
		table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		table.setRowHeight(25);
		table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				handleTableSelection();
			}
		});

		var scrollPane = new JScrollPane(table);
		scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER)); // Kẻ khung
		panel.add(scrollPane, BorderLayout.CENTER);

		return panel;
	}


	/**
	 * Load dữ liệu vào table
	 */
	private void loadData() {
		tableModel.setRowCount(0);
		var list = dao.getAll();
		for (var sp : list) {
			tableModel.addRow(new Object[]{
				sp.getMaSanPham(),
				sp.getTenSanPham(),
				sp.getDonViTinh(),
				sp.getGiaBanDeXuat(),
				sp.getLoaiSanPham(),
				sp.getMoTa() != null ? sp.getMoTa() : "",
				sp.getMucTonToiThieu()
			});
		}
	}

	/**
	 * Xử lý khi chọn row trong table
	 */
	private void handleTableSelection() {
		int row = table.getSelectedRow();
		if (row >= 0) {
			int maSP = (Integer) tableModel.getValueAt(row, 0);
			var sp = dao.findById(maSP);
			if (sp != null) {
				fillForm(sp);
			}
		}
	}

	/**
	 * Điền form với dữ liệu sản phẩm
	 */
	private void fillForm(SanPham sp) {
		txtMaSP.setText(String.valueOf(sp.getMaSanPham()));
		txtTenSP.setText(sp.getTenSanPham());
		txtDonViTinh.setText(sp.getDonViTinh());
		txtGiaBan.setText(sp.getGiaBanDeXuat().toString());
		comboLoaiSP.setSelectedItem(sp.getLoaiSanPham());
		txtMoTa.setText(sp.getMoTa() != null ? sp.getMoTa() : "");
		txtMucTonToiThieu.setText(String.valueOf(sp.getMucTonToiThieu()));
	}

	/**
	 * Xử lý thêm mới
	 */
	private void handleThem() {
		if (!validateForm()) {
			return;
		}

		var sp = new SanPham();
		sp.setTenSanPham(txtTenSP.getText().trim());
		sp.setDonViTinh(txtDonViTinh.getText().trim());
		try {
			sp.setGiaBanDeXuat(new BigDecimal(txtGiaBan.getText().trim()));
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Giá bán không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			return;
		}
		sp.setLoaiSanPham((String) comboLoaiSP.getSelectedItem());
		sp.setMoTa(txtMoTa.getText().trim());
		try {
			sp.setMucTonToiThieu(Integer.parseInt(txtMucTonToiThieu.getText().trim()));
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Mức tồn tối thiểu không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (dao.insert(sp)) {
			JOptionPane.showMessageDialog(this, "Thêm sản phẩm thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
			handleLamMoi();
			loadData();
		} else {
			JOptionPane.showMessageDialog(this, "Thêm sản phẩm thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Xử lý cập nhật
	 */
	private void handleSua() {
		if (txtMaSP.getText().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng chọn sản phẩm cần sửa!", "Thông báo", JOptionPane.WARNING_MESSAGE);
			return;
		}

		if (!validateForm()) {
			return;
		}

		var sp = new SanPham();
		sp.setMaSanPham(Integer.parseInt(txtMaSP.getText()));
		sp.setTenSanPham(txtTenSP.getText().trim());
		sp.setDonViTinh(txtDonViTinh.getText().trim());
		try {
			sp.setGiaBanDeXuat(new BigDecimal(txtGiaBan.getText().trim()));
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Giá bán không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			return;
		}
		sp.setLoaiSanPham((String) comboLoaiSP.getSelectedItem());
		sp.setMoTa(txtMoTa.getText().trim());
		try {
			sp.setMucTonToiThieu(Integer.parseInt(txtMucTonToiThieu.getText().trim()));
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Mức tồn tối thiểu không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (dao.update(sp)) {
			JOptionPane.showMessageDialog(this, "Cập nhật sản phẩm thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
			handleLamMoi();
			loadData();
		} else {
			JOptionPane.showMessageDialog(this, "Cập nhật sản phẩm thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Xử lý xóa
	 */
	private void handleXoa() {
		if (txtMaSP.getText().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng chọn sản phẩm cần xóa!", "Thông báo", JOptionPane.WARNING_MESSAGE);
			return;
		}

		int option = JOptionPane.showConfirmDialog(this,
			"Bạn có chắc chắn muốn xóa sản phẩm này?",
			"Xác nhận xóa",
			JOptionPane.YES_NO_OPTION);

		if (option == JOptionPane.YES_OPTION) {
			int maSP = Integer.parseInt(txtMaSP.getText());
			if (dao.delete(maSP)) {
				JOptionPane.showMessageDialog(this, "Xóa sản phẩm thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
				handleLamMoi();
				loadData();
			} else {
				JOptionPane.showMessageDialog(this, "Xóa sản phẩm thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	/**
	 * Xử lý làm mới
	 */
	private void handleLamMoi() {
		txtMaSP.setText("");
		txtTenSP.setText("");
		txtDonViTinh.setText("Hộp");
		txtGiaBan.setText("");
		comboLoaiSP.setSelectedIndex(0);
		txtMoTa.setText("");
		txtMucTonToiThieu.setText("10");
		table.clearSelection();
	}

	/**
	 * Xử lý tìm kiếm
	 */
	private void handleTimKiem() {
		String keyword = txtTimKiem.getText().trim();
		if (keyword.isEmpty()) {
			loadData();
			return;
		}

		tableModel.setRowCount(0);
		var list = dao.searchByName(keyword);
		for (var sp : list) {
			tableModel.addRow(new Object[]{
				sp.getMaSanPham(),
				sp.getTenSanPham(),
				sp.getDonViTinh(),
				sp.getGiaBanDeXuat(),
				sp.getLoaiSanPham(),
				sp.getMoTa() != null ? sp.getMoTa() : "",
				sp.getMucTonToiThieu()
			});
		}
	}

	/**
	 * Validate form
	 */
	private boolean validateForm() {
		if (txtTenSP.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng nhập tên sản phẩm!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			txtTenSP.requestFocus();
			return false;
		}

		if (txtGiaBan.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng nhập giá bán!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			txtGiaBan.requestFocus();
			return false;
		}

		try {
			BigDecimal gia = new BigDecimal(txtGiaBan.getText().trim());
			if (gia.compareTo(BigDecimal.ZERO) < 0) {
				JOptionPane.showMessageDialog(this, "Giá bán phải >= 0!", "Lỗi", JOptionPane.ERROR_MESSAGE);
				txtGiaBan.requestFocus();
				return false;
			}
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Giá bán không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			txtGiaBan.requestFocus();
			return false;
		}

		return true;
	}
}
