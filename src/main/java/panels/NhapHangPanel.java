package panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

import common.ColorScheme;
import common.UIHelper;
import components.ChiTietPhieuNhapTableModel;
import dao.ChiTietPhieuNhapDao;
import dao.NhaCungCapDao;
import dao.PhieuNhapDao;
import dao.SanPhamDao;
import entity.NguoiDung;

/**
 * NhapHangPanel - Panel nhập hàng theo lô
 * 
 * @author Generated
 * @version 1.0
 */
public class NhapHangPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private PhieuNhapDao phieuNhapDao;
	private ChiTietPhieuNhapDao chiTietDao;
	private SanPhamDao sanPhamDao;
	private NhaCungCapDao nhaCungCapDao;
	private ChiTietPhieuNhapTableModel tableModel;
	private JTable table;
	private JTextField txtMaPhieuNhap;
	private JComboBox<String> comboNhaCungCap;
	private JComboBox<String> comboSanPham;
	private JTextField txtSoLo;
	private JTextField txtHanSuDung;
	private JTextField txtGiaNhap;
	private JTextField txtSoLuong;
	private JTextArea txtGhiChu;
	private JLabel lblTongTien;
	private JButton btnTaoPhieuNhap;
	private JButton btnThemSanPham;
	private JButton btnHuy;
	private Integer currentMaPhieuNhap;
	private NguoiDung currentUser;

	/**
	 * Create the panel.
	 */
	public NhapHangPanel(NguoiDung currentUser) {
		this.currentUser = currentUser;
		phieuNhapDao = new PhieuNhapDao();
		chiTietDao = new ChiTietPhieuNhapDao();
		sanPhamDao = new SanPhamDao();
		nhaCungCapDao = new NhaCungCapDao();
		initialize();
		resetForm();
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
		var lblTitle = new JLabel("Nhập hàng");
		lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
		lblTitle.setForeground(ColorScheme.TEXT_PRIMARY);
		titlePanel.add(lblTitle, BorderLayout.WEST);
		add(titlePanel, BorderLayout.NORTH);

		var mainPanel = new JPanel(new BorderLayout(15, 0));
		mainPanel.setOpaque(false);

		var leftPanel = createLeftPanel();
		mainPanel.add(leftPanel, BorderLayout.WEST);

		var rightPanel = createRightPanel();
		mainPanel.add(rightPanel, BorderLayout.CENTER);

		add(mainPanel, BorderLayout.CENTER);
	}

	/**
	 * Tạo panel bên trái - Form thông tin phiếu nhập
	 */
	private JPanel createLeftPanel() {
		var panel = new JPanel();
		panel.setBackground(ColorScheme.PANEL_BG);
		panel.setBorder(new TitledBorder(
			BorderFactory.createLineBorder(ColorScheme.BORDER, 1),
			"Thông tin phiếu nhập",
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

		var lblMaPhieuNhap = new JLabel("Mã phiếu nhập:");
		lblMaPhieuNhap.setBounds(20, y, labelWidth, 25);
		panel.add(lblMaPhieuNhap);

		txtMaPhieuNhap = new JTextField();
		txtMaPhieuNhap.setEditable(false);
		txtMaPhieuNhap.setBackground(ColorScheme.INPUT_DISABLED);
		txtMaPhieuNhap.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtMaPhieuNhap.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtMaPhieuNhap);

		y += spacing;

		var lblNhaCungCap = new JLabel("Nhà cung cấp:");
		lblNhaCungCap.setBounds(20, y, labelWidth, 25);
		panel.add(lblNhaCungCap);

		comboNhaCungCap = new JComboBox<>();
		comboNhaCungCap.addItem("-- Chọn nhà cung cấp --");
		var nccList = nhaCungCapDao.getAll();
		for (var ncc : nccList) {
			comboNhaCungCap.addItem(ncc.getMaNCC() + " - " + ncc.getTenNCC());
		}
		comboNhaCungCap.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(comboNhaCungCap);

		y += spacing;

		var lblSanPham = new JLabel("Sản phẩm:*");
		lblSanPham.setBounds(20, y, labelWidth, 25);
		panel.add(lblSanPham);

		comboSanPham = new JComboBox<>();
		comboSanPham.setBounds(140, y, fieldWidth, fieldHeight);
		loadSanPham();
		panel.add(comboSanPham);

		y += spacing;

		var lblSoLo = new JLabel("Số lô:*");
		lblSoLo.setBounds(20, y, labelWidth, 25);
		panel.add(lblSoLo);

		txtSoLo = new JTextField();
		txtSoLo.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtSoLo.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtSoLo);

		y += spacing;

		var lblHanSuDung = new JLabel("Hạn sử dụng:*");
		lblHanSuDung.setBounds(20, y, labelWidth, 25);
		panel.add(lblHanSuDung);

		txtHanSuDung = new JTextField();
		txtHanSuDung.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtHanSuDung.setBounds(140, y, fieldWidth, fieldHeight);
		txtHanSuDung.setToolTipText("Định dạng: dd/MM/yyyy (ví dụ: 31/12/2024)");
		panel.add(txtHanSuDung);

		y += spacing;

		var lblGiaNhap = new JLabel("Giá nhập:*");
		lblGiaNhap.setBounds(20, y, labelWidth, 25);
		panel.add(lblGiaNhap);

		txtGiaNhap = new JTextField();
		txtGiaNhap.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtGiaNhap.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtGiaNhap);

		y += spacing;

		var lblSoLuong = new JLabel("Số lượng:*");
		lblSoLuong.setBounds(20, y, labelWidth, 25);
		panel.add(lblSoLuong);

		txtSoLuong = new JTextField();
		txtSoLuong.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER, 1));
		txtSoLuong.setBounds(140, y, fieldWidth, fieldHeight);
		panel.add(txtSoLuong);

		y += spacing;

		var lblGhiChu = new JLabel("Ghi chú:");
		lblGhiChu.setBounds(20, y, labelWidth, 25);
		panel.add(lblGhiChu);

		txtGhiChu = new JTextArea();
		txtGhiChu.setLineWrap(true);
		txtGhiChu.setWrapStyleWord(true);
		var scrollGhiChu = new JScrollPane(txtGhiChu);
		scrollGhiChu.setBounds(140, y, fieldWidth, 60);
		panel.add(scrollGhiChu);

		y += 70;

		btnThemSanPham = UIHelper.createSuccessButton("Thêm sản phẩm");
		btnThemSanPham.setBounds(20, y, 320, 40);
		btnThemSanPham.addActionListener(e -> handleThemSanPham());
		panel.add(btnThemSanPham);

		y += 50;

		btnTaoPhieuNhap = UIHelper.createPrimaryButton("Tạo phiếu nhập mới");
		btnTaoPhieuNhap.setBounds(20, y, 320, 40);
		btnTaoPhieuNhap.addActionListener(e -> handleTaoPhieuNhap());
		panel.add(btnTaoPhieuNhap);

		y += 50;

		btnHuy = UIHelper.createDangerButton("Hủy");
		btnHuy.setBounds(20, y, 320, 45);
		btnHuy.addActionListener(e -> handleHuy());
		panel.add(btnHuy);

		return panel;
	}

	/**
	 * Tạo panel bên phải - Chi tiết phiếu nhập
	 */
	private JPanel createRightPanel() {
		var panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.PANEL_BG);
		panel.setBorder(new TitledBorder(
			BorderFactory.createLineBorder(ColorScheme.BORDER, 1),
			"Chi tiết phiếu nhập",
			TitledBorder.LEADING,
			TitledBorder.TOP,
			new Font("Segoe UI", Font.BOLD, 14),
			ColorScheme.TEXT_PRIMARY
		));

		tableModel = new ChiTietPhieuNhapTableModel();
		table = new JTable(tableModel);
		table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		table.setRowHeight(25);
		table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		table.setShowGrid(true);
		table.setGridColor(ColorScheme.BORDER);

		var scrollPane = new JScrollPane(table);
		scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER));
		panel.add(scrollPane, BorderLayout.CENTER);

		// Panel tổng tiền
		var totalPanel = new JPanel(new BorderLayout());
		totalPanel.setOpaque(false);
		totalPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		var lblTotal = new JLabel("Tổng tiền:");
		lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 16));
		lblTotal.setForeground(ColorScheme.TEXT_PRIMARY);
		totalPanel.add(lblTotal, BorderLayout.WEST);

		lblTongTien = new JLabel("0 đ");
		lblTongTien.setFont(new Font("Segoe UI", Font.BOLD, 20));
		lblTongTien.setForeground(ColorScheme.DANGER);
		totalPanel.add(lblTongTien, BorderLayout.EAST);

		panel.add(totalPanel, BorderLayout.SOUTH);

		return panel;
	}


	/**
	 * Load danh sách sản phẩm
	 */
	private void loadSanPham() {
		comboSanPham.removeAllItems();
		var list = sanPhamDao.getAll();
		for (var sp : list) {
			comboSanPham.addItem(sp.getMaSanPham() + " - " + sp.getTenSanPham());
		}
	}

	/**
	 * Xử lý tạo phiếu nhập mới
	 */
	private void handleTaoPhieuNhap() {
		Integer maNCC = null;
		if (comboNhaCungCap.getSelectedIndex() > 0) {
			String selected = (String) comboNhaCungCap.getSelectedItem();
			maNCC = Integer.parseInt(selected.split(" - ")[0]);
		}

		String ghiChu = txtGhiChu.getText().trim();

		Integer maPN = phieuNhapDao.createPhieuNhap(currentUser.getMaNguoiDung(), maNCC, ghiChu);
		if (maPN != null) {
			currentMaPhieuNhap = maPN;
			txtMaPhieuNhap.setText(String.valueOf(maPN));
			JOptionPane.showMessageDialog(this,
				"Tạo phiếu nhập thành công! Mã phiếu nhập: " + maPN,
				"Thông báo",
				JOptionPane.INFORMATION_MESSAGE);
			tableModel.setRowCount(0);
			updateTongTien();
		} else {
			JOptionPane.showMessageDialog(this,
				"Tạo phiếu nhập thất bại!",
				"Lỗi",
				JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Xử lý thêm sản phẩm vào phiếu nhập
	 */
	private void handleThemSanPham() {
		if (currentMaPhieuNhap == null) {
			JOptionPane.showMessageDialog(this,
				"Vui lòng tạo phiếu nhập trước!",
				"Thông báo",
				JOptionPane.WARNING_MESSAGE);
			return;
		}

		if (comboSanPham.getSelectedIndex() < 0) {
			JOptionPane.showMessageDialog(this,
				"Vui lòng chọn sản phẩm!",
				"Lỗi",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		String selected = (String) comboSanPham.getSelectedItem();
		int maSP = Integer.parseInt(selected.split(" - ")[0]);

		if (txtSoLo.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(this,
				"Vui lòng nhập số lô!",
				"Lỗi",
				JOptionPane.ERROR_MESSAGE);
			return;
		}
		String soLo = txtSoLo.getText().trim();

		if (txtHanSuDung.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(this,
				"Vui lòng nhập hạn sử dụng!",
				"Lỗi",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		LocalDate hanSuDung;
		try {
			hanSuDung = LocalDate.parse(txtHanSuDung.getText().trim(),
				DateTimeFormatter.ofPattern("dd/MM/yyyy"));
		} catch (DateTimeParseException e) {
			JOptionPane.showMessageDialog(this,
				"Hạn sử dụng không đúng định dạng! Vui lòng nhập dd/MM/yyyy",
				"Lỗi",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Kiểm tra hạn sử dụng không được quá hạn
		if (hanSuDung.isBefore(LocalDate.now())) {
			JOptionPane.showMessageDialog(this,
				"Không thể nhập lô đã quá hạn sử dụng!",
				"Lỗi",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (txtGiaNhap.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(this,
				"Vui lòng nhập giá nhập!",
				"Lỗi",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		BigDecimal giaNhap;
		try {
			giaNhap = new BigDecimal(txtGiaNhap.getText().trim());
			if (giaNhap.compareTo(BigDecimal.ZERO) < 0) {
				throw new NumberFormatException();
			}
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this,
				"Giá nhập không hợp lệ!",
				"Lỗi",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (txtSoLuong.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(this,
				"Vui lòng nhập số lượng!",
				"Lỗi",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		int soLuong;
		try {
			soLuong = Integer.parseInt(txtSoLuong.getText().trim());
			if (soLuong <= 0) {
				throw new NumberFormatException();
			}
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this,
				"Số lượng phải là số nguyên dương!",
				"Lỗi",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Gọi stored procedure để nhập hàng theo lô
		if (phieuNhapDao.addItemBatch(currentMaPhieuNhap, maSP, soLo, hanSuDung, giaNhap, soLuong)) {
			JOptionPane.showMessageDialog(this,
				"Thêm sản phẩm thành công!",
				"Thông báo",
				JOptionPane.INFORMATION_MESSAGE);
			loadChiTietPhieuNhap();
			clearProductForm();
		} else {
			JOptionPane.showMessageDialog(this,
				"Thêm sản phẩm thất bại! Kiểm tra lại thông tin.",
				"Lỗi",
				JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Load chi tiết phiếu nhập vào table
	 */
	private void loadChiTietPhieuNhap() {
		if (currentMaPhieuNhap == null) {
			return;
		}

		tableModel.setRowCount(0);
		var list = chiTietDao.getDetailForDisplay(currentMaPhieuNhap);
		for (var row : list) {
			tableModel.addRow(row);
		}
		updateTongTien();
	}

	/**
	 * Cập nhật tổng tiền
	 */
	private void updateTongTien() {
		BigDecimal total = BigDecimal.ZERO;
		for (int i = 0; i < tableModel.getRowCount(); i++) {
			BigDecimal thanhTien = (BigDecimal) tableModel.getValueAt(i, 6);
			total = total.add(thanhTien);
		}
		lblTongTien.setText(total.toString() + " đ");
	}

	/**
	 * Xử lý hủy
	 */
	private void handleHuy() {
		if (currentMaPhieuNhap == null) {
			return;
		}

		int option = JOptionPane.showConfirmDialog(this,
			"Bạn có chắc chắn muốn hủy phiếu nhập này?",
			"Xác nhận",
			JOptionPane.YES_NO_OPTION);

		if (option == JOptionPane.YES_OPTION) {
			resetForm();
		}
	}

	/**
	 * Clear form sản phẩm
	 */
	private void clearProductForm() {
		comboSanPham.setSelectedIndex(-1);
		txtSoLo.setText("");
		txtHanSuDung.setText("");
		txtGiaNhap.setText("");
		txtSoLuong.setText("");
	}

	/**
	 * Reset form
	 */
	private void resetForm() {
		currentMaPhieuNhap = null;
		txtMaPhieuNhap.setText("");
		comboNhaCungCap.setSelectedIndex(0);
		clearProductForm();
		txtGhiChu.setText("");
		tableModel.setRowCount(0);
		updateTongTien();
	}
}
