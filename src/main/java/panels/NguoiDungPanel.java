package panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import common.ColorScheme;
import common.UIHelper;
import components.NguoiDungTableModel;
import dao.NguoiDungDao;
import entity.NguoiDung;

/**
 * NguoiDungPanel - Panel quản lý người dùng (chỉ Admin)
 * 
 * @author Generated
 * @version 1.0
 */
public class NguoiDungPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private NguoiDungDao dao;
	private NguoiDungTableModel tableModel;
	private JTable table;
	private JTextField txtMaND;
	private JTextField txtTenDangNhap;
	private JPasswordField txtMatKhau;
	private JComboBox<String> comboVaiTro;
	private JTextField txtHoTen;
	private JTextField txtEmail;
	private JTextField txtSoDienThoai;
	private JButton btnThem;
	private JButton btnSua;
	private JButton btnXoa;
	private JButton btnLamMoi;
	private NguoiDung currentUser;

	/**
	 * Create the panel.
	 */
	public NguoiDungPanel(NguoiDung currentUser) {
	    this.currentUser = currentUser;
	    dao = new NguoiDungDao();
	    initialize();
	    loadData();
		if (!"Admin".equals(currentUser.getVaiTro())) {
			// Disable panel interactions instead of throwing an exception
			setPermissionsDisabled();
		}
	}

	private void setPermissionsDisabled() {
		btnThem.setEnabled(false);
		btnSua.setEnabled(false);
		btnXoa.setEnabled(false);
		btnLamMoi.setEnabled(false);
	}


	/**
	 * Kiểm tra quyền truy cập
	 */
	private boolean checkPermission() {
	    if (!"Admin".equals(currentUser.getVaiTro())) {
	        JOptionPane.showMessageDialog(this,
	            "Chỉ Admin mới có quyền truy cập chức năng này!",
	            "Không có quyền",
	            JOptionPane.WARNING_MESSAGE);
	        return false; // CHẶN
	    }
	    return true;
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
		var lblTitle = new JLabel("Quản lý người dùng");
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
			"Thông tin người dùng",
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

		var lblMaND = new JLabel("Mã ND:");
		lblMaND.setBounds(20, y, labelWidth, 25);
		panel.add(lblMaND);

		txtMaND = new JTextField();
		txtMaND.setEditable(false);
		txtMaND.setBackground(ColorScheme.INPUT_DISABLED);
		txtMaND.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtMaND.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtMaND);

		y += spacing;

		var lblTenDangNhap = new JLabel("Tên đăng nhập:*");
		lblTenDangNhap.setBounds(20, y, labelWidth, 25);
		panel.add(lblTenDangNhap);

		txtTenDangNhap = new JTextField();
		txtTenDangNhap.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtTenDangNhap);

		y += spacing;

		var lblMatKhau = new JLabel("Mật khẩu:*");
		lblMatKhau.setBounds(20, y, labelWidth, 25);
		panel.add(lblMatKhau);

		txtMatKhau = new JPasswordField();
		txtMatKhau.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtMatKhau);

		y += spacing;

		var lblVaiTro = new JLabel("Vai trò:*");
		lblVaiTro.setBounds(20, y, labelWidth, 25);
		panel.add(lblVaiTro);

		comboVaiTro = new JComboBox<>(new String[]{"Admin", "QuanLy", "NhanVien"});
		comboVaiTro.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(comboVaiTro);

		y += spacing;

		var lblHoTen = new JLabel("Họ tên:*");
		lblHoTen.setBounds(20, y, labelWidth, 25);
		panel.add(lblHoTen);

		txtHoTen = new JTextField();
		txtHoTen.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtHoTen);

		y += spacing;

		var lblEmail = new JLabel("Email:*");
		lblEmail.setBounds(20, y, labelWidth, 25);
		panel.add(lblEmail);

		txtEmail = new JTextField();
		txtEmail.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtEmail);

		y += spacing;

		var lblSoDienThoai = new JLabel("Số điện thoại:");
		lblSoDienThoai.setBounds(20, y, labelWidth, 25);
		panel.add(lblSoDienThoai);

		txtSoDienThoai = new JTextField();
		txtSoDienThoai.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtSoDienThoai);

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
			"Danh sách người dùng",
			TitledBorder.LEADING,
			TitledBorder.TOP,
			new Font("Segoe UI", Font.BOLD, 14),
			ColorScheme.TEXT_PRIMARY
		));

		tableModel = new NguoiDungTableModel();
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
		for (var nd : list) {
			tableModel.addRow(new Object[]{
				nd.getMaNguoiDung(),
				nd.getTenDangNhap(),
				nd.getVaiTro(),
				nd.getHoTen(),
				nd.getEmail(),
				nd.getSoDienThoai() != null ? nd.getSoDienThoai() : ""
			});
		}
	}

	private void handleTableSelection() {
		int row = table.getSelectedRow();
		if (row >= 0) {
			int maND = (Integer) tableModel.getValueAt(row, 0);
			var list = dao.getAll();
			var nd = list.stream()
				.filter(n -> n.getMaNguoiDung() == maND)
				.findFirst()
				.orElse(null);
			if (nd != null) {
				fillForm(nd);
			}
		}
	}

	private void fillForm(NguoiDung nd) {
		txtMaND.setText(String.valueOf(nd.getMaNguoiDung()));
		txtTenDangNhap.setText(nd.getTenDangNhap());
		txtMatKhau.setText(""); // Không hiển thị mật khẩu
		comboVaiTro.setSelectedItem(nd.getVaiTro());
		txtHoTen.setText(nd.getHoTen());
		txtEmail.setText(nd.getEmail());
		txtSoDienThoai.setText(nd.getSoDienThoai() != null ? nd.getSoDienThoai() : "");
	}

	private void handleThem() {
		if (!validateForm()) {
			return;
		}

		var nd = new NguoiDung();
		nd.setTenDangNhap(txtTenDangNhap.getText().trim());
		nd.setMatKhau(new String(txtMatKhau.getPassword()));
		nd.setVaiTro((String) comboVaiTro.getSelectedItem());
		nd.setHoTen(txtHoTen.getText().trim());
		nd.setEmail(txtEmail.getText().trim());
		nd.setSoDienThoai(txtSoDienThoai.getText().trim().isEmpty() ? null : txtSoDienThoai.getText().trim());

		if (dao.insert(nd)) {
			JOptionPane.showMessageDialog(this, "Thêm người dùng thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
			handleLamMoi();
			loadData();
		} else {
			JOptionPane.showMessageDialog(this, "Thêm người dùng thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void handleSua() {
		if (txtMaND.getText().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng chọn người dùng cần sửa!", "Thông báo", JOptionPane.WARNING_MESSAGE);
			return;
		}

		if (!validateForm()) {
			return;
		}

		var nd = new NguoiDung();
		nd.setMaNguoiDung(Integer.parseInt(txtMaND.getText()));
		nd.setTenDangNhap(txtTenDangNhap.getText().trim());
		String matKhau = new String(txtMatKhau.getPassword());
		if (!matKhau.isEmpty()) {
			nd.setMatKhau(matKhau);
		} else {
			// Nếu không nhập mật khẩu mới, giữ nguyên mật khẩu cũ
			// Cần lấy từ database (tạm thời bỏ qua, yêu cầu nhập mật khẩu mới)
			JOptionPane.showMessageDialog(this, "Vui lòng nhập mật khẩu mới!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			return;
		}
		nd.setVaiTro((String) comboVaiTro.getSelectedItem());
		nd.setHoTen(txtHoTen.getText().trim());
		nd.setEmail(txtEmail.getText().trim());
		nd.setSoDienThoai(txtSoDienThoai.getText().trim().isEmpty() ? null : txtSoDienThoai.getText().trim());

		if (dao.update(nd)) {
			JOptionPane.showMessageDialog(this, "Cập nhật người dùng thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
			handleLamMoi();
			loadData();
		} else {
			JOptionPane.showMessageDialog(this, "Cập nhật người dùng thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void handleXoa() {
		if (txtMaND.getText().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng chọn người dùng cần xóa!", "Thông báo", JOptionPane.WARNING_MESSAGE);
			return;
		}

		// Kiểm tra không cho xóa Admin
		String vaiTro = (String) comboVaiTro.getSelectedItem();
		if ("Admin".equals(vaiTro)) {
			JOptionPane.showMessageDialog(this,
				"Không thể xóa người dùng Admin!",
				"Lỗi",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		int option = JOptionPane.showConfirmDialog(this,
			"Bạn có chắc chắn muốn xóa người dùng này?",
			"Xác nhận xóa",
			JOptionPane.YES_NO_OPTION);

		if (option == JOptionPane.YES_OPTION) {
			int maND = Integer.parseInt(txtMaND.getText());
			if (dao.delete(maND)) {
				JOptionPane.showMessageDialog(this, "Xóa người dùng thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
				handleLamMoi();
				loadData();
			} else {
				JOptionPane.showMessageDialog(this, "Xóa người dùng thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void handleLamMoi() {
		txtMaND.setText("");
		txtTenDangNhap.setText("");
		txtMatKhau.setText("");
		comboVaiTro.setSelectedIndex(0);
		txtHoTen.setText("");
		txtEmail.setText("");
		txtSoDienThoai.setText("");
		table.clearSelection();
	}

	private boolean validateForm() {
		if (txtTenDangNhap.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng nhập tên đăng nhập!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			txtTenDangNhap.requestFocus();
			return false;
		}

		if (txtMatKhau.getPassword().length == 0) {
			JOptionPane.showMessageDialog(this, "Vui lòng nhập mật khẩu!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			txtMatKhau.requestFocus();
			return false;
		}

		if (txtHoTen.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng nhập họ tên!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			txtHoTen.requestFocus();
			return false;
		}

		if (txtEmail.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng nhập email!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			txtEmail.requestFocus();
			return false;
		}

		return true;
	}
}
