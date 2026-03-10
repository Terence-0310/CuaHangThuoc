package panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import common.ColorScheme;
import common.UIHelper;
import components.NhaCungCapTableModel;
import dao.NhaCungCapDao;
import entity.NguoiDung;
import entity.NhaCungCap;

/**
 * NhaCungCapPanel - Panel quản lý nhà cung cấp
 * 
 * @author Generated
 * @version 1.0
 */
public class NhaCungCapPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private NhaCungCapDao dao;
	private NhaCungCapTableModel tableModel;
	private JTable table;
	private JTextField txtMaNCC;
	private JTextField txtTenNCC;
	private JTextField txtSoDienThoai;
	private JTextField txtEmail;
	private JTextField txtDiaChi;
	private JButton btnThem;
	private JButton btnSua;
	private JButton btnXoa;
	private JButton btnLamMoi;

	/**
	 * Create the panel.
	 */
	public NhaCungCapPanel(NguoiDung currentUser) {
		dao = new NhaCungCapDao();
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

		var titlePanel = new JPanel(new BorderLayout());
		titlePanel.setOpaque(false);
		var lblTitle = new JLabel("Quản lý nhà cung cấp");
		lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
		lblTitle.setForeground(ColorScheme.TEXT_PRIMARY);
		titlePanel.add(lblTitle, BorderLayout.WEST);
		add(titlePanel, BorderLayout.NORTH);

		var mainPanel = new JPanel(new BorderLayout(15, 0));
		mainPanel.setOpaque(false);

		var formPanel = createFormPanel();
		mainPanel.add(formPanel, BorderLayout.WEST);

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
			"Thông tin nhà cung cấp",
			TitledBorder.LEADING,
			TitledBorder.TOP,
			new Font("Segoe UI", Font.BOLD, 14),
			ColorScheme.TEXT_PRIMARY
		));
		panel.setLayout(null);
		panel.setPreferredSize(new java.awt.Dimension(350, 0));

		int y = 30;
		int labelWidth = 120;
		int fieldWidth = 200;
		int fieldHeight = 30;
		int spacing = 40;

		var lblMaNCC = new JLabel("Mã NCC:");
		lblMaNCC.setBounds(20, y, labelWidth, 25);
		panel.add(lblMaNCC);

		txtMaNCC = new JTextField();
		txtMaNCC.setEditable(false);
		txtMaNCC.setBackground(ColorScheme.INPUT_DISABLED);
		txtMaNCC.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtMaNCC.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtMaNCC);

		y += spacing;

		var lblTenNCC = new JLabel("Tên NCC:*");
		lblTenNCC.setBounds(20, y, labelWidth, 25);
		panel.add(lblTenNCC);

		txtTenNCC = new JTextField();
		txtTenNCC.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtTenNCC);

		y += spacing;

		var lblSoDienThoai = new JLabel("Số điện thoại:");
		lblSoDienThoai.setBounds(20, y, labelWidth, 25);
		panel.add(lblSoDienThoai);

		txtSoDienThoai = new JTextField();
		txtSoDienThoai.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtSoDienThoai);

		y += spacing;

		var lblEmail = new JLabel("Email:");
		lblEmail.setBounds(20, y, labelWidth, 25);
		panel.add(lblEmail);

		txtEmail = new JTextField();
		txtEmail.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtEmail);

		y += spacing;

		var lblDiaChi = new JLabel("Địa chỉ:");
		lblDiaChi.setBounds(20, y, labelWidth, 25);
		panel.add(lblDiaChi);

		txtDiaChi = new JTextField();
		txtDiaChi.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtDiaChi);

		y += spacing + 10;

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
			"Danh sách nhà cung cấp",
			TitledBorder.LEADING,
			TitledBorder.TOP,
			new Font("Segoe UI", Font.BOLD, 14),
			ColorScheme.TEXT_PRIMARY
		));

		tableModel = new NhaCungCapTableModel();
		table = new JTable(tableModel);
		table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		table.setRowHeight(25);
		table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		table.setShowGrid(true);
		table.setGridColor(ColorScheme.BORDER);
		table.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				handleTableSelection();
			}
		});

		var scrollPane = new JScrollPane(table);
		scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER));
		panel.add(scrollPane, BorderLayout.CENTER);

		return panel;
	}


	private void loadData() {
		tableModel.setRowCount(0);
		var list = dao.getAll();
		for (var ncc : list) {
			tableModel.addRow(new Object[]{
				ncc.getMaNCC(),
				ncc.getTenNCC(),
				ncc.getSoDienThoai() != null ? ncc.getSoDienThoai() : "",
				ncc.getEmail() != null ? ncc.getEmail() : "",
				ncc.getDiaChi() != null ? ncc.getDiaChi() : ""
			});
		}
	}

	private void handleTableSelection() {
		int row = table.getSelectedRow();
		if (row >= 0) {
			int maNCC = (Integer) tableModel.getValueAt(row, 0);
			var ncc = dao.findById(maNCC);
			if (ncc != null) {
				fillForm(ncc);
			}
		}
	}

	private void fillForm(NhaCungCap ncc) {
		txtMaNCC.setText(String.valueOf(ncc.getMaNCC()));
		txtTenNCC.setText(ncc.getTenNCC());
		txtSoDienThoai.setText(ncc.getSoDienThoai() != null ? ncc.getSoDienThoai() : "");
		txtEmail.setText(ncc.getEmail() != null ? ncc.getEmail() : "");
		txtDiaChi.setText(ncc.getDiaChi() != null ? ncc.getDiaChi() : "");
	}

	private void handleThem() {
		if (!validateForm()) {
			return;
		}

		var ncc = new NhaCungCap();
		ncc.setTenNCC(txtTenNCC.getText().trim());
		ncc.setSoDienThoai(txtSoDienThoai.getText().trim().isEmpty() ? null : txtSoDienThoai.getText().trim());
		ncc.setEmail(txtEmail.getText().trim().isEmpty() ? null : txtEmail.getText().trim());
		ncc.setDiaChi(txtDiaChi.getText().trim().isEmpty() ? null : txtDiaChi.getText().trim());

		if (dao.insert(ncc)) {
			JOptionPane.showMessageDialog(this, "Thêm nhà cung cấp thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
			handleLamMoi();
			loadData();
		} else {
			JOptionPane.showMessageDialog(this, "Thêm nhà cung cấp thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void handleSua() {
		if (txtMaNCC.getText().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng chọn nhà cung cấp cần sửa!", "Thông báo", JOptionPane.WARNING_MESSAGE);
			return;
		}

		if (!validateForm()) {
			return;
		}

		var ncc = new NhaCungCap();
		ncc.setMaNCC(Integer.parseInt(txtMaNCC.getText()));
		ncc.setTenNCC(txtTenNCC.getText().trim());
		ncc.setSoDienThoai(txtSoDienThoai.getText().trim().isEmpty() ? null : txtSoDienThoai.getText().trim());
		ncc.setEmail(txtEmail.getText().trim().isEmpty() ? null : txtEmail.getText().trim());
		ncc.setDiaChi(txtDiaChi.getText().trim().isEmpty() ? null : txtDiaChi.getText().trim());

		if (dao.update(ncc)) {
			JOptionPane.showMessageDialog(this, "Cập nhật nhà cung cấp thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
			handleLamMoi();
			loadData();
		} else {
			JOptionPane.showMessageDialog(this, "Cập nhật nhà cung cấp thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void handleXoa() {
		if (txtMaNCC.getText().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng chọn nhà cung cấp cần xóa!", "Thông báo", JOptionPane.WARNING_MESSAGE);
			return;
		}

		int option = JOptionPane.showConfirmDialog(this,
			"Bạn có chắc chắn muốn xóa nhà cung cấp này?",
			"Xác nhận xóa",
			JOptionPane.YES_NO_OPTION);

		if (option == JOptionPane.YES_OPTION) {
			int maNCC = Integer.parseInt(txtMaNCC.getText());
			if (dao.delete(maNCC)) {
				JOptionPane.showMessageDialog(this, "Xóa nhà cung cấp thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
				handleLamMoi();
				loadData();
			} else {
				JOptionPane.showMessageDialog(this, "Xóa nhà cung cấp thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void handleLamMoi() {
		txtMaNCC.setText("");
		txtTenNCC.setText("");
		txtSoDienThoai.setText("");
		txtEmail.setText("");
		txtDiaChi.setText("");
		table.clearSelection();
	}

	private boolean validateForm() {
		if (txtTenNCC.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng nhập tên nhà cung cấp!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			txtTenNCC.requestFocus();
			return false;
		}
		return true;
	}
}
