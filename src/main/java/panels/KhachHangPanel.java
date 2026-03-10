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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import common.ColorScheme;
import common.UIHelper;
import components.KhachHangTableModel;
import dao.KhachHangDao;
import entity.KhachHang;
import entity.NguoiDung;

/**
 * KhachHangPanel - Panel quản lý khách hàng
 * 
 * @author Generated
 * @version 1.0
 */
public class KhachHangPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private KhachHangDao dao;
	private KhachHangTableModel tableModel;
	private JTable table;
	private JTextField txtMaKH;
	private JTextField txtHoTen;
	private JTextField txtSoDienThoai;
	private JTextField txtEmail;
	private JTextField txtDiaChi;
	private JTextArea txtHoSoBenhAn;
	private JButton btnThem;
	private JButton btnSua;
	private JButton btnXoa;
	private JButton btnLamMoi;

	/**
	 * Create the panel.
	 */
	public KhachHangPanel(NguoiDung currentUser) {
		dao = new KhachHangDao();
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
		var lblTitle = new JLabel("Quản lý khách hàng");
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
			"Thông tin khách hàng",
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

		var lblMaKH = new JLabel("Mã KH:");
		lblMaKH.setBounds(20, y, labelWidth, 25);
		panel.add(lblMaKH);

		txtMaKH = new JTextField();
		txtMaKH.setEditable(false);
		txtMaKH.setBackground(ColorScheme.INPUT_DISABLED);
		txtMaKH.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtMaKH.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtMaKH);

		y += spacing;

		var lblHoTen = new JLabel("Họ tên:*");
		lblHoTen.setBounds(20, y, labelWidth, 25);
		panel.add(lblHoTen);

		txtHoTen = new JTextField();
		txtHoTen.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtHoTen.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtHoTen);

		y += spacing;

		var lblSoDienThoai = new JLabel("Số điện thoại:");
		lblSoDienThoai.setBounds(20, y, labelWidth, 25);
		panel.add(lblSoDienThoai);

		txtSoDienThoai = new JTextField();
		txtSoDienThoai.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtSoDienThoai.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtSoDienThoai);

		y += spacing;

		var lblEmail = new JLabel("Email:");
		lblEmail.setBounds(20, y, labelWidth, 25);
		panel.add(lblEmail);

		txtEmail = new JTextField();
		txtEmail.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtEmail.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtEmail);

		y += spacing;

		var lblDiaChi = new JLabel("Địa chỉ:");
		lblDiaChi.setBounds(20, y, labelWidth, 25);
		panel.add(lblDiaChi);

		txtDiaChi = new JTextField();
		txtDiaChi.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtDiaChi.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtDiaChi);

		y += spacing;

		var lblHoSo = new JLabel("Hồ sơ bệnh án:");
		lblHoSo.setBounds(20, y, labelWidth, 25);
		panel.add(lblHoSo);

		txtHoSoBenhAn = new JTextArea();
		txtHoSoBenhAn.setLineWrap(true);
		txtHoSoBenhAn.setWrapStyleWord(true);
		var scrollHoSo = new JScrollPane(txtHoSoBenhAn);
		scrollHoSo.setBounds(140, y, fieldWidth, 60);
		panel.add(scrollHoSo);

		y += 70;

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
			"Danh sách khách hàng",
			TitledBorder.LEADING,
			TitledBorder.TOP,
			new Font("Segoe UI", Font.BOLD, 14),
			ColorScheme.TEXT_PRIMARY
		));

		tableModel = new KhachHangTableModel();
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
		for (var kh : list) {
			tableModel.addRow(new Object[]{
				kh.getMaKhachHang(),
				kh.getHoTen(),
				kh.getSoDienThoai() != null ? kh.getSoDienThoai() : "",
				kh.getEmail() != null ? kh.getEmail() : "",
				kh.getDiaChi() != null ? kh.getDiaChi() : ""
			});
		}
	}

	private void handleTableSelection() {
		int row = table.getSelectedRow();
		if (row >= 0) {
			int maKH = (Integer) tableModel.getValueAt(row, 0);
			var kh = dao.findById(maKH);
			if (kh != null) {
				fillForm(kh);
			}
		}
	}

	private void fillForm(KhachHang kh) {
		txtMaKH.setText(String.valueOf(kh.getMaKhachHang()));
		txtHoTen.setText(kh.getHoTen());
		txtSoDienThoai.setText(kh.getSoDienThoai() != null ? kh.getSoDienThoai() : "");
		txtEmail.setText(kh.getEmail() != null ? kh.getEmail() : "");
		txtDiaChi.setText(kh.getDiaChi() != null ? kh.getDiaChi() : "");
		txtHoSoBenhAn.setText(kh.getHoSoBenhAn() != null ? kh.getHoSoBenhAn() : "");
	}

	private void handleThem() {
		if (!validateForm()) {
			return;
		}

		var kh = new KhachHang();
		kh.setHoTen(txtHoTen.getText().trim());
		kh.setSoDienThoai(txtSoDienThoai.getText().trim().isEmpty() ? null : txtSoDienThoai.getText().trim());
		kh.setEmail(txtEmail.getText().trim().isEmpty() ? null : txtEmail.getText().trim());
		kh.setDiaChi(txtDiaChi.getText().trim().isEmpty() ? null : txtDiaChi.getText().trim());
		kh.setHoSoBenhAn(txtHoSoBenhAn.getText().trim().isEmpty() ? null : txtHoSoBenhAn.getText().trim());

		if (dao.insert(kh)) {
			JOptionPane.showMessageDialog(this, "Thêm khách hàng thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
			handleLamMoi();
			loadData();
		} else {
			JOptionPane.showMessageDialog(this, "Thêm khách hàng thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void handleSua() {
		if (txtMaKH.getText().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng chọn khách hàng cần sửa!", "Thông báo", JOptionPane.WARNING_MESSAGE);
			return;
		}

		if (!validateForm()) {
			return;
		}

		var kh = new KhachHang();
		kh.setMaKhachHang(Integer.parseInt(txtMaKH.getText()));
		kh.setHoTen(txtHoTen.getText().trim());
		kh.setSoDienThoai(txtSoDienThoai.getText().trim().isEmpty() ? null : txtSoDienThoai.getText().trim());
		kh.setEmail(txtEmail.getText().trim().isEmpty() ? null : txtEmail.getText().trim());
		kh.setDiaChi(txtDiaChi.getText().trim().isEmpty() ? null : txtDiaChi.getText().trim());
		kh.setHoSoBenhAn(txtHoSoBenhAn.getText().trim().isEmpty() ? null : txtHoSoBenhAn.getText().trim());

		if (dao.update(kh)) {
			JOptionPane.showMessageDialog(this, "Cập nhật khách hàng thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
			handleLamMoi();
			loadData();
		} else {
			JOptionPane.showMessageDialog(this, "Cập nhật khách hàng thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void handleXoa() {
		if (txtMaKH.getText().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng chọn khách hàng cần xóa!", "Thông báo", JOptionPane.WARNING_MESSAGE);
			return;
		}

		int option = JOptionPane.showConfirmDialog(this,
			"Bạn có chắc chắn muốn xóa khách hàng này?",
			"Xác nhận xóa",
			JOptionPane.YES_NO_OPTION);

		if (option == JOptionPane.YES_OPTION) {
			int maKH = Integer.parseInt(txtMaKH.getText());
			if (dao.delete(maKH)) {
				JOptionPane.showMessageDialog(this, "Xóa khách hàng thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
				handleLamMoi();
				loadData();
			} else {
				JOptionPane.showMessageDialog(this, "Xóa khách hàng thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void handleLamMoi() {
		txtMaKH.setText("");
		txtHoTen.setText("");
		txtSoDienThoai.setText("");
		txtEmail.setText("");
		txtDiaChi.setText("");
		txtHoSoBenhAn.setText("");
		table.clearSelection();
	}

	private boolean validateForm() {
		if (txtHoTen.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng nhập họ tên!", "Lỗi", JOptionPane.ERROR_MESSAGE);
			txtHoTen.requestFocus();
			return false;
		}
		return true;
	}
}
